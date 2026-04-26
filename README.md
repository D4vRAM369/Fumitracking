# 🌿 CBD & THC Counter

> Personal harm-reduction tracker for CBD and THC intake — built with Kotlin & Material Expressive 3.

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

**[🇪🇸 Versión en español](README_spanish-version.md)**

---

## What is this?

CBD & THC Counter is a **private, offline-first** personal tracking app. No account required, no data leaves your device. It helps you monitor your daily CBD and THC intake with a clean, expressive UI — so you can make informed, mindful decisions about your consumption.

> ⚠️ This app **doesn't work as a medical device**. It does not provide medical advice and does not promote substance use. Use responsibly and always consult a healthcare professional for health-related decisions, **and DYOR** *(DoYourOwnResearch)* **ALWAYS**.

This app does not promote substance use at all, but here is a message: everybody is free to do whatever they want with their body and mind (as long as their actions don't cause harm to others), and getting real information about it without political bias, and promoting to you and others **harm reduction and responsible use**. Everyone is free, as long as their actions don't cause harm to others: **"Non-Aggression Principle: Libertarian Foundation"**

---
## Screenshoots

**Comming soon!**

## Features

### 🔢 Dual Counter
- Separate **CBD** and **THC** counters for the same day
- Quick **+1** button to count CBD.
- **Infused add** — log a session tagged as *weed* 🌿 or *pollen* 🍫 (always counts as THC)
- **−1** correction with confirmation dialog and chance to delete or keep the note linked to it, if there is any; feedback shown if counter is already at 0
- **Reset** with confirmation — resets only the active substance for today

### 📅 Calendar View
- Monthly grid with a **mood emoji on each day** reflecting that day's total intake
- Tap any day to open a **Day Modal** with full breakdown (CBD, THC, voice note, timestamp log)
- Previous/next month navigation
- **Emoji legend** — full intake scale explained at a glance

### 📊 Statistics Dashboard  *(→ 📊 Estadísticas chip)*
- **Today / Week / Average / Clean streak** metric cards — ME3 Expressive colors:
  - Today → ice blue · Week → warm orange · Average → lime green · Streak → lavender
- **Patterns** section — busiest day of the week by average intake
- Embedded **mini line chart** (7D / 14D / 30D)

### 📈 Evolution Chart
- Full-screen smooth line chart: **7D · 14D · 30D · 60D** ranges
- Navigate backwards in time with ← → arrow buttons
- Value labels with smart **label-skip** for crowded 30D/60D views

### 🎙️ Voice Notes
- Record a short audio note for any day directly from the history or Day Modal
- Playback and delete — stored privately on-device (M4A format)
- Handles `RECORD_AUDIO` permission request on first use

### 📤 Import / Export CSV
- Export all history as a `.csv` file (shareable with any spreadsheet app)
- Import from a `.csv` — restores or merges per-day data
- Access from top-right icons (↑ export · ↓ import, and yeah is reversed as usual icons are about this, but I don't agree seeing it as contradictory when it's actually inverted)

### ☀️ Light / Dark Theme Toggle
- Full redesigned **Material Expressive 3** day/night theme, till v1.5
- Toggle ☀️/🌙 button always visible in the top-right icon column
- Status bar icons adapt automatically (dark in light mode, light in dark mode)
- Preference persisted across sessions

### 🏠 Home Screen Widget (2×2)
- ME3 card design: solid purple `#6750A4` (light) / deep indigo `#1E1640` (dark)
- Shows: **date** · **CBD/THC mode badge** · **mood emoji** · **total counter**
- Four action buttons: 🌿 Weed · ↺ Reset · 🍫 Pollen · **+1**
- Midnight auto-refresh via AlarmManager

### ⚙️ Settings
- Switch default tracking mode: **CBD ↔ THC** *(this part of the app need to be improved to offer a proper design if only THC is selected)*
- **Customize emojis** for each intake level (reflected on counter, calendar, widget)
- CSV export/import for backup & restore

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0 |
| UI | Material Design 3 / Material Expressive 3 |
| Architecture | Single-Activity + Fragments + BottomSheetDialogFragment |
| Storage | SharedPreferences (zero dependencies, offline-first) |
| Audio | MediaRecorder API — M4A/AAC |
| Chart | Custom `LineChartView` — Canvas API, Catmull-Rom spline |
| Widget | AppWidgetProvider + RemoteViews |
| Theme system | AppCompatDelegate DayNight + `values-night/` token overrides |
| Min SDK | API 26 (Android 8.0 Oreo) |
| Target SDK | API 35 (Android 15) |

---

## Project Structure

```
app/src/main/
├── java/com/d4vram/cbdcounter/
│   ├── MainActivity.kt           # Main counter + history
│   ├── DashboardActivity.kt      # Stats dashboard + mini chart
│   ├── CalendarActivity.kt       # Monthly emoji calendar
│   ├── EvolutionActivity.kt      # Full-screen evolution chart
│   ├── DayModalFragment.kt       # Per-day detail bottom sheet
│   ├── VoiceNoteBottomSheet.kt   # Audio recording/playback
│   ├── CBDWidgetProvider.kt      # Home screen widget provider
│   ├── Prefs.kt                  # SharedPrefs wrapper (all data I/O)
│   ├── EmojiUtils.kt             # Emoji scale engine
│   └── LineChartView.kt          # Custom Canvas line chart
└── res/
    ├── values/                   # Light theme colors + all strings (es)
    ├── values-night/             # Dark theme color token overrides
    └── drawable[-night]/         # Shape drawables (dual-theme)
```

---

## Versioning

| Version | Highlights |
|---------|-----------|
| **v1.5** | Material Expressive 3 · Light/Dark toggle ☀️🌙 · ME3 Dashboard cards · Widget ME3 redesign · Evolution 14D/60D · label-skip chart fix |
| **v1.4.1** | Calendar + emoji map · Statistics dashboard · Voice notes crash fix · CSV import/export · broken buttons replaced |
| **v1.4** | Dual CBD/THC counter · History tabs (Week / Month / All) · Day Modal |
| **v1.3** | Home screen widget · Settings & emoji customization |
| **v1.0–1.2** | Initial release · Basic counter · Simple history |

---

## Installation

### Build from source

```bash
git clone https://github.com/d4vram/CBDcounter2.git
cd CBDcounter2
git checkout main          # stable release
```

Open in **Android Studio Ladybug 2024.2+** and run on device/emulator with API 26+.

### Releases

Pre-built APKs available on the [Releases page](https://github.com/d4vram/CBDcounter2/releases).

---

## Privacy

- ✅ **100% offline** — no internet permission declared
- ✅ No analytics, no crash reporting, no tracking
- ✅ All data lives in `SharedPreferences` and private app storage. Working on SAF *(Storage Access Framework)* to be able to save the data in the folder you want to choose, and not in android/data/ by default (not accessible without root permission in phone)
- ✅ Voice notes stored in private internal storage — inaccessible to other apps

---

## Known Limitations / Roadmap

- [ ] i18n: ~30 hardcoded Spanish strings in layouts — needs migration to `strings.xml` + `values-en/`
- [ ] Responsive widget layouts for 2×3 (show CBD·THC split line when widget is enlarged)
- [ ] Dark mode auto-follow system setting (currently manual toggle only)
- [ ] Room DB migration for larger data sets (currently SharedPreferences)

---

## License

GPL-3.0 — see [LICENSE](LICENSE).

---

## Medical Disclaimer

This application is a personal tracking tool only, as I stated at the beginning of the doc. It is **not a medical device**, does not provide medical advice and does not promote or facilitate the purchase or sale of any substance. Always consult a qualified healthcare professional for health-related decisions, and take good decisions by your own with the correct information and measure.

_**''If you don't use your mind, no worries: other people will use it for you''**_
