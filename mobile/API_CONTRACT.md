# API Контракт - Frontend и Backend

Этот документ описывает контракт API между Frontend (Android приложение) и Backend. Используйте его для синхронизации между командами.

## Базовый URL

```
https://api.example.com/  // ВРЕМЕННО: Заменить на реальный адрес Backend API
```

## Аутентификация

Все запросы к защищенным endpoints требуют заголовок `Authorization` с bearer токеном:

```
Authorization: Bearer <access_token>
```

## Endpoints

### 1. Обмен кода авторизации на токен

**POST** `/auth/token`

**Request Body:**
```json
{
  "code": "string",
  "redirect_uri": "string",
  "client_id": "string",
  "code_verifier": "string",
  "grant_type": "authorization_code"
}
```

**Response:**
```json
{
  "access_token": "string",
  "refresh_token": "string",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "string"
}
```

**Status Codes:**
- `200 OK` - Успешный обмен кода на токен
- `400 Bad Request` - Неверный запрос
- `401 Unauthorized` - Неверный код авторизации

---

### 2. Обновление токена

**POST** `/auth/refresh`

**Request Body:**
```json
{
  "refresh_token": "string",
  "client_id": "string",
  "grant_type": "refresh_token"
}
```

**Response:**
```json
{
  "access_token": "string",
  "refresh_token": "string",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "string"
}
```

**Status Codes:**
- `200 OK` - Успешное обновление токена
- `401 Unauthorized` - Неверный refresh token

---

### 3. Получение списка счетов

**GET** `/accounts`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "accounts": [
    {
      "id": "string",
      "bank_id": "string",
      "account_number": "string",
      "account_type": "checking|savings|credit|debit_card|credit_card",
      "balance": "string",
      "currency": "string",
      "name": "string",
      "is_active": true
    }
  ]
}
```

**Status Codes:**
- `200 OK` - Успешное получение списка счетов
- `401 Unauthorized` - Токен недействителен
- `500 Internal Server Error` - Ошибка сервера

---

### 4. Получение транзакций по счету

**GET** `/accounts/{accountId}/transactions`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Query Parameters:**
- `from` (optional) - Дата начала в формате ISO 8601 (например: "2024-01-01T00:00:00.000Z")
- `to` (optional) - Дата окончания в формате ISO 8601 (например: "2024-01-31T23:59:59.999Z")

**Path Parameters:**
- `accountId` - ID счета

**Response:**
```json
{
  "transactions": [
    {
      "id": "string",
      "account_id": "string",
      "bank_id": "string",
      "amount": "string",
      "currency": "string",
      "type": "income|expense|transfer",
      "category": "string",
      "description": "string",
      "date": "2024-01-15T10:30:00.000Z",
      "merchant_name": "string",
      "reference": "string"
    }
  ]
}
```

**Status Codes:**
- `200 OK` - Успешное получение транзакций
- `401 Unauthorized` - Токен недействителен
- `404 Not Found` - Счет не найден
- `500 Internal Server Error` - Ошибка сервера

---

### 5. Получение всех транзакций

**GET** `/transactions`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Query Parameters:**
- `from` (optional) - Дата начала в формате ISO 8601
- `to` (optional) - Дата окончания в формате ISO 8601

**Response:**
```json
{
  "transactions": [
    {
      "id": "string",
      "account_id": "string",
      "bank_id": "string",
      "amount": "string",
      "currency": "string",
      "type": "income|expense|transfer",
      "category": "string",
      "description": "string",
      "date": "2024-01-15T10:30:00.000Z",
      "merchant_name": "string",
      "reference": "string"
    }
  ]
}
```

**Status Codes:**
- `200 OK` - Успешное получение транзакций
- `401 Unauthorized` - Токен недействителен
- `500 Internal Server Error` - Ошибка сервера

---

## Модели данных

### AccountDto

| Поле | Тип | Обязательное | Описание |
|------|-----|--------------|----------|
| id | string | Да | Уникальный идентификатор счета |
| bank_id | string | Да | Идентификатор банка |
| account_number | string | Да | Номер счета |
| account_type | string | Да | Тип счета: checking, savings, credit, debit_card, credit_card |
| balance | string | Да | Баланс счета (в формате строки для точности) |
| currency | string | Да | Валюта (например: RUB, USD, EUR) |
| name | string | Нет | Название счета |
| is_active | boolean | Да | Активен ли счет |

### TransactionDto

| Поле | Тип | Обязательное | Описание |
|------|-----|--------------|----------|
| id | string | Да | Уникальный идентификатор транзакции |
| account_id | string | Да | Идентификатор счета |
| bank_id | string | Да | Идентификатор банка |
| amount | string | Да | Сумма транзакции (положительная для income, отрицательная для expense) |
| currency | string | Да | Валюта транзакции |
| type | string | Да | Тип транзакции: income, expense, transfer |
| category | string | Нет | Категория транзакции |
| description | string | Да | Описание транзакции |
| date | string | Да | Дата и время в формате ISO 8601 |
| merchant_name | string | Нет | Название торговой точки |
| reference | string | Нет | Референс транзакции |

### AuthResponseDto

| Поле | Тип | Обязательное | Описание |
|------|-----|--------------|----------|
| access_token | string | Да | Токен доступа |
| refresh_token | string | Нет | Токен обновления |
| token_type | string | Да | Тип токена (обычно "Bearer") |
| expires_in | number | Да | Время жизни токена в секундах |
| scope | string | Нет | Область доступа токена |

## Форматы данных

### Дата и время
Все даты и время передаются в формате ISO 8601:
```
2024-01-15T10:30:00.000Z
```

### Валюта
Валюта указывается трехбуквенным кодом (ISO 4217):
- `RUB` - Российский рубль
- `USD` - Доллар США
- `EUR` - Евро

### Типы счетов
- `checking` - Текущий счет
- `savings` - Сберегательный счет
- `credit` - Кредитный счет
- `debit_card` - Дебетовая карта
- `credit_card` - Кредитная карта

### Типы транзакций
- `income` - Доход
- `expense` - Расход
- `transfer` - Перевод

## Обработка ошибок

Все ошибки должны возвращаться в стандартном формате:

```json
{
  "error": "string",
  "error_description": "string",
  "error_code": "string"
}
```

### Стандартные коды ошибок

- `400` - Bad Request - Неверный формат запроса
- `401` - Unauthorized - Требуется аутентификация или токен недействителен
- `403` - Forbidden - Доступ запрещен
- `404` - Not Found - Ресурс не найден
- `500` - Internal Server Error - Внутренняя ошибка сервера

## Примечания для Backend команды

1. Все endpoints должны возвращать данные в формате, указанном в этом документе
2. При изменении API контракта необходимо уведомить Frontend команду
3. Рекомендуется версионирование API (например: `/v1/accounts`)
4. Все строковые поля должны поддерживать UTF-8 кодировку для корректной работы с кириллицей
5. Даты должны быть в формате ISO 8601 с указанием timezone (UTC рекомендуется)

## Примечания для Frontend команды

1. Все DTO модели в пакете `data/dto` должны соответствовать этому контракту
2. При изменении API контракта Backend командой необходимо обновить:
   - DTO модели
   - API интерфейсы в `BankApiService`
   - Обработку ответов в репозиториях
3. Используйте Mock данные для разработки до подключения реального Backend
4. При работе с реальным API используйте обработку ошибок и retry логику


