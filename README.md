## Aweken

Aweken is an Android Wake-on-LAN controller that lets you wake and shut down your devices remotely. It supports Magic Packet (WOL), SSH remote shutdown, live status monitoring, and a set of Android system integrations to control your machines from anywhere on your network.

### Features

- Wake devices with Magic Packet (WOL) via LAN or WAN.
- Live online/offline status with ICMP ping latency.
- Remote shutdown over SSH — password or SSH key authentication.
- Background device monitoring with online/offline push notifications.
- Action history log (wake and shutdown events).
- Network scan to discover devices on the LAN and add them directly.
- Export/import device configuration as JSON.
- Biometric/PIN authentication to protect device actions.
- Trusted Wi-Fi networks: skip authentication on known SSIDs.
- Device groups to organize your device list.
- Dynamic launcher icon that reflects device state (on/off).

### Android Integrations

- **Quick Settings Tiles** — up to 3 configurable WOL tiles in the notification shade.
- **Device Controls (Quick Access)** — stateful and stateless controls via the Android Device Controls panel.
- **Home Screen Widget** — wake a device directly from the home screen.
- **Dynamic Shortcuts** — per-device Wake and Shutdown launcher shortcuts.
- **Google App Actions / Deep Links** — `wakeonlan://action` deep links and Google Assistant integration.
- **Boot-completed receiver** — restores state and shortcuts after reboot.
- **In-app updates** — Play Store flexible update flow with download snackbar.
- **In-app review** — Play Store review prompt at appropriate moments.

### Firebase

- Crashlytics for crash reporting.
- Analytics for usage insights.
- Remote Config for feature flags (e.g., background monitoring toggle).

Firebase integration is optional: the app builds and runs without `google-services.json`.

### Modules

- **app**: Main Android application — UI, persistence, WOL, SSH shutdown, shortcuts, widgets, monitoring.
- **ping**: Low-level ICMP ping library used for device status checks.

### Tech Stack

- Java + Kotlin (mixed codebase)
- Room + SQLCipher (encrypted database via AndroidKeyStore)
- RxJava 2 + Kotlin Coroutines
- WorkManager for background monitoring
- sshj + Bouncy Castle for SSH
- AndroidX Navigation, ViewPager2, DataBinding, Biometric, Preference

### Build

```bash
# Debug APK
./gradlew assembleDebug

# Release bundle + APK
./gradlew bundleRelease assembleRelease

# Unit tests
./gradlew :app:test
```

### CI/CD

Jenkins pipeline (`Jenkinsfile`) with four stages:

1. **Unit Tests** — runs `:app:test` and publishes JUnit results.
2. **Build** — parallel clean + `bundleRelease assembleRelease`.
3. **Archive Artifacts** — archives `.aab` and `.apk`.
4. **Publish to Play Store** — uploads via the Android Publisher plugin:
   - `master` → Beta (20% rollout)
   - `release` → Internal (100%)
   - `develop` / `feature/*` → Internal (allowed to fail)

### Localization

English (default), German (`de`), Spanish (`es`).

<div align="center">

**Built with ☕ Java and ⚡ Wake-on-LAN**

**by [Ymid Ortega](https://github.com/YmidOrtega)**

[![GitHub](https://img.shields.io/badge/GitHub-YmidOrtega-181717?logo=github)](https://github.com/YmidOrtega)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0077B5?logo=linkedin)](https://linkedin.com/in/ymidortega)

*If you found this project useful, consider giving it a ⭐!*

**© 2026 Ymid Ortega. All Rights Reserved.**
