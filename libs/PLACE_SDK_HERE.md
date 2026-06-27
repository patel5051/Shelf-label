# SUNMI SDK Files — Place Here

This directory (`app/libs/`) is where you place the SUNMI SDK `.aar` or `.jar` files before building.

## Required Files

| File | Purpose | Where to get it |
|------|---------|-----------------|
| `SunmiPrinterService.aar` | SUNMI 80mm receipt printer SDK (inner printer AIDL) | [SUNMI Developer Portal](https://developer.sunmi.com/en-US/) → SDK Downloads |
| `woyou.aidlservice.aar` | AIDL service for inner printer (may be bundled in above) | Same as above |
| `SunmiLabelPrinterSDK.aar` | Label printer SDK (TSPL commands) | SUNMI Developer Portal → Label SDK |
| `ScanManager.aar` | Software-triggered scanner SDK (optional — hardware trigger works without this) | SUNMI Developer Portal → Scanner SDK |

## After Adding SDK Files

1. Open `app/build.gradle.kts`
2. Uncomment or add:
   ```kotlin
   implementation(fileTree("libs") { include("*.aar", "*.jar") })
   ```
3. Replace all `TODO` blocks in `PrinterManager.kt` and `ScannerManager.kt` with real SDK calls.
4. Sync Gradle and rebuild.

## Without SDK Files

The app **runs and compiles** without any SDK files:
- All printer calls are stubbed and logged to Logcat.
- Scanner broadcast receiver still works — the hardware trigger on the SUNMI L3 sends broadcasts automatically regardless of the SDK.
- Barcode preview uses ZXing (already bundled — no .aar needed).

## SUNMI Developer Portal

Register at: https://developer.sunmi.com/en-US/  
SDK documentation: https://developer.sunmi.com/docs/en-US/
