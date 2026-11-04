.DEFAULT_GOAL := help
.PHONY: help setup env-check up up-fast-sla down stop status logs test smoke manual postman clean-data

help:
	@powershell -NoProfile -Command "Write-Host 'Targets: setup up up-fast-sla down stop status logs test smoke manual postman clean-data'"
	@powershell -NoProfile -Command "Write-Host 'setup       Crea .env desde .env.example si no existe'"
	@powershell -NoProfile -Command "Write-Host 'up          Construye e inicia todos los contenedores'"
	@powershell -NoProfile -Command "Write-Host 'up-fast-sla Inicia Triage con SLA CRITICO de 1 minuto para probar escalamiento'"
	@powershell -NoProfile -Command "Write-Host 'smoke       Ejecuta pruebas HTTP manuales contra los servicios iniciados'"
	@powershell -NoProfile -Command "Write-Host 'manual      Muestra URLs y pasos para importar/ejecutar Postman'"
	@powershell -NoProfile -Command "Write-Host 'test        Ejecuta las pruebas automatizadas Gradle'"
	@powershell -NoProfile -Command "Write-Host 'down/stop   Detiene contenedores, conserva volumenes'"
	@powershell -NoProfile -Command "Write-Host 'clean-data  Elimina contenedores y volumenes (destructivo)'"

setup:
	@powershell -NoProfile -Command "if (Test-Path .env) { Write-Host '.env ya existe; no se modifico' } else { Copy-Item .env.example .env; Write-Host 'Copia creada: edita .env y configura API_KEY, DB_PASSWORD y LLM_API_KEY' }"

env-check:
	@powershell -NoProfile -Command "if (!(Test-Path .env)) { Write-Error 'Falta .env. Ejecuta make setup y configura sus valores.'; exit 1 }"
	@docker compose config --quiet

up: env-check
	@docker compose up --build -d

up-fast-sla: env-check
	@powershell -NoProfile -Command "$$env:TRIAGE_SLA_CRITICO_MINUTES='1'; $$env:TRIAGE_SCHEDULER_DELAY_MS='1000'; docker compose up --build -d --force-recreate triage"

down:
	@docker compose down

stop: down

status:
	@docker compose ps --all

logs:
	@docker compose logs -f --tail=100

test:
	@powershell -NoProfile -ExecutionPolicy Bypass -Command "& .\gradlew.bat test bootJar :assistant-service:bootJar"

smoke:
	@powershell -NoProfile -ExecutionPolicy Bypass -File scripts\smoke-test.ps1

manual:
	@powershell -NoProfile -Command "Write-Host 'Importa postman\VidaCruz.postman_collection.json y postman\VidaCruz Local.postman_environment.json en Postman.'"
	@powershell -NoProfile -Command "Write-Host 'Selecciona el entorno VidaCruz Local, configura apiKey con el valor API_KEY de .env y ejecuta la colección en orden.'"
	@powershell -NoProfile -Command "Write-Host 'Triage: http://localhost:8080 | Asistente: http://localhost:8081 | Smoke test: make smoke'"

postman: manual

clean-data:
	@powershell -NoProfile -Command "$$confirmation = Read-Host 'Esto borrará los volúmenes PostgreSQL y Redis del proyecto. Escribe BORRAR para continuar'; if ($$confirmation -cne 'BORRAR') { Write-Host 'Cancelado'; exit 1 }; docker compose down -v"
