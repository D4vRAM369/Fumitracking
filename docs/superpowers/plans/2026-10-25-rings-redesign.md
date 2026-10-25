# Rediseño "Anillos del día" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sustituir el bloque emoji + números de la pantalla principal por dos anillos concéntricos (CBD exterior, THC interior) que se llenan según las metas diarias, con la barra superior reducida y los dos temas.

**Architecture:** La lógica de progreso vive en un objeto puro testeable (`RingProgress`). Una `View` custom (`RingsView`, patrón de `LineChartView`) solo dibuja. `MainActivity` le pasa conteos y metas en `updateDisplay()`. Los colores son roles nombrados en `values/` y `values-night/`, validados por un test de contraste que lee los XML.

**Tech Stack:** Kotlin 2.0, vistas XML, Material 3 (DayNight), JUnit 4. Sin Compose ni librerías nuevas.

**Spec:** `SPEC.md` (raíz). Contexto: `HANDOFF.md`, `ROADMAP.md`, `docs/02_ARQUITECTURA.md`.

## Global Constraints

- Vistas XML, sin Compose, sin dependencias nuevas.
- Metas: `GoalsActivity.PREFS_NAME = "goals_prefs"`, `KEY_CBD_MAX = "goal_cbd_max"`, `KEY_THC_MAX = "goal_thc_max"`; **0 = sin meta**; cada anillo se evalúa por separado; con meta 0 no se calcula fracción.
- Anillo lleno = "llegaste al límite". Textos exactos: `En tu límite` y `+N sobre tu meta`. Sin ✓ de logro, sin sonido, sin confeti.
- Estados: bajo el límite (color propio), 100% (aviso), por encima (rojo suave + segunda vuelta fina), sin meta (pista tenue, sin relleno).
- Modo claro y oscuro en la v1; el botón de tema existente sigue funcionando (`setDarkMode`).
- No cambiar el modelo de datos, CSV/backup, `IntakeController`/undo, ni el widget (`CBDWidgetProvider`, Fase 5).
- Accesibilidad: `contentDescription` de los anillos con valores y metas; el estado no depende solo del color.
- Español en UI, comentarios y commits (`feat(rings): …`). Atribución de commits según la configuración de la sesión.
- **Chronicler:** cada tarea termina con la entrada en `docs/03_PASO_A_PASO.md` (más glosario/errores si aplica) y con `HANDOFF.md`/`ROADMAP.md` al día.
- Comandos: tests `./gradlew testDebugUnitTest`; build `./gradlew assembleDebug`.

## Review Focus

1. **Una sola meta a 0** (p. ej. CBD meta 5, THC meta 0 con 2 tomas): el anillo THC queda tenue sin relleno, sin división entre cero, y el número no se pinta como "exceso".
2. **Exceso enorme** (3 de meta y 50 tomas): la segunda vuelta se limita a una vuelta completa y el texto dice `+47 sobre tu meta`; el arco nunca supera 360°.
3. **Conteo negativo o desfasado** (undo defectuoso → -1): se trata como 0, el anillo no dibuja sweep negativo.
4. **Cambiar metas y volver** (`GoalsActivity` → atrás): los anillos se actualizan sin reiniciar la app; también tras cruzar medianoche con la app abierta.
5. **Pantalla pequeña / fuente grande** (360×640, escala de fuente 1.3): los anillos y las etiquetas no se solapan con la hoja inferior (peek 260dp).

---

### Task 0: Preparar la rama

**Files:** ninguno de código.

- [ ] **Step 1: Confirmar con Brian qué hacer con los cambios sin commitear.** `MainActivity.kt` tiene un cambio ajeno al rediseño (`calculateCleanStreak`, racha limpia) y `app/release/baselineProfiles/*.dm` son artefactos generados. Opciones: commitear la racha aparte, o `git stash`. No commitear los `.dm`.
- [ ] **Step 2: Cerrar o mergear `feat/smart-undo-button`** (según decida Brian) y crear `feat/rings-redesign` desde ahí.
- [ ] **Step 3: Verificar base verde.** Run: `./gradlew testDebugUnitTest` → Expected: PASS (todos los tests actuales).

