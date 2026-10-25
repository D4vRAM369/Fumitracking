# ROADMAP

Leyenda: ✅ hecho · 🔄 en curso · ⬜ pendiente

## Base (hecho)

- ✅ v1.5: contadores duales CBD/THC, infusión weed/polen, calendario, stats, widget, backup CSV
- ✅ Metas diarias (`GoalsActivity`)
- ✅ Botón −1 deshace la última toma real por icono (`feat/smart-undo-button`, commits `9d2043a`, `f04e907`, `6edd3e5`)

## Rediseño "Anillos del día"

- ✅ **Fase 0 — Brainstorming y diseño** (`SPEC.md` aprobado 2026-10-25)
- ✅ **Fase 1 — `RingProgress` + `RingsView`** (lógica con tests; vista custom)
- ✅ **Fase 2 — Pantalla principal** claro y oscuro (roles de color + test de contraste)
- ✅ **Fase 3 — Barra superior ⋯ y hoja inferior** (botón por modo, −1 apagado sin tomas)
- ✅ **Fase 4 — Pulido**: animación de llenado, accesibilidad, contraste; probado en 360×640 + letra 1,3
- ⬜ **Fase 5 — Widget a juego** (fuera de la v1)

Rama `feat/rings-redesign` (sin mergear, ver `HANDOFF.md`). Plan: `docs/superpowers/plans/2026-10-25-rings-redesign.md`.

## Pendiente antes de publicar

- ⬜ Revisión y prueba en móvil real por Brian; decidir merge (`feat/smart-undo-button` → `feat/rings-redesign` → `main`)
- ⬜ Probar cambio de día con la app abierta y el widget
- ⬜ Decidir el color del aviso de límite si el naranja se confunde con el ámbar de THC

## Ideas futuras (sin compromiso)

- Línea de tiempo de 24 h con la hora de cada toma (idea de la dirección C)
- Paleta/tema a elegir por el usuario
