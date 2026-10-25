# HANDOFF

Última actualización: 2026-10-25 · Rama: `feat/smart-undo-button`

## Dónde estamos

Sesión de brainstorming → SPEC → plan → implementación del rediseño "Anillos del día" (2026-10-25).

## ✅ ESTADO (actualizado tras la implementación)

Rediseño **implementado** en la rama `feat/rings-redesign` (8 commits sobre `feat/smart-undo-button`), 35 tests en verde, verificado en emulador (claro/oscuro, 360×640 con letra 1,3). **No mergeado ni pusheado.** Pendiente de Brian: revisar, probar en móvil real y decidir el merge; probar el cambio de día con la app abierta y el widget (no verificados aquí). El cambio ajeno de racha limpia (`calculateCleanStreak`) estaba en `git stash`; se reaplicó al árbol de trabajo sin commitear.

## Paquete cambiado

Nuevo paquete `dev.d4vram.cbdcounter` (namespace y applicationId). **Es otra app para Android**: la versión instalada antes (`com.d4vram.cbdcounter`, p. ej. desde APK de Releases) no se actualiza; exportar CSV/backup e importar. Ver `docs/02_ARQUITECTURA.md` D7. Faltan: actualizar los enlaces/menciones del paquete antiguo si algún sitio externo lo usa.

## Siguiente paso

Brian: probar en móvil real, revisar el diff de `feat/rings-redesign` y decidir el merge. Plan y spec: `docs/superpowers/plans/2026-10-25-rings-redesign.md`, `SPEC.md`.

## Estado del árbol git (al escribir esto)

- Modificado sin commitear: `MainActivity.kt` (trabajo previo, no del rediseño — revisar antes de mezclar).
- Modificados: `app/release/baselineProfiles/*/app-release.dm` (artefactos generados, no commitear).
- Sin seguimiento: `2026-01-28-tras-implementar-…txt`, `CLAUDE-FABLE-5.md` (volcado de system prompt, ignorar).

## Trampas

- Las metas son **máximos**, y `0` significa "sin meta" — no dividir por cero al calcular el progreso del anillo.
- El widget (`CBDWidgetProvider`) debe seguir sincronizado con la app; ya hubo bugs (`docs/pbl/2026-01-27-widget-sync-bug-*`).
- El −1 deshace por icono la última toma real; no romper `IntakeController` (ver `docs/pbl/2026-06-25_premortem_boton_undo.md`).
- Mockups en `.superpowers/brainstorm/` (servidor local del visual companion; puede estar apagado).
