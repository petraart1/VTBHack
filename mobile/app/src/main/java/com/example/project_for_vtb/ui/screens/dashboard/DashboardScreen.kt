package com.example.project_for_vtb.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.project_for_vtb.ui.components.BankCard
import com.example.project_for_vtb.ui.viewmodel.DashboardViewModel
import com.example.project_for_vtb.ui.viewmodel.DashboardUiState
import java.math.BigDecimal

/**
 * Главный экран дашборда с банковской картой
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToBanks: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBalanceVisible by viewModel.isBalanceVisible.collectAsState()
    val recentTransactions = remember { emptyList<com.example.project_for_vtb.domain.model.Transaction>() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Добро пожаловать")
                        Text(
                            text = "Ваши финансы",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is DashboardUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Ошибка загрузки данных",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Повторить")
                    }
                }
            }
            is DashboardUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Банковская карта
                    item {
                        // Используем primaryAccount из state, так как он всегда синхронизирован
                        val primaryAccount = state.primaryAccount
                        
                        // Детальное логирование для отладки
                        android.util.Log.d("DashboardScreen", "🔍 primaryAccount: $primaryAccount")
                        android.util.Log.d("DashboardScreen", "🔍 primaryAccount?.balance: ${primaryAccount?.balance}")
                        android.util.Log.d("DashboardScreen", "🔍 Всего счетов: ${state.accounts.size}")
                        state.accounts.forEachIndexed { index, account ->
                            android.util.Log.d("DashboardScreen", "🔍 Счет $index: номер=${account.accountNumber}, баланс=${account.balance}, валюта=${account.currency}, тип=${account.accountType}, активен=${account.isActive}")
                        }
                        
                        // Получаем баланс основного счета
                        val displayBalance = primaryAccount?.balance ?: run {
                            // Если primaryAccount null, пытаемся найти любой активный счет
                            val fallbackAccount = state.accounts.firstOrNull { it.isActive }
                            android.util.Log.d("DashboardScreen", "⚠️ primaryAccount null, используем fallback: ${fallbackAccount?.balance}")
                            fallbackAccount?.balance ?: BigDecimal.ZERO
                        }
                        
                        val cardNumber = primaryAccount?.accountNumber?.let {
                            "**** ${it.takeLast(4)}"
                        } ?: state.accounts.firstOrNull()?.accountNumber?.let {
                            "**** ${it.takeLast(4)}"
                        } ?: "**** 9749"
                        
                        val currency = primaryAccount?.currency 
                            ?: state.accounts.firstOrNull()?.currency 
                            ?: "₽"
                        
                        android.util.Log.d("DashboardScreen", "💰 Отображаемый баланс: $displayBalance $currency")
                        
                        BankCard(
                            balance = displayBalance,
                            cardNumber = cardNumber,
                            currency = currency,
                            isBalanceVisible = isBalanceVisible,
                            onBalanceVisibilityToggle = { 
                                android.util.Log.d("DashboardScreen", "👁️ Переключение видимости баланса")
                                viewModel.toggleBalanceVisibility() 
                            },
                            onClick = {
                                // Переход на детализацию счета
                                onNavigateToAccounts()
                            }
                        )
                    }
                    
                    // Быстрые действия
                    item {
                        Text(
                            text = "Быстрые действия",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionCard(
                                title = "Счета",
                                icon = Icons.Default.AccountBalance,
                                onClick = onNavigateToAccounts,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                title = "Транзакции",
                                icon = Icons.Default.Receipt,
                                onClick = onNavigateToTransactions,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionCard(
                                title = "Аналитика",
                                icon = Icons.Default.Analytics,
                                onClick = onNavigateToAnalytics,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                title = "Банки",
                                icon = Icons.Default.Business,
                                onClick = onNavigateToBanks,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Недавние транзакции
                    if (recentTransactions.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Недавние транзакции",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TextButton(onClick = onNavigateToTransactions) {
                                    Text("Все")
                                }
                            }
                        }
                        
                        items(recentTransactions.take(5)) { transaction ->
                            // TODO: Использовать TransactionCard компонент
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

