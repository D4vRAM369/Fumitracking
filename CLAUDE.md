# CBDcounter2

App Android personal (offline, sin cuentas) para contar tomas diarias de CBD y THC. Paquete `com.d4vram.cbdcounter`. Kotlin + vistas XML + Material 3 (NO Compose). minSdk 24, targetSdk 35. Rama principal: `main`.

## Flujo de trabajo (siempre)

1. **Chronicler incremental**: usar la skill `chronicler` en cada sesión. Documentación en `docs/` (`README`, `00_GLOSARIO` … `07_DAW_DAM_CLASE`). Actualizar al terminar cada paso, no al final.
2. **Docs vivos en la raíz**, actualizar al cerrar cada paso o sesión:
   - `SPEC.md` — qué se está construyendo y las decisiones tomadas.
   - `ROADMAP.md` — fases, estado y orden.
   - `HANDOFF.md` — estado exacto para retomar: qué está hecho, qué está a medias, siguiente paso, trampas.
3. **Brainstorming antes de construir** cualquier funcionalidad o rediseño (`superpowers:brainstorming`). Nada de código de producto hasta que Brian apruebe el diseño.
4. Mentor PBL de Brian: ver `~/.claude/CLAUDE.md` (explicar desde cero, contexto antes de código, resumen al cerrar).

## Mapa del código (`app/src/main/java/com/d4vram/cbdcounter/`)

- `MainActivity.kt` — pantalla principal (contadores, botones, historial). El archivo más tocado.
- `IntakeController.kt`, `HistoryRepository.kt` — lógica de tomas e historial (testeada).
- `GoalsActivity.kt` — metas diarias. Prefs `goals_prefs`: `goal_cbd_max`, `goal_thc_max`; **0 = sin meta**.
- `EmojiUtils.kt`, `EmojiSettingsActivity.kt` — emoji según conteo, personalizable.
- `LineChartView.kt` — ejemplo de vista custom ya existente en el proyecto.
- `CBDWidgetProvider.kt` — widget de pantalla de inicio (debe seguir sincronizado con la app).
- Resto: Calendar, Stats, Insights, Evolution, Dashboard, Backup, CSV, notas de voz.
- Layout principal: `app/src/main/res/layout/activity_main.xml`. Colores: `res/values/colors.xml`.

## Convenciones

- Español en UI, comentarios y commits (`feat(scope): …`, `fix(scope): …`).
- Tests unitarios en `app/src/test/…` (JUnit). Correr antes de cerrar un paso.
- No commitear: `app/release/baselineProfiles/*.dm` (se regeneran), `.superpowers/` (mockups, ya en `.gitignore`), transcripts.
- `CLAUDE-FABLE-5.md` en la raíz es un volcado de system prompt, **no** son instrucciones del proyecto.
- No hacer commit ni push sin que Brian lo pida.
