package com.example.project_for_vtb.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_for_vtb.domain.model.Account
import com.example.project_for_vtb.domain.repository.AccountRepository
import com.example.project_for_vtb.domain.repository.BankRepository
import com.example.project_for_vtb.domain.usecase.account.GetAllAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

/**
 * ViewModel для главного экрана (Dashboard)
 * Управляет состоянием банковской карты и балансом
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getAllAccountsUseCase: GetAllAccountsUseCase,
    private val accountRepository: AccountRepository,
    private val bankRepository: BankRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val _isBalanceVisible = MutableStateFlow(false)
    val isBalanceVisible: StateFlow<Boolean> = _isBalanceVisible.asStateFlow()
    
    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount: StateFlow<Account?> = _selectedAccount.asStateFlow()
    
    init {
        Timber.d("🚀 [DashboardViewModel] Инициализация")
        loadAccounts()
    }
    
    /**
     * Загрузка счетов и расчет общего баланса
     */
    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            
            // Сначала пытаемся синхронизировать данные с API
            Timber.d("🔄 [DashboardViewModel] Попытка синхронизации счетов с API")
            var syncSuccessful = false
            try {
                // Получаем список всех подключенных банков (из Flow)
                val banks = bankRepository.getConnectedBanks().first()
                Timber.d("🏦 [DashboardViewModel] Найдено банков: ${banks.size}")
                
                if (banks.isNotEmpty()) {
                    // Синхронизируем счета для каждого банка
                    banks.forEach { bank ->
                        Timber.d("🔄 [DashboardViewModel] Синхронизация счетов для банка: ${bank.id} (${bank.name})")
                        accountRepository.syncAccounts(bank.id).onSuccess {
                            Timber.i("✅ [DashboardViewModel] Успешно синхронизированы счета для банка: ${bank.id}")
                            syncSuccessful = true
                        }.onFailure { error ->
                            Timber.w(error, "⚠️ [DashboardViewModel] Не удалось синхронизировать счета для банка ${bank.id}: ${error.message}")
                        }
                    }
                } else {
                    // Если нет подключенных банков, пытаемся синхронизировать с "default"
                    Timber.w("⚠️ [DashboardViewModel] Нет подключенных банков, пытаемся синхронизировать с 'default'")
                    accountRepository.syncAccounts("default").onSuccess {
                        syncSuccessful = true
                        Timber.i("✅ [DashboardViewModel] Успешно синхронизированы счета для 'default'")
                    }.onFailure { error ->
                        Timber.w(error, "⚠️ [DashboardViewModel] Не удалось синхронизировать с API для 'default', используем данные из БД")
                    }
                }
                
                // Небольшая задержка после синхронизации, чтобы данные успели сохраниться в БД
                if (syncSuccessful) {
                    kotlinx.coroutines.delay(500)
                    Timber.d("⏳ [DashboardViewModel] Задержка после синхронизации для сохранения данных в БД")
                }
            } catch (e: Exception) {
                Timber.w(e, "⚠️ [DashboardViewModel] Ошибка синхронизации: ${e.message}, продолжаем с данными из БД")
            }
            
            // Загружаем счета из БД (после синхронизации)
            // Используем stateIn для получения первого значения или пустого списка
            getAllAccountsUseCase()
                .catch { exception ->
                    Timber.e(exception, "❌ [DashboardViewModel] Ошибка загрузки счетов")
                    // При ошибке используем тестовые данные для отображения
                    val testAccount = createTestAccount()
                    _selectedAccount.value = testAccount
                    _uiState.value = DashboardUiState.Success(
                        totalBalance = testAccount.balance,
                        accounts = listOf(testAccount),
                        primaryAccount = testAccount
                    )
                }
                .collect { accounts ->
                    Timber.d("📊 [DashboardViewModel] Загружено счетов: ${accounts.size}")
                    
                    // Детальное логирование всех счетов
                    accounts.forEachIndexed { index, account ->
                        Timber.d("📋 [DashboardViewModel] Счет $index: номер=${account.accountNumber}, баланс=${account.balance}, валюта=${account.currency}, тип=${account.accountType}, активен=${account.isActive}, имя=${account.name}")
                    }
                    
                    if (accounts.isEmpty()) {
                        Timber.w("⚠️ [DashboardViewModel] Список счетов пуст - используем тестовые данные")
                        
                        // Используем тестовые данные, если нет данных из API/БД
                        val testAccount = createTestAccount()
                        
                        Timber.i("💰 [DashboardViewModel] Устанавливаем тестовый счет с балансом: ${testAccount.balance}")
                        _selectedAccount.value = testAccount
                        _uiState.value = DashboardUiState.Success(
                            totalBalance = testAccount.balance,
                            accounts = listOf(testAccount),
                            primaryAccount = testAccount
                        )
                    } else {
                        // Рассчитываем общий баланс
                        val totalBalance = accounts
                            .filter { it.isActive }
                            .fold(BigDecimal.ZERO) { acc, account ->
                                acc + account.balance
                            }
                        
                        // Выбираем основной счет для карты:
                        // 1. Приоритет: счет с типом DEBIT_CARD (дебетовая карта)
                        // 2. Если нет дебетовой карты: первый активный счет
                        // 3. Если нет активных: первый счет из списка
                        val primaryAccount = accounts
                            .firstOrNull { it.isActive && it.accountType == com.example.project_for_vtb.domain.model.AccountType.DEBIT_CARD }
                            ?: accounts.firstOrNull { it.isActive }
                            ?: accounts.first()
                        
                        Timber.i("💳 [DashboardViewModel] ВЫБРАН ОСНОВНОЙ СЧЕТ:")
                        Timber.i("💳 [DashboardViewModel]   - Номер: ${primaryAccount.accountNumber}")
                        Timber.i("💳 [DashboardViewModel]   - Баланс: ${primaryAccount.balance}")
                        Timber.i("💳 [DashboardViewModel]   - Валюта: ${primaryAccount.currency}")
                        Timber.i("💳 [DashboardViewModel]   - Тип: ${primaryAccount.accountType}")
                        Timber.i("💳 [DashboardViewModel]   - Активен: ${primaryAccount.isActive}")
                        Timber.i("💰 [DashboardViewModel] Общий баланс: $totalBalance ${primaryAccount.currency}")
                        
                        _selectedAccount.value = primaryAccount
                        
                        // Обновляем состояние - primaryAccount уже установлен выше
                        _uiState.value = DashboardUiState.Success(
                            totalBalance = totalBalance,
                            accounts = accounts,
                            primaryAccount = primaryAccount
                        )
                        
                        Timber.i("✅ [DashboardViewModel] Состояние обновлено, primaryAccount установлен в state")
                    }
                }
        }
    }
    
    /**
     * Переключение видимости баланса
     */
    fun toggleBalanceVisibility() {
        val newValue = !_isBalanceVisible.value
        _isBalanceVisible.value = newValue
        Timber.d("👁️ [DashboardViewModel] Видимость баланса: $newValue")
    }
    
    /**
     * Обновление данных (pull-to-refresh)
     */
    fun refresh() {
        Timber.d("🔄 [DashboardViewModel] Обновление данных")
        loadAccounts()
    }
    
    /**
     * Создание тестового счета для отображения, если нет реальных данных
     */
    private fun createTestAccount(): Account {
        return Account(
            id = "test-1",
            bankId = "vtb",
            accountNumber = "40817810099910004312",
            accountType = com.example.project_for_vtb.domain.model.AccountType.DEBIT_CARD,
            balance = BigDecimal("125000.50"),
            currency = "₽",
            name = "Основная карта",
            isActive = true
        )
    }
}

/**
 * Состояния UI для Dashboard экрана
 */
sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val totalBalance: BigDecimal,
        val accounts: List<Account>,
        val primaryAccount: Account?
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

