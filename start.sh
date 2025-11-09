#!/bin/bash

# Скрипт для быстрого запуска проекта

set -e

echo "🚀 Запуск проекта Мультибанк..."

# Проверка наличия .env файла
if [ ! -f .env ]; then
    echo "⚠️  Файл .env не найден. Создаю шаблон..."
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

# JWT секрет
JWT_SECRET=34cf09cbe5571911e487cafcd49b72946a133fe881c3de75ab2d412f226ef06f

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
EOF
    echo "✅ Файл .env создан. Вы можете отредактировать его при необходимости."
fi

# Проверка наличия .env файла для фронтенда
if [ ! -f frontend/.env ]; then
    echo "⚠️  Файл frontend/.env не найден. Создаю шаблон..."
    cat > frontend/.env << 'EOF'
# API Base URL
VITE_API_BASE_URL=http://localhost:80
EOF
    echo "✅ Файл frontend/.env создан."
fi

# Проверка Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker не установлен. Установите Docker для продолжения."
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose не установлен. Установите Docker Compose для продолжения."
    exit 1
fi

echo "📦 Запуск Docker Compose..."
docker compose up --build -d

echo "⏳ Ожидание запуска сервисов..."
sleep 10

echo "🔍 Проверка статуса сервисов..."
docker compose ps

echo ""
echo "✅ Проект запущен!"
echo ""
echo "📍 Доступные сервисы:"
echo "   - Frontend:        http://localhost:3000"
echo "   - API Gateway:     http://localhost:80"
echo "   - AuthService:     http://localhost:8080"
echo "   - BankService:     http://localhost:8081"
echo "   - RadarService:    http://localhost:8082"
echo "   - Prometheus:      http://localhost:9090"
echo ""
echo "📚 Swagger UI:"
echo "   - AuthService:     http://localhost:8080/auth/swagger-ui.html"
echo "   - BankService:     http://localhost:8081/bank/swagger-ui.html"
echo "   - RadarService:    http://localhost:8082/radar/swagger-ui.html"
echo ""
echo "📝 Просмотр логов: docker compose logs -f"
echo "🛑 Остановка:     docker compose down"
echo ""

