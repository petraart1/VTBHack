# Мультибанк - Банковское приложение для управления счетами

## Описание проекта

Мультибанк — это комплексное банковское приложение, которое позволяет пользователям управлять счетами из разных банков в едином интерфейсе. Проект был разработан для хакатона ВТБ и включает веб-приложение, мобильное Android-приложение и микросервисную архитектуру бэкенда.

### Основные возможности

- Безопасная аутентификация через OAuth2 и JWT
- Управление счетами из нескольких банков (VBank, ABank, SBank)
- Аналитика транзакций с визуализацией расходов
- Умный радар подписок — автоматическое обнаружение и мониторинг подписок
- Кроссплатформенность — веб и мобильное приложение
- Уведомления о важных событиях и изменениях

## Архитектура проекта

Проект построен на микросервисной архитектуре с разделением на следующие компоненты:

```
VTBHack/
├── backend/              # Микросервисы бэкенда
│   ├── authService/      # Сервис аутентификации
│   ├── bankService/      # Основной банковский сервис
│   ├── radarService/     # Сервис аналитики подписок
│   └── _infrastructure/  # Инфраструктурные компоненты
├── frontend/             # Веб-приложение (React + TypeScript)
├── mobile/               # Android приложение (Kotlin + Jetpack Compose)
└── ansible/              # Автоматизация развертывания
```

## Технологический стек

### Backend

- Язык: Java 17+
- Фреймворк: Spring Boot 3.5.7
- База данных: PostgreSQL 16
- Кеширование: Redis
- Миграции: Liquibase
- API Gateway: Nginx
- Мониторинг: Prometheus
- Безопасность: JWT, OAuth2
- Устойчивость: Resilience4j (Circuit Breaker, Retry)

### Frontend

- Язык: TypeScript
- Фреймворк: React 18
- Сборщик: Vite
- Стилизация: Tailwind CSS
- Роутинг: React Router
- Аутентификация: Supabase

### Mobile

- Язык: Kotlin
- UI: Jetpack Compose
- Архитектура: Clean Architecture, MVVM
- DI: Hilt
- Networking: Retrofit 2
- Локальная БД: Room
- Навигация: Navigation Compose

### Infrastructure

- Контейнеризация: Docker, Docker Compose
- Оркестрация: Ansible
- Reverse Proxy: Nginx
- Мониторинг: Prometheus

## Микросервисы

### 1. AuthService (Порт: 8080)

Сервис аутентификации и авторизации пользователей. Отвечает за регистрацию, вход, генерацию JWT токенов и интеграцию с OAuth2 провайдерами.

Основные функции:
- Регистрация и вход пользователей
- OAuth2 авторизация через GitHub
- Генерация и валидация JWT токенов
- Управление refresh токенами
- CORS настройки

Технологии:
- Spring Security
- JWT (JSON Web Tokens)
- OAuth2 Client
- PostgreSQL

API документация доступна по адресу: `http://localhost:8080/auth/swagger-ui.html`

### 2. BankService (Порт: 8081)

Основной банковский сервис для работы со счетами и транзакциями. Интегрируется с внешними банковскими API и предоставляет единый интерфейс для работы с несколькими банками.

Основные функции:
- Управление банковскими счетами
- Просмотр транзакций
- Интеграция с внешними банками (VBank, ABank, SBank)
- Кеширование данных в Redis
- Обработка переводов

Технологии:
- Spring Data JPA
- Redis для кеширования
- Resilience4j для устойчивости к сбоям
- Liquibase для миграций
- WebClient для интеграций

API документация доступна по адресу: `http://localhost:8081/bank/swagger-ui.html`

### 3. RadarService (Порт: 8082)

Сервис аналитики и обнаружения подписок. Анализирует транзакции пользователей и автоматически определяет регулярные платежи, отслеживает изменения цен и предоставляет рекомендации.

Основные функции:
- Автоматическое обнаружение подписок из транзакций
- Мониторинг изменения цен подписок
- Рекомендации альтернативных сервисов
- Генерация уведомлений об изменениях
- Аналитика расходов на подписки

Технологии:
- Spring Scheduling для фоновых задач
- Caffeine Cache для кеширования
- ML-алгоритмы для обнаружения паттернов
- Интеграция с BankService

API документация доступна по адресу: `http://localhost:8082/radar/swagger-ui.html`

