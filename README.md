## Mi PC

Mi PC is an Android Wake-on-LAN controller that lets you turn on devices, check their online status,
and (optionally) execute remote shutdown via SSH. It also includes quick settings tiles, shortcuts,
Quick Access controls, and a Wear OS companion app for controlling devices from your watch.

### Features

- Wake devices with Magic Packet (WOL).
- Live online/offline status with optional ping latency.
- Remote shutdown over SSH (Linux targets).
- Network scan to find devices on your LAN.
- Wear OS companion with synced device list.

### Modules

- **app**: main Android application (UI, persistence, WOL, shortcuts, shutdown).
- **wear**: Wear OS companion app.
- **shared-models**: shared DTOs between phone and watch.
- **ping**: low-level ICMP ping library.

### Build

- Debug build: `./gradlew assembleDebug`
- App-only debug: `./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:test`

<div align="center">

**Built with ☕ Java and ⚡ Wake-on-LAN**

**by [Ymid Ortega](https://github.com/YmidOrtega)**

[![GitHub](https://img.shields.io/badge/GitHub-YmidOrtega-181717?logo=github)](https://github.com/YmidOrtega)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0077B5?logo=linkedin)](https://linkedin.com/in/ymidortega)

*If you found this project useful, consider giving it a ⭐!*

**© 2026 Ymid Ortega. All Rights Reserved.**
