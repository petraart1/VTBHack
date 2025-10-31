.PHONY: test

SERVICES = authService bankService radarService

up:
	docker-compose --env-file=.env up --build

down:
	docker-compose down

delete:
	docker compose down --volumes

psql-auth-db:
	psql -h localhost -U postgres -d bank

psql-bank-db:
	psql -h localhost -U postgres -d bank -p 5433

psql-radar-db:
	psql -h localhost -U postgres -d messenger -p 5434

test:
	@for dir in $(SERVICES); do \
        		echo "🚀 Запуск тестов в $$dir..."; \
        		(cd $$dir && ./gradlew test --no-daemon --parallel &); \
        	done; \
        	wait