# Проект для ВТБ - Android Frontend

## Обзор

Это Android приложение на Kotlin с использованием Jetpack Compose для разработки Frontend части банковского приложения. Backend API разрабатывается отдельной командой.

## Архитектура проекта

Проект использует Clean Architecture с разделением на слои:

```
app/src/main/java/com/example/project_for_vtb/
├── data/           # Слой данных
│   ├── dto/        # DTO модели для API (должны соответствовать Backend контракту)
│   ├── local/      # Локальная база данных (Room)
│   ├── remote/     # API сервисы (Retrofit)
│   ├── mock/       # Mock сервис для разработки Frontend
│   └── mapper/     # Мапперы между слоями
├── domain/         # Бизнес-логика
│   ├── model/      # Доменные модели
│   ├── repository/ # Интерфейсы репозиториев
│   └── usecase/    # Use cases
├── repository/     # Реализация репозиториев
├── ui/             # UI слой (Jetpack Compose)
│   ├── screens/    # Экраны приложения
│   ├── navigation/ # Навигация
│   └── theme/      # Тема оформления
└── di/             # Dependency Injection (Hilt)
```

## Разделение ответственности

### Frontend разработчик (этот проект)

**Ответственность:**
- Разработка UI экранов (Jetpack Compose)
- Интеграция с Backend API через Retrofit
- Локальное кеширование данных (Room)
- Обработка состояний загрузки и ошибок
- Навигация между экранами
- Управление состоянием приложения (ViewModel, StateFlow)
- Обработка авторизации на клиенте

**НЕ ответственность:**
- Разработка Backend API
- Определение бизнес-правил на сервере
- Управление данными на сервере

### Backend команда

**Ответственность:**
- Разработка REST API
- Определение API контрактов (DTO, endpoints)
- Бизнес-логика на сервере
- Аутентификация и авторизация
- Хранение и обработка данных

## API Контракт

Все DTO модели и API интерфейсы находятся в пакете `data/remote/api/` и `data/dto/`. 

**ВАЖНО:** Эти контракты должны соответствовать спецификации, предоставленной Backend командой. При изменении API контракта Backend командой необходимо обновить соответствующие DTO и интерфейсы в этом проекте.

### Текущие API endpoints:

- `POST /auth/token` - Обмен кода авторизации на токен
- `POST /auth/refresh` - Обновление токена
- `GET /accounts` - Получение списка счетов
- `GET /accounts/{accountId}/transactions` - Получение транзакций по счету
- `GET /transactions` - Получение всех транзакций

### DTO модели:

- `AuthResponseDto` - Ответ с токенами авторизации
- `AccountDto` - Данные счета
- `TransactionDto` - Данные транзакции

## Разработка Frontend

### Mock данные

Для разработки Frontend без подключенного Backend используется `MockDataService`, который предоставляет тестовые данные из JSON файлов в `app/src/main/assets/mockResponses/`.

**Использование Mock данных:**
- Mock данные используются по умолчанию в репозиториях
- При подключении реального Backend необходимо обновить репозитории для использования `BankApiService` вместо `MockDataService`

### Подключение реального Backend

Когда Backend API будет готов:

1. Обновить `baseUrl` в `NetworkModule.kt`:
```kotlin
@Provides
@Singleton
@Named("baseUrl")
fun provideBaseUrl(): String {
    return "https://your-backend-api.com/" // Заменить на реальный URL
}
```

2. Убедиться, что DTO модели соответствуют API контракту Backend

3. Обновить репозитории для использования `BankApiService` вместо `MockDataService`

4. Настроить обработку ошибок API и статусов ответов

## Технологии

- **Язык:** Kotlin
- **UI:** Jetpack Compose
- **Архитектура:** Clean Architecture, MVVM
- **Dependency Injection:** Hilt
- **Networking:** Retrofit 2, OkHttp
- **Локальная БД:** Room
- **Навигация:** Navigation Compose
- **Асинхронность:** Coroutines, Flow
- **Авторизация:** AppAuth (OAuth2 + PKCE)

## Зависимости

Все зависимости управляются через `gradle/libs.versions.toml`. Основные библиотеки:

- Jetpack Compose
- Hilt для DI
- Retrofit для сетевых запросов
- Room для локальной БД
- Navigation Compose
- Coil для загрузки изображений

## Конфигурация

### Build Config

- `minSdk`: 35
- `targetSdk`: 36
- `compileSdk`: 36

### Environment Variables

Для настройки различных окружений (dev, staging, production) можно использовать BuildConfig или конфигурационные файлы.

## Запуск проекта

1. Клонировать репозиторий
2. Открыть проект в Android Studio
3. Синхронизировать Gradle
4. Запустить приложение

## Тестирование

- Unit тесты: `app/src/test/`
- Instrumented тесты: `app/src/androidTest/`

## UI/UX Дизайн

Приложение реализовано согласно техническому заданию с современным Material3 дизайном.

### Реализованные экраны:

1. **Welcome Screen** - Экран приветствия с анимацией и описанием возможностей
2. **Login Screen** - Авторизация с поддержкой биометрии и OAuth2
3. **Dashboard** - Главный экран с общим балансом и быстрыми действиями
4. **Accounts Screen** - Список счетов и карт с детальной информацией
5. **Transactions Screen** - История транзакций с поиском и фильтрами
6. **Analytics Screen** - Аналитика трат с графиками и категориями
7. **Settings Screen** - Настройки профиля, темы, безопасности
8. **Bank Connection Screen** - Подключение банков через OAuth2

### Компоненты UI:

- **BalanceCard** - Карточка общего баланса с анимацией
- **AccountCard** - Карточка счета с информацией о типе и балансе
- **TransactionCard** - Карточка транзакции с категорией и суммой
- Переиспользуемые компоненты для единообразного дизайна

### Навигация:

- Bottom Navigation Bar для основных экранов (Dashboard, Accounts, Transactions, Analytics, Settings)
- Навигация между экранами с поддержкой состояния
- Анимации переходов

### Тема оформления:

- Банковская цветовая схема (синий/зеленый)
- Поддержка темной темы
- Material3 компоненты
- Типографика для банковского приложения

### Локализация:

- Русский язык (RU)
- Подготовка для добавления английского языка (EN)

## Контакты

При вопросах по API контракту обращаться к Backend команде.
При вопросах по Frontend разработке - к команде Frontend.

