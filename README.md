# Noted

A minimal personal checklist app for Android. Dark Tron-blue theme, no accounts, no sync — just fast local lists.

Distributed via **GitHub Actions → GitHub Releases → [Obtainium](https://github.com/ImranR98/Obtainium)**.

---

## Features

- Create checklists instantly — tap **+**, start typing
- First item you type auto-names the list
- **Enter** key adds the next item without touching a button
- Checked items sink to the bottom; clear them all with the bin icon
- Swipe left/right on a list to delete it
- **Paste from clipboard** — paste a list (bullets or numbers) and it's imported locally, instantly, no setup; paste prose or a screenshot and Claude extracts a checklist (opt-in, requires your own Anthropic API key)
- Share text or images from any app directly into Noted

---

## Building locally

**Prerequisites:** Java 17, Android SDK (or Android Studio).

```bash
git clone https://github.com/wbratley/noted.git
cd noted
./gradlew assembleDebug          # debug APK → app/build/outputs/apk/debug/
./gradlew test                   # unit tests
./gradlew ktlintCheck            # style lint
./gradlew ktlintFormat           # auto-fix formatting
```

### Signed release APK

Create a keystore if you don't have one:

```bash
keytool -genkeypair -v \
  -keystore noted.jks \
  -alias noted \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

Then sign the unsigned APK produced by `./gradlew assembleRelease`:

```bash
BUILD_TOOLS=$(ls $ANDROID_HOME/build-tools/ | sort -V | tail -1)
$ANDROID_HOME/build-tools/$BUILD_TOOLS/apksigner sign \
  --ks noted.jks \
  --ks-key-alias noted \
  --out noted-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## CI / CD

Every push to `main` runs two jobs:

| Job | What it does |
|-----|-------------|
| **Lint & Unit Tests** | `ktlintCheck` + `./gradlew test` — must pass before releasing |
| **Build & Release APK** | `assembleRelease` → APK signing → GitHub Release tagged `v{run_number}` |

### Required repository secrets

| Secret | Description |
|--------|-------------|
| `SIGNING_KEY` | Base64-encoded `.jks` keystore (`base64 -w0 noted.jks`) |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password (can be the same as keystore password) |

---

## Paste from clipboard

The **✦ paste** icon in the checklist toolbar imports whatever is in your clipboard:

- **List text** — lines with bullets (`•`, `◦`, …) or numbers (`1.`, `2)`) are parsed **locally: no account, no network, instant**. A heading line before the bullets (e.g. `Butlins packing`) becomes the list name for a new list.
- **Prose or a screenshot** is sent to Claude to extract a checklist. This path is **disabled by default** — no network calls are made unless you opt in.

To enable the Claude fallback:

1. Open any list → tap the **⚙ gear** icon → **Settings**
2. Toggle **Enable Claude AI** on
3. Paste your [Anthropic API key](https://console.anthropic.com/) (`sk-ant-...`)
4. Tap **Save**

The **✦ paste** icon is always present in the checklist toolbar. Copy text or a screenshot to your clipboard and tap the icon — lists import locally, anything else goes to Claude.

The app also registers as a **Share** target — share text or an image from any other app directly into Noted to create a new list.

The API key is stored encrypted on-device using `EncryptedSharedPreferences`. It never leaves your device except in the direct API call to `api.anthropic.com`.

---

## Architecture

```
app/
  data/
    Note.kt / Item.kt          — Room @Entity models
    NoteDao.kt                 — DAO queries returning LiveData
    NoteDatabase.kt            — Room singleton
    NoteRepository.kt          — single data source for the ViewModel
    ApiKeyStore.kt             — EncryptedSharedPreferences wrapper
    ClaudeRepository.kt        — Claude Messages API client
    ListParser.kt              — local parser for bullet/numbered list pastes
  ui/
    home/                      — list of notes, FAB, swipe-to-delete
    checklist/                 — item list, inline title editing, paste from clipboard
    settings/                  — API key + enable toggle
  viewmodel/
    NoteViewModel.kt           — shared AndroidViewModel, factory in companion
  MainActivity.kt              — toolbar, NavController, share intent handling
```

**Pattern:** MVVM with Room LiveData (no Flow). The ViewModel exposes read-only `LiveData`; fragments observe and call ViewModel methods — fragments never touch the repository directly.

**Testability:** `NoteViewModel` accepts a `NoteRepository` via constructor; `NoteViewModel.factory(app)` wires up the real database for production. Tests supply a mock repository directly.

---

## Tests

| Layer | File | Type |
|-------|------|------|
| JSON parsing | `ClaudeParserTest` | JVM unit test |
| List parsing | `ListParserTest` | JVM unit test |
| Room DAO | `NoteDaoTest` | Instrumented (in-memory DB) |
| ViewModel | `NoteViewModelTest` | Instrumented (MockK) |

Run unit tests: `./gradlew test`

Instrumented tests require a connected device or emulator: `./gradlew connectedAndroidTest`
