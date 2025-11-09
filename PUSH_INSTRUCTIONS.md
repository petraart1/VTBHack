# Инструкция по пушу в отдельную ветку

## Быстрый способ

```bash
# 1. Создать новую ветку и переключиться на неё
git checkout -b feature/frontend-api-integration

# 2. Добавить все изменения
git add .

# 3. Создать коммит с описанием изменений
git commit -m "feat: интеграция фронтенда с бэкенд API

- Добавлен API клиент для работы с authService и bankService
- Обновлен AuthContext для работы с JWT токенами
- Обновлены страницы Login, Register, Dashboard, Transactions
- Исправлены Dockerfile для устранения конфликтов Gradle кеша
- Добавлены скрипты для быстрого запуска и тестирования
- Обновлена документация"

# 4. Запушить ветку в удаленный репозиторий
git push -u origin feature/frontend-api-integration
```

## Альтернативные варианты

### Если хотите выбрать другое имя ветки:

```bash
# Например, для исправления багов
git checkout -b fix/gradle-cache-conflict

# Или для документации
git checkout -b docs/quickstart-guide
```

### Если хотите добавить файлы выборочно:

```bash
# Добавить только измененные файлы
git add backend/authService/Dockerfile backend/bankService/Dockerfile backend/radarService/Dockerfile

# Добавить только новые файлы
git add frontend/src/lib/api.ts frontend/src/lib/api/ QUICKSTART.md start.sh test-api.sh

# Или добавить все изменения
git add .
```

### Если нужно обновить существующую ветку:

```bash
# Переключиться на существующую ветку
git checkout feature/frontend-api-integration

# Добавить изменения
git add .

# Создать коммит
git commit -m "описание изменений"

# Запушить
git push
```

## Создание Pull Request

После пуша ветки:

1. Откройте репозиторий в браузере (GitHub/GitLab)
2. Нажмите "New Pull Request" или "Create Merge Request"
3. Выберите базовую ветку (обычно `main`) и вашу ветку
4. Заполните описание изменений
5. Создайте Pull Request

## Полезные команды

```bash
# Посмотреть статус изменений
git status

# Посмотреть список веток
git branch -a

# Посмотреть изменения перед коммитом
git diff

# Отменить изменения в файле (если нужно)
git restore <файл>

# Удалить локальную ветку (после мерджа)
git branch -d feature/frontend-api-integration

# Удалить удаленную ветку
git push origin --delete feature/frontend-api-integration
```

