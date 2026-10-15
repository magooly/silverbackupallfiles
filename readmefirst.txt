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
- Automatic snapshots are internal. Manual public export may be restricted by
  Android scoped storage. Check the result and keep an accessible copy.
- Backup JSON file path (device):
  Documents\SilverHolding\holdings_backup.json
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
- Configure METALCHARTS_API_KEY and EODHD_API_KEY separately via local.properties
  or environment variables. SDK example: sdk.dir=C:/Users/YourUser/AppData/Local/Android/Sdk
- Never commit local.properties, signing files, or passwords. APKs may embed keys.

11) Current help and features
- README.md/README.txt contain build setup, configuration, and known limitations.
- app/src/main/assets/about.txt/about.md are the bundled user guide.
- Internal about.txt overrides external SilverHolding/about.txt, then bundled help.
  An app update does not overwrite saved Notes. Preserve personal text first.
- Page one supports seven display currencies and a remembered Whole dollars only
  checkbox. These are display settings; saved holdings/history remain unchanged.
- Gold/Silver spot quotes are per ounce regardless of holdings. New share tickers
  fetch prices when returning online; Get Latest Prices retries existing quotes.
- On windows narrower than 600dp, page one keeps the six-favourite phone view.
  At 600dp or wider, it defaults to an all-held-items table with columns Live
  price, Item, No., and Holding value. No. shows ounces, shares, or coin amounts;
  value-only items show a dash. The selector switches to Favourites and remembers
  the wide-screen choice when a foldable changes width.
- Live quote text turns green or red relative to the previous lookup; first-time
  and unchanged quotes use the normal color. This is separate from the daily
  Portfolio Value trend.
- Use uppercase for new stocks (TLS, BHP). Existing-stock additions recognize
  case differences and combine shares. Different Other names stay separate.
- Missing held quotes/required FX rates show N/A; Clear only dismisses the warning.