## Установка и запуск

### Требования

Для работы проекта потребуется:
- Docker и Docker Compose
- Java 17+ (для локальной разработки)
- Node.js 18+ (для frontend)
- Gradle 8+ (для backend сервисов)
- Android Studio (для mobile)

### Быстрый старт с Docker Compose

**Самый простой способ:**

```bash
# Запустить скрипт автоматической настройки и запуска
./start.sh
```

Скрипт автоматически:
- Создаст необходимые `.env` файлы
- Запустит все сервисы
- Покажет статус и доступные URL

**Или вручную:**

1. Клонируйте репозиторий:
```bash
git clone <repository-url>
cd VTBHack
```

2. Создайте файл `.env` в корне проекта:
```env
POSTGRES_DB=bank
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres123
DB_PORT=5432
AUTH_DB_CONTAINER_NAME=auth-db
BANK_DB_CONTAINER_NAME=bank-db
RADAR_DB_CONTAINER_NAME=radar-db
JWT_SECRET=34cf09cbe5571911e487cafcd49b72946a133fe881c3de75ab2d412f226ef06f
REDIS_HOST=redis
REDIS_PORT=6379
```

3. Создайте файл `frontend/.env`:
```env
VITE_API_BASE_URL=http://localhost:80
```

4. Запустите все сервисы:
```bash
docker compose up --build
```

5. Проверьте статус сервисов:
```bash
docker compose ps
```

### Тестирование API

После запуска сервисов можно протестировать API:

```bash
# Запустить автоматические тесты API
./test-api.sh
```

Или вручную через curl (см. подробную инструкцию в `QUICKSTART.md`).

### Доступные сервисы

После запуска доступны следующие сервисы:

- Frontend: http://localhost:3000
- Nginx (API Gateway): http://localhost:80
- AuthService: http://localhost:8080
- BankService: http://localhost:8081
- RadarService: http://localhost:8082
- Prometheus: http://localhost:9090

### Локальная разработка

Если нужно запускать сервисы локально без Docker:

#### Backend сервисы

```bash
# AuthService
cd backend/authService
./gradlew bootRun

# BankService
cd backend/bankService
./gradlew bootRun

# RadarService
cd backend/radarService
./gradlew bootRun
```

#### Frontend

```bash
cd frontend
npm install
npm run dev
```

#### Mobile

1. Откройте проект в Android Studio
2. Синхронизируйте Gradle
3. Запустите приложение на эмуляторе или устройстве

## Структура проекта

### Backend

```
backend/
├── authService/
│   ├── src/main/java/com/bank/
│   │   ├── controller/      # REST контроллеры
│   │   ├── service/          # Бизнес-логика
│   │   ├── security/         # JWT и Security конфигурация
│   │   └── entity/           # JPA сущности
│   └── src/main/resources/
│       └── application.yaml   # Конфигурация
│
├── bankService/
│   ├── src/main/java/com/bank/
│   │   ├── controller/       # REST API
│   │   ├── service/          # Бизнес-логика
│   │   ├── repository/       # Data Access Layer
│   │   ├── dto/              # Data Transfer Objects
│   │   └── client/           # Внешние API клиенты
│   └── src/main/resources/
│       ├── application.yaml
│       └── db/changelog/     # Liquibase миграции
│
├── radarService/
│   ├── src/main/java/com/bank/
│   │   ├── controller/       # REST API
│   │   ├── service/          # Логика обнаружения подписок
│   │   ├── scheduler/        # Фоновые задачи
│   │   └── entity/           # Доменные модели
│   └── src/main/resources/
│       ├── application.yaml
│       └── db/changelog/
│
└── _infrastructure/
    ├── nginx/                # Nginx конфигурация
    ├── prometheus/           # Prometheus конфигурация
    └── redis/                # Redis конфигурация
```

### Frontend

```
frontend/
├── src/
│   ├── components/           # React компоненты
│   ├── pages/               # Страницы приложения
│   ├── contexts/            # React Context (Auth)
│   ├── lib/                 # Утилиты (Supabase)
│   └── types/               # TypeScript типы
├── public/                  # Статические файлы
└── supabase/
    └── migrations/          # Supabase миграции
```

### Mobile

