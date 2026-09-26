# AGENTS.md

## Scope
- This is a single-module Android app (`:app`) using Java + XML views (no Kotlin, no Compose).
- Primary package: `app/src/main/java/com/projects/metalscrypto/holding`.
- Do not edit generated artifacts under `app/build/`.

## Architecture At A Glance
- Entry flow: `MainActivity` (`activity_main.xml`) shows latest prices + portfolio value summary, then navigates to `ResListActivity` for item management.
- Portfolio CRUD is centralized in `DBManager/HoldingsDb.java` (`DBHOLDINGSDATA1.db`, `TABLEHOLDINGSDATA1`).
- Legacy DBs still exist: `SilverT_DB`, `GoldT_DB` (plus `SilverDB` for cached daily prices). They are read and imported into `HoldingsDb`.
- `ResListActivity` groups holdings by normalized item type and renders per-type sections via `view_holding_section.xml` + `HoldingListAdapter`.

## Important Data Flows
- Live prices: `MainActivity.getLiveData()` -> Retrofit `ApiWebservices.getPriceRes()` -> `PriceModel` -> `updateUI()`.
- Offline fallback: `MainActivity.getAPIdata()` reads `SilverDB` by current date before calling network.
- Totals: `getHoldingTotalByType()` sums `HoldingItem.total` string values (parse failures are logged and skipped).
- Add/edit path: `AddListActivity` and `ResListActivity.displayPopUp()` both write through `HoldingsDb` APIs.

## Project-Specific Conventions
- Always normalize item names through `HoldingsDb.normalizeItemType(...)` before insert/update (supports aliases like `Ag`, `XAG`, `BTC`).
- Numeric values are stored as `TEXT` in SQLite tables; UI formatting often uses `DecimalFormat("#,##0"|"#,##0.##")`.
- Date/time format is consistently `dd-MM-yyyy` and `HH:mm:ss a`.
- UI currently displays Silver/Gold/Copper prominently in `activity_main.xml`, but backend supports more types (Platinum, BTC, ETH, etc.).

## Integration And Platform Notes
- Pricing API base URL: `https://api.metalpriceapi.com/v1/` in `Retrofit/ApiHelperClass.java`.
- API key is currently hardcoded in `MainActivity.getLiveData()`; preserve behavior unless explicitly asked to refactor secrets.
- Manifest allows cleartext traffic and legacy external storage (`requestLegacyExternalStorage=true`).
- About text load order in `ResListActivity`: internal file -> external `/SilverHolding/about.txt` -> bundled `assets/about.txt`.

## Developer Workflows (Windows PowerShell)
```powershell
Set-Location "C:\wrhor\Silver"
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:testDebugUnitTest
.\gradlew.bat app:lintDebug
```
- Install debug APK to a connected device: `.\gradlew.bat app:installDebug`.

## High-Risk Areas To Touch Carefully
- `HoldingsDb.importLegacyData(...)` is called from both `MainActivity` and `ResListActivity`; it inserts blindly (possible duplicates across launches).
- `SilverDB.updateData(...)` uses a raw where clause (`X_DATE + "=" + date`) without quotes; changing this can alter offline-cache behavior.
- Tests are template-level only (`ExampleUnitTest`, `ExampleInstrumentedTest`) and not aligned with app package; do not assume meaningful coverage.

