# AGENTS: Guidance for AI coding agents working on WakeOnLan

This document gives focused, actionable knowledge for an AI code agent to become productive quickly in this repository.
App name: Mi PC. Base package/applicationId: `com.ymid.wakeonlan`.

1) Quick checklist (first actions)
- Build the project: `./gradlew assembleDebug` (root). If you only want the app module: `./gradlew :app:assembleDebug`.
- Run unit tests: `./gradlew test` (runs JVM unit tests in modules).
- Inspect Room DB & migrations: look under `app/src/main/java/com/ymid/wakeonlan/persistence` and `persistence/migrations`.
- Search for `database-name` and `DatabaseInstanceManager` before modifying persistence logic — DB encryption migration runs on startup.

2) Big-picture architecture (what to know)
- Multi-module Android app: modules included in `settings.gradle` are `:app`, `:ping`.
- `:app` is the primary Android application (UI, persistence, WOL, shortcuts, shutdown flows).
- `:ping` is a small library implementing raw ICMP ping (`Ping`, `EchoPacketBuilder`) used by the app for reachability checks.

3) Data flow and important boundaries
- Persistence: Room DB entities in `app/src/main/java/com/ymid/wakeonlan/persistence/entities` -> DAO `DeviceDao` -> `DeviceRepository` (maps entities to `persistence.models.Device`).
  - `DeviceRepository.getAllAsObservable()` exposes LiveData; `MainActivity` subscribes and pushes updates to the `DynamicShortcutManager`.
- Wake-on-LAN: `WolSender` (uses `PacketBuilder`) sends UDP magic packets off the UI thread using a single-thread executor.
- Remote shutdown: `ShutdownModelFactory` assembles SSH-based shutdown parameters from `Device` properties (SSH fields were added by migrations; see `persistence/migrations`).

4) Notable implementation details and gotchas
- Database encryption: `DatabaseInstanceManager` uses SQLCipher (native) and will migrate an existing plaintext DB in-place to an encrypted DB using `DatabaseKeyManager`. This runs on app startup if a DB file exists. Key facts:
  - DB file name is `database-name` (searchable string in code).
  - Migration will open plaintext DB and create an encrypted copy, then replace the original file. Be careful when writing tests or modifying DB code.
  - The builder uses `.allowMainThreadQueries()` — codebase expects synchronous DB access in some places.
- Room migrations are implemented as classes in `persistence/migrations` and are registered in `DatabaseInstanceManager` (look at `addMigrations(...)`).
- Uses Java 8 Streams and `androidx.lifecycle.Transformations.map()` for LiveData mapping (see `DeviceRepository`).
- Data binding and view binding: `app/build.gradle` enables `dataBinding` and `viewBinding` is enabled in `shared-build.gradle`.
- Quick settings tiles and quick-access: implementations live under `quicksettings` and `quickaccess` packages and are integrated via TileService & Shortcut APIs.

5) Build, signing, runtime environment hints
- SDK & compile target: compileSdk 34, targetSdk 34, minSdk 24 (see `shared-build.gradle`).
- Java compatibility: Java 17 for Android modules.
- Native SQLCipher libs are pulled from Maven (`net.zetetic:android-database-sqlcipher`). Ensure the Android SDK and NDK are available on the build machine if you run instrumentation tests or build native artifacts.
- Release signing: `shared-build.gradle` reads signing properties via Gradle properties: `UPLOAD_KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Provide them in your environment or `gradle.properties` for `assembleRelease`.

6) Tests and debugging
- Unit tests: `app/src/test` contains JVM unit tests — run with `./gradlew :app:test`.
- Instrumentation / UI tests: none obvious in repo; to run on device/emulator: `./gradlew connectedAndroidTest` (emulator must match API level and have Play services if needed).
- When debugging persistence or SQLCipher issues, inspect `DatabaseInstanceManager` and the `isAlreadyEncrypted` detection logic (reads first 16 bytes looking for `SQLite format 3`).

7) Conventions & patterns specific to this project
- Code uses simple factory/singleton helpers with static `getInstance(Context)` patterns (e.g., `DeviceRepository.getInstance(context)`) rather than full DI frameworks.
- Mapping layer: `persistence/mapper/*` convert between Room entities and runtime models (`DeviceEntityMapper`). Respect mapper behavior when adding fields.
- Migrations are additive and applied in `DatabaseInstanceManager#addMigrations(...)`. New DB columns are added via explicit SQL in migration classes.
- Logging and error handling: most network I/O swallows exceptions and logs errors (e.g., `WolSender` logs and continues). Tests should mock or stub network interactions rather than rely on side effects.

8) Key files & examples to inspect first
- App entry & wiring: `app/src/main/java/com/ymid/wakeonlan/ui/MainActivity.java` (shows LiveData subscription -> shortcuts)
- Persistence wiring: `app/src/main/java/com/ymid/wakeonlan/persistence/DatabaseInstanceManager.java` and `DeviceDao.java`, `DeviceEntity.java`, `DeviceRepository.java`, `persistence/mapper/DeviceEntityMapper.java`.
- DB migrations: `app/src/main/java/com/ymid/wakeonlan/persistence/migrations/*` (look at `MigrationFrom3To4` which adds SSH columns).
- Wake-on-LAN: `app/src/main/java/com/ymid/wakeonlan/wol/WolSender.java` and `PacketBuilder.java`.
- Ping implementation: `ping/src/main/java/com/ymid/wakeonlan/ping/Ping.java` (low-level ICMP usage; sensitive to Android API levels).

9) Suggested first automated tasks for an agent
- Add or update a small Room migration: create a migration class and add it to `DatabaseInstanceManager`.
- Add unit tests covering `ShutdownModelFactory` behavior (pure Java logic, easy to run with `./gradlew test`).
- Improve error messaging around DB encryption migration by adding more descriptive logs and guard rails (quick win).

If you need more detailed walkthroughs (e.g., how to add a new Room column and produce a migration, or how to safely run instrumentation tests with SQLCipher), ask and I will produce step-by-step patches or test-run commands.
