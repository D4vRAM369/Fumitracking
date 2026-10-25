# HANDOFF

Última actualización: 2026-10-25 · Rama: `feat/smart-undo-button`

## Dónde estamos

Sesión de **brainstorming** para rediseñar la pantalla principal. Brian eligió la dirección **A · Anillos del día**. No se ha escrito código de producto. Solo se crearon docs (`CLAUDE.md`, `SPEC.md`, `ROADMAP.md`, `HANDOFF.md`, `docs/00…07`).

## Hecho

- Revisión de la UI actual (`activity_main.xml`, `colors.xml`).
- 3 direcciones mockeadas (A anillos, B jardín, C bitácora) y 3 estados de A (vacío, normal, excedido).
- Memoria de proyecto guardada: regla Chronicler + docs vivos.

## Plan de implementación

`SPEC.md` aprobado por Brian (2026-10-25). Plan escrito: `docs/superpowers/plans/2026-10-25-rings-redesign.md` (Tasks 0–7). **Pendiente:** Brian revisa el plan, resuelve el choque de color ámbar (THC vs aviso) y elige método de ejecución (subagentes o nativo). No se ha escrito código.

## Siguiente paso exacto (anterior, ya cumplido salvo lo de arriba)

Decidido: sin metas → anillos tenues + toque suave para configurar (ver `SPEC.md`). Decidido también: anillo lleno = límite, aviso suave. Widget → Fase 5 (fuera de la v1). Modo claro entra en la v1. Todas las dudas de diseño están resueltas. Siguiente: Brian aprueba `SPEC.md` → `superpowers:writing-plans` (plan en `docs/superpowers/plans/`).

## Estado del árbol git (al escribir esto)

- Modificado sin commitear: `MainActivity.kt` (trabajo previo, no del rediseño — revisar antes de mezclar).
- Modificados: `app/release/baselineProfiles/*/app-release.dm` (artefactos generados, no commitear).
- Sin seguimiento: `2026-01-28-tras-implementar-…txt`, `CLAUDE-FABLE-5.md` (volcado de system prompt, ignorar).

## Trampas

- Las metas son **máximos**, y `0` significa "sin meta" — no dividir por cero al calcular el progreso del anillo.
- El widget (`CBDWidgetProvider`) debe seguir sincronizado con la app; ya hubo bugs (`docs/pbl/2026-01-27-widget-sync-bug-*`).
- El −1 deshace por icono la última toma real; no romper `IntakeController` (ver `docs/pbl/2026-06-25_premortem_boton_undo.md`).
- Mockups en `.superpowers/brainstorm/` (servidor local del visual companion; puede estar apagado).
