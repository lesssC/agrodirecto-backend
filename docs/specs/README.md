# Specs de tareas — entrega final (2-dic-2026)

Cada tarea tiene su spec (`P#-##.md`). Úsala como instrucción para tu agente. Las specs de frontend (P3 y parte de P4) están en el repo `agrodirecto-project/docs/specs/`.

## Flujo por tarea
1. Crea la rama `tipo/ID-slug` desde `main` (ej. `feat/P2-01-subida-imagenes`).
2. Prompt base al agente: *"Lee `docs/specs/<ID>.md`. Implementa solo lo descrito, tests primero, no hagas commit ni añadas Co-Authored-By. Al final lista los archivos tocados y cómo verificar."*
3. Verifica: backend `./mvnw test` (con Docker, ver `CONTRIBUTING.md`); frontend `pnpm test && pnpm typecheck && pnpm lint`.
4. Commit y push (lo haces tú), PR enlazando la spec, revisión de otra persona, merge a `main` con CI verde.
5. Si el contrato de una spec debe cambiar, edita primero la spec en un PR aparte y avisa a quien dependa de ella.

## Roles y dueños de archivos
| Rol | Área | Dueño de |
|---|---|---|
| P1 | Backend: seguridad/producción y pagos | `pagos/**`, `config/SeguridadConfig`, `application*.properties`, `render.yaml`, `Dockerfile`, migraciones V9–V10 |
| P2 | Backend: archivos, correo, SMS, admin | `archivos/**` (nuevo), `notificaciones/**`, reset en `usuarios/**`, `administracion/**`, migraciones V11–V14 |
| P3 | Frontend | `src/components/**`, `src/hooks/**`, `src/services/api/**` |
| P4 | QA, CI, limpieza, despliegue, demo | `tests/**`, `.github/**`, `docs/**`, ROADMAP/README |

## Calendario (PR abierto → merge). Desarrollo termina el sáb 28
| Spec | PR | Merge |
|---|---|---|
| contratos | mié 25 | mié 25 |
| P1-01 | mié 25 | jue 26 |
| P1-02 | jue 26 | vie 27 |
| P1-03 (go/no-go jue 26 noche) | vie 27 | sáb 28 |
| P1-04 | vie 27 | sáb 28 |
| P2-01 | jue 26 | jue 26 |
| P2-02 | vie 27 | vie 27 |
| P2-03 | vie 27 | sáb 28 |
| P2-04, P2-05 (si sobra tiempo) | sáb 28 | sáb 28 |
| P3-01 | mié 25 | jue 26 |
| P3-02, P3-06 | jue 26 | vie 27 |
| P3-04 | vie 27 | vie 27 |
| P3-03, P3-05 | vie 27 | sáb 28 |
| P3-07 | sáb 28 | sáb 28 |
| P4-01, P4-02 | mié 25 | jue 26 |
| P4-03 | vie 27 | sáb 28 |
| P4-04 | sáb 28 | dom 29 |
| P4-05 | dom 29 | dom 29 |
| P4-06 | lun 30 | lun 30 |

Hitos: **jue 26 noche** decisión pasarela · **sáb 28** congelar funcionalidades · **dom 29** despliegue · **lun 30** solo bugs · **mar 1** ensayo ×2 · **mié 2** presentación. Daily de 10 min cada noche.

## Reglas de migraciones Flyway
Rangos reservados para no chocar: P1 → V9, V10; P2 → V11–V14. Nunca editar una migración ya mergeada.

## Plantilla de spec
Objetivo · Contexto/archivos · Contrato · Reglas de negocio · Criterios de aceptación (Dado/Cuando/Entonces) · Tests requeridos · Fuera de alcance · Cómo verificar.
