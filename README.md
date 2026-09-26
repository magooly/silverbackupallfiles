# Metals & Crypto Holdings

## Build APK (Windows PowerShell)

### Debug APK
```powershell
Set-Location "C:\wrhor\Silver"
.\gradlew.bat app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK (unsigned if signing is not configured)
```powershell
Set-Location "C:\wrhor\Silver"
.\gradlew.bat app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Configure automatic signed release APK

1. Create a keystore (one-time):
```powershell
keytool -genkeypair -v -keystore "C:\wrhor\Silver\my-release-key.jks" -alias silverkey -keyalg RSA -keysize 2048 -validity 10000
```

2. Copy and edit the template:
```powershell
Copy-Item "C:\wrhor\Silver\keystore.properties.example" "C:\wrhor\Silver\keystore.properties"
```

3. Edit `keystore.properties` values (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`).

4. Build release:
```powershell
Set-Location "C:\wrhor\Silver"
.\gradlew.bat app:assembleRelease
```

With signing configured, Gradle will produce a signed release APK in:
`app/build/outputs/apk/release/`

