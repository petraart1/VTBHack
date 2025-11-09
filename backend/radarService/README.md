# RadarService - Умный Радар Подписок 🎯

## 📋 Описание

RadarService - это микросервис для автоматического обнаружения и управления подписками пользователей. Система анализирует транзакции из BankService, определяет повторяющиеся платежи, отслеживает изменения цен и помогает пользователям оптимизировать расходы.

## ✨ Основные возможности

### 🔍 Автоматическое обнаружение подписок
- Анализ транзакций за последние 6 месяцев
- ML-алгоритм определения регулярных платежей
- Confidence score для каждой обнаруженной подписки
- Классификация по категориям (STREAMING, SOFTWARE, GYM, etc.)

### 📊 Мониторинг изменения цен
- Автоматическое отслеживание роста цен
- История изменений с процентами
- Алерты при превышении порога (по умолчанию 5%)

### 💡 Рекомендации альтернатив
- Поиск более выгодных аналогов
- Расчет потенциальной экономии
- Ссылки на альтернативные сервисы

### 🔔 Умные уведомления
- Обнаружение новых подписок
- Предупреждения о росте цен
- Напоминания о неиспользуемых подписках
- Уведомления о предстоящих платежах

### ❌ Помощь в отмене
- Инструкции по отмене подписок
- Ссылки на страницы отмены
- Отслеживание отмененных подписок

## 🏗️ Архитектура

### Domain Model

```
Subscription
├── Характеристики (merchantName, amount, frequency)
├── Статус (DETECTED, CONFIRMED, CANCELLED, INACTIVE)
├── Аналитика (totalPayments, totalSpent, confidenceScore)
└── Даты (firstPayment, lastPayment, nextExpected)

PriceHistory
├── oldPrice, newPrice
├── priceChange, percentageChange
└── detectedAt

SubscriptionAlert
├── AlertType (PRICE_INCREASE, NEW_SUBSCRIPTION, etc.)
├── title, message, actionUrl
└── isRead

SubscriptionAlternative
├── alternativeName, price
├── savings, rating
└── referralLink

TransactionPattern (для ML)
├── Статистика (averageAmount, stdDeviation)
├── Временные паттерны (averageDaysBetween, stdDevDays)
└── recentPaymentDates
```

### Алгоритм обнаружения

1. **Группировка** транзакций по merchant_name
2. **Анализ частоты**: вычисление интервалов между платежами
3. **Статистический анализ**: среднее и std deviation
4. **Определение типа**: MONTHLY, YEARLY, WEEKLY, QUARTERLY
5. **Расчет Confidence Score**:
   ```
   confidence = (countScore * 0.4) + (regularityScore * 0.6)
   где:
   - countScore = min(paymentCount / 12, 1.0)
   - regularityScore = max(0, 1.0 - (stdDev / avgDays))
   ```

## 🚀 API Endpoints

### Подписки

```http
GET    /api/v1/radar/subscriptions
GET    /api/v1/radar/subscriptions/{id}
POST   /api/v1/radar/subscriptions/detect
POST   /api/v1/radar/subscriptions/{id}/confirm
POST   /api/v1/radar/subscriptions/{id}/cancel
GET    /api/v1/radar/subscriptions/stats
```

### Алерты

```http
GET    /api/v1/radar/alerts
GET    /api/v1/radar/alerts/{id}
PUT    /api/v1/radar/alerts/{id}/mark-read
```

### Альтернативы

```http
GET    /api/v1/radar/subscriptions/{id}/alternatives
GET    /api/v1/radar/subscriptions/{id}/price-history
```

## 🛠️ Технологии

- **Spring Boot 3.5.7** - фреймворк
- **Spring Data JPA** - работа с БД
- **PostgreSQL** - база данных
- **Liquibase** - миграции БД
- **Spring WebFlux** - интеграция с BankService
- **Resilience4j** - Circuit Breaker + Retry
- **Quartz** - планировщик задач
- **JWT** - аутентификация
- **Caffeine** - кеширование
- **OpenAPI/Swagger** - документация API

## ⚙️ Конфигурация

### application.yaml

```yaml
subscription:
  detection:
    enabled: true
    cron: "0 0 2 * * *"        # Каждую ночь в 2:00
    min-occurrences: 3          # Минимум 3 платежа
    max-std-dev-days: 7         # Макс отклонение 7 дней
    lookback-months: 6          # Анализ за 6 месяцев
    
  price-monitoring:
    enabled: true
    cron: "0 0 */6 * * *"      # Каждые 6 часов
    price-increase-threshold: 5.0  # Порог алерта 5%
    
  unused-detection:
    enabled: true
    cron: "0 0 3 * * MON"      # Каждый понедельник
    days-without-activity: 60   # 60 дней без активности
```

