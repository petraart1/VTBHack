# Заметки для Frontend разработчика

## Быстрый старт

1. Этот проект - **только Frontend разработка**
2. Backend API разрабатывается другой командой
3. Для разработки используются **Mock данные** из `MockDataService`
4. API контракты определены в `API_CONTRACT.md`

## Ключевые файлы

### API и данные
- `data/remote/api/BankApiService.kt` - Интерфейс API (контракт с Backend)
- `data/dto/` - DTO модели (должны соответствовать Backend)
- `data/mock/MockDataService.kt` - Mock данные для разработки

### Репозитории
- `repository/AccountRepositoryImpl.kt` - Репозиторий счетов
- `repository/TransactionRepositoryImpl.kt` - Репозиторий транзакций
- `repository/BankRepositoryImpl.kt` - Репозиторий банков

### UI
- `ui/screens/` - Все экраны приложения
- `ui/navigation/NavGraph.kt` - Навигация

## Работа с Mock данными

**Текущее состояние:** Репозитории используют `MockDataService` для получения данных.

**Для переключения на реальный Backend:**
1. Обновить `baseUrl` в `NetworkModule.kt`
2. Заменить вызовы `mockDataService` на `bankApiService` в репозиториях
3. Добавить обработку токенов авторизации
4. Добавить обработку ошибок API

## API Контракт

Все изменения API контракта должны быть согласованы с Backend командой.

**При изменении API:**
1. Обновить `BankApiService.kt` (интерфейсы методов)
2. Обновить DTO модели в `data/dto/`
3. Обновить `API_CONTRACT.md`
4. Обновить репозитории для работы с новым форматом

## Структура проекта

```
app/src/main/java/com/example/project_for_vtb/
├── data/          # Слой данных (DTO, API, Mock, Local DB)
├── domain/        # Бизнес-логика (модели, репозитории, use cases)
├── repository/    # Реализация репозиториев
├── ui/            # UI слой (Compose screens, navigation, theme)
└── di/            # Dependency Injection (Hilt модули)
```

## Что НЕ нужно делать

❌ Разрабатывать Backend API  
❌ Изменять API контракт без согласования с Backend командой  
❌ Писать бизнес-логику на стороне сервера  

## Что нужно делать

✅ Разрабатывать UI экраны  
✅ Интегрировать с Backend API через Retrofit  
✅ Обрабатывать состояния загрузки и ошибок  
✅ Кешировать данные локально (Room)  
✅ Обрабатывать авторизацию на клиенте  

## Полезные команды

```bash
# Запуск приложения
./gradlew installDebug

# Запуск тестов
./gradlew test

# Сборка release
./gradlew assembleRelease
```

## Контакты

При вопросах по API контракту - обращаться к Backend команде.


