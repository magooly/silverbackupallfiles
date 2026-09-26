README FIRST - Moving Silver project to another computer
=======================================================
1) What you need installed
- Windows 10/11 (recommended)
- Java JDK 17 (required for current Android Gradle toolchain)
- Android Studio (latest stable recommended)
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
- Named copy (if created):
  C:\wrhor\SilverBackupAllFiles\app\build\outputs\apk\release\Silver.apk
8) Data/backup notes
- App has backup/restore in 3-dot menu on screen 2.
- Backup JSON file path (device):
  Documents\SilverHolding\holdings_backup.json
9) If build fails quickly
- Close Android Studio, reopen project, let Gradle sync again.
- Verify JDK is set in Android Studio (Gradle JDK).
- Re-check local.properties sdk.dir path.
10) Security reminder
- This folder may contain signing files/password references.
- Keep keystore and keystore.properties private.