---

### Task 1: Lógica de progreso (`RingProgress`)

**Files:**
- Create: `app/src/main/java/com/d4vram/fumitracking/RingProgress.kt`
- Test: `app/src/test/java/com/d4vram/fumitracking/RingProgressTest.kt`

**Interfaces:**
- Produces:
  - `enum class RingState { NO_GOAL, UNDER, AT_LIMIT, OVER }`
  - `object RingProgress` con:
    - `fun state(count: Int, goal: Int): RingState`
    - `fun fraction(count: Int, goal: Int): Float` — 0f..1f; 0f si `NO_GOAL`
    - `fun overflowFraction(count: Int, goal: Int): Float` — 0f..1f, segunda vuelta; 0f salvo `OVER`
    - `fun overBy(count: Int, goal: Int): Int` — 0 salvo `OVER`

- [ ] **Step 1: Write the failing tests** en `RingProgressTest` (JUnit, backticks como `HeatmapUtilsTest`):
  - `goal 0 is NO_GOAL with zero fraction` : `state(3,0)==NO_GOAL`, `fraction(3,0)==0f`, `overflowFraction(3,0)==0f`, `overBy(3,0)==0`
  - `negative goal is NO_GOAL`: `state(2,-1)==NO_GOAL`
  - `under goal` : `state(3,5)==UNDER`, `fraction(3,5)==0.6f` (delta 0.0001)
  - `zero count with goal is UNDER with zero fraction`: `state(0,5)==UNDER`, `fraction(0,5)==0f`
  - `count equals goal is AT_LIMIT`: `state(5,5)==AT_LIMIT`, `fraction(5,5)==1f`, `overBy(5,5)==0`
  - `over goal`: `state(4,3)==OVER`, `fraction(4,3)==1f`, `overBy(4,3)==1`, `overflowFraction(4,3)` ≈ `1/3f`
  - `huge overflow is clamped to one lap`: `overflowFraction(50,3)==1f`, `overBy(50,3)==47`
  - `negative count is treated as zero`: `state(-1,5)==UNDER`, `fraction(-1,5)==0f`
- [ ] **Step 2: Run to verify it fails.** Run: `./gradlew testDebugUnitTest --tests "*RingProgressTest"` → Expected: FAIL (clase no definida).
- [ ] **Step 3: Implement** `RingState` y `RingProgress` en `RingProgress.kt`. Regla única: `c = max(count, 0)`; `goal <= 0` → `NO_GOAL`; `c < goal` → `UNDER`; `c == goal` → `AT_LIMIT`; `c > goal` → `OVER`. Sin Context ni Android.
- [ ] **Step 4: Run to verify it passes.** Mismo comando → Expected: PASS.
- [ ] **Step 5: Chronicler** — glosario ("estado del anillo", "clamp") y `03_PASO_A_PASO.md`.
- [ ] **Step 6: Commit** `feat(rings): lógica pura de progreso de anillos`.

---

### Task 2: Roles de color en ambos temas + test de contraste

**Files:**
- Modify: `app/src/main/res/values/colors.xml`, `app/src/main/res/values-night/colors.xml`
- Test: `app/src/test/java/com/d4vram/fumitracking/ColorContrastTest.kt`

**Interfaces:**
- Produces (nombres idénticos en ambos archivos): `main_bg`, `ring_track`, `ring_cbd`, `ring_thc`, `ring_limit`, `ring_over`. Además se **redefinen** los tokens de la pantalla principal `main_sheet_bg` y `main_action_tonal_bg` (solo los usa `activity_main.xml` y dos drawables; confirmar con `grep -rn "main_sheet_bg\|main_action_tonal_bg" app/src` antes de tocar).
- Valores (calculados contra `main_bg`):

