package com.example.project_for_vtb.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_for_vtb.data.dto.AuthResponseDto
import com.example.project_for_vtb.data.remote.api.AuthApiService
import com.example.project_for_vtb.data.remote.api.LoginRequest
import com.example.project_for_vtb.data.security.TokenManager
import com.example.project_for_vtb.domain.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel для экрана авторизации
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager,
    private val gson: Gson
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    init {
        Timber.d("🚀 [LoginViewModel] Инициализация")
        checkAuthStatus()
    }
    
    /**
     * Авторизация по email и паролю
     * 
     * ВАЖНО: Согласно API_CONTRACT.md, endpoint /api/v1/auth/login может быть не реализован на сервере.
     * Сервер использует только OAuth2 flow для авторизации.
     * 
     * Этот метод пытается использовать /api/v1/auth/login, но если сервер возвращает HTML редирект,
     * это означает, что требуется OAuth2 авторизация через браузер.
     * 
     * Обрабатывает как JSON ответ с токенами, так и HTML/строковые ответы (OAuth2 редирект)
     */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Заполните все поля")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            
            Timber.d("🔐 [LoginViewModel] Попытка авторизации: email=$email")
            Timber.d("🌐 [LoginViewModel] Используется РЕАЛЬНЫЙ API: http://158.160.102.225/api/v1/auth/login")
            
            try {
                val response = authApiService.login(
                    LoginRequest(email = email, password = password)
                )
                
                Timber.d("📡 [LoginViewModel] Ответ получен. Код статуса: ${response.code()}")
                Timber.d("📡 [LoginViewModel] URL запроса: ${response.raw().request.url}")
                
                if (response.isSuccessful) {
                    try {
                        val responseBody = response.body()
                        
                        // Проверяем, есть ли тело ответа
                        if (responseBody == null) {
                            Timber.w("⚠️ [LoginViewModel] Сервер вернул успешный ответ (${response.code()}), но тело ответа пустое")
                            _uiState.value = LoginUiState.Error(
                                "Сервер вернул пустой ответ. Возможно, endpoint логина не реализован или требует OAuth2 авторизации."
                            )
                            return@launch
                        }
                        
                        // Пытаемся прочитать тело ответа как строку
                        val responseBodyString = responseBody.string()
                        
                        // Логируем полный ответ для отладки
                        Timber.d("📄 [LoginViewModel] Полный ответ от сервера:")
                        Timber.d("📄 [LoginViewModel] Длина ответа: ${responseBodyString.length} символов")
                        Timber.d("📄 [LoginViewModel] Содержимое ответа: $responseBodyString")
                        
                        // Проверяем на пустой ответ
                        if (responseBodyString.isBlank()) {
                            Timber.w("⚠️ [LoginViewModel] Сервер вернул пустую строку")
                            _uiState.value = LoginUiState.Error(
                                "Сервер вернул пустой ответ. Возможно, endpoint логина не реализован или требует OAuth2 авторизации."
                            )
                            return@launch
                        }
                        
                        // Проверяем заголовки ответа на наличие Location (редирект)
                        val locationHeader = response.headers()["Location"]
                        if (locationHeader != null) {
                            Timber.w("⚠️ [LoginViewModel] Сервер вернул редирект в заголовке Location: $locationHeader")
                            Timber.w("⚠️ [LoginViewModel] Это означает, что endpoint /api/v1/auth/login не поддерживает прямую авторизацию")
                            _uiState.value = LoginUiState.Error(
                                "Сервер не поддерживает прямую авторизацию по email/паролю. Endpoint перенаправляет на OAuth2: $locationHeader\n\nСогласно API контракту, сервер использует только OAuth2 flow. Используйте кнопку 'Подключить банк' для входа через OAuth2."
                            )
                            return@launch
                        }
                        
                        // Проверяем, не является ли ответ HTML (перенаправление)
                        if (responseBodyString.trimStart().startsWith("<", ignoreCase = false)) {
                            Timber.w("⚠️ [LoginViewModel] Сервер вернул HTML (возможно, страница перенаправления)")
                            Timber.w("⚠️ [LoginViewModel] Это означает, что endpoint /api/v1/auth/login не поддерживает прямую авторизацию по email/паролю")
                            Timber.w("⚠️ [LoginViewModel] Согласно API_CONTRACT.md, сервер использует только OAuth2 flow для авторизации")
                            
                            // Пытаемся извлечь URL из HTML (мета-редирект или JavaScript редирект)
                            val extractedUrl = extractUrlFromHtml(responseBodyString)
                            
                            if (extractedUrl != null) {
                                Timber.d("🔗 [LoginViewModel] Извлечен URL из HTML: $extractedUrl")
                                _uiState.value = LoginUiState.Error(
                                    "Сервер не поддерживает прямую авторизацию по email/паролю. Требуется OAuth2 авторизация. Сервер перенаправляет на: $extractedUrl\n\nИспользуйте кнопку 'Подключить банк' для входа через OAuth2."
                                )
                            } else {
                                // Пытаемся найти OAuth2 URL в тексте HTML
                                val oauthUrlPattern = Regex("(https?://[^\\s\"'<>]+(?:oauth2|authorization|auth)[^\\s\"'<>]*)", RegexOption.IGNORE_CASE)
                                val oauthMatch = oauthUrlPattern.find(responseBodyString)
                                
                                if (oauthMatch != null) {
                                    val oauthUrl = oauthMatch.groupValues[1]
                                    Timber.d("🔗 [LoginViewModel] Найден OAuth2 URL в HTML: $oauthUrl")
                                    _uiState.value = LoginUiState.Error(
                                        "Сервер не поддерживает прямую авторизацию по email/паролю. Требуется OAuth2 авторизация. Найден URL: $oauthUrl\n\nИспользуйте кнопку 'Подключить банк' для входа через OAuth2."
                                    )
                                } else {
                                    // Проверяем, не является ли это страницей ошибки
                                    val errorPatterns = listOf(
                                        "404", "Not Found", "не найдено",
                                        "500", "Internal Server Error", "ошибка сервера",
                                        "401", "Unauthorized", "не авторизован",
                                        "403", "Forbidden", "запрещено"
                                    )
                                    val containsError = errorPatterns.any { pattern ->
                                        responseBodyString.contains(pattern, ignoreCase = true)
                                    }
                                    
                                    if (containsError) {
                                        Timber.e("❌ [LoginViewModel] HTML содержит сообщение об ошибке")
                                        _uiState.value = LoginUiState.Error(
                                            "Endpoint /api/v1/auth/login не найден или не поддерживает прямую авторизацию.\n\nСогласно API контракту, сервер использует только OAuth2 flow. Используйте кнопку 'Подключить банк' для входа через OAuth2."
                                        )
                                    } else {
                                        // Логируем первые 500 символов HTML для анализа
                                        Timber.e("❌ [LoginViewModel] HTML ответ (первые 500 символов):")
                                        Timber.e("${responseBodyString.take(500)}")
                                        _uiState.value = LoginUiState.Error(
                                            "Сервер не поддерживает прямую авторизацию по email/паролю. Endpoint /api/v1/auth/login возвращает HTML вместо JSON.\n\nТребуется OAuth2 авторизация. Используйте кнопку 'Подключить банк' для входа через OAuth2."
                                        )
                                    }
                                }
                            }
                            return@launch
                        }
                        
                        // Пытаемся распарсить как JSON объект
                        try {
                            // Проверяем, не является ли ответ пустым JSON объектом {}
                            val trimmedResponse = responseBodyString.trim()
                            if (trimmedResponse == "{}" || trimmedResponse.isEmpty()) {
                                Timber.w("⚠️ [LoginViewModel] Сервер вернул пустой JSON объект")
                                _uiState.value = LoginUiState.Error(
                                    "Сервер вернул пустой ответ. Endpoint логина может быть не реализован. Попробуйте использовать OAuth2 авторизацию."
                                )
                                return@launch
                            }
                            
                            val authResponse = gson.fromJson(responseBodyString, AuthResponseDto::class.java)
                            
                            // Проверяем, что все обязательные поля присутствуют
                            if (authResponse != null && authResponse.accessToken.isNotBlank()) {
                                Timber.i("✅ [LoginViewModel] Успешная авторизация через РЕАЛЬНЫЙ API")
                                Timber.d("🔑 [LoginViewModel] Токен будет действителен ${authResponse.expiresIn} секунд")
                                
                                // Сохраняем токен
                                tokenManager.saveTokens(
                                    bankId = "default",
                                    accessToken = authResponse.accessToken,
                                    refreshToken = authResponse.refreshToken,
                                    expiresIn = authResponse.expiresIn
                                )
                                
                                _uiState.value = LoginUiState.Success
                            } else {
                                Timber.w("⚠️ [LoginViewModel] Получен null или пустой токен в ответе")
                                Timber.d("⚠️ [LoginViewModel] authResponse: $authResponse")
                                _uiState.value = LoginUiState.Error(
                                    "Сервер вернул некорректный ответ. Токен отсутствует в ответе. Проверьте логи для деталей."
                                )
                            }
                        } catch (e: JsonSyntaxException) {
                            // Если не удалось распарсить как JSON, возможно, это строка (OAuth2 URL)
                            Timber.w(e, "⚠️ [LoginViewModel] Не удалось распарсить ответ как JSON (JsonSyntaxException)")
                            Timber.d("⚠️ [LoginViewModel] Сообщение об ошибке: ${e.message}")
                            Timber.d("⚠️ [LoginViewModel] Ответ от сервера: $responseBodyString")
                            
                            // Проверяем, не является ли ответ OAuth2 URL
                            if (responseBodyString.contains("oauth2", ignoreCase = true) || 
                                responseBodyString.contains("authorization", ignoreCase = true) ||
                                responseBodyString.contains("http://", ignoreCase = true) ||
                                responseBodyString.contains("https://", ignoreCase = true)) {
                                Timber.w("⚠️ [LoginViewModel] Сервер вернул URL вместо токенов (вероятно, OAuth2 redirect)")
                                _uiState.value = LoginUiState.Error(
                                    "Сервер вернул URL перенаправления вместо токенов. Требуется OAuth2 авторизация. Используйте кнопку подключения банка для входа через OAuth2."
                                )
                            } else if (responseBodyString.startsWith("\"") && responseBodyString.endsWith("\"")) {
                                // Это JSON строка (строка в кавычках)
                                val unquotedString = responseBodyString.removeSurrounding("\"")
                                Timber.w("⚠️ [LoginViewModel] Сервер вернул JSON строку: $unquotedString")
                                _uiState.value = LoginUiState.Error(
                                    "Сервер вернул строку: $unquotedString. Ожидался JSON объект с токенами. Возможно, требуется OAuth2 авторизация."
                                )
                            } else {
                                // Неизвестный формат ответа
                                Timber.e("❌ [LoginViewModel] Неизвестный формат ответа от сервера")
                                Timber.e("❌ [LoginViewModel] Первые 500 символов ответа: ${responseBodyString.take(500)}")
                                _uiState.value = LoginUiState.Error(
                                    "Сервер вернул некорректный формат данных: ${responseBodyString.take(100)}${if (responseBodyString.length > 100) "..." else ""}. Ожидался JSON объект с токенами. Проверьте логи для полного ответа."
                                )
                            }
                        } catch (e: IllegalStateException) {
                            // Обработка ошибок парсинга
                            Timber.e(e, "❌ [LoginViewModel] Ошибка парсинга (IllegalStateException)")
                            Timber.e("❌ [LoginViewModel] Сообщение: ${e.message}")
                            Timber.e("❌ [LoginViewModel] Ответ от сервера: $responseBodyString")
                            
                            val errorMessage = when {
                                e.message?.contains("Expected BEGIN_OBJECT but was STRING") == true -> {
                                    "Сервер вернул строку вместо JSON объекта. Ответ: ${responseBodyString.take(100)}. Возможно, требуется OAuth2 авторизация."
                                }
                                e.message?.contains("Expected") == true -> {
                                    "Неожиданный формат ответа от сервера. Ошибка: ${e.message}. Проверьте конфигурацию API."
                                }
                                else -> {
                                    "Ошибка обработки ответа сервера: ${e.message}. Ответ: ${responseBodyString.take(100)}"
                                }
                            }
                            
                            _uiState.value = LoginUiState.Error(errorMessage)
                        } catch (e: Exception) {
                            Timber.e(e, "❌ [LoginViewModel] Неожиданная ошибка при парсинге JSON")
                            Timber.e("❌ [LoginViewModel] Тип ошибки: ${e.javaClass.simpleName}")
                            Timber.e("❌ [LoginViewModel] Сообщение: ${e.message}")
                            Timber.e("❌ [LoginViewModel] Ответ от сервера: $responseBodyString")
                            _uiState.value = LoginUiState.Error(
                                "Ошибка при обработке ответа сервера: ${e.javaClass.simpleName}: ${e.message}. Проверьте логи для деталей."
                            )
                        }
                    } catch (e: java.io.IOException) {
                        Timber.e(e, "❌ [LoginViewModel] Ошибка чтения тела ответа (IOException)")
                        _uiState.value = LoginUiState.Error("Ошибка чтения ответа от сервера: ${e.message ?: "Неизвестная ошибка"}")
                    } catch (e: Exception) {
                        Timber.e(e, "❌ [LoginViewModel] Неожиданная ошибка при чтении ответа")
                        _uiState.value = LoginUiState.Error("Ошибка чтения ответа от сервера: ${e.javaClass.simpleName}: ${e.message ?: "Неизвестная ошибка"}")
                    }
                } else {
                    // Обрабатываем ошибку от сервера
                    val errorBody = try {
                        response.errorBody()?.string()
                    } catch (e: Exception) {
                        Timber.e(e, "❌ [LoginViewModel] Не удалось прочитать тело ошибки")
                        null
                    }
                    
                    Timber.e("❌ [LoginViewModel] Ошибка авторизации. Код: ${response.code()}")
                    Timber.e("❌ [LoginViewModel] Тело ошибки: $errorBody")
                    
                    _uiState.value = LoginUiState.Error(
                        when (response.code()) {
                            401 -> "Неверный email или пароль"
                            404 -> "Endpoint авторизации не найден. Возможно, нужно использовать OAuth2"
                            500 -> "Ошибка сервера. Попробуйте позже"
                            else -> errorBody ?: "Ошибка авторизации. Код: ${response.code()}"
                        }
                    )
                }
            } catch (e: com.google.gson.JsonSyntaxException) {
                // Обработка ошибок парсинга JSON
                Timber.e(e, "❌ [LoginViewModel] Ошибка парсинга JSON (JsonSyntaxException)")
                
                val errorMessage = when {
                    e.message?.contains("Expected BEGIN_OBJECT but was STRING") == true -> {
                        "Сервер вернул некорректный формат данных (строка вместо JSON). Возможно, требуется OAuth2 авторизация."
                    }
                    e.message?.contains("Expected") == true -> {
                        "Неожиданный формат ответа от сервера. Проверьте конфигурацию API."
                    }
                    else -> {
                        "Ошибка обработки ответа сервера: ${e.message}"
                    }
                }
                
                _uiState.value = LoginUiState.Error(errorMessage)
            } catch (e: java.lang.IllegalStateException) {
                // Обработка других ошибок парсинга
                Timber.e(e, "❌ [LoginViewModel] Ошибка парсинга (IllegalStateException)")
                
                val errorMessage = when {
                    e.message?.contains("Expected BEGIN_OBJECT but was STRING") == true -> {
                        "Сервер вернул некорректный формат данных. Возможно, требуется OAuth2 авторизация."
                    }
                    e.message?.contains("Expected") == true -> {
                        "Неожиданный формат ответа от сервера. Проверьте конфигурацию API."
                    }
                    else -> {
                        "Ошибка обработки ответа сервера: ${e.message}"
                    }
                }
                
                _uiState.value = LoginUiState.Error(errorMessage)
            } catch (e: Exception) {
                Timber.e(e, "❌ [LoginViewModel] Исключение при авторизации")
                
                // Проверяем тип ошибки
                val errorMessage = when {
                    e is java.net.UnknownHostException || 
                    e.message?.contains("Unable to resolve host") == true -> 
                        "Не удалось подключиться к серверу. Проверьте интернет-соединение"
                    e is java.net.ConnectException || 
                    e.message?.contains("Failed to connect") == true -> 
                        "Не удалось подключиться к серверу. Возможно, сервер недоступен"
                    e.message?.contains("404") == true -> 
                        "Endpoint авторизации не найден. Возможно, нужно использовать OAuth2 для подключения банка"
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Превышено время ожидания ответа сервера. Попробуйте позже"
                    else -> 
                        "Ошибка авторизации: ${e.javaClass.simpleName}: ${e.message ?: "Неизвестная ошибка"}"
                }
                
                _uiState.value = LoginUiState.Error(errorMessage)
            }
        }
    }
    
    /**
     * Извлекает URL из HTML ответа (мета-редирект, JavaScript редирект и т.д.)
     */
    private fun extractUrlFromHtml(html: String): String? {
        // 1. Проверяем мета-редирект: <meta http-equiv="refresh" content="0; url=http://...">
        val metaRefreshPattern = Regex(
            "<meta[^>]+http-equiv\\s*=\\s*['\"]?refresh['\"]?[^>]+content\\s*=\\s*['\"][^'\"]*url\\s*=\\s*([^'\">\\s]+)",
            RegexOption.IGNORE_CASE
        )
        metaRefreshPattern.find(html)?.let { match ->
            return match.groupValues[1]
        }
        
        // 2. Проверяем JavaScript редирект: window.location = "http://..." или window.location.href = "http://..."
        val jsRedirectPattern = Regex(
            "window\\.location(?:\\.href)?\\s*=\\s*['\"](https?://[^'\"]+)['\"]",
            RegexOption.IGNORE_CASE
        )
        jsRedirectPattern.find(html)?.let { match ->
            return match.groupValues[1]
        }
        
        // 3. Проверяем JavaScript редирект: location.replace("http://...")
        val jsReplacePattern = Regex(
            "location\\.replace\\s*\\(['\"](https?://[^'\"]+)['\"]\\)",
            RegexOption.IGNORE_CASE
        )
        jsReplacePattern.find(html)?.let { match ->
            return match.groupValues[1]
        }
        
        // 4. Ищем ссылки на OAuth2/authorization в href
        val hrefPattern = Regex(
            "href\\s*=\\s*['\"](https?://[^'\"]*(?:oauth2|authorization|auth)[^'\"]*)['\"]",
            RegexOption.IGNORE_CASE
        )
        hrefPattern.find(html)?.let { match ->
            return match.groupValues[1]
        }
        
        return null
    }
    
    /**
     * Проверка авторизации при старте
     */
    fun checkAuthStatus() {
        viewModelScope.launch {
            val isAuthenticated = authRepository.isAuthenticated()
            Timber.d("🔍 [LoginViewModel] Проверка авторизации: $isAuthenticated")
            
            if (isAuthenticated) {
                Timber.i("✅ [LoginViewModel] Пользователь уже авторизован")
                _uiState.value = LoginUiState.Success
            } else {
                Timber.d("ℹ️ [LoginViewModel] Пользователь не авторизован")
                _uiState.value = LoginUiState.Idle
            }
        }
    }
}

/**
 * Состояния UI для экрана авторизации
 */
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

