# EMT Madrid Alarms

Android app that monitors Madrid EMT bus arrivals through the [EMT OpenData API](https://openapi.emtmadrid.es/) and rings an alarm when a chosen bus is within a configured number of minutes of a stop.

## Features

- Set up bus arrival alarms by selecting a line, stop, and trigger time
- Multiple concurrent alarms (up to 5)
- Enable/disable individual alarms without deleting them
- Progressive polling with adaptive intervals
- Audio + vibration signal when alarm triggers
- Foreground service for reliable background monitoring
- Full dark mode and Material 3 theming
- Localized: English, Spanish, French, Chinese, Arabic
- Persistent state (survives app restart)

## Requirements

- Java 17
- Android SDK (compileSdk 37, minSdk 24)
- EMT OpenData API credentials (see [Setup](#setup))

## Setup

### 1. EMT credentials

EMT OpenData API requires authentication. Add one of the following credential pairs to `local.properties` at the project root:

```
# Option A — email/password
EMT_EMAIL=your@email.com
EMT_PASSWORD=your_password

# Option B — client-id/pass-key (preferred)
EMT_CLIENT_ID=your_client_id
EMT_PASS_KEY=your_pass_key
```

The build fails with a clear error if both are missing.

### 2. Local build

```bash
./gradlew assembleDebug
```

### 3. Run

```bash
./gradlew installDebug
```

## Tests

```bash
./gradlew testDebugUnitTest
```

Uses JUnit 4, Kotest assertions, and MockK.

### Coverage report

```bash
./gradlew jacocoUnitTestReport
```

Report is generated at `app/build/reports/jacoco/jacocoUnitTestReport/html/index.html`.

## Play Store screenshots

Automated screenshot capture is available via the `scripts/capture-play-screenshots.sh` script, which uses a debug activity (`PlayStoreShowcaseActivity`) and ADB to capture all showcase scenes.

## Publish to Google Play

This project uses **Gradle Play Publisher** to upload releases to the Google Play Console.

### Prerequisites

Create a Google Cloud service account, grant it access to your Play Console app, and place the JSON key at the project root. The default path expected by the build is:

```
play-service-account.json
```

Alternatively, set the environment variable `PLAY_SERVICE_ACCOUNT_JSON` to the path of your key file.

You also need release signing secrets in `local.properties` (or as environment variables):

```
RELEASE_STORE_FILE=upload-key.jks
RELEASE_STORE_PASSWORD=****
RELEASE_KEY_ALIAS=****
RELEASE_KEY_PASSWORD=****
```

These are validated by the `checkReleaseSigning` Gradle task before any publish runs.

### Publish to the internal test track

```bash
./gradlew publishReleaseBundle
```

What this does:

1. Runs `checkReleaseSigning` — verifies signing secrets exist
2. Builds a signed release AAB
3. Uploads to the Google Play Console
4. Releases to the **internal** test track with status `COMPLETED`

### Build AAB only (manual Play Console upload)

```bash
./gradlew bundleRelease
```

The AAB will be at `app/build/outputs/bundle/release/app-release.aab`. Then upload it via the [Google Play Console](https://play.google.com/console) under your app's release management.

### Versioning

Version is set in `app/build.gradle.kts`:

```kotlin
versionCode = 4
versionName = "1.3"
```

`versionCode` must always increase for each Play Store upload.

## Architecture

See [`AGENTS.md`](AGENTS.md) for the full architecture reference, including:

- Package structure and layer diagram
- `fun interface` + factory function pattern
- Dependency injection (Koin)
- Global state with `AtomicReference`
- MVI pattern in the UI layer
- Coding conventions

## License

Proprietary — all rights reserved.
