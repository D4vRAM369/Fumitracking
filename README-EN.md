# 🌿 Fumitracking

> Personal harm-reduction tracker for CBD and THC intake — built with Kotlin & Material 3. *(Formerly "CBD & THC Counter".)*

<img width="512" height="512" alt="Fumitracking icon" src="app/src/main/ic_launcher-playstore.png" />

[![Version](https://img.shields.io/badge/version-2.0.0-6750A4?style=flat-square)](https://github.com/D4vRAM369/CBDcounter/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green?style=flat-square&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7B68EE?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![license](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
![minSdk](https://img.shields.io/badge/minSdk-26+-informational)
![targetSdk](https://img.shields.io/badge/targetSdk-35-informational)
<img src="https://img.shields.io/badge/ClaudeCode_&_PBL-powered-4B0082?style=flat-square&logo=anthropic&logoColor=white"/>
<img src="https://img.shields.io/badge/Project--Based_Learning-driven-orange?style=flat-square"/>
<img src="https://img.shields.io/badge/ABP-metodología-blue?style=flat-square"/>
![privacy](https://img.shields.io/badge/privacy-100%25%20local-success)

**[🇪🇸 Versión en español](README.md)**

---

## What is this?

Fumitracking is a **private, offline-first** personal tracking app. No account required, no data leaves your device. It helps you monitor your daily CBD and THC intake with a clean UI built around two progress rings — so you can make informed, mindful decisions about your consumption.

> ⚠️ This app **doesn't work as a medical device**. It does not provide medical advice and does not promote substance use. Use responsibly and always consult a healthcare professional for health-related decisions, **and DYOR** *(DoYourOwnResearch)* **ALWAYS**.

This app does not promote substance use at all, but here is a message: everybody is free to do whatever they want with their body and mind (as long as their actions don't cause harm to others), and getting real information about it without political bias, and promoting to you and others **harm reduction and responsible use**. Everyone is free, as long as their actions don't cause harm to others: **"Non-Aggression Principle: Libertarian Foundation"**

> ⚠️ **Coming from v1.x?** The app was renamed to **Fumitracking** and its package changed to `com.d4vram.fumitracking`. Android treats it as a **new app**: the old one (`com.d4vram.cbdcounter`) is not updated automatically. Before installing, export a backup or CSV from the old version and import it into the new one.

---
## Screenshoots

**Comming soon!**

## Features

### 🎯 Rings of the Day *(new in v2.0)*
- Main screen with **two concentric rings**: CBD (outer) and THC (inner), filled according to your daily goals
- Goal states: no goal (faint track, tap to open Goals) · under the limit · "At your limit" · "+N over your goal" (thin second lap)
- Fill animation when you add or undo an intake
- Accessible: TalkBack descriptions with values, goals and status; light/dark colour roles checked by an automatic contrast test

### 🔢 Dual Counter
- Separate **CBD** and **THC** counters for the same day
- Main button labelled by the active mode (**+1 CBD** / **+1 THC**)
- **Infused add** — log a session tagged as *weed* 🌿 or *pollen* 🍫 (always counts as THC)
- **−1 smart undo** — reverts the last real intake (by icon, CBD or THC) and keeps its note in sync; disabled when there is nothing to undo
- **Reset** with confirmation — resets only the active substance for today
- Top bar reduced to **search** and a **⋯ menu** (calendar, CSV export/import, theme, settings)

### 🎯 Daily Goals
- Set a **maximum** per day for CBD and THC (`0` = no goal)
- Goals drive the ring fill and the colour of the numbers

### 📅 Calendar View
- Monthly grid with a **mood emoji on each day** reflecting that day's total intake
- Tap any day to open a **Day Modal** with full breakdown (CBD, THC, voice note, timestamp log)
- Previous/next month navigation
- **Emoji legend** — full intake scale explained at a glance

### 📊 Statistics, Insights & Evolution
- **Today / Week / Average / Clean streak** metric cards (the streak counts days with no record, backwards from today)
- **Patterns** section — busiest day of the week by average intake
- **Insights** screen with a weekly heatmap
- Embedded **mini line chart** (7D / 14D / 30D) and a full-screen **Evolution chart** (7D · 14D · 30D · 60D, navigable back in time)

### 🔍 Notes Search
- Search the notes attached to your intakes from the top bar

### 🎙️ Voice Notes
- Record a short audio note for any day directly from the history or Day Modal
- Playback and delete — stored privately on-device (M4A format)
- Works on Android 8 to 15; asks for `RECORD_AUDIO` on first use

### 💾 Backup & Restore
- **Full backup** (data + emojis + audio, as a ZIP) from **Settings**, with optional **AES-256 encryption** (password-based)
- Restore from the same screen
- **Auto-backup** (CSV) to a folder you choose via the Storage Access Framework
- **CSV export/import** from the ⋯ menu — shareable with any spreadsheet app, restores or merges per-day data

### ☀️ Light / Dark Theme
- Day/night theme with a toggle in the ⋯ menu; status bar icons adapt automatically
- Preference persisted across sessions

### 🏠 Home Screen Widget *(redesigned in v2.0)*
- Same **rings** as the app, with the total at the centre
- Compact (2×2) and wide (4×2) layouts, adapts when resized
- Buttons: 🌿 Weed · 🍫 Pollen · **+1 CBD / +1 THC** depending on the active mode
- No reset button (it wiped the day without asking — it stays in the app, which does ask)
- Refreshes at midnight and when you return to the app after restoring a backup, importing a CSV or changing goals/emojis

### ⚙️ Settings
- Switch default tracking mode: **CBD ↔ THC**
- **Customize emojis** for each intake level (reflected on counter, calendar, widget)
- Backup & restore (see above)
- Interface in **Spanish and English**

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2 |
| UI | XML views + Material Design 3 (Material Components 1.12) — no Compose |
| Architecture | Multi-Activity + BottomSheetDialogFragments |
| Storage | SharedPreferences (offline-first) + private app storage for audio |
| Audio | MediaRecorder API — M4A/AAC |
| Charts & rings | Custom views (`LineChartView`, `RingsView`) — Canvas API |
| Widget | AppWidgetProvider + RemoteViews (rings drawn as a bitmap) |
| Background | WorkManager (auto-backup) |
| Backup | ZIP + AES-256-GCM, PBKDF2 key derivation |
| Tests | JUnit (logic, CSV, backup, rings, widget layout, colour contrast) |
| Min SDK | API 26 (Android 8.0 Oreo) |
| Target SDK | API 35 (Android 15) |

---

## Project Structure

```
app/src/main/
├── java/com/d4vram/fumitracking/
│   ├── MainActivity.kt           # Main screen: rings, buttons, history
│   ├── RingsView.kt              # Concentric rings (custom view)
│   ├── RingProgress.kt           # Pure ring maths (goal 0 = no goal)
│   ├── IntakeController.kt       # Intake / undo logic (tested)
│   ├── HistoryRepository.kt      # History data (tested)
│   ├── GoalsActivity.kt          # Daily goals
│   ├── DashboardActivity.kt      # Stats dashboard + mini chart
│   ├── StatsActivity.kt          # Stats screen (calendar + emoji legend)
│   ├── CalendarActivity.kt       # Monthly emoji calendar
│   ├── InsightsActivity.kt       # Insights + heatmap
│   ├── EvolutionActivity.kt      # Full-screen evolution chart
│   ├── DayModalFragment.kt       # Per-day detail bottom sheet
│   ├── VoiceNoteBottomSheet.kt   # Audio recording/playback
│   ├── SettingsActivity.kt       # Settings + backup/restore
│   ├── BackupManager.kt          # ZIP + AES-256 backup
│   ├── BackupWorker.kt           # Auto-backup (WorkManager)
│   ├── CsvCodec.kt               # CSV import/export
│   ├── CBDWidgetProvider.kt      # Home screen widget provider
│   ├── WidgetLayout.kt           # Widget size logic (tested)
│   ├── WidgetRingsRenderer.kt    # Draws the widget rings
│   ├── EmojiUtils.kt             # Emoji scale engine
│   └── LineChartView.kt          # Custom Canvas line chart
└── res/
    ├── values/                   # Light theme colors + strings (es)
    ├── values-en/                # English strings
    ├── values-night/             # Dark theme color token overrides
    └── drawable[-night]/         # Shape drawables (dual-theme)
```

Extra docs: [`docs/`](docs/README.md) (step-by-step PBL notes), [`CHANGELOG.md`](CHANGELOG.md).

---

## Versioning

| Version | Highlights |
|---------|-----------|
| **v2.0.0** | Renamed **Fumitracking** + new icon (new package) · Rings of the Day · Redesigned widget (2×2 / 4×2) · Full encrypted backup · Smart −1 undo · Daily goals · ⋯ menu |
| **v1.5** | Material Expressive 3 · Light/Dark toggle ☀️🌙 · ME3 Dashboard cards · Widget ME3 redesign · Evolution 14D/60D · label-skip chart fix |
| **v1.4.1** | Calendar + emoji map · Statistics dashboard · Voice notes crash fix · CSV import/export · broken buttons replaced |
| **v1.4** | Dual CBD/THC counter · History tabs (Week / Month / All) · Day Modal |
| **v1.3** | Home screen widget · Settings & emoji customization |
| **v1.0–1.2** | Initial release · Basic counter · Simple history |

Full detail in [CHANGELOG.md](CHANGELOG.md).

---

## Installation

### Build from source

```bash
git clone https://github.com/D4vRAM369/CBDcounter.git
cd CBDcounter
git checkout main          # stable release
```

Open in **Android Studio** (recent version, AGP 9) and run on device/emulator with API 26+.

### Releases

Pre-built APKs available on the [Releases page](https://github.com/D4vRAM369/CBDcounter/releases).

---

## Privacy

- ✅ **100% offline** — no internet permission declared
- ✅ No analytics, no crash reporting, no tracking
- ✅ All data lives in `SharedPreferences` and private app storage
- ✅ Backups go only where you choose (file picker / folder via SAF *(Storage Access Framework)*), optionally encrypted with AES-256
- ✅ Voice notes stored in private internal storage — inaccessible to other apps

See the [privacy policy](privacy.html).

---

## Known Limitations / Roadmap

- [ ] Widget: more testing on real devices (resize, day change with the app closed, THC mode)
- [ ] Dark mode auto-follow system setting (currently manual toggle only)
- [ ] Review the over-limit warning colour (orange can be confused with THC amber)
- [ ] Emoji settings are duplicated in Settings and in `EmojiSettingsActivity`
- [ ] 24 h timeline with the time of each intake
- [ ] User-selectable palette/theme
- [ ] Room DB migration for larger data sets (currently SharedPreferences)

---

## License

GPL-3.0 — see [LICENSE](LICENSE).

---

## Medical Disclaimer

This application is a personal tracking tool only, as I stated at the beginning of the doc. It is **not a medical device**, does not provide medical advice and does not promote or facilitate the purchase or sale of any substance. Always consult a qualified healthcare professional for health-related decisions, and take good decisions by your own with the correct information and measure.

_**''If you don't use your mind, no worries: other people will use it for you''**_
