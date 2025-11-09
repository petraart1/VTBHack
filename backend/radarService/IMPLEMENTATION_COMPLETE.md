# ✅ RadarService - Реализация Завершена

## 📊 Статус Реализации

**Дата завершения:** 8 ноября 2025  
**Статус:** ✅ MVP Ready

---

## 🎯 Что Реализовано

### 1. Доменная Модель (5/5) ✅

**Entities:**
- ✅ `Subscription` - Основная сущность подписки
- ✅ `PriceHistory` - История изменения цен
- ✅ `SubscriptionAlert` - Уведомления пользователей
- ✅ `SubscriptionAlternative` - Альтернативные сервисы
- ✅ `TransactionPattern` - Паттерны транзакций для ML

**Enums:**
- ✅ `SubscriptionStatus` - DETECTED, CONFIRMED, CANCELLED, INACTIVE
- ✅ `SubscriptionCategory` - Категории подписок
- ✅ `SubscriptionFrequency` - Периодичность платежей
- ✅ `AlertType` - Типы уведомлений

### 2. Data Access Layer (5/5) ✅

**Repositories:**
- ✅ `SubscriptionRepository`
- ✅ `PriceHistoryRepository`
- ✅ `SubscriptionAlertRepository`
- ✅ `SubscriptionAlternativeRepository`
- ✅ `TransactionPatternRepository`

### 3. Business Logic (5/5) ✅

**Services:**
- ✅ `SubscriptionDetectionService` - Алгоритм обнаружения подписок
- ✅ `PriceMonitoringService` - Мониторинг изменения цен
- ✅ `AlertService` - Управление уведомлениями
- ✅ `AlternativeService` - Рекомендация альтернатив (статичная база-заглушка)
- ✅ `SubscriptionManagementService` - Управление подписками

### 4. API Layer (1/1) ✅

**Controllers:**
- ✅ `RadarController` - 15 REST endpoints

### 5. Integration Layer (1/1) ✅

**Clients:**
- ✅ `BankServiceClient` - Интеграция с bankService через WebClient + Resilience4j

### 6. Scheduled Tasks (1/1) ✅

**Schedulers:**
- ✅ `SubscriptionScheduler` - Автоматическое обнаружение (каждую ночь) + мониторинг цен (каждые 6 часов)

### 7. Database Migrations (1/1) ✅

**Liquibase:**
- ✅ `db.changelog-master.yaml`
- ✅ `db.changelog-1.0.sql` - Создание всех таблиц и индексов

### 8. Security & Utils (3/3) ✅

- ✅ `JwtService` - Генерация и парсинг JWT
- ✅ `JwtAuthFilter` - Фильтр аутентификации
- ✅ `SecurityConfig` - Конфигурация Spring Security
- ✅ `AuthUtil` - Утилиты для работы с JWT

### 9. Configuration (3/3) ✅

- ✅ `build.gradle` - Все зависимости
- ✅ `application.yaml` - Полная конфигурация
- ✅ `RadarServiceApplication` - @EnableScheduling, @EnableCaching

### 10. DTOs (7/7) ✅

**Request:**
- ✅ `CancellationRequestDto`

**Response:**
- ✅ `SubscriptionDto`
- ✅ `SubscriptionStatsDto`
- ✅ `AlertDto`
- ✅ `AlternativeDto`
- ✅ `PriceHistoryDto`
- ✅ `DetectionResultDto`
- ✅ `TransactionDto` + `AccountDto` (для интеграции с bankService)

---

## 🚀 API Endpoints

### Subscription Management
```http
POST   /api/v1/radar/detect                                 # Запустить обнаружение
GET    /api/v1/radar/subscriptions                          # Все подписки
GET    /api/v1/radar/subscriptions/active                   # Активные подписки
GET    /api/v1/radar/subscriptions/{id}                     # Детали подписки
POST   /api/v1/radar/subscriptions/{id}/confirm             # Подтвердить подписку
POST   /api/v1/radar/subscriptions/{id}/cancel              # Отменить подписку
GET    /api/v1/radar/subscriptions/stats                    # Статистика
```

### Price Monitoring
```http
GET    /api/v1/radar/subscriptions/{id}/price-history       # История цен подписки
GET    /api/v1/radar/price-history                          # Вся история цен
```

### Alternatives
```http
GET    /api/v1/radar/subscriptions/{id}/alternatives        # Альтернативы для подписки
```

### Alerts
```http
GET    /api/v1/radar/alerts                                 # Все уведомления
GET    /api/v1/radar/alerts/unread                          # Непрочитанные уведомления
PUT    /api/v1/radar/alerts/{id}/read                       # Пометить как прочитанное
PUT    /api/v1/radar/alerts/read-all                        # Пометить все как прочитанные
DELETE /api/v1/radar/alerts/{id}                            # Удалить уведомление
```