| Rol | Oscuro | Claro |
|---|---|---|
| `main_bg` | `#0E1A14` | `#F4F7F2` |
| `ring_track` | `#26382E` | `#DDE5DD` |
| `ring_cbd` | `#4ADE80` | `#2E7D32` |
| `ring_thc` | `#FBBF24` | `#B45309` |
| `ring_limit` | `#F97316` | `#C2410C` |
| `ring_over` | `#F87171` | `#DC2626` |
| `main_sheet_bg` | `#16261D` | `#E8EFE6` |
| `main_action_tonal_bg` | `#24402F` | `#D4E6D4` |

- [ ] **Step 1: Write the failing test** `ColorContrastTest`: helper que lee `src/main/res/values/colors.xml` y `src/main/res/values-night/colors.xml` (el directorio de trabajo de los tests de Gradle es `app/`), extrae `<color name="X">#RRGGBB</color>` con regex y calcula el ratio de contraste WCAG. Tests:
  - `ring colors reach 3 to 1 against background in both themes`: para `ring_cbd, ring_thc, ring_limit, ring_over` vs `main_bg` → ratio ≥ 3.0, en claro y oscuro.
  - `track is visible but subtle`: `ring_track` vs `main_bg` → 1.1 ≤ ratio ≤ 2.0, en ambos temas.
  - `every ring role exists in both themes`: los 6 nombres presentes en los dos archivos.
- [ ] **Step 2: Run to verify it fails.** Run: `./gradlew testDebugUnitTest --tests "*ColorContrastTest"` → Expected: FAIL (roles ausentes).
- [ ] **Step 3: Añadir los roles y redefinir los dos tokens** con la tabla de arriba.
- [ ] **Step 4: Run to verify it passes.** Mismo comando → Expected: PASS.
- [ ] **Step 5: Chronicler** — `docs/02_ARQUITECTURA.md` (roles de color) y glosario ("contraste WCAG").
- [ ] **Step 6: Commit** `feat(rings): roles de color claro/oscuro y test de contraste`.

---

### Task 3: `RingsView`

**Files:**
- Create: `app/src/main/java/com/d4vram/fumitracking/RingsView.kt`

**Interfaces:**
- Consumes: `RingProgress`, `RingState` (Task 1); roles de color (Task 2).
- Produces:
  - `class RingsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View`
  - `fun setProgress(cbd: Int, thc: Int, cbdGoal: Int, thcGoal: Int, animate: Boolean = false)`
  - `fun colorFor(isThc: Boolean, state: RingState): Int` — color del anillo/número: `NO_GOAL`/`UNDER` → `ring_cbd`/`ring_thc`; `AT_LIMIT` → `ring_limit`; `OVER` → `ring_over`.

- [ ] **Step 1: Implement la vista** (cuadrada: `onMeasure` usa el menor de ancho/alto). Dibujo en `onDraw` con `Canvas.drawArc` y `Paint` con `StrokeCap.ROUND`: pista de cada anillo siempre dibujada con `ring_track`; anillo exterior = CBD, interior = THC; arranque a -90° (arriba); sweep = `360 * RingProgress.fraction(...)`; en `OVER` una segunda vuelta más fina sobre el mismo radio con sweep `360 * overflowFraction(...)`. Los `Paint` se crean una vez, no dentro de `onDraw`. Guardar los valores y resolver los colores con `ContextCompat.getColor` en el constructor y en `onConfigurationChanged`/al re-medir, para que el cambio de tema se refleje.
- [ ] **Step 2: Verificación visual.** Incluir el `RingsView` provisionalmente en `activity_main.xml` (Task 4 lo deja definitivo) y comprobar en emulador los 4 estados cambiando metas en Ajustes → Metas: vacío, parcial, 100%, exceso, y el caso sin metas. Run: `./gradlew assembleDebug` → Expected: BUILD SUCCESSFUL; capturas en claro y oscuro.
- [ ] **Step 3: Chronicler** — `04_CONCEPTOS.md` (cómo se dibuja un arco), `07_DAW_DAM_CLASE.md` lección 3, glosario (`onDraw`, `StrokeCap`).
- [ ] **Step 4: Commit** `feat(rings): RingsView dibuja anillos concéntricos`.

