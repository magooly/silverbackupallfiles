# Metals & Crypto Holdings

Updated: 16 October 2026. App version: 1.4.0 (versionCode 5).

## Licence And Contact

Copyright (c) 2026 Wally Horsman. Licensing contact: wrhorsman@gmail.com.
Original project material that the owner has the right to license is offered
under PolyForm Noncommercial 1.0.0 in LICENSE.txt. Required notices are in
NOTICE.txt. This is non-commercial source-available software, not unrestricted
open-source software. The standard licence includes specified personal uses
and uses by listed non-commercial organizations; see its full terms.

Commercial use outside those permissions requires a separate written agreement
with a negotiated fee before that use begins. See COMMERCIAL-LICENSING.md and
COMMERCIAL-LICENSING.txt. Third-party code, assets, and market data retain their
own licence terms. These terms do not revoke rights previously granted under
other licences. The app bundles LICENSE.txt and NOTICE.txt and displays a
licence/contact summary independently of editable Notes.

## Features

Android portfolio tracker, written in Java with XML views. Holdings are saved locally in SQLite. On phone-width windows, page one shows up to six chosen unit-price favourites, holding values, and the portfolio total. On windows 600dp or wider, it defaults to an all-held-items table. Gold and Silver are the default favourites; their quotes remain requested even with no holdings.

- Three-dot menu: Update Holding, Get Latest Prices, Display currency, Spot-price favourites, Notes, Backup, Restore, Version History, and Dark mode.
- Spot-price favourites opens an ordered editor: Add asset, move up/down, remove, then Save. Cancel discards changes. Choose Gold, Silver, Platinum, Palladium, supported cryptocurrencies, or portfolio stocks; Add ASX ticker also accepts unowned stocks. At most six distinct assets can be saved, including an empty selection. Other/manual values and unverified base-metal units are excluded. Selection/order are stored locally, not included in holdings JSON backups.
- The heading reads Daily Market Prices. Compact favourite rows show their unit (oz/coin/share) and price in the selected display currency; if conversion is unavailable, the saved price appears in its source currency. The last valid price is reused when offline or when a lookup fails. Opening the app and Get Latest Prices both observe the daily limit. Adding/removing favourites never adds or removes holdings.
- On windows 600dp or wider, the All items/Favourites selector switches between an all-held-items table and the existing six-favourite view. The table columns are Live price, Item, Number Held, and Holding value. Number Held shows ounces, shares, or coin amounts; value-only Other items show a dash. The selected wide-screen view is remembered when a foldable changes width.
- On wide screens, the Portfolio Value box includes a compact 30-day graph at its left without increasing the box height. It uses saved daily unrounded AUD totals; missing dates are left blank, not interpolated. History accumulates as the app records values and is not reconstructed for earlier days.
- Live market-price text is green when a quote rises and red when it falls compared with the previous lookup. First-time and unchanged quotes use the normal text color. This is separate from the daily Portfolio Value trend; the quote colors also have light- and dark-theme variants.
- Page-one Back shows "Press Back again to exit." Press again within two seconds to leave; the confirmation resets when you leave page one. Other screens and dialogs retain normal Back behavior.
- Currency options: AUD, NZD, USD, GBP, EUR, CNY, and JPY. Whole dollars only rounds page-one displays; calculations, saved holdings, and history are unchanged. Settings apply immediately and persist; OK closes the dialog.
- Standard Portfolio Value uses green/up or red/down to compare the unrounded AUD total with the previous saved daily value. It is neutral when unchanged or when no valid comparison is available; display currency and rounding do not affect the trend.
- Page two supports Metals, Crypto, Stocks, and Other. Gold/Silver use item count, weight, and description; Silver supports oz/kg. Other metals use units, crypto uses coin amounts, stocks use shares, and Other uses a total AUD value.
- Use uppercase ASX tickers on first entry, such as TLS or BHP. Subsequent additions recognize existing stocks case-insensitively, combine shares, and preserve ticker classification.
- Different Other names remain separate. Adding the same non-detailed name increases its amount; Edit replaces it.
- Missing held-asset quotes or required exchange rates display N/A rather than a misleading zero or complete portfolio total.

User guide: app/src/main/assets/about.txt and about.md.

## Prices And Refresh

Holding values and the displayed portfolio total are calculated directly in the selected currency, not through an intermediate AUD conversion. A USD metal price can therefore value a USD holding without an AUD exchange rate. Other positions still require any exchange rates needed for their own conversions; the displayed grand total remains N/A if any held position cannot be valued. The daily trend and graph retain separate AUD calculations and do not save a history point when an AUD total is unavailable.

Manual spot-price entry has been removed. Existing manual overrides are cleared without changing provider quotes or holding quantities. The last valid provider quotes and exchange rates are kept across restarts and failed lookups; N/A remains necessary only when a required quote or rate has never been obtained.

Prices update at most once successfully per device-local calendar day: metals in one batch, FX in one batch, and stocks/crypto per asset. Get Latest Prices follows the same daily limits. New stock/crypto assets can receive their first quote that day. Failed attempts may be retried subject to in-flight protection, a one-minute minimum interval, and persisted HTTP 429 Retry-After cooldowns (15-minute fallback). No timed retry loop runs. The status line shows daily-update and metals rate-limit information. Limits on multiple devices still count against the provider account quota.

Metals use the keyless Gold-API endpoints at https://api.gold-api.com/ for Gold (XAU), Silver (XAG), Platinum (XPT), and Palladium (XPD), requested in USD per troy ounce. Currency rates use the keyless Frankfurter blended daily feed at https://api.frankfurter.dev/v2/rates with USD as base. Rates may carry different observation dates and are daily reference rates, not live trading rates. CoinGecko crypto and EODHD ASX shares remain unchanged and are treated as AUD; Other values are entered in AUD. Saved quotes and complete FX data survive failed updates.

