# Makefile for BankService Ansible Deployment

.PHONY: help setup deploy deploy-staging rollback update-secrets check ping vault-create vault-edit vault-view

# Default target
help:
	@echo "Available targets:"
	@echo "  setup          - Initial server setup"
	@echo "  deploy         - Deploy to production"
	@echo "  deploy-staging - Deploy to staging"
	@echo "  rollback       - Rollback to previous version"
	@echo "  update-secrets - Update secrets without full redeploy"
	@echo "  check          - Dry run deployment"
	@echo "  ping           - Test connection to servers"
	@echo "  vault-create   - Create new vault file"
	@echo "  vault-edit     - Edit vault file"
	@echo "  vault-view     - View vault file"

# Initial server setup
setup:
	ansible-playbook playbooks/setup-server.yml --ask-become-pass

# Deploy to production
deploy:
	ansible-playbook playbooks/deploy-backend.yml --ask-vault-pass

# Deploy to staging
deploy-staging:
	ansible-playbook playbooks/deploy-backend.yml --limit staging --ask-vault-pass

# Rollback
rollback:
	ansible-playbook playbooks/rollback.yml --ask-vault-pass

# Update secrets
update-secrets:
	ansible-playbook playbooks/update-secrets.yml --ask-vault-pass

# Dry run
check:
	ansible-playbook playbooks/deploy-backend.yml --check --ask-vault-pass

# Test connection
ping:
	ansible all -m ping

# Vault operations
vault-create:
	ansible-vault create inventory/group_vars/vault.yaml

vault-edit:
	ansible-vault edit inventory/group_vars/vault.yaml

vault-view:
	ansible-vault view inventory/group_vars/vault.yaml

# Vault management
setup-vault:
	ansible-playbook playbooks/setup-vault.yaml --ask-become-pass

load-secrets:
	ansible-playbook playbooks/load-secrets.yaml --ask-vault-pass

vault-status:
	curl -H "X-Vault-Token: $(VAULT_TOKEN)" http://localhost:8200/v1/sys/health || echo "Vault not running"