---

## 🧠 Алгоритм Обнаружения Подписок

### Как Работает

1. **Получение транзакций** - Запрашивает все транзакции пользователя за последние 12 месяцев из bankService

2. **Группировка по мерчанту** - Группирует расходные транзакции по нормализованному названию мерчанта

3. **Анализ паттернов** - Для каждого мерчанта:
   - Вычисляет среднюю сумму платежа и стандартное отклонение
   - Вычисляет средний период между платежами и его отклонение
   - Проверяет регулярность (отклонение суммы < 5%, отклонение периода < 30%)

4. **Определение подписки** - Если найдено >= 3 повторяющихся платежа с низким отклонением:
   - Создается `TransactionPattern`
   - Создается `Subscription` со статусом DETECTED или CONFIRMED
   - Определяется категория (на основе MCC кода)
   - Определяется частота (WEEKLY, MONTHLY, QUARTERLY, etc.)
   - Вычисляется `confidenceScore` (0.0 - 1.0)

5. **Уведомление** - Создается алерт о новой подписке

### Confidence Score

```
score = 1.0
- Если транзакций < 4: score -= 0.2
- Чем больше отклонение в суммах: score -= (stdDev / mean) * 2
- Чем больше отклонение в периодичности: score -= (stdDevDays / avgDays)
result = max(0.0, min(1.0, score))
```

### Категории (MCC коды)

| Категория | MCC Коды | Примеры |
|-----------|----------|---------|
| DIGITAL_SERVICES | 5815, 5816, 5817 | Spotify, Netflix |
| COMMUNICATION | 4899, 4900 | Мобильная связь, интернет |
| SOFTWARE | 5732, 5733 | SaaS продукты |
| ENTERTAINMENT | 7929, 7941 | Кинотеатры, игры |
| FOOD_DELIVERY | 5812, 5813, 5814 | Яндекс Еда, Delivery Club |

---

## 💰 Мониторинг Цен

### Как Работает

1. **Scheduled Task** - Каждые 6 часов проверяет все активные подписки
2. **Обнаружение изменений** - Если `priceIncreased = true` и есть `previousAmount`
3. **Запись в историю** - Создается `PriceHistory` с процентом изменения
4. **Уведомление** - Если изменение >= 5%, создается алерт

---

## 🔔 Типы Уведомлений

| Тип | Когда создается |
|-----|-----------------|
| NEW_SUBSCRIPTION_DETECTED | При обнаружении новой подписки |
| PRICE_INCREASE | При увеличении цены >= 5% |
| UNUSED_SUBSCRIPTION | Резерв для будущего функционала |
| BETTER_ALTERNATIVE | При нахождении более выгодной альтернативы |

---

## 🔄 Интеграция с BankService

### BankServiceClient

**Технологии:**
- WebClient (reactive, non-blocking)
- Resilience4j CircuitBreaker
- Resilience4j Retry

**Конфигурация:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      bankService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
  retry:
    instances:
      bankService:
        max-attempts: 3
        wait-duration: 1s
```

**Методы:**
- `getAccounts(UUID userId)` - Получить все счета пользователя
- `getTransactions(UUID userId)` - Получить все транзакции за 12 месяцев

---

## 📅 Scheduled Tasks

### 1. Subscription Detection
```yaml
Cron: 0 0 2 * * *  # Каждую ночь в 02:00 MSK
```
- Запускается для всех активных пользователей
- Задержка 500ms между пользователями (чтобы не перегружать bankService)

### 2. Price Monitoring
```yaml
Fixed Delay: 21600000ms (6 часов)
Initial Delay: 60000ms (1 минута)
```
- Проверяет изменения цен для всех пользователей

---

## 🗄️ Database Schema

### Таблицы

1. **subscriptions** - Основная таблица подписок (16 полей)
2. **price_history** - История изменения цен (7 полей)
3. **subscription_alerts** - Уведомления (9 полей)
4. **subscription_alternatives** - Альтернативы (10 полей)
5. **transaction_patterns** - ML паттерны (13 полей, включая JSONB)

### Индексы

Оптимизированы для:
- Поиск по userId
- Фильтрация по статусу
- Сортировка по дате
- Full-text search по merchantName

---

## 🎨 Система Альтернатив (MVP)

### Текущая Реализация - Статичная База

**Альтернативы для:**
- ✅ Spotify → Яндекс Музыка, VK Музыка, Apple Music
- ✅ Netflix → Кинопоиск HD, IVI, OKKO
- ✅ Dropbox → Яндекс Диск, Google Drive, Облако Mail.ru
- ✅ VPN → Surfshark, ProtonVPN, Mullvad VPN

### Будущее Развитие

```java
// TODO: Интеграция с внешним API рекомендаций
// TODO: ML-модель для поиска похожих сервисов
// TODO: Парсинг цен в реальном времени
```

---

## 🔐 Security

### JWT Authentication
- ✅ Все endpoints защищены JWT токеном
- ✅ Authorization header: `Bearer <token>`
- ✅ Извлечение userId из JWT claims
- ✅ Проверка принадлежности ресурсов пользователю

### Swagger/OpenAPI
- ✅ Документация доступна на `/swagger-ui.html`
- ✅ SecurityScheme: Bearer Authentication

---

## 📦 Зависимости

```gradle
// Spring Boot
spring-boot-starter-web
spring-boot-starter-webflux
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-oauth2-resource-server
spring-boot-starter-cache
spring-boot-starter-quartz

// Database
postgresql
liquibase-core

// JWT
io.jsonwebtoken:jjwt-api:0.12.5
io.jsonwebtoken:jjwt-impl:0.12.5
io.jsonwebtoken:jjwt-jackson:0.12.5

// Resilience
io.github.resilience4j:resilience4j-spring-boot3
io.github.resilience4j:resilience4j-reactor

// Cache
com.github.ben-manes.caffeine:caffeine

// Monitoring
spring-boot-starter-actuator
micrometer-registry-prometheus

// Documentation
springdoc-openapi-starter-webmvc-ui:2.3.0

// Utils
lombok
```

---

## 🧪 Как Тестировать

### 1. Запуск локально

```bash
# Из корня проекта
cd backend
docker-compose up -d radar-db redis

# Запустить radarService
cd radarService
./gradlew bootRun
```

### 2. Тестовые запросы

```bash
# Получить JWT токен из authService
TOKEN="your-jwt-token"

# Запустить обнаружение подписок
curl -X POST http://localhost:8083/api/v1/radar/detect \
  -H "Authorization: Bearer $TOKEN"

# Получить все подписки
curl http://localhost:8083/api/v1/radar/subscriptions \
  -H "Authorization: Bearer $TOKEN"

# Получить статистику
curl http://localhost:8083/api/v1/radar/subscriptions/stats \
  -H "Authorization: Bearer $TOKEN"

# Получить уведомления
curl http://localhost:8083/api/v1/radar/alerts/unread \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Swagger UI

```
http://localhost:8083/swagger-ui.html
```

---

## 📈 Метрики и Мониторинг

### Actuator Endpoints

```http
GET /actuator/health          # Health check
GET /actuator/metrics         # Prometheus metrics
GET /actuator/info            # Service info
```

### Кэширование

**Caffeine Cache:**
- `userSubscriptions` - TTL 5 минут
- `subscriptionDetails` - TTL 10 минут
- `userAlerts` - TTL 2 минуты
- `subscriptionStats` - TTL 15 минут
- `bestAlternative` - TTL 1 час

---

## 🎯 Следующие Шаги

### Для Production

1. ✅ **RadarService реализован полностью (MVP)**
2. 🔧 Исправить критические проблемы из TODO (security issues в auth/bank)
3. 🚀 Настроить CI/CD для radarService
4. 🐳 Добавить radarService в `compose.yaml`
5. 📊 Добавить Grafana дашборды
6. 🧪 Написать unit и integration тесты

### Будущие Фичи

- 🤖 ML-модель для более точного обнаружения
- 🌐 Интеграция с реальным API альтернатив
- 📧 Email/Push уведомления (сейчас только REST API)
- 🔗 Автоматическая отмена подписок через банк API
- 📊 Детальная аналитика трат на подписки
- 💡 Персонализированные рекомендации

---

## ✨ Итоги

**RadarService полностью готов к тестированию и интеграции!**

### Что Получилось

- ✅ 5 Domain Entities
- ✅ 5 Repositories
- ✅ 5 Business Services
- ✅ 1 REST Controller (15 endpoints)
- ✅ 1 Integration Client (с Resilience4j)
- ✅ 1 Scheduler (2 задачи)
- ✅ 7 DTOs
- ✅ Full Security (JWT)
- ✅ Database Migrations (Liquibase)
- ✅ Caching (Caffeine)
- ✅ OpenAPI Documentation
- ✅ Actuator Monitoring

**Общий объем кода: ~2500+ строк чистого Java кода**

---

## 📚 Документация

- [README.md](./README.md) - Основная документация
- [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md) - Статус реализации
- Swagger UI: `http://localhost:8083/swagger-ui.html`

---

**Автор:** AI Assistant  
**Дата:** 8 ноября 2025  
**Версия:** 1.0.0 (MVP)