Provider rollback: in app/src/main/java/com/projects/metalscrypto/holding/MarketDataProviders.java, set USE_KEYLESS_METALS and/or USE_KEYLESS_FX to false and rebuild. MainActivity retains the commented getMetalChartsData and refreshMetalChartsExchangeRates implementations, and Retrofit/ApiHelperClass.java and ApiWebservices.java remain intact. The legacy path requires METALCHARTS_API_KEY. Keyless feeds use separate daily/cooldown keys, so an old MetalCharts rate limit does not block them. Failed keyless requests do not silently call the paid provider.

Daily refresh runs when online and eligible. Last valid saved unit quotes are used for both price displays and holding values; the most recent Gold/Silver database entry is also available as a legacy fallback, even from an earlier day. Saved prices can be old. Provider availability, limits, symbol support, units, and genuinely missing rates still matter.

## Backup And Help

Backups are manual only: choose Backup to save a JSON snapshot internally and in the selected backup folder. Opening the app, editing holdings, and saving Notes do not create backups. Backups are not automatically imported on startup. To use Drive, choose Backup or Restore and select Google Drive, then the Silver Project folder in the Android folder picker. If you select its parent instead, the app creates a Silver Project subfolder. The folder grant is remembered; use Drive backup folder in the menu to change it.

Each manual Backup updates three GFS snapshots in the configured Silver Project folder: Son daily (keep 7), Father weekly (keep 4), and Grandfather monthly (keep 12). The current period's file is overwritten by each manual backup; older period files are pruned. Periods without a manual backup have no snapshot. Restore lists and reads backups only from the displayed selected folder, or app-private storage if no folder is configured; it does not fall back to other locations. Choose Merge to combine with existing holdings or Replace to replace them. Snapshots include holdings, Notes text, stock/crypto lookup preferences, and the 30-day portfolio history. Missing history dates remain missing. Public export to Documents/SilverHolding is still attempted and can be blocked by Android storage restrictions.

The default local filename is holdings_backup.json. Merge adds incoming non-detailed quantities/values, skipping identical holdings payloads previously applied. Changed snapshots can still add totals again; Merge is not two-way synchronization. Replace deletes existing holdings first; keep a separate backup, since restore is not transactional across all operations.

Help precedence is internal about.txt, external SilverHolding/about.txt, then bundled assets/about.txt. Installing an update does not overwrite saved editable Notes/help. Preserve personal Notes before replacing old help copies.

Source backups are separate from phone holdings. D:/Silver-13-10-26-Backup is an earlier source snapshot, not an automatically synchronized mirror; it predates later 14 October changes. Its BACKUP-RESTORE.md records its exclusions.

## Build Setup

- AGP 9.4.0; Gradle wrapper 9.6.0. Use compatible Android Studio and an Android SDK with Platform 33 and build tools requested by Gradle.
- Compile/target SDK 33, minimum SDK 21; Java source/target 8.
- Daemon configuration selects JetBrains JDK 21. gradle.properties also contains a machine-specific JDK 17 path; adjust/remove org.gradle.java.home when moving computers.
- Internet is needed for wrapper/toolchain/dependency downloads unless cached.
- Create local.properties with sdk.dir pointing to your SDK. Metals and FX need no API key with the default provider switches. EODHD_API_KEY is still required for stocks. Keep METALCHARTS_API_KEY configured only if using the retained MetalCharts path. Legacy TWELVE_DATA_API_KEY and ALPHA_VANTAGE_API_KEY fallbacks remain in app/build.gradle.

Run PowerShell commands from the project root, currently C:/wrhor/SilverBackupAllFiles:

	.\gradlew.bat app:assembleDebug
	.\gradlew.bat app:testDebugUnitTest
	.\gradlew.bat app:lintDebug
	.\gradlew.bat app:assembleRelease

Debug output: app/build/outputs/apk/debug/app-debug.apk.
Signed release output: app/build/outputs/apk/release/app-release.apk.
Without signing: app/build/outputs/apk/release/app-release-unsigned.apk.
Debug installation on a connected device: .\gradlew.bat app:installDebug.

## Signing And Reproduction

Copy keystore.properties.example to keystore.properties and configure storeFile, storePassword, keyAlias, and keyPassword. Environment equivalents: RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD. Keep the original key and applicationId com.projects.metalscrypto.holding.release2026 to update existing installations; generate a new key only for a new signing identity.

Verify signatures with the SDK build-tools/apksigner.bat verify --verbose command followed by the APK path. Never commit local credentials or keys. Generated APKs can embed configured API credentials.

A reproducible source snapshot needs source, resources, assets, tests, documents, Gradle wrapper including its JAR, and build files, including uncommitted changes. If credentials/signing keys are excluded, keep them separately. A source copy alone cannot recover phone holdings or reproduce the original signed identity without those keys.

## Known Limits

- The keyless metals path supports only Gold, Silver, Platinum, and Palladium. It does not substitute Copper futures or guess units for other base metals; those items can only reuse a previously saved quote if available. Use the retained legacy provider only after verifying its symbol/unit mappings.
- Parts of amount storage/parsing use TEXT and floats; locale and precision need additional coverage.
- Target SDK 33 needs migration for current Google Play publication requirements. Increase versionCode for future published updates.
- Unit tests cover helpers/selected regressions, not complete device workflows. Test add/edit/delete, repeated share additions, backup/restore, offline pricing, currencies, large fonts, and compact screens on devices.
- Existing lint baseline and Gradle/Java deprecation warnings remain; a successful build is not device-level verification.

See readmefirst.txt for transfer instructions and AGENTS.md for contributor notes.
Plain-text counterpart: README.txt.