---

### Task 4: Pantalla principal con anillos

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml` (bloque central: `emojiText`, `dualCounterContainer` y las etiquetas estáticas CBD/THC; fondo raíz y `topArea` de `@color/m3_surface` a `@color/main_bg`)
- Modify: `app/src/main/java/com/d4vram/fumitracking/MainActivity.kt` (`updateDisplay`, `updateCounterColors`, `onResume`)
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `RingsView.setProgress`, `RingsView.colorFor`, `RingProgress.state`/`overBy`; `GoalsActivity.PREFS_NAME`, `KEY_CBD_MAX`, `KEY_THC_MAX`.
- Produces (ids en el layout): `ringsContainer` (180dp cuadrado, contiene `ringsView` match_parent y `emojiText` centrado, 40sp), `cbdCountText`, `thcCountText` (se conservan los ids), `cbdLabelText`, `thcLabelText` (nuevos, bajo cada número), `ringsHintText` (nuevo).
- Strings nuevos: `ring_label_goal` = `%1$s · meta %2$d`; `ring_at_limit` = `En tu límite`; `ring_over_by` = `+%1$d sobre tu meta`; `ring_set_goals_hint` = `Toca para fijar tus metas`; `ring_cd_goal` = `%1$s: %2$d de %3$d`; `ring_cd_no_goal` = `%1$s: %2$d, sin meta`.

- [ ] **Step 1: Reemplazar el bloque central del layout** por `ringsContainer` + una fila con dos columnas (número + etiqueta) + `ringsHintText` (gone por defecto). Eliminar `dualCounterContainer` y las etiquetas estáticas (no las referencia ningún `.kt`). Verificar sin referencias rotas: `grep -rn "dualCounterContainer" app/src` → sin resultados.
- [ ] **Step 2: Cablear `updateDisplay()`.** Leer las metas con `getSharedPreferences(GoalsActivity.PREFS_NAME, MODE_PRIVATE).getInt(...)` y llamar a `ringsView.setProgress(cbdCount, thcCount, cbdGoal, thcGoal, animate = false)`. Etiqueta de cada columna según `RingProgress.state`: `NO_GOAL` → `CBD`/`THC`; `UNDER` → `ring_label_goal`; `AT_LIMIT` → `ring_at_limit`; `OVER` → `ring_over_by` con `overBy`. Color del número = `ringsView.colorFor(isThc, state)`. `updateCounterColors()` deja de usar los umbrales por conteo (≤4, ≤6) y usa esto. Conservar `highlightActiveCounter`.
- [ ] **Step 3: Toque suave sin metas.** Si `cbdGoal == 0 || thcGoal == 0`: `ringsHintText` visible y `ringsContainer.setOnClickListener { startActivity(Intent(this, GoalsActivity::class.java)) }`; si ambas metas > 0: ocultar la pista y quitar el listener (`setOnClickListener(null)` y `isClickable = false`). Sin diálogos ni banners.
- [ ] **Step 4: Accesibilidad.** `ringsContainer.contentDescription` formado con `ring_cd_goal`/`ring_cd_no_goal` para CBD y THC más el texto de estado cuando sea `AT_LIMIT`/`OVER`.
- [ ] **Step 5: Verificación manual** (emulador, claro y oscuro) y marcar en `HANDOFF.md`:
  - [ ] Sin metas: anillos tenues, pista visible, toque abre Metas.
  - [ ] Solo meta CBD (THC en 0): THC tenue y sin "exceso" (Review Focus 1).
  - [ ] +1 hasta 100% → "En tu límite"; un +1 más → "+1 sobre tu meta" y segunda vuelta.
  - [ ] Cambiar metas en Metas y volver: los anillos se actualizan (Review Focus 4; `onResume` ya llama a `updateDisplay`).
  - [ ] Cambiar la hora del emulador a después de medianoche con la app abierta: se reinician (`reloadIfDateChanged`).
  - [ ] 360×640 y escala de fuente 1.3: sin solape con la hoja (Review Focus 5).
  - [ ] TalkBack lee valores y metas.
- [ ] **Step 6: Run** `./gradlew testDebugUnitTest assembleDebug` → Expected: PASS / BUILD SUCCESSFUL.
- [ ] **Step 7: Chronicler** — `03_PASO_A_PASO.md` con capturas, `05_ERRORES_Y_SOLUCIONES.md` si algo falló.
- [ ] **Step 8: Commit** `feat(rings): pantalla principal con anillos del día`.

---

### Task 5: Barra superior con menú ⋯

**Files:**
- Create: `app/src/main/res/menu/main_more.xml`
- Modify: `app/src/main/res/layout/activity_main.xml` (barra superior), `MainActivity.kt` (`initViews`, `setupClickListeners`, `updateThemeIcon`)
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: `moreButton` (ImageButton, mismo estilo `bg_top_icon_button`) junto a `searchButton`. Menú con ids `action_calendar`, `action_export`, `action_import`, `action_theme`, `action_settings`.

- [ ] **Step 1: Quitar de layout y de `MainActivity`** los cinco botones `calendarButton`, `exportCsvIconButton`, `importCsvIconButton`, `themeToggleButton`, `settingsButton` (propiedades `lateinit`, `findViewById` y listeners). Comprobar con `grep -rn "calendarButton\|exportCsvIconButton\|importCsvIconButton\|themeToggleButton\|settingsButton" app/src` que solo queden usos ajenos (p. ej. `SettingsActivity` tiene su propio `btnGoals`, no confundir). `searchButton` se queda.
- [ ] **Step 2: `moreButton` abre un `PopupMenu`** inflando `main_more.xml`; cada item llama a lo que hacía su botón (`CalendarActivity`, `exportCsv()`, `importCsvLauncher.launch(importMimeTypes)`, `setDarkMode(!isDarkModeEnabled())`, `SettingsActivity`). El título/icono del item de tema se decide al abrir el menú según `isDarkModeEnabled()` (en oscuro "Tema claro", en claro "Tema oscuro"). `updateThemeIcon()` conserva solo la parte de iconos de la barra de estado.
- [ ] **Step 3: Verificación manual.** Los cinco destinos funcionan; el cambio de tema aplica en caliente y la pantalla vuelve a pintar bien en ambos temas. Run: `./gradlew assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 4: Chronicler** + `docs/07_DAW_DAM_CLASE.md` (menús y `PopupMenu`).
- [ ] **Step 5: Commit** `feat(rings): barra superior con búsqueda y menú ⋯`.

