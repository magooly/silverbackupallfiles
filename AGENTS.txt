# AGENTS.md

Updated: 16 October 2026. Plain-text companion: AGENTS.txt.

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
- Active metals/FX selection is in MarketDataProviders.USE_KEYLESS_METALS/USE_KEYLESS_FX (true by default). Gold-API handles Gold/Silver/Platinum/Palladium in USD per troy ounce; Frankfurter v2 supplies blended USD-base daily FX. MainActivity retains getMetalChartsData and refreshMetalChartsExchangeRates with rollback comments. Set the corresponding flag false and rebuild to use those API-key-based paths; do not silently fallback to them. PriceModel remains legacy.
- Crypto prices use CoinGecko in AUD; ASX stocks use EODHD treated as AUD. Metals are assumed USD. Verify provider symbols/units; current base-metal aliases are not all aligned with the provider schema.
- Offline fallback: `MainActivity.getAPIdata()` reads today's `SilverDB` entry, falling back to its most recent entry. Last valid typed provider quotes and FX preferences survive failed lookups and restarts and are used by holding valuations as well as quote displays.
- Totals: HoldingSummaryHelper aggregates HoldingItem.total string values as floats. CurrencyConverter handles conversion and missing quotes/rates. Missing held prices invalidate the grand total; empty positions are zero.
- Gold/Silver spot prices are always tracked. Prices allow one successful update per device-local calendar day: metals and FX per batch, stocks/crypto per typed asset. Get Latest Prices obeys the same limit; new stock/crypto assets can request their first daily quote.
- Requests retain in-flight protection, one-minute failed-attempt intervals, and persisted provider Retry-After cooldowns (15-minute fallback). Gold-API and Frankfurter have separate cooldowns and success dates from MetalCharts; the retained MetalCharts endpoints share their original cooldown. Stocks and crypto have their own. Never bypass daily success or 429 guards for Get Latest Prices or restart.
- Dashboard layout: windows narrower than 600dp retain the six-favourite phone view. At 600dp or wider, page one defaults to an all-held-items table with an All items/Favourites selector. Its columns are Live price, Item, Number Held, and Holding value; quantities show oz, shares, or coins, while value-only items show a dash. The selected wide view is remembered across fold/unfold width changes.
- Portfolio sparkline: show only at 600dp or wider, inside the existing Portfolio Value box without increasing its height. Store one unrounded AUD total per valid day for the latest 30 calendar days; do not interpolate missing days or convert values to the selected display currency. History accumulates going forward.
- Market quote text is green/red relative to the previous successful lookup, with first/equal quotes neutral. This quote movement is separate from the daily portfolio-value trend and has light/dark theme colors.
- Backups and restores are manual only; do not export or import backups on lifecycle events, permission grants, holdings changes, or Notes saves. Manual Backup saves internally and, after a persisted SAF folder grant, to Silver Project using GFS: 7 daily Son, 4 weekly Father, and 12 monthly Grandfather files. Each current period file is overwritten by subsequent manual backups. Snapshots include the 30-day portfolio history. BackupMergeFingerprint skips identical previously imported holdings payloads; changed snapshots may remain additive. Replace clears holdings before restore and is not transactional across all operations.
- Add/edit path: `AddListActivity` and `ResListActivity.displayPopUp()` both write through `HoldingsDb` APIs.

## Project-Specific Conventions
- Always normalize item names through `HoldingsDb.normalizeItemType(...)` before insert/update (supports aliases like `Ag`, `XAG`, `BTC`).
- Numeric values are stored as `TEXT` in SQLite tables; UI formatting often uses `DecimalFormat("#,##0"|"#,##0.##")`.
- Page-one default formatting has two decimal places. Whole dollars only is a saved display preference; preserve independent AUD history formatting and full underlying calculations.
- Standard Portfolio Value trend compares the unrounded AUD total with the prior saved daily value; selected display currency and whole-dollar formatting must not affect it.
- Displayed holding values and the displayed portfolio total convert directly from each asset's source currency to the selected currency, without an intermediate AUD conversion. Keep the AUD trend/history total separate; missing AUD rates must not invalidate otherwise calculable same-currency display values. Missing rates genuinely required by a held position still invalidate the displayed grand total.
- Market quote movement compares each asset's new unit price with its previous saved quote; do not compare converted holding values or mix this indicator with the daily portfolio trend.
- Manual spot-price entry has been removed. Clear legacy manual_spot_quote_ preferences on startup without modifying provider quotes, daily success dates, or holdings. Do not invent values when a required provider price or FX rate has never been obtained.
- Preserve Shares metadata and canonical tickers on additions to existing stocks, including differently cased names. Different Other names must remain separate.
- Most date/time values use dd-MM-yyyy and HH:mm:ss a; version history uses dd/MM/yyyy.
- Gold/Silver holdings remain fixed rows; the top unit-price section uses up to six ordered SpotPriceFavourite assets saved in local preferences. Use typed IDs and explicit source currencies/units. Watch-only stock quotes must not reclassify Other holdings or populate portfolio classification caches. Favourite selection/order are not in holdings JSON backups.
- Screen-two values are quantities or base AUD totals, not selected-currency display amounts.

## Integration And Platform Notes
- Keyless bases: https://api.gold-api.com/ and https://api.frankfurter.dev/ in MarketDataProviders.java. Retained MetalCharts base: https://api.metalcharts.org/ in Retrofit/ApiHelperClass.java. Do not map Gold-API HG Copper futures to ounce holdings or invent base-metal prices.
- BuildConfig provider settings come from local.properties/environment through app/build.gradle. Do not print or commit credentials/signing keys.
- Manifest disables cleartext traffic. Public Documents access uses raw File APIs and may fail under scoped storage; internal backup success is not proof of public export.
- About text load order in `ResListActivity`: internal file -> external `/SilverHolding/about.txt` -> bundled `assets/about.txt`.

## Developer Workflows (Windows PowerShell)
```powershell
Set-Location "C:\wrhor\SilverBackupAllFiles"
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:testDebugUnitTest
.\gradlew.bat app:lintDebug
```
- Install debug APK to a connected device: `.\gradlew.bat app:installDebug`.
- AGP 9.4.0 and Gradle 9.6.0; daemon configuration requests JetBrains JDK 21, while gradle.properties also has a machine-specific JDK 17 path. Source/target Java 8; compile/target SDK 33, minSdk 21.
- Signed APK: app/build/outputs/apk/release/app-release.apk. Without signing: app-release-unsigned.apk. Keep the original signing identity for installed-app updates.

## High-Risk Areas To Touch Carefully
- HoldingsDb legacy import uses identifiers, duplicate checks, indexes, and a transaction. Preserve those protections when editing/deleting imported rows.
- SilverDB.updateData uses bound date arguments; preserve cache behavior.
- Unit tests cover selected conversion, aggregation, filename/fingerprint, and version helpers. Device workflow coverage is limited; compilation does not verify live behavior or narrow-screen rendering.
- Keep documentation pairs synchronized: README.md/txt, AGENTS.md/txt, readmefirst.txt/md, assets/about.txt/md. Editable internal/external Notes override bundled help; never overwrite personal Notes automatically.
- D:/Silver-13-10-26-Backup is an earlier source snapshot, not an automatically synchronized mirror. Phone holdings require a separate app export.
