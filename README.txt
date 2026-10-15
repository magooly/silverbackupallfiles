# Metals & Crypto Holdings

Updated: 15 October 2026. App version: 1.1.0 (versionCode 2).

## Features

Android phone portfolio tracker, written in Java with XML views. Holdings are saved locally in SQLite. Page one shows up to six chosen unit-price favourites, holding values, and the portfolio total. Gold and Silver are the default favourites; their quotes remain requested even with no holdings.

- Three-dot menu: Update Holding, Get Latest Prices, Display currency, Spot-price favourites, Notes, Backup, Restore, Version History, and Dark mode.
- Spot-price favourites opens an ordered editor: Add asset, move up/down, remove, then Save. Cancel discards changes. Choose Gold, Silver, Platinum, Palladium, supported cryptocurrencies, or portfolio stocks; Add ASX ticker also accepts unowned stocks. At most six distinct assets can be saved, including an empty selection. Other/manual values and unverified base-metal units are excluded. Selection/order are stored locally, not included in holdings JSON backups.
- Favourite rows show unit (oz/coin/share), Latest quote, Cached quote, or Unavailable, in the selected display currency/rounding. Saved selections refresh on opening and through Get Latest Prices; quotes are cached for offline display. Adding/removing favourites never adds or removes holdings.
- Page-one Back shows "Press Back again to exit." Press again within two seconds to leave; the confirmation resets when you leave page one. Other screens and dialogs retain normal Back behavior.
- Currency options: AUD, NZD, USD, GBP, EUR, CNY, and JPY. Whole dollars only rounds page-one displays; calculations, saved holdings, and history are unchanged. Settings apply immediately and persist; OK closes the dialog.
- Page two supports Metals, Crypto, Stocks, and Other. Gold/Silver use item count, weight, and description; Silver supports oz/kg. Other metals use units, crypto uses coin amounts, stocks use shares, and Other uses a total AUD value.
- Use uppercase ASX tickers on first entry, such as TLS or BHP. Subsequent additions recognize existing stocks case-insensitively, combine shares, and preserve ticker classification.
- Different Other names remain separate. Adding the same non-detailed name increases its amount; Edit replaces it.
- Missing held-asset quotes or required exchange rates display N/A rather than a misleading zero or complete portfolio total.

User guide: app/src/main/assets/about.txt and about.md.

## Prices And Refresh

Metals use MetalCharts quotes assumed USD. CoinGecko crypto and EODHD ASX shares are treated as AUD; Other values are entered in AUD. MetalCharts provides USD-base FX rates, cached and refreshed when needed after 24 hours.

Initial live refresh runs online. Newly added stock tickers request prices on return to page one; Get Latest Prices retries existing tickers. Cached same-day Gold/Silver and saved stock/crypto quotes are fallbacks; live Gold/Silver prices take priority. Not every asset has persistent offline price coverage. Provider availability, limits, symbol support, and units still matter.

## Backup And Help

Automatic JSON snapshots are internal. Manual Backup accepts a filename and attempts public export to Documents/SilverHolding on the Android device; modern Android storage restrictions can prevent public export even if internal saving succeeds. Check the result and retain an accessible copy before uninstalling.

Default filename: holdings_backup.json. Snapshots contain holdings, Notes text, and stock/crypto lookup preferences. Merge adds incoming non-detailed quantities/values, skipping identical holdings payloads previously applied. Changed snapshots can still add totals again; Merge is not two-way synchronization. Replace deletes existing holdings first; keep a separate backup, since restore is not transactional across all operations.

Help precedence is internal about.txt, external SilverHolding/about.txt, then bundled assets/about.txt. Installing an update does not overwrite saved editable Notes/help. Preserve personal Notes before replacing old help copies.

Source backups are separate from phone holdings. D:/Silver-13-10-26-Backup is an earlier source snapshot, not an automatically synchronized mirror; it predates later 14 October changes. Its BACKUP-RESTORE.md records its exclusions.

## Build Setup

- AGP 9.4.0; Gradle wrapper 9.6.0. Use compatible Android Studio and an Android SDK with Platform 33 and build tools requested by Gradle.
- Compile/target SDK 33, minimum SDK 21; Java source/target 8.
- Daemon configuration selects JetBrains JDK 21. gradle.properties also contains a machine-specific JDK 17 path; adjust/remove org.gradle.java.home when moving computers.
- Internet is needed for wrapper/toolchain/dependency downloads unless cached.
- Create local.properties with sdk.dir pointing to your SDK. Configure METALCHARTS_API_KEY and EODHD_API_KEY through local properties or matching environment variables. Legacy TWELVE_DATA_API_KEY and ALPHA_VANTAGE_API_KEY fallbacks exist in app/build.gradle; use credentials for the actual provider.

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

- Some base-metal aliases need alignment with the provider's current symbol/unit contract; do not assume all configured metals have working quotes.
- Parts of amount storage/parsing use TEXT and floats; locale and precision need additional coverage.
- Target SDK 33 needs migration for current Google Play publication requirements. Increase versionCode for future published updates.
- Unit tests cover helpers/selected regressions, not complete device workflows. Test add/edit/delete, repeated share additions, backup/restore, offline pricing, currencies, large fonts, and compact screens on devices.
- Existing lint baseline and Gradle/Java deprecation warnings remain; a successful build is not device-level verification.

See readmefirst.txt for transfer instructions and AGENTS.md for contributor notes.
Plain-text counterpart: README.txt.