---

### Task 6: Hoja inferior (botones y −1)

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml` (botones `addButton`, `addInfusedButton`, `subtractButton`, `resetButton`), `MainActivity.kt` (`updateDisplay`)
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `Prefs.getSubstanceType(ctx)` ("CBD" por defecto), `cbdCount`, `thcCount`.
- Produces: strings `add_cbd_label` = `+1 CBD` y `add_thc_label` ya existente (`🍁 +THC`); `addButton.text` se fija en `updateDisplay()` según el modo activo.

- [ ] **Step 1: Corregir la etiqueta de `addButton`.** Hoy muestra `🍁 +THC` pero `registerStandardIntake()` suma al modo activo (CBD por defecto). Fijar el texto en `updateDisplay()`: `add_cbd_label` si el modo es CBD, `add_thc_label` si es THC; color de fondo `ring_cbd`/`ring_thc` correspondiente (lógica de la toma sin cambios).
- [ ] **Step 2: Colores por tipo** en `addInfusedButton` (THC, `ring_thc`) y estilo de `subtractButton`/`resetButton` con los tokens redefinidos en Task 2.
- [ ] **Step 3: Apagar −1 sin tomas.** En `updateDisplay()`: `subtractButton.isEnabled = (cbdCount + thcCount) > 0` y `alpha` 0.4 cuando está apagado (misma condición que ya usa el click listener). El diálogo de confirmación y `IntakeController.undoLast` no se tocan.
- [ ] **Step 4: Verificación manual en ambos temas:** −1 apagado con 0 tomas y activo tras un +1; undo sigue borrando la línea de nota (flujo FM1); historial, chip de estadísticas y pestañas intactos. Run: `./gradlew testDebugUnitTest assembleDebug` → PASS / BUILD SUCCESSFUL (incluye `IntakeControllerTest` sin cambios).
- [ ] **Step 5: Chronicler** + `05_ERRORES_Y_SOLUCIONES.md` (la etiqueta "+THC" que sumaba CBD).
- [ ] **Step 6: Commit** `feat(rings): botones de la hoja por tipo y −1 apagado sin tomas`.

---

### Task 7: Animación, pulido y cierre

**Files:**
- Modify: `RingsView.kt` (animación), `CHANGELOG.md`, `README.md` (capturas)

**Interfaces:**
- Consumes: `RingsView.setProgress(..., animate = true)` (Task 3).

- [ ] **Step 1: Animar el llenado.** Con `animate = true`, `ValueAnimator` interpola del sweep anterior al nuevo (duración 400 ms, `DecelerateInterpolator`); con `false` pinta directo. `updateDisplay(animate)` pasa su propio parámetro: animado tras +1/−1, sin animar en `onResume`/primer dibujo. Si el sistema tiene animaciones desactivadas (`ANIMATOR_DURATION_SCALE = 0`), `ValueAnimator` ya salta a la meta; no añadir lógica extra.
- [ ] **Step 2: Matriz de cierre** (claro y oscuro, emulador 360×640 y uno normal): los 5 puntos de Review Focus, widget aún sincronizado (añadir toma desde el widget y volver a la app), cambio de tema en caliente, rotación sin perder estado, undo.
- [ ] **Step 3: Run** `./gradlew testDebugUnitTest assembleDebug` → PASS / BUILD SUCCESSFUL.
- [ ] **Step 4: Documentar.** `CHANGELOG.md` (entrada del rediseño), capturas reales en `README.md`/`README_spanish-version.md` (hoy "Coming soon"), `HANDOFF.md`, `ROADMAP.md` (Fases 0–4 ✅), `docs/` completo (README, glosario, `06_SIGUIENTE_NIVEL.md`: widget a juego y línea de 24 h).
- [ ] **Step 5: Commit** `feat(rings): animación de llenado y cierre del rediseño`.

---

## Decisiones tomadas al planificar (revisar)

1. **Choque de color ámbar:** el spec pide THC ámbar *y* aviso ámbar al 100%. El plan usa naranja más intenso (`ring_limit`) para el aviso; en oscuro sigue siendo parecido al ámbar de THC (ratio 1.7), y en claro `ring_thc` y `ring_limit` son casi idénticos. El texto "En tu límite" sostiene el significado, pero visualmente es débil. **Alternativa:** que THC vuelva al violeta de la app actual (`m3_tertiary`).
2. **Emoji de amanecer en vacío: omitido.** `EmojiUtils` es personalizable y lo usa el widget; cambiar el default del 0 afectaría a otros sitios. Se añade editando ese default si quieres.
3. **Botón +1 mal etiquetado:** la app mostraba "🍁 +THC" pero suma al modo activo (CBD por defecto, y ningún código cambia el modo). El plan lo etiqueta según el modo (Task 6).
4. **Toque suave:** pista de texto "Toca para fijar tus metas" visible si *alguna* meta es 0; el toque en los anillos abre `GoalsActivity`. Texto y pista son decisión mía.
5. **Pantalla pequeña:** anillos fijos de 180dp, verificados a 360×640; no hay tamaño adaptable.
6. **Umbrales por conteo eliminados:** los colores del número dejan de depender de ≤4/≤6 tomas y pasan a depender de las metas.
