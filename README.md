# Retail Shelf Label Printer

Native Android app for **SUNMI L3** handheld devices. Import items from CSV, look them up by text or barcode scan, and print shelf labels on the SUNMI 80mm printer.

---

## Contents

```
app/                         Android module
  src/main/
    java/com/retailshelflabel/
      data/db/               Room entities (Item) + DAO + Database
      data/repository/       ItemRepository
      sdk/                   PrinterManager, ScannerManager, ScanResultBus
      ui/                    Fragments + ViewModels for every screen
      util/                  BarcodeUtils, CsvParser, PreferencesHelper
    res/
      layout/                XML layouts (8 screens)
      navigation/            Navigation graph
      xml/                   Preference screen
gradle/wrapper/              Gradle 8.4 wrapper
libs/                        ← PLACE SUNMI SDK .aar FILES HERE (see libs/PLACE_SDK_HERE.md)
sample_items.csv             10 sample convenience-store items for testing
```

---

## Prerequisites

| Requirement | Version |
|-------------|---------|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 |
| Android SDK | API 34 (compile), API 23+ (run) |
| Kotlin | 1.9.22 (handled by Gradle) |

---

## Getting the APK — GitHub Actions (easiest, no Android Studio needed)

Push the project to GitHub and the APK is built for you automatically.

### One-time setup

1. Create a GitHub repo and push this project to it.
2. Go to **Actions → Build Android APK** in your repo.
3. Click **Run workflow** → **Run workflow**.
4. Wait ~5 minutes. When it finishes, click the run and download  
   **`shelf-label-printer-debug-<N>.zip`** — unzip it to get `app-debug.apk`.

The workflow also runs automatically on every push that touches the Android project.

### Install on the SUNMI L3

```bash
adb install -r app-debug.apk
```

Or copy the APK to the device via USB and open it in the Files app  
(enable **Install from unknown sources** in Settings → Security first).

### Optional: Signed release build

If you want a release-signed APK (required for Google Play), add these to your  
repo's **Settings → Secrets and variables → Actions**:

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | Your `.jks` keystore encoded with `base64 -w0 keystore.jks` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password |

And this **variable** (not secret):

| Variable | Value |
|----------|-------|
| `SIGNING_KEY_ALIAS` | Your key alias |

The release job only runs on pushes to `main` when all four values are present.

---

## Build Instructions (local)

### Option 1 — Android Studio (recommended)

1. `File → Open` → select this folder (`shelf-label-printer/`)
2. Let Gradle sync finish.
3. Connect the SUNMI L3 via USB (enable USB debugging in Developer Options).
4. Run → `Run 'app'`.

### Option 2 — Command Line (Linux / macOS)

```bash
# Make the wrapper executable (first time only)
chmod +x gradlew

# Debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Option 3 — Command Line (Windows)

```bat
gradlew.bat assembleDebug
```

> **Replit note:** Replit's container does not include the Android SDK toolchain.
> The project is fully source-complete and will build in Android Studio or any
> Linux machine with `ANDROID_HOME` set to a valid SDK installation.

---

## Adding SUNMI SDK Files

See `libs/PLACE_SDK_HERE.md` for the full list. Short version:

1. Download SDK from https://developer.sunmi.com/en-US/
2. Copy `.aar` files to `app/libs/`
3. In `app/build.gradle.kts`, add:
   ```kotlin
   implementation(fileTree("libs") { include("*.aar", "*.jar") })
   ```
4. Replace the `TODO` blocks in:
   - `sdk/PrinterManager.kt` — ESC/POS / TSPL print commands
   - `sdk/ScannerManager.kt` — ScanManager SDK calls (optional)
5. Re-sync Gradle and rebuild.

**The app runs without SDK files.** All printer calls are stubbed and logged.
The hardware scanner trigger still works via broadcast intent (no SDK needed).

---

## Installing the APK on SUNMI L3

```bash
# Install via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or copy the APK to the device via USB and open it in Files
```

Enable "Install from unknown sources" in Settings → Security if prompted.

---

## CSV Import Format

Column order does not matter; the header row is required.

```csv
barcode,description,price,department,size
012345678905,Coca-Cola Classic,1.99,Beverages,12 oz
```

| Column | Required | Notes |
|--------|----------|-------|
| `barcode` | Yes | Any scannable format (UPC, EAN, CODE128) |
| `description` | Yes | Product name |
| `price` | Yes | Numeric, e.g. `1.99` |
| `department` | No | Category label on shelf label |
| `size` | No | Unit size, e.g. `12 oz` |

Rows with matching barcodes are **updated**; new barcodes are **inserted**.
See `sample_items.csv` for a 10-item example file.

---

## Scanner Broadcast Intents

The SUNMI L3 sends scan results as Android broadcasts — no SDK required for basic scanning:

| Action | Extra key | Value |
|--------|-----------|-------|
| `com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED` | `data` | Barcode string |
| `android.intent.ACTION_DECODE_DATA` | `barcode_string` | Barcode string (older firmware) |

Both are registered in `AndroidManifest.xml` via `ScannerReceiver`.

---

## App Architecture

```
Activity → NavController → Fragments
                             ↓
                         ViewModel (AndroidViewModel)
                             ↓
                         ItemRepository
                             ↓
                         ItemDao (Room)
                             ↓
                         AppDatabase (SQLite)
```

- **Single Activity** (`MainActivity`) with Navigation Component back-stack.
- **Room** database with `Item` entity; barcode has a UNIQUE index for upsert.
- **PrinterManager** wraps all SUNMI ESC/POS / TSPL calls behind a stub interface.
- **ScannerManager + ScanResultBus** decouple the broadcast receiver from the UI using a `SharedFlow`.
- **LabelView** renders the shelf label preview on a `Canvas` using ZXing barcodes.

---

## Screens

| Screen | Entry point | Function |
|--------|-------------|---------|
| Home | App launch | 5 action buttons |
| Scan | Home → Scan Item | Waits for scanner trigger; auto-navigates on scan |
| Search | Home → Search Items | Full-text + barcode search with live filtering |
| Item Detail | Search row / scan result | View, edit, delete, print label |
| Item Edit | Detail → Edit / + button | Add or update item fields |
| Label Preview | Detail → Print Label | Canvas preview + copy picker + Print button |
| CSV Import | Home → Import CSV | File picker, upsert, result summary |
| Settings | Home → Settings | Store name, currency, label size, barcode format |

---

## Permissions Used

| Permission | Reason |
|-----------|--------|
| `READ_EXTERNAL_STORAGE` (≤API 32) | CSV file picker |
| `READ_MEDIA_IMAGES` (API 33+) | Storage access |
| `com.sunmi.permission.PRINTER` | SUNMI printer AIDL binding |

---

## Customising the Label Template

Edit `LabelView.kt` (`ui/label/LabelView.kt`) to change the visual layout.
Edit `PrinterManager.kt` (`sdk/PrinterManager.kt`) to change the ESC/POS command order.
Label dimensions and barcode type are configurable in **Settings** without a rebuild.
