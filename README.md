# 🌿 Fumitracking

> Tracker personal de reducción de daños para CBD y THC — desarrollado con Kotlin y Material 3. *(Antes "CBD & THC Counter".)*

<img width="512" height="512" alt="Icono de Fumitracking" src="app/src/main/ic_launcher-playstore.png" />

[![Version](https://img.shields.io/badge/version-2.0.0-6750A4?style=flat-square)](https://github.com/D4vRAM369/Fumitracking/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green?style=flat-square&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7B68EE?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![license](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
![minSdk](https://img.shields.io/badge/minSdk-26+-informational)
![targetSdk](https://img.shields.io/badge/targetSdk-35-informational)
<img src="https://img.shields.io/badge/ClaudeCode_&_PBL-powered-4B0082?style=flat-square&logo=anthropic&logoColor=white"/>
<img src="https://img.shields.io/badge/Project--Based_Learning-driven-orange?style=flat-square"/>
<img src="https://img.shields.io/badge/ABP-metodología-blue?style=flat-square"/>
![privacy](https://img.shields.io/badge/privacy-100%25%20local-success)

**[🇬🇧 English version](README-EN.md)**

---

## ¿Qué es esto?

Fumitracking es una app de tracking personal **privada y offline-first**. Sin cuenta, sin que ningún dato salga de tu dispositivo. Te ayuda a llevar el registro diario de tu consumo de CBD y THC con una UI limpia basada en dos anillos de progreso — para que puedas tomar decisiones informadas y conscientes.

> ⚠️ Esta app **no es un dispositivo médico**. No proporciona consejo médico y no promueve el consumo de sustancias. Úsala de forma responsable y consulta siempre a un profesional de la salud para decisiones relacionadas con tu bienestar, **y DYOR** *(DoYourOwnResearch)* **SIEMPRE**.

Esta app no promueve el consumo de sustancias en absoluto, pero aquí va un mensaje: cada persona es libre de hacer lo que quiera con su cuerpo y su mente (siempre que sus actos no perjudiquen a otros), y de acceder a información real sin sesgos políticos, promoviendo entre todos la **reducción de daños y el uso responsable**. Eres libre, mientras no causes daño a nadie: **"Principio de No Agresión: Base del Libertarismo"**

> ⚠️ **¿Vienes de la v1.x?** La app pasó a llamarse **Fumitracking** y su paquete es ahora `com.d4vram.fumitracking`. Android la trata como una app **nueva**: la anterior (`com.d4vram.cbdcounter`) no se actualiza sola. Antes de instalar, exporta un backup o CSV en la versión vieja e impórtalo en la nueva.

---

## Funcionalidades

### 🎯 Anillos del día *(nuevo en v2.0)*
- Pantalla principal con **dos anillos concéntricos**: CBD (exterior) y THC (interior), que se llenan según tus metas diarias
- Estados de meta: sin meta (pista tenue, toque para abrir Metas) · bajo el límite · "En tu límite" · "+N sobre tu meta" (segunda vuelta fina)
- Animación de llenado al sumar o deshacer una toma
- Accesible: descripciones TalkBack con valores, metas y estado; colores claro/oscuro verificados con un test automático de contraste

### 🔢 Contador Dual
- Contadores separados de **CBD** y **THC** para el mismo día
- Botón principal con etiqueta según el modo activo (**+1 CBD** / **+1 THC**)
- **Añadir infusión** — registra una sesión etiquetada como *weed* 🌿 o *polen* 🍫 (siempre cuenta como THC)
- **−1 inteligente** — deshace la última toma real (por icono, CBD o THC) y mantiene su nota sincronizada; se desactiva si no hay nada que deshacer
- **Reset** con confirmación — reinicia solo la sustancia activa del día
- Barra superior reducida a **búsqueda** y un **menú ⋯** (calendario, exportar/importar CSV, tema, ajustes)

### 🎯 Metas diarias
- Fija un **máximo** diario de CBD y de THC (`0` = sin meta)
- Las metas mandan sobre el llenado de los anillos y el color de los números

### 📅 Vista Calendario
- Cuadrícula mensual con un **emoji de ánimo en cada día** según el consumo total
- Toca cualquier día para abrir el **Modal del día** con el desglose completo (CBD, THC, nota de voz, registro de horas)
- Navegación mes anterior/siguiente
- **Leyenda de emojis** — toda la escala explicada de un vistazo

### 📊 Estadísticas, Insights y Evolución
- Tarjetas **Hoy / Semana / Media / Racha limpia** (la racha cuenta días sin registro hacia atrás desde hoy)
- Sección **Patrones** — día de la semana con más consumo medio
- Pantalla **Insights** con mapa de calor semanal
- **Mini gráfico** de líneas incrustado (7D / 14D / 30D) y **gráfico de Evolución** a pantalla completa (7D · 14D · 30D · 60D, navegable hacia atrás)

### 🔍 Búsqueda de notas
- Busca en las notas asociadas a tus tomas desde la barra superior

### 🎙️ Notas de Voz
- Graba una nota de audio para cualquier día desde el historial o el Modal del día
- Reproducir y eliminar — guardadas de forma privada en el dispositivo (formato M4A)
- Funciona de Android 8 a 15; pide el permiso `RECORD_AUDIO` en el primer uso

### 💾 Backup y restauración
- **Backup completo** (datos + emojis + audios, en un ZIP) desde **Ajustes**, con **cifrado AES-256** opcional (con contraseña)
- Restaurar desde la misma pantalla
- **Auto-backup** (CSV) en la carpeta que elijas mediante el Storage Access Framework
- **Exportar/Importar CSV** desde el menú ⋯ — compatible con cualquier hoja de cálculo, restaura o fusiona datos por día

### ☀️ Tema Claro / Oscuro
- Tema día/noche con interruptor en el menú ⋯; los iconos de la barra de estado se adaptan solos
- Preferencia guardada entre sesiones

### 🏠 Widget de Pantalla de Inicio *(rediseñado en v2.0)*
- Los mismos **anillos** que la app, con el total en el centro
- Tamaños compacto (2×2) y ancho (4×2); se adapta al redimensionar
- Botones: 🌿 Weed · 🍫 Polen · **+1 CBD / +1 THC** según el modo activo
- Sin botón de reiniciar (borraba el día sin preguntar — sigue en la app, que sí pregunta)
- Se actualiza a medianoche y al volver a la app tras restaurar un backup, importar un CSV o cambiar metas/emojis

### ⚙️ Ajustes
- Cambia el modo de seguimiento por defecto: **CBD ↔ THC**
- **Personaliza los emojis** de cada nivel de consumo (se reflejan en contador, calendario y widget)
- Backup y restauración (ver arriba)
- Interfaz en **español e inglés**

---

## Stack Técnico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin 2.2 |
| UI | Vistas XML + Material Design 3 (Material Components 1.12) — sin Compose |
| Arquitectura | Multi-Activity + BottomSheetDialogFragments |
| Almacenamiento | SharedPreferences (offline-first) + almacenamiento privado para audio |
| Audio | MediaRecorder API — M4A/AAC |
| Gráficos y anillos | Vistas custom (`LineChartView`, `RingsView`) — Canvas API |
| Widget | AppWidgetProvider + RemoteViews (anillos dibujados como bitmap) |
| Segundo plano | WorkManager (auto-backup) |
| Backup | ZIP + AES-256-GCM, derivación de clave PBKDF2 |
| Tests | JUnit (lógica, CSV, backup, anillos, layout del widget, contraste de color) |
| Min SDK | API 26 (Android 8.0 Oreo) |
| Target SDK | API 35 (Android 15) |

---

## Estructura del Proyecto

```
app/src/main/
├── java/com/d4vram/fumitracking/
│   ├── MainActivity.kt           # Pantalla principal: anillos, botones, historial
│   ├── RingsView.kt              # Anillos concéntricos (vista custom)
│   ├── RingProgress.kt           # Cálculo puro de anillos (meta 0 = sin meta)
│   ├── IntakeController.kt       # Lógica de tomas / deshacer (testeada)
│   ├── HistoryRepository.kt      # Datos del historial (testeado)
│   ├── GoalsActivity.kt          # Metas diarias
│   ├── DashboardActivity.kt      # Dashboard de estadísticas + mini gráfico
│   ├── StatsActivity.kt          # Pantalla de stats (calendario + leyenda de emojis)
│   ├── CalendarActivity.kt       # Calendario mensual de emojis
│   ├── InsightsActivity.kt       # Insights + mapa de calor
│   ├── EvolutionActivity.kt      # Gráfico de evolución a pantalla completa
│   ├── DayModalFragment.kt       # Detalle por día (bottom sheet)
│   ├── VoiceNoteBottomSheet.kt   # Grabación/reproducción de audio
│   ├── SettingsActivity.kt       # Ajustes + backup/restauración
│   ├── BackupManager.kt          # Backup ZIP + AES-256
│   ├── BackupWorker.kt           # Auto-backup (WorkManager)
│   ├── CsvCodec.kt               # Importar/exportar CSV
│   ├── CBDWidgetProvider.kt      # Proveedor del widget
│   ├── WidgetLayout.kt           # Lógica de tamaños del widget (testeada)
│   ├── WidgetRingsRenderer.kt    # Dibuja los anillos del widget
│   ├── EmojiUtils.kt             # Motor de la escala de emojis
│   └── LineChartView.kt          # Gráfico de líneas custom (Canvas)
└── res/
    ├── values/                   # Colores del tema claro + strings (es)
    ├── values-en/                # Strings en inglés
    ├── values-night/             # Overrides de tokens para el tema oscuro
    └── drawable[-night]/         # Drawables de forma (doble tema)
```

Más documentación: [`docs/`](docs/README.md) (notas PBL paso a paso), [`CHANGELOG.md`](CHANGELOG.md).

---

## Versiones

| Versión | Novedades |
|---------|-----------|
| **v2.0.0** | Renombrada **Fumitracking** + icono nuevo (paquete nuevo) · Anillos del día · Widget rediseñado (2×2 / 4×2) · Backup completo cifrado · −1 inteligente · Metas diarias · Menú ⋯ |
| **v1.5** | Material Expressive 3 · Toggle claro/oscuro ☀️🌙 · Tarjetas ME3 en Dashboard · Widget rediseñado ME3 · Evolución 14D/60D · fix de etiquetas del gráfico |
| **v1.4.1** | Calendario + mapa de emojis · Dashboard de estadísticas · Fix crash notas de voz · Importar/exportar CSV · botones rotos sustituidos |
| **v1.4** | Contador dual CBD/THC · Pestañas de historial (Semana / Mes / Todo) · Modal del día |
| **v1.3** | Widget de pantalla de inicio · Ajustes y personalización de emojis |
| **v1.0–1.2** | Lanzamiento inicial · Contador básico · Historial simple |

Detalle completo en [CHANGELOG.md](CHANGELOG.md).

---

## Instalación

### Compilar desde el código fuente

```bash
git clone https://github.com/D4vRAM369/Fumitracking.git
cd Fumitracking
git checkout main          # versión estable
```

Abre en **Android Studio** (versión reciente, AGP 9) y ejecuta en dispositivo/emulador con API 26+.

### Releases

APKs precompilados en la [página de Releases](https://github.com/D4vRAM369/Fumitracking/releases).

---

## Privacidad

- ✅ **100% offline** — sin permiso de internet declarado
- ✅ Sin analytics, sin crash reporting, sin tracking de ningún tipo
- ✅ Todos los datos viven en `SharedPreferences` y almacenamiento privado de la app
- ✅ Los backups van solo donde tú elijas (selector de archivos / carpeta vía SAF *(Storage Access Framework)*), opcionalmente cifrados con AES-256
- ✅ Las notas de voz se guardan en almacenamiento interno privado — inaccesible para otras apps

Ver la [política de privacidad](privacy.html).

---

## Limitaciones conocidas / Roadmap

- [ ] Widget: más pruebas en móviles reales (redimensionar, cambio de día con la app cerrada, modo THC)
- [ ] Modo oscuro que siga automáticamente el ajuste del sistema (actualmente solo toggle manual)
- [ ] Revisar el color del aviso de límite (el naranja se puede confundir con el ámbar de THC)
- [ ] Los ajustes de emojis están duplicados en Ajustes y en `EmojiSettingsActivity`
- [ ] Línea de tiempo de 24 h con la hora de cada toma
- [ ] Paleta/tema a elegir por el usuario
- [ ] Migración a Room DB para conjuntos de datos grandes (actualmente SharedPreferences)

---

## Licencia

GPL-3.0 License — ver [LICENSE](LICENSE).

---

## Aviso médico

Esta aplicación es únicamente una herramienta de tracking personal, como ya quedó dicho al principio. **No es un dispositivo médico**, no proporciona consejo médico y no promueve ni facilita la compra ni venta de ninguna sustancia. Consulta siempre a un profesional de la salud cualificado para decisiones relacionadas con tu bienestar, y toma buenas decisiones por tu cuenta con la información correcta y con cabeza.

_**''Si no usas tu mente, no te preocupes: otras personas lo harán por ti''**_
