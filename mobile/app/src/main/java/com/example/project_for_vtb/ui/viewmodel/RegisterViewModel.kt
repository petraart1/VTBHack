package com.example.project_for_vtb.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_for_vtb.data.remote.api.AuthApiService
import com.example.project_for_vtb.data.remote.api.RegisterRequest
import com.example.project_for_vtb.data.security.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel для экрана регистрации
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()
    
    /**
     * Регистрация нового пользователя
     * Использует реальный API endpoint /api/v1/auth/register
     * Согласно Swagger API, возвращает пустой объект {} при успехе
     */
    fun register(
        firstName: String,
        lastName: String,
        birthOfDate: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        // Валидация данных
        if (firstName.isBlank() || lastName.isBlank() || birthOfDate.isBlank() || 
            email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = RegisterUiState.Error("Заполните все обязательные поля")
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = RegisterUiState.Error("Введите корректный email адрес")
            return
        }
        
        if (password.length < 6) {
            _uiState.value = RegisterUiState.Error("Пароль должен содержать минимум 6 символов")
            return
        }
        
        if (password != confirmPassword) {
            _uiState.value = RegisterUiState.Error("Пароли не совпадают")
            return
        }
        
        // Валидация формата даты (YYYY-MM-DD)
        val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        if (!dateRegex.matches(birthOfDate)) {
            _uiState.value = RegisterUiState.Error("Дата рождения должна быть в формате ГГГГ-ММ-ДД (например: 1990-01-01)")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            
            Timber.d("📝 [RegisterViewModel] Попытка регистрации: email=$email, firstName=$firstName, lastName=$lastName")
            Timber.d("🌐 [RegisterViewModel] Используется РЕАЛЬНЫЙ API: http://158.160.102.225/api/v1/auth/register")
            
            try {
                val response = authApiService.register(
                    RegisterRequest(
                        firstName = firstName,
                        lastName = lastName,
                        birthOfDate = birthOfDate,
                        email = email,
                        password = password
                    )
                )
                
                Timber.d("📡 [RegisterViewModel] Ответ получен. Код статуса: ${response.code()}")
                Timber.d("📡 [RegisterViewModel] URL запроса: ${response.raw().request.url}")
                
                if (response.isSuccessful) {
                    // Согласно Swagger API, при успехе (200 OK) возвращается пустой объект {}
                    // Регистрация успешна, но токены не выдаются - нужно войти отдельно
                    try {
                        // Закрываем тело ответа (не нужно его читать, важен только статус код)
                        response.body()?.close()
                        Timber.i("✅ [RegisterViewModel] Успешная регистрация через РЕАЛЬНЫЙ API")
                        Timber.d("✅ [RegisterViewModel] Пользователь успешно зарегистрирован. Код ответа: ${response.code()}")
                        
                        // Регистрация успешна, переходим на экран входа
                        _uiState.value = RegisterUiState.Success
                    } catch (e: Exception) {
                        // Даже если не удалось прочитать тело, но статус 200 - считаем успехом
                        Timber.w(e, "⚠️ [RegisterViewModel] Не удалось прочитать тело ответа, но статус успешный")
                        Timber.i("✅ [RegisterViewModel] Регистрация успешна (статус ${response.code()})")
                        _uiState.value = RegisterUiState.Success
                    }
                } else {
                    // Обрабатываем ошибку от сервера
                    val errorBody = try {
                        response.errorBody()?.string()
                    } catch (e: Exception) {
                        Timber.e(e, "❌ [RegisterViewModel] Не удалось прочитать тело ошибки")
                        null
                    }
                    
                    Timber.e("❌ [RegisterViewModel] Ошибка регистрации. Код: ${response.code()}")
                    Timber.e("❌ [RegisterViewModel] Тело ошибки: $errorBody")
                    
                    // Извлекаем понятное сообщение об ошибке
                    val errorMessage = when {
                        errorBody != null && errorBody.isNotBlank() -> {
                            // Пытаемся извлечь понятное сообщение из ответа
                            when {
                                errorBody.contains("already exists", ignoreCase = true) || 
                                errorBody.contains("уже существует", ignoreCase = true) ->
                                    "Пользователь с таким email уже существует"
                                errorBody.contains("invalid", ignoreCase = true) || 
                                errorBody.contains("неверн", ignoreCase = true) ->
                                    "Неверные данные. Проверьте правильность введенной информации"
                                errorBody.length < 200 -> errorBody // Показываем короткие сообщения как есть
                                else -> "Ошибка регистрации"
                            }
                        }
                        else -> when (response.code()) {
                            400 -> "Неверные данные. Проверьте правильность введенной информации"
                            401 -> "Ошибка аутентификации"
                            409 -> "Пользователь с таким email уже существует"
                            404 -> "Endpoint регистрации не найден. Проверьте конфигурацию API"
                            500 -> "Ошибка сервера. Попробуйте позже"
                            else -> "Ошибка регистрации. Код: ${response.code()}"
                        }
                    }
                    
                    _uiState.value = RegisterUiState.Error(errorMessage)
                }
            } catch (e: com.google.gson.JsonIOException) {
                // Обработка ошибок ввода/вывода при работе с JSON
                Timber.e(e, "❌ [RegisterViewModel] Ошибка ввода/вывода JSON (JsonIOException)")
                
                val errorMessage = when {
                    e.message?.contains("JSON") == true -> {
                        "Сервер вернул некорректный формат данных. Возможно, endpoint регистрации возвращает не JSON или не реализован."
                    }
                    e.cause is java.io.IOException -> {
                        "Ошибка чтения ответа от сервера. Проверьте подключение к интернету."
                    }
                    else -> {
                        "Ошибка обработки данных от сервера: ${e.message ?: "Неизвестная ошибка"}"
                    }
                }
                
                _uiState.value = RegisterUiState.Error(errorMessage)
            } catch (e: com.google.gson.JsonSyntaxException) {
                // Специальная обработка ошибок парсинга JSON от Gson
                Timber.e(e, "❌ [RegisterViewModel] Ошибка парсинга JSON (JsonSyntaxException)")
                
                val errorMessage = when {
                    e.message?.contains("Expected BEGIN_OBJECT but was STRING") == true -> {
                        "Сервер вернул некорректный формат данных (строка вместо JSON). Проверьте, что endpoint регистрации работает корректно."
                    }
                    e.message?.contains("Expected") == true -> {
                        "Неожиданный формат ответа от сервера. Проверьте конфигурацию API."
                    }
                    else -> {
                        "Ошибка обработки ответа сервера: ${e.message}"
                    }
                }
                
                _uiState.value = RegisterUiState.Error(errorMessage)
            } catch (e: java.lang.IllegalStateException) {
                // Обработка других ошибок парсинга
                Timber.e(e, "❌ [RegisterViewModel] Ошибка парсинга (IllegalStateException)")
                
                val errorMessage = when {
                    e.message?.contains("Expected BEGIN_OBJECT but was STRING") == true -> {
                        "Сервер вернул некорректный формат данных. Возможно, endpoint регистрации не реализован на сервере."
                    }
                    e.message?.contains("Expected") == true -> {
                        "Неожиданный формат ответа от сервера. Проверьте конфигурацию API."
                    }
                    else -> {
                        "Ошибка обработки ответа сервера: ${e.message}"
                    }
                }
                
                _uiState.value = RegisterUiState.Error(errorMessage)
            } catch (e: Exception) {
                Timber.e(e, "❌ [RegisterViewModel] Исключение при регистрации")
                
                // Проверяем тип ошибки
                val errorMessage = when {
                    e is java.net.UnknownHostException || 
                    e.message?.contains("Unable to resolve host") == true -> 
                        "Не удалось подключиться к серверу. Проверьте интернет-соединение"
                    e is java.net.ConnectException || 
                    e.message?.contains("Failed to connect") == true -> 
                        "Не удалось подключиться к серверу. Возможно, сервер недоступен"
                    e.message?.contains("404") == true -> 
                        "Endpoint регистрации не найден. Возможно, сервер не поддерживает регистрацию"
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Превышено время ожидания ответа сервера. Попробуйте позже"
                    else -> 
                        "Ошибка регистрации: ${e.javaClass.simpleName}: ${e.message ?: "Неизвестная ошибка"}"
                }
                
                _uiState.value = RegisterUiState.Error(errorMessage)
            }
        }
    }
}

/**
 * Состояния UI для экрана регистрации
 */
sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    object Success : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

