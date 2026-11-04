# ADR-001: Servicios separados y eventos de triaje

- **Estado:** Aceptado
- **Contexto:** Triage debe seguir registrando y clasificando aunque el proveedor de IA o el asistente fallen. El asistente necesita reconstruir conteos activos sin consultar la API de Triage.
- **Decisión:** Mantener dos aplicaciones ejecutables independientes. Triage escribe cambios clínicos, auditoría y outbox en una transacción R2DBC/PostgreSQL y publica el outbox de forma asíncrona en Kafka. Kafka usa `patientId` como clave para ordenar eventos por paciente. El asistente consume `triaje.eventos` y proyecta los pacientes activos en conjuntos Redis por nivel mediante un script Lua atómico; confirma cada offset tras persistir la proyección.
- **Consecuencias:** Triage no depende de la disponibilidad del asistente. Outbox implica entrega al menos una vez, por lo que la proyección es idempotente y ordena por timestamp. Una pérdida total de Redis requiere reprocesar el topic para reconstruir la vista. PostgreSQL, Kafka y Redis se despliegan como dependencias separadas.
- **Alternativas consideradas:** llamada HTTP síncrona entre servicios (acoplamiento y falla en cascada); publicar en Kafka antes de confirmar la transacción (evento huérfano); actualizar solo en memoria (pérdida ante reinicio).
