#!/bin/bash

# Скрипт для тестирования API

set -e

API_BASE="http://localhost:80"
AUTH_ENDPOINT="$API_BASE/api/v1/auth/auth"

echo "🧪 Тестирование API Мультибанк"
echo ""

# Цвета для вывода
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Функция для проверки ответа
check_response() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ Успешно${NC}"
    else
        echo -e "${RED}❌ Ошибка${NC}"
    fi
}

# Тест 1: Регистрация пользователя
echo "1️⃣  Тест регистрации пользователя..."
REGISTER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$AUTH_ENDPOINT/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Тест",
    "lastName": "Пользователь",
    "birthOfDate": "1990-01-01",
    "email": "test'$(date +%s)'@example.com",
    "password": "test123456"
  }')

HTTP_CODE=$(echo "$REGISTER_RESPONSE" | tail -n1)
BODY=$(echo "$REGISTER_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✅ Регистрация успешна${NC}"
    echo "   Ответ: $BODY"
else
    echo -e "${RED}❌ Ошибка регистрации (HTTP $HTTP_CODE)${NC}"
    echo "   Ответ: $BODY"
fi

echo ""

# Тест 2: Вход в систему
echo "2️⃣  Тест входа в систему..."
TEST_EMAIL="test$(date +%s)@example.com"
TEST_PASSWORD="test123456"

# Сначала регистрируем пользователя
curl -s -X POST "$AUTH_ENDPOINT/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"firstName\": \"Тест\",
    \"lastName\": \"Пользователь\",
    \"birthOfDate\": \"1990-01-01\",
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\"
  }" > /dev/null

sleep 1

# Теперь входим
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$AUTH_ENDPOINT/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\"
  }")

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✅ Вход успешен${NC}"
    TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    if [ -n "$TOKEN" ]; then
        echo "   Токен получен: ${TOKEN:0:50}..."
        echo ""
        
        # Тест 3: Получение списка счетов
        echo "3️⃣  Тест получения списка счетов..."
        ACCOUNTS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_BASE/api/v1/bank/api/v1/bank/accounts" \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json")
        
        ACCOUNTS_HTTP_CODE=$(echo "$ACCOUNTS_RESPONSE" | tail -n1)
        ACCOUNTS_BODY=$(echo "$ACCOUNTS_RESPONSE" | sed '$d')
        
        if [ "$ACCOUNTS_HTTP_CODE" -eq 200 ]; then
            echo -e "${GREEN}✅ Список счетов получен${NC}"
            echo "   Ответ: $ACCOUNTS_BODY"
        else
            echo -e "${YELLOW}⚠️  Ошибка получения счетов (HTTP $ACCOUNTS_HTTP_CODE)${NC}"
            echo "   Это нормально, если банк еще не подключен"
            echo "   Ответ: $ACCOUNTS_BODY"
        fi
    else
        echo -e "${RED}❌ Токен не найден в ответе${NC}"
    fi
else
    echo -e "${RED}❌ Ошибка входа (HTTP $HTTP_CODE)${NC}"
    echo "   Ответ: $BODY"
fi

echo ""
echo "🏁 Тестирование завершено"
echo ""
echo "💡 Для более детального тестирования используйте:"
echo "   - Swagger UI: http://localhost:8080/auth/swagger-ui.html"
echo "   - Или откройте фронтенд: http://localhost:3000"

