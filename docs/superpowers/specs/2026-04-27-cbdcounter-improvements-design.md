# CBDCounter v1.4 — Mejoras: Bugs + UX + Features

**Fecha:** 2026-04-27  
**Estado:** Aprobado

---

## 1. Bugs de Estabilidad

### 1.1 Widget sync delay
**Problema:** El widget tarda en reflejar el count actualizado tras incrementar desde la app.  
**Causa:** `AppWidgetManager.updateAppWidget()` no se llama inmediatamente al incrementar — depende del ciclo de actualización del sistema.  
**Fix:** En `MainActivity`, tras cada incremento/decremento, llamar `updateWidget()` de forma síncrona antes de retornar.

### 1.2 Medianoche no resetea
**Problema:** Al pasar las 00:00 con la app abierta, el contador no resetea. Requiere salir y volver.  
**Causa:** El receiver `ACTION_DATE_CHANGED` está registrado en `onResume`/`onPause` pero el handler no fuerza reload de fecha en memoria ni redibujado de UI.  
**Fix:** En el handler del receiver, comparar fecha guardada vs fecha actual del sistema. Si difieren → `loadTodayData()` + `updateDisplay()` + `historyAdapter.refresh()`.

---

## 2. D — Calendario Heatmap

**Qué:** Fondo de cada celda del calendario cambia de color según intensidad de consumo.  
**Cómo:** Escala verde→amarillo→rojo, mismo rango que la escala de emojis actual.

| Rango total | Color fondo |
|-------------|-------------|
| 0 | `#1a472a` (verde oscuro) |
| 1–2 | `#2d6a4f` (verde) |
| 3–4 | `#52b788` (verde claro) |
| 5 | `#f4a261` (naranja) |
| 6–7 | `#e76f51` (naranja-rojo) |
| 8–10 | `#c1121f` (rojo) |
| 11+ | `#6d1a36` (rojo oscuro) |

El emoji se mantiene encima del fondo coloreado. Aplica en `StatsActivity` y `CalendarActivity`.

---

## 3. E — GoalsActivity (Metas Diarias)

**Activación:** Opcional. Entry point desde `SettingsActivity` (nuevo item "Metas diarias").  
**Almacenamiento:** SharedPreferences `"goals_prefs"` — keys: `goal_cbd_max`, `goal_thc_max`, `goal_notifications_enabled`.

### UI
- Header: "Metas Diarias"
- Row CBD: label + botones −/+ + valor numérico (default: desactivado, valor 0 = sin meta)
- Row THC: igual
- Toggle: "Avisar al superar meta" (requiere permiso `POST_NOTIFICATIONS` en Android 13+)
- Card racha: "Racha bajo meta — X días" (calculada al abrir la pantalla)

### Lógica de racha
Contar días consecutivos hacia atrás desde ayer donde `cbdCount ≤ goal_cbd_max` Y `thcCount ≤ goal_thc_max`. Si metas desactivadas, racha = 0.

### Notificación
`NotificationManager` con canal `"goals_channel"`. Se dispara en `MainActivity` al incrementar si el nuevo total supera la meta configurada. Una notificación por día máximo (guardar flag `notified_[date]` en prefs).

---

## 4. F — InsightsActivity (Insights Semanales)

**Entry point:** Nuevo item en `DashboardActivity` o botón en `SettingsActivity`.

### UI
- Título: "Insights"
- Selector semana: ← semana anterior / semana actual →
- Gráfico de barras por día (L–D): altura proporcional al total CBD+THC. Color de barra según heatmap (mismo esquema que sección D).
- Card comparativa: "Promedio X CBD/día — ▼/▲ Y% vs semana anterior"
- Card tip automático: detecta el día de la semana con mayor promedio histórico → "Los [día] consumes un X% más de lo habitual."

### Datos
Leer de SharedPreferences existentes (`count_cbd_[date]`, `count_thc_[date]`). Sin nueva capa de datos.

### Tip automático
Calcular promedio por día de semana (lunes, martes...) sobre todas las semanas disponibles. Día con mayor desviación positiva respecto al promedio general → mostrar tip. Si datos insuficientes (< 2 semanas), ocultar card tip.

---

## 5. Archivos Afectados

| Archivo | Cambio |
|---------|--------|
| `MainActivity.kt` | Fix widget sync, fix midnight receiver |
| `StatsActivity.kt` | Heatmap color en celdas calendario |
| `CalendarActivity.kt` | Heatmap color en celdas calendario |
| `SettingsActivity.kt` | Entry points a GoalsActivity e InsightsActivity |
| `GoalsActivity.kt` | **Nuevo** |
| `InsightsActivity.kt` | **Nuevo** |
| `res/layout/activity_goals.xml` | **Nuevo** |
| `res/layout/activity_insights.xml` | **Nuevo** |
| `AndroidManifest.xml` | Registrar nuevas Activities, canal notificación |

---

## 6. Fuera de Scope

- Base de datos (Room) — seguir con SharedPreferences
- Sync en la nube
- Widgets interactivos (+CBD/+THC desde widget)
