# Ansible Deployment для BankService

## 🚀 Quick Start

### 1. Настроить inventory
```bash
# Отредактировать IP адреса серверов
vim inventory/hosts.yml
```

### 2. Создать vault с секретами
```bash
# Создать зашифрованный файл
ansible-vault create inventory/group_vars/vault.yml

# Добавить секреты (см. пример в vault.yml)
```

### 3. Первоначальная настройка сервера
```bash
# Установить Docker, Docker Compose, настроить firewall
ansible-playbook playbooks/setup-server.yml --ask-become-pass
```

### 4. Deploy backend
```bash
# Production deployment
ansible-playbook playbooks/deploy-backend.yml --ask-vault-pass

# Staging deployment
ansible-playbook playbooks/deploy-backend.yml --limit staging --ask-vault-pass
```

## 📋 Доступные Playbooks

| Playbook | Описание | Использование |
|----|----|---|
| `setup-server.yml` | Первоначальная настройка VM | Один раз для новой VM |
| `deploy-backend.yml` | Deploy BankService | Каждый deployment |
| `rollback.yml` | Rollback к предыдущей версии | При проблемах |
| `update-secrets.yml` | Обновление секретов | Когда нужно поменять secrets |

## 🔐 Работа с Ansible Vault

### Создать vault файл
```bash
ansible-vault create inventory/group_vars/vault.yml
```

### Редактировать vault
```bash
ansible-vault edit inventory/group_vars/vault.yml
```

### Просмотр vault
```bash
ansible-vault view inventory/group_vars/vault.yml
```

### Запуск playbook с vault
```bash
# Интерактивный ввод пароля
ansible-playbook playbooks/deploy-backend.yml --ask-vault-pass

# Использовать файл с паролем
ansible-playbook playbooks/deploy-backend.yml --vault-password-file ~/.ansible_vault_pass
```

## 🔍 Troubleshooting

### Проверка подключения
```bash
ansible all -m ping
```

### Запуск с отладкой
```bash
ansible-playbook playbooks/deploy-backend.yml -vvv
```

### Проверка синтаксиса
```bash
ansible-playbook playbooks/deploy-backend.yml --syntax-check
```

### Dry run
```bash
ansible-playbook playbooks/deploy-backend.yml --check
```

## 📖 Документация

- [Ansible Documentation](https://docs.ansible.com/)
- [Ansible Vault](https://docs.ansible.com/ansible/latest/user_guide/vault.html)
- [Docker Compose](https://docs.docker.com/compose/)

---

## ✅ CHECKLIST ДЛЯ НАСТРОЙКИ

```bash
# 1. Создать структуру
mkdir -p ansible/{inventory/group_vars,playbooks,templates}

# 2. Скопировать файлы из этого README

# 3. Создать vault с секретами
ansible-vault create ansible/inventory/group_vars/vault.yml

# 4. Отредактировать hosts.yml с реальными IP адресами
vim ansible/inventory/hosts.yml

# 5. Запустить setup
ansible-playbook -i ansible/inventory/hosts.yml ansible/playbooks/setup-server.yml --ask-become-pass

# 6. Deploy!
ansible-playbook -i ansible/inventory/hosts.yml ansible/playbooks/deploy-backend.yml --ask-vault-pass
```

**Готово!** 🎉

## 🔧 Структура проекта

```
ansible/
├── inventory/
│   ├── hosts.yml                    # Серверы для deployment
│   └── group_vars/
│       ├── production.yml           # Production переменные
│       ├── staging.yml              # Staging переменные
│       └── vault.yml                # Зашифрованные секреты
├── playbooks/
│   ├── deploy-backend.yml           # Основной deployment
│   ├── setup-server.yml             # Настройка сервера
│   ├── rollback.yml                 # Rollback к предыдущей версии
│   └── update-secrets.yml           # Обновление секретов
├── templates/
│   ├── docker-compose.prod.j2       # Шаблон docker-compose
│   └── env.j2                       # Шаблон .env файла
├── ansible.cfg                      # Конфигурация Ansible
└── README.md                        # Эта документация
```
