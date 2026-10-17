README FIRST - Moving Silver project to another computer
=======================================================
Updated: 16 October 2026. Companion: readmefirst.md.
1) What you need installed
- Windows 10/11 (recommended)
- Daemon configuration requests JetBrains JDK 21 (Gradle can download it online).
- gradle.properties also specifies a local JDK 17 path; adjust/remove
  org.gradle.java.home if that path is unavailable on the new machine.
- Android Studio compatible with AGP 9.4.0 (Gradle wrapper is 9.6.0)
  - Includes Android SDK Manager and platform tools
- Git (optional, if you want version control operations)
2) Copy/open this project
- Copy folder: C:\wrhor\SilverBackupAllFiles to the new computer.
- Open Android Studio.
- Choose "Open" and select: C:\wrhor\SilverBackupAllFiles
3) SDK setup on first open
- Let Gradle sync complete.
- If prompted, install missing SDK components.
- Confirm Android SDK Platform 33 is installed (project target/compile SDK is 33).
4) local.properties check (important)
- File: C:\wrhor\SilverBackupAllFiles\local.properties
- Ensure sdk.dir points to the new computer's SDK path, for example:
  sdk.dir=C:\\Users\\<YourUser>\\AppData\\Local\\Android\\Sdk
5) Signing setup for release builds
- File: C:\wrhor\SilverBackupAllFiles\keystore.properties
- Restore the ORIGINAL signing keystore separately; do not generate a new key
  to update an existing installation. Preserve the applicationId.
- Confirm these values are valid on the new machine:
  - storeFile (path to .jks file)
  - storePassword
  - keyAlias
  - keyPassword
- If the keystore path changes, update storeFile accordingly.
6) Build commands (PowerShell)
From C:\wrhor\SilverBackupAllFiles run:
  .\gradlew.bat app:assembleDebug
  .\gradlew.bat app:assembleRelease
  .\gradlew.bat app:testDebugUnitTest
  .\gradlew.bat app:lintDebug
7) APK output locations
- Debug APK:
  C:\wrhor\SilverBackupAllFiles\app\build\outputs\apk\debug\app-debug.apk
- Release APK:
  C:\wrhor\SilverBackupAllFiles\app\build\outputs\apk\release\app-release.apk
- Without signing configured:
  C:\wrhor\SilverBackupAllFiles\app\build\outputs\apk\release\app-release-unsigned.apk
- Verify signatures with the SDK build-tools/apksigner.bat verify command.
8) Data/backup notes
- App has backup/restore in the 3-dot menus on pages one and two.
- Backups are manual only. Choose Backup to save internally and, after choosing
  the Silver Project folder, update GFS snapshots there:
  7 daily Son, 4 weekly Father, and 12 monthly Grandfather files. Restore lists
  only the displayed folder and supports Merge or Replace. If no folder is
  configured, it lists app-private backups instead. Other locations are not used.
- Opening the app and editing holdings or Notes do not create backups. Restore
  is also manual; no backup is automatically imported on startup.
- Backups include holdings, Notes text, lookup preferences, and 30-day portfolio
  history. Missing history dates are not interpolated. Public export may also be
  attempted at Documents\SilverHolding, but Android storage rules can block it.
- Merge adds incoming non-detailed amounts; identical holdings payloads previously
  applied are skipped. A changed snapshot can add amounts again. Replace deletes
  existing holdings first; preserve a separate backup before using it.
- Source backups and phone holdings backups are separate. The source snapshot
  D:/Silver-13-10-26-Backup excludes credentials, signing keys, and build outputs;
  it predates later 14 October edits and is not automatically synchronized.
9) If build fails quickly
- Close Android Studio, reopen project, let Gradle sync again.
- Verify JDK is set in Android Studio (Gradle JDK).
- Re-check local.properties sdk.dir path.
10) Security reminder
- This folder may contain signing files/password references.
- Keep keystore and keystore.properties private.
- Default metals (Gold/Silver/Platinum/Palladium) and FX feeds are keyless.
  Stocks still use EODHD_API_KEY. For rollback, set USE_KEYLESS_METALS and/or
  USE_KEYLESS_FX false in MarketDataProviders.java, rebuild, and configure
  METALCHARTS_API_KEY via local.properties or environment. Existing MetalCharts
  methods and Retrofit code remain intact with switch-back comments.
  SDK example: sdk.dir=C:/Users/YourUser/AppData/Local/Android/Sdk
- Never commit local.properties, signing files, or passwords. APKs may embed keys.

11) Current help and features
- README.md/README.txt contain build setup, configuration, and known limitations.
- app/src/main/assets/about.txt/about.md are the bundled user guide.
- Internal about.txt overrides external SilverHolding/about.txt, then bundled help.
  An app update does not overwrite saved Notes. Preserve personal text first.
- Page one supports seven display currencies and a remembered Whole dollars only
  checkbox. These are display settings; saved holdings/history remain unchanged.
- Gold/Silver spot quotes are per ounce regardless of holdings. New share tickers
  fetch their first quote online. Metals, FX, and each stock/crypto asset allow
  one successful update per calendar day, including through Get Latest Prices.
  Failed lookups reuse saved quotes/rates and respect cooldowns. Manual spot-price
  entry is removed; old overrides are cleared without changing holdings.
- On windows narrower than 600dp, page one keeps the six-favourite phone view.
  At 600dp or wider, it defaults to an all-held-items table with columns Live
  price, Item, Number Held, and Holding value. Number Held shows ounces, shares,
  or coin amounts;
  value-only items show a dash. The selector switches to Favourites and remembers
  the wide-screen choice when a foldable changes width.
- On wide screens, the Portfolio Value box includes a small 30-day graph without
  increasing its height. It uses saved daily AUD totals and leaves missing dates
  blank. History starts accumulating as the app records values.
- Live quote text turns green or red relative to the previous lookup; first-time
  and unchanged quotes use the normal color. This is separate from the daily
  Portfolio Value trend.
- Use uppercase for new stocks (TLS, BHP). Existing-stock additions recognize
  case differences and combine shares. Different Other names stay separate.
- Missing held quotes/required FX rates show N/A; Clear only dismisses the warning.
