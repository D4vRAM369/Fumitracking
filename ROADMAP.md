# ROADMAP

Leyenda: ✅ hecho · 🔄 en curso · ⬜ pendiente

## Base (hecho)

- ✅ v1.5: contadores duales CBD/THC, infusión weed/polen, calendario, stats, widget, backup CSV
- ✅ Metas diarias (`GoalsActivity`)
- ✅ Botón −1 deshace la última toma real por icono (`feat/smart-undo-button`, commits `9d2043a`, `f04e907`, `6edd3e5`)

## Rediseño "Anillos del día"

- ✅ **Fases 1–4 implementadas** (rama `feat/rings-redesign`, sin mergear; ver `HANDOFF.md`)
- ✅ **Fase 0 — Brainstorming y diseño (cerrada)**: dirección A elegida; caso "sin metas" decidido; semántica "lleno = límite" decidida; widget confirmado para Fase 5; falta la duda del modo claro y aprobar `SPEC.md`
- ⬜ **Fase 1 — `RingsView`**: vista custom + tests de la lógica de progreso (fracción, exceso, sin meta)
- ⬜ **Fase 2 — Pantalla principal**: reemplazar bloque emoji+números en `activity_main.xml`; colores/temas **claro y oscuro** (modo claro en la v1)
- ⬜ **Fase 3 — Hoja inferior y barra superior**: botones nuevos, menú ⋯, estado del −1
- ⬜ **Fase 4 — Pulido**: animaciones de llenado, accesibilidad, revisión de contraste en ambos temas
- ⬜ **Fase 5 — Widget a juego** (si se decide)

Plan detallado: `docs/superpowers/plans/2026-10-25-rings-redesign.md` (Tasks 0–7 ≈ Fases 1–4). SPEC aprobado; plan pendiente de revisión.

## Antes de empezar la Fase 1

- ⬜ Cerrar/mergear `feat/smart-undo-button` (hay cambios sin commitear en `MainActivity.kt`)
- ⬜ Aprobación explícita de `SPEC.md` por Brian → luego `superpowers:writing-plans`

## Ideas futuras (sin compromiso)

- Línea de tiempo de 24 h con la hora de cada toma (idea de la dirección C)
- Paleta/tema a elegir por el usuario
