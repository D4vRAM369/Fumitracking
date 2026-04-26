# 🌿 CBD & THC Counter

> Tracker personal de reducción de daños para CBD y THC — desarrollado con Kotlin y Material Expressive 3.

<img width="512" height="512" alt="icono_cbdcounter2_cleaned" src="https://github.com/user-attachments/assets/0d6eff86-af54-4a67-a132-9e63a87eef40" />

[![Version](https://img.shields.io/badge/version-1.5-6750A4?style=flat-square)](https://github.com/d4vram/CBDcounter2/releases/tag/v1.5)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green?style=flat-square&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7B68EE?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![license](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
![minSdk](https://img.shields.io/badge/minSdk-24+-informational)
![targetSdk](https://img.shields.io/badge/targetSdk-35-informational)
<img src="https://img.shields.io/badge/ClaudeCode_&_PBL-powered-4B0082?style=flat-square&logo=anthropic&logoColor=white"/>
<img src="https://img.shields.io/badge/Project--Based_Learning-driven-orange?style=flat-square"/>
<img src="https://img.shields.io/badge/ABP-metodología-blue?style=flat-square"/>
![privacy](https://img.shields.io/badge/privacy-100%25%20local-success)

**[🇬🇧 English version](README.md)**

---

## ¿Qué es esto?

CBD & THC Counter es una app de tracking personal **privada y offline-first**. Sin cuenta, sin que ningún dato salga de tu dispositivo. Te ayuda a llevar el registro diario de tu consumo de CBD y THC con una UI limpia y expresiva — para que puedas tomar decisiones informadas y conscientes.

> ⚠️ Esta app **no es un dispositivo médico**. No proporciona consejo médico y no promueve el consumo de sustancias. Úsala de forma responsable y consulta siempre a un profesional de la salud para decisiones relacionadas con tu bienestar, **y DYOR** *(DoYourOwnResearch)* **SIEMPRE**.

Esta app no promueve el consumo de sustancias en absoluto, pero aquí va un mensaje: cada persona es libre de hacer lo que quiera con su cuerpo y su mente (siempre que sus actos no perjudiquen a otros), y de acceder a información real sin sesgos políticos, promoviendo entre todos la **reducción de daños y el uso responsable**. Eres libre, mientras no causes daño a nadie: **"Principio de No Agresión: Base del Libertarismo"**

---

## Funcionalidades

### 🔢 Contador Dual
- Contadores separados de **CBD** y **THC** para el mismo día
- Botón rápido **+1** para contar CBD
- **Añadir aliñado** — registra una sesión etiquetada como *weed* 🌿 o *polen* 🍫 (siempre suma a THC)
- Corrección **−1** con diálogo de confirmación y opción de borrar o conservar la nota vinculada, si la hay; feedback si el contador ya está en 0
- **Reset** con confirmación — reinicia solo la sustancia activa del día

### 📅 Vista Calendario
- Cuadrícula mensual con un **emoji de estado en cada día** según el total consumido
- Toca cualquier día para abrir un **Modal de día** con el desglose completo (CBD, THC, nota de voz, registro con timestamps)
- Navegación mes anterior/siguiente
- **Leyenda de emojis** — escala completa de consumo explicada de un vistazo

### 📊 Dashboard de Estadísticas  *(→ chip 📊 Estadísticas)*
- Tarjetas métricas **Hoy / Semana / Promedio / Racha limpia** — colores ME3 Expressive:
  - Hoy → azul hielo · Semana → naranja cálido · Promedio → verde lima · Racha → lavanda
- Sección **Patrones** — día de la semana con mayor consumo medio
- **Mini gráfico de líneas** integrado (7D / 14D / 30D)

### 📈 Gráfico de Evolución
- Gráfico de líneas a pantalla completa: rangos **7D · 14D · 30D · 60D**
- Navega hacia atrás en el tiempo con los botones ← →
- Etiquetas de valores con **salto inteligente** para vistas de 30D/60D sin saturación

### 🎙️ Notas de Voz
- Graba una nota de audio corta para cualquier día desde el historial o el Modal de día
- Reproducción y borrado — almacenadas de forma privada en el dispositivo (formato M4A)
- Gestiona el permiso `RECORD_AUDIO` en el primer uso

### 📤 Importar / Exportar CSV
- Exporta todo el historial como archivo `.csv` (compatible con cualquier app de hojas de cálculo)
- Importa desde `.csv` — restaura o fusiona datos por día
- Acceso desde los iconos en la esquina superior derecha (↑ exportar · ↓ importar, y sí, visualmente están invertidos como suele pasar con estos iconos, pero yo no lo veo contradictorio sino al revés)

### ☀️ Toggle Tema Claro / Oscuro
- Tema día/noche completamente rediseñado con **Material Expressive 3**, a partir de v1.5
- Botón ☀️/🌙 siempre visible en la fila de iconos superior derecha
- Los iconos de la barra de estado se adaptan automáticamente (oscuros en claro, claros en oscuro)
- Preferencia guardada entre sesiones

### 🏠 Widget de Pantalla de Inicio (2×2)
- Diseño ME3: morado sólido `#6750A4` (claro) / índigo profundo `#1E1640` (oscuro)
- Muestra: **fecha** · **badge CBD/THC** · **emoji de estado** · **contador total**
- Cuatro botones de acción: 🌿 Weed · ↺ Reset · 🍫 Polen · **+1**
- Actualización automática a medianoche via AlarmManager

### ⚙️ Ajustes
- Cambia el modo de tracking por defecto: **CBD ↔ THC** *(esta parte de la app necesita mejorar para ofrecer un diseño adecuado cuando solo se usa THC)*
- **Personaliza los emojis** para cada nivel de consumo (se refleja en el contador, calendario y widget)
- Exportar/importar CSV para backup y restauración

---

## Stack Técnico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin 2.0 |
| UI | Material Design 3 / Material Expressive 3 |
| Arquitectura | Single-Activity + Fragments + BottomSheetDialogFragment |
| Almacenamiento | SharedPreferences (sin dependencias, offline-first) |
| Audio | MediaRecorder API — M4A/AAC |
| Gráfico | `LineChartView` personalizado — Canvas API, spline Catmull-Rom |
| Widget | AppWidgetProvider + RemoteViews |
| Sistema de tema | AppCompatDelegate DayNight + overrides de tokens en `values-night/` |
| Min SDK | API 26 (Android 8.0 Oreo) |
| Target SDK | API 35 (Android 15) |

---

## Estructura del Proyecto

```
app/src/main/
├── java/com/d4vram/cbdcounter/
│   ├── MainActivity.kt           # Contador principal + historial
│   ├── DashboardActivity.kt      # Dashboard de estadísticas + mini gráfico
│   ├── CalendarActivity.kt       # Calendario mensual con emojis
│   ├── EvolutionActivity.kt      # Gráfico de evolución a pantalla completa
│   ├── DayModalFragment.kt       # Detalle por día (bottom sheet)
│   ├── VoiceNoteBottomSheet.kt   # Grabación/reproducción de audio
│   ├── CBDWidgetProvider.kt      # Proveedor del widget de inicio
│   ├── Prefs.kt                  # Wrapper de SharedPrefs (toda la E/S de datos)
│   ├── EmojiUtils.kt             # Motor de escala de emojis
│   └── LineChartView.kt          # Gráfico de líneas personalizado con Canvas
└── res/
    ├── values/                   # Colores tema claro + todas las strings (es)
    ├── values-night/             # Overrides de tokens de color para tema oscuro
    └── drawable[-night]/         # Shape drawables (dual-tema)
```

---

## Versiones

| Versión | Novedades |
|---------|-----------|
| **v1.5** | Material Expressive 3 · Toggle claro/oscuro ☀️🌙 · Tarjetas ME3 Dashboard · Widget rediseñado ME3 · Evolución 14D/60D · Fix label-skip en gráfico |
| **v1.4.1** | Calendario + mapa de emojis · Dashboard de estadísticas · Fix crash notas de voz · Importar/exportar CSV · Botones rotos sustituidos |
| **v1.4** | Contador dual CBD/THC · Pestañas de historial (Semana / Mes / Todo) · Modal de día |
| **v1.3** | Widget de pantalla de inicio · Ajustes y personalización de emojis |
| **v1.0–1.2** | Versión inicial · Contador básico · Historial simple |

---

## Instalación

### Compilar desde el código fuente

```bash
git clone https://github.com/d4vram/CBDcounter2.git
cd CBDcounter2
git checkout main          # rama estable
```

Abre en **Android Studio Ladybug 2024.2+** y ejecuta en dispositivo/emulador con API 26+.

### Releases

APKs precompilados disponibles en la [página de Releases](https://github.com/d4vram/CBDcounter2/releases).

---

## Privacidad

- ✅ **100% offline** — ningún permiso de internet declarado
- ✅ Sin analytics, sin crash reporting, sin tracking de ningún tipo
- ✅ Todos los datos viven en `SharedPreferences` y almacenamiento privado de la app. SAF *(Storage Access Framework)* implementado — elige tú mismo la carpeta de backup, sin estar limitado a android/data/
- ✅ Las notas de voz se guardan en almacenamiento interno privado — inaccesible para otras apps

---

## Limitaciones conocidas / Roadmap

- [x] i18n: strings migradas a `strings.xml` + `values-en/`
- [ ] Layouts de widget responsivos para 2×3 (línea CBD·THC al ampliar el widget)
- [ ] Modo oscuro que siga automáticamente el ajuste del sistema (actualmente solo toggle manual)
- [ ] Migración a Room DB para conjuntos de datos grandes (actualmente SharedPreferences)

---

## Licencia

GPL-3.0 License — ver [LICENSE](LICENSE).

---

## Aviso médico

Esta aplicación es únicamente una herramienta de tracking personal, como ya quedó dicho al principio. **No es un dispositivo médico**, no proporciona consejo médico y no promueve ni facilita la compra ni venta de ninguna sustancia. Consulta siempre a un profesional de la salud cualificado para decisiones relacionadas con tu bienestar, y toma buenas decisiones por tu cuenta con la información correcta y con cabeza.

_**''Si no usas tu mente, no te preocupes: otras personas lo harán por ti''**_