## 🔄 Scheduled Tasks

### Обнаружение подписок
- **Расписание**: Каждую ночь в 2:00
- **Процесс**:
  1. Получение транзакций за последние 6 месяцев
  2. Группировка по merchant
  3. Анализ паттернов
  4. Создание/обновление подписок
  5. Генерация алертов

### Мониторинг цен
- **Расписание**: Каждые 6 часов
- **Процесс**:
  1. Проверка активных подписок
  2. Сравнение с последними транзакциями
  3. Обнаружение изменений > 5%
  4. Создание записей в price_history
  5. Генерация алертов

### Обнаружение неактивных подписок
- **Расписание**: Каждый понедельник в 3:00
- **Процесс**:
  1. Поиск подписок без транзакций > 60 дней
  2. Пометка как INACTIVE
  3. Создание алертов

## 🔗 Интеграция с BankService

### BankServiceClient

```java
// Получение всех транзакций пользователя
List<TransactionDto> transactions = 
    bankServiceClient.getAllUserTransactions(
        LocalDateTime.now().minusMonths(6),
        LocalDateTime.now(),
        jwtToken
    );

// Circuit Breaker защита
@CircuitBreaker(name = "bankService")
@Retry(name = "bankService")
public List<AccountDto> getAccounts(String jwtToken) { ... }
```

### Resilience4j конфигурация

```yaml
resilience4j:
  circuitbreaker:
    instances:
      bankService:
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
  retry:
    instances:
      bankService:
        maxAttempts: 3
        waitDuration: 1s
```

## 📊 Database Schema

### Основные таблицы

- `subscriptions` - подписки пользователей
- `price_history` - история изменения цен
- `subscription_alerts` - уведомления
- `subscription_alternatives` - альтернативы
- `transaction_patterns` - ML паттерны

### Индексы (оптимизированные)

```sql
-- Критичные для производительности
idx_subscriptions_user_id
idx_subscriptions_next_payment
idx_alerts_user_unread
idx_price_history_subscription
idx_patterns_user
```

## 🔐 Security

### JWT Authentication
- Токены от authService
- Валидация через JwtAuthFilter
- Извлечение userId из claims

### Эндпоинты

- ✅ `/actuator/health`, `/actuator/prometheus` - публичные
- ✅ `/swagger-ui/**`, `/v3/api-docs/**` - публичные
- 🔒 `/api/v1/radar/**` - требуют JWT

## 🚀 Запуск

### Локальная разработка

```bash
# 1. Запустить PostgreSQL
docker-compose up -d radar-db

# 2. Установить переменные окружения
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=your_password
export POSTGRES_DB=radar_db
export JWT_SECRET=your_jwt_secret
export RADAR_DB_CONTAINER_NAME=radar-db
export DB_PORT=5432

# 3. Запустить приложение
./gradlew bootRun
```

### Docker

```bash
# Сборка
./gradlew build
docker build -t radarservice .

# Запуск
docker-compose up radar
```

## 📈 Мониторинг

### Actuator Endpoints

- `/actuator/health` - Health check
- `/actuator/metrics` - Метрики
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/info` - Информация о приложении

### Метрики для мониторинга

- `subscriptions.detected.total` - всего обнаружено
- `subscriptions.confirmed.total` - подтверждено пользователями
- `price.increases.detected` - обнаружено повышений цен
- `alerts.created.total` - создано алертов

## 🧪 Тестирование

```bash
# Unit тесты
./gradlew test

# Integration тесты
./gradlew integrationTest

# Проверка coverage
./gradlew jacocoTestReport
```

## 📝 TODO (Future Enhancements)

- [ ] ML модель для улучшения обнаружения (Python microservice)
- [ ] Email/Push уведомления об алертах
- [ ] Интеграция с Product Hunt API для альтернатив
- [ ] Автоматическая отмена подписок через bank API
- [ ] Графики и аналитика расходов
- [ ] Сравнение с другими пользователями (anonymous)
- [ ] Кешированиеslow queries через Redis
- [ ] WebSocket для real-time уведомлений

## 📚 Документация API

После запуска приложения доступна по адресу:
- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **API Docs**: http://localhost:8082/v3/api-docs

## 👥 Разработчики

Разработано в соответствии со стандартами:
- Центрального Банка РФ
- OpenAPI 3.1.0
- Open Banking Russia

## 📄 License

MIT License