```
mobile/
├── app/src/main/java/com/example/
│   ├── data/                # Слой данных
│   │   ├── dto/             # DTO модели
│   │   ├── local/           # Room база данных
│   │   └── remote/          # Retrofit API
│   ├── domain/              # Бизнес-логика
│   ├── repository/          # Реализация репозиториев
│   └── ui/                  # Jetpack Compose UI
│       ├── screens/         # Экраны приложения
│       └── navigation/      # Навигация
└── gradle/
    └── libs.versions.toml   # Управление зависимостями
```

## Безопасность

### Аутентификация

В проекте используется JWT токены с access и refresh токенами для безопасной аутентификации. Также реализована интеграция с OAuth2 через GitHub. Для production окружения необходимо настроить HTTPS. CORS настройки настроены для безопасного взаимодействия между фронтендом и бэкендом.

### Конфигурация безопасности

Все секреты хранятся в переменных окружения:
- `JWT_SECRET` — секретный ключ для подписи JWT
- `POSTGRES_PASSWORD` — пароль базы данных
- `GITHUB_CLIENT_SECRET` — секрет OAuth2 приложения

Важно: никогда не коммитьте секреты в репозиторий! Используйте `.env` файлы и Ansible Vault для production окружения.

## Мониторинг и логирование

### Prometheus

Метрики доступны по адресу: `http://localhost:9090`

### Actuator Endpoints

Каждый сервис предоставляет следующие endpoints для мониторинга:

- `/actuator/health` — проверка здоровья сервиса
- `/actuator/metrics` — метрики приложения
- `/actuator/prometheus` — метрики для Prometheus
- `/actuator/info` — информация о приложении

### Логирование

Все сервисы используют структурированное логирование с настраиваемыми уровнями через `application.yaml`. Уровни логирования можно настроить для каждого пакета отдельно.

## Тестирование

### Backend

Для запуска всех тестов бэкенда:
```bash
cd backend
make test
```

Для запуска тестов конкретного сервиса:
```bash
cd backend/authService
./gradlew test
```

### Frontend

```bash
cd frontend
npm run test
```

### Mobile

```bash
# Unit тесты
./gradlew test

# Instrumented тесты
./gradlew connectedAndroidTest
```

## Развертывание

### Ansible Deployment

Проект включает Ansible playbooks для автоматического развертывания на серверах. Подробная документация находится в `ansible/README.md`.

Быстрый старт:

```bash
# 1. Настроить inventory
vim ansible/inventory/hosts.yaml

# 2. Создать vault с секретами
ansible-vault create ansible/inventory/group_vars/vault.yaml

# 3. Развернуть
ansible-playbook ansible/playbooks/deploy-backend.yml --ask-vault-pass
```

### Docker Compose Production

Для production окружения используйте шаблоны из `ansible/templates/`. Они автоматически генерируют docker-compose файлы с правильными настройками для production.

## API Документация

Все сервисы предоставляют Swagger UI для интерактивной документации API:

- AuthService: http://localhost:8080/auth/swagger-ui.html
- BankService: http://localhost:8081/bank/swagger-ui.html
- RadarService: http://localhost:8082/radar/swagger-ui.html

## CI/CD

Рекомендуется настроить CI/CD pipeline для автоматического запуска тестов, сборки Docker образов и развертывания на staging/production окружениях.

## Дополнительная документация

- [QUICKSTART.md](QUICKSTART.md) — подробная инструкция по локальному запуску и тестированию
- [Mobile README](mobile/README.md) — документация Android приложения
- [Ansible README](ansible/README.md) — документация по развертыванию
- [RadarService README](backend/radarService/README.md) — подробная документация сервиса аналитики

## Участие в разработке

1. Создайте feature branch: `git checkout -b feature/amazing-feature`
2. Внесите изменения и закоммитьте: `git commit -m 'Add amazing feature'`
3. Запушьте в branch: `git push origin feature/amazing-feature`
4. Создайте Pull Request

## Лицензия

MIT License

## Команда

Проект разработан для хакатона ВТБ.

## Поддержка

При возникновении проблем:

1. Проверьте логи сервисов: `docker compose logs <service-name>`
2. Убедитесь, что все переменные окружения установлены
3. Проверьте статус сервисов: `docker compose ps`
4. Обратитесь к документации конкретного сервиса

---

Версия: 1.0.0  
Последнее обновление: 2025
