# Vendra backend — convenience targets. Run from backend/.
.PHONY: help db-push db-seed db-reset run build test openapi spotless

help:
	@echo "Vendra backend targets:"
	@echo "  make db-push    - apply Supabase migrations (supabase db push)"
	@echo "  make db-seed    - run the idempotent seed"
	@echo "  make db-reset   - local: reset DB (replays migrations + seed)"
	@echo "  make run        - run the Spring Boot API (localhost:8080, Swagger /docs)"
	@echo "  make build      - mvn package (skip tests)"
	@echo "  make test       - mvn test"
	@echo "  make openapi    - regenerate ../docs/openapi.json"
	@echo "  make spotless   - apply code formatting"

db-push:
	cd supabase && supabase db push

db-seed:
	cd supabase && supabase db execute -f seed.sql

db-reset:
	cd supabase && supabase db reset

run:
	cd server && ./mvnw spring-boot:run

build:
	cd server && ./mvnw -DskipTests package

test:
	cd server && ./mvnw test

openapi:
	cd server && ./mvnw test -Dtest=OpenApiExportTest

spotless:
	cd server && ./mvnw spotless:apply
