# Быстрый старт - Локальный запуск и тестирование

## Предварительные требования

- Docker и Docker Compose установлены
- Git (для клонирования репозитория, если нужно)

## Шаг 1: Настройка переменных окружения

Создайте файл `.env` в корне проекта:

```bash
cd VTBHack
cat > .env << 'EOF'
# База данных
POSTGRES_DB=bank
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres123

# Порты баз данных
DB_PORT=5432
AUTH_DB_CONTAINER_NAME=auth-db
BANK_DB_CONTAINER_NAME=bank-db
RADAR_DB_CONTAINER_NAME=radar-db

# JWT секрет (сгенерируйте свой или используйте этот для тестирования)
JWT_SECRET=34cf09cbe5571911e487cafcd49b72946a133fe881c3de75ab2d412f226ef06f

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
EOF
```

## Шаг 2: Настройка переменных окружения для фронтенда

Создайте файл `.env` в папке `frontend`:

```bash
cd frontend
cat > .env << 'EOF'
# API Base URL (через nginx proxy)
VITE_API_BASE_URL=http://localhost:80
EOF
```

## Шаг 3: Запуск всех сервисов

Из корня проекта выполните:

```bash
# Запуск всех сервисов с пересборкой
docker compose up --build

# Или в фоновом режиме
docker compose up --build -d
```

Это запустит:
- 3 базы данных PostgreSQL (auth-db, bank-db, radar-db)
- Redis
- AuthService (порт 8080)
- BankService (порт 8081)
- RadarService (порт 8082)
- Nginx (порт 80) - API Gateway
- Frontend (порт 3000)
- Prometheus (порт 9090)

## Шаг 4: Проверка статуса сервисов

```bash
# Проверить статус всех контейнеров
docker compose ps

# Проверить логи конкретного сервиса
docker compose logs auth
docker compose logs bank
docker compose logs frontend

# Проверить логи всех сервисов
docker compose logs -f
```

## Шаг 5: Доступ к приложению

После запуска сервисы будут доступны по следующим адресам:

- **Frontend (веб-приложение)**: http://localhost:3000
- **API Gateway (Nginx)**: http://localhost:80
- **AuthService Swagger**: http://localhost:8080/auth/swagger-ui.html
- **BankService Swagger**: http://localhost:8081/bank/swagger-ui.html
- **RadarService Swagger**: http://localhost:8082/radar/swagger-ui.html
- **Prometheus**: http://localhost:9090

## Шаг 6: Тестирование интеграции

### 6.1. Регистрация пользователя

1. Откройте http://localhost:3000
2. Перейдите на страницу регистрации
3. Заполните форму:
   - Имя: Иван
   - Фамилия: Иванов
   - Дата рождения: 1990-01-01
   - Email: test@example.com
   - Пароль: password123

### 6.2. Вход в систему

1. После регистрации перейдите на страницу входа
2. Введите email и пароль
3. После успешного входа вы должны быть перенаправлены на Dashboard

### 6.3. Проверка API через Swagger

1. Откройте Swagger UI для AuthService: http://localhost:8080/auth/swagger-ui.html
2. Попробуйте выполнить запрос `/auth/login` с тестовыми данными
3. Проверьте ответ - должен вернуться JWT токен

### 6.4. Проверка работы с банковскими данными

**Важно**: Для работы с банковскими данными нужно сначала подключить банк через API.

1. Войдите в систему через фронтенд
2. Откройте консоль браузера (F12)
3. Проверьте, что токен сохранился в localStorage:
   ```javascript
   localStorage.getItem('auth_token')
   ```

4. Попробуйте получить список счетов через API:
   ```bash
   # Получите токен из localStorage браузера
   TOKEN="ваш_токен_из_localStorage"
   
   # Запрос к API через curl
   curl -X GET "http://localhost:80/api/v1/bank/api/v1/bank/accounts" \
     -H "Authorization: Bearer $TOKEN"
   ```

## Шаг 7: Тестирование через curl

### Регистрация пользователя

```bash
curl -X POST "http://localhost:80/api/v1/auth/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Иван",
    "lastName": "Иванов",
    "birthOfDate": "1990-01-01",
    "email": "test@example.com",
    "password": "password123"
  }'
```

### Вход в систему

```bash
curl -X POST "http://localhost:80/api/v1/auth/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

Ответ должен содержать JWT токен:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Получение списка счетов (требует токен)

```bash
TOKEN="ваш_токен_из_ответа_выше"

curl -X GET "http://localhost:80/api/v1/bank/api/v1/bank/accounts" \
  -H "Authorization: Bearer $TOKEN"
```

## Шаг 8: Отладка проблем

### Проблема: Сервисы не запускаются

```bash
# Проверить логи
docker compose logs

# Пересобрать контейнеры
docker compose down
docker compose up --build
```

### Проблема: База данных не подключается

```bash
# Проверить статус баз данных
docker compose ps | grep db

# Проверить логи базы данных
docker compose logs auth-db
docker compose logs bank-db
```

### Проблема: Frontend не подключается к API

1. Проверьте, что переменная `VITE_API_BASE_URL` установлена в `.env` файле фронтенда
2. Перезапустите контейнер фронтенда:
   ```bash
   docker compose restart frontend
   ```

3. Проверьте консоль браузера на наличие ошибок CORS или 404

### Проблема: 401 Unauthorized

1. Проверьте, что токен сохранился в localStorage
2. Проверьте формат заголовка Authorization: `Bearer <token>`
3. Проверьте, что токен не истек

### Проблема: Неправильные пути API

Если возникают проблемы с путями API (404 ошибки), проверьте конфигурацию nginx:

```bash
# Проверить конфигурацию nginx
docker compose exec nginx cat /etc/nginx/conf.d/server.conf
```

Пути должны быть:
- `/api/v1/auth/*` → проксируется на `auth_service/`
- `/api/v1/bank/*` → проксируется на `bank_service/`
- `/api/v1/radar/*` → проксируется на `radar_service/`

## Шаг 9: Остановка сервисов

```bash
# Остановить все сервисы
docker compose down

# Остановить и удалить volumes (удалит все данные)
docker compose down -v
```

## Шаг 10: Очистка и перезапуск

Если нужно начать с чистого листа:

```bash
# Остановить и удалить все контейнеры и volumes
docker compose down -v

# Удалить все образы (опционально)
docker compose down --rmi all

# Запустить заново
docker compose up --build
```

## Полезные команды

```bash
# Просмотр логов в реальном времени
docker compose logs -f [service_name]

# Выполнить команду в контейнере
docker compose exec [service_name] sh

# Перезапустить конкретный сервис
docker compose restart [service_name]

# Проверить использование ресурсов
docker stats

# Подключиться к базе данных
docker compose exec auth-db psql -U postgres -d bank
```

## Проверка работоспособности

### Health checks

```bash
# Проверить health endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### Проверка метрик Prometheus

Откройте http://localhost:9090 и проверьте метрики сервисов.

## Следующие шаги

После успешного запуска:

1. Протестируйте регистрацию и вход через фронтенд
2. Проверьте работу с банковскими данными (требует подключения банка)
3. Изучите Swagger документацию для каждого сервиса
4. Проверьте логи на наличие ошибок

## Примечания

- При первом запуске сборка может занять несколько минут
- Базы данных автоматически создаются при первом запуске
- JWT токены хранятся в localStorage браузера
- Для production окружения используйте сильные пароли и секретные ключи

