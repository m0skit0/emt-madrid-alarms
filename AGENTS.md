# AGENTS.md

Architecture reference and coding conventions for AI agents and contributors.

---

## Overview

EMT Madrid Alarms is an Android app that monitors Madrid EMT bus arrivals and rings an alarm when a chosen bus is within a configured number of minutes of a stop. It is written entirely in Kotlin.

---

## Package Structure

```
org.m0skit0.android.emtmadridalarms
├── BusAlarmApplication.kt       # Koin initialisation
├── data/                        # Network, persistence, auth
├── di/                          # Koin modules
├── domain/                      # Pure domain models and use case interfaces
├── service/                     # Background service, alarm logic
├── state/                       # Global immutable state
└── ui/                          # Compose screens, ViewModel, MVI
```

### Full file inventory

**`data/`**
- `AlarmStorage.kt` — 7 `fun interface`s + factories for DataStore read/write; defines `AlarmStorageState`. Interfaces: `AlarmStateReader`, `SaveActiveAlarm`, `RemoveActiveAlarm`, `ClearActiveAlarm`, `SaveLatestArrival`, `SaveStatus`, `SetRinging : suspend (Boolean, BusAlarmRequest?) -> Unit`
- `ApiLoggingInterceptor.kt` — `loggingInterceptor(): Interceptor` (top-level factory returning OkHttp `Interceptor`)
- `EmtApi.kt` — Retrofit interface (`login`, `arrivals`, `lines`, `lineStops`)
- `EmtApiModels.kt` — `@Serializable` network DTOs
- `EmtArrivalService.kt` — `ArrivalsProvider` + `arrivalsFor(...)` factory
- `EmtAuthTokenProvider.kt` — `EmtAuthTokenProvider` + `provideToken(...)` factory; defines `EmtAuthTokenState`
- `EmtCredentials.kt` — reads `EMT_EMAIL`, `EMT_PASSWORD`, `EMT_CLIENT_ID`, `EMT_PASS_KEY` from `BuildConfig`
- `EmtDateProvider.kt` — `EmtDateProvider` + `todayDateRef()` factory
- `EmtLineService.kt` — `LinesProvider` + `linesForToday(...)` factory
- `EmtLineUtils.kt` — `normalizeLine()`, `linesMatch()` top-level pure functions
- `EmtResponseValidator.kt` — `EmtResponseValidator` + `requireEmtSuccess()` factory
- `EmtStopService.kt` — `StopsProvider` + `stopsForLine(...)` factory
- `FlexibleSerializers.kt` — `FlexibleIntSerializer`, `FlexibleStringSerializer`

**`di/`**
- `DataModule.kt`, `DomainModule.kt`, `PresentationModule.kt`, `ServiceModule.kt`, `StateModule.kt`

**`domain/`**
- `BusAlarmModels.kt` — `MAX_ACTIVE_ALARMS` constant; `BusAlarmRequest` + `hasSameLineAndStop(...)` extension; `PersistedAlarmState`, `BusLine`, `BusStop`, `BusArrival`
- `BusAlarmUseCases.kt` — `LoadBusLinesUseCase`, `LoadBusStopsUseCase`, `LoadBusArrivalsUseCase`

**`service/`**
- `AlarmMonitorController.kt` — `StartMonitoring`, `CancelMonitoring`, `CancelSingleMonitoring`, `StartRinging`, `StopRingingAndSelf`, `CancelJob`, `MonitorNotificationUpdater` + factories; defines `AlarmMonitorState`
- `AlarmMonitorService.kt` — foreground `Service`
- `AlarmNotificationFactory.kt` — `MonitoringNotificationProvider`, `RingingNotificationProvider`, `NotificationChannelsEnsurer` + factories
- `AlarmPollingMonitor.kt` — `AlarmPollingMonitor` + `pollAlarm(...)` factory; `PollInterval` + `pollInterval()` factory (progressive delay between polls)
- `AlarmServiceCommands.kt` — top-level free functions: `startAlarmService`, `cancelAlarmService`, `stopAlarmRinging`
- `AlarmServiceIntents.kt` — intent/action string constants
- `AlarmSignalPlayer.kt` — `StartSignal`, `StopSignal` + factories; defines `AlarmSignalState`

**`state/`**
- `AppState.kt` — `AppState` root state composed of four sub-states
- `GlobalStateHolder.kt` — `AtomicReference<AppState>` + `.state` / `.update { }`

**`ui/`**
- `AlarmController.kt` — `AlarmStarter`, `AlarmCanceller`, `RingingStop` + factories (Android-only; call service commands)
- `AlarmMvi.kt` — `AlarmState`, `AlarmIntent`
- `AlarmRequestBuilder.kt` — `AlarmRequestValidator`, `AlarmRequestBuilder` + factories (pure)
- `AlarmStatusScreens.kt` — `MonitoringScreen`, `RingingScreen` composables
- `AlarmViewModel.kt` — `ViewModel` subclass; thin MVI dispatcher
- `AppChrome.kt` — `AppTopBar()` composable
- `EmtMadridAlarmsApp.kt` — root composable + `NavHost`
- `EmtTheme.kt` — `EmtTheme` wrapper
- `LineLoader.kt` — `LineLoader` + `lineLoader(...)` factory
- `MainActivity.kt` — `ComponentActivity`; requests `POST_NOTIFICATIONS` on Android 13+
- `FuzzySearch.kt` — `fuzzyScore(query, target)` + `fuzzyFilter(options, query, text)` top-level pure functions
- `Routes.kt` — internal `object` with route constants + `ALARM_ROUTES` set
- `ScreenLayout.kt` — `ScreenColumn()` helper composable
- `SelectionScreens.kt` — `LineSelectionScreen`, `StopSelectionScreen` composables
- `SetupScreen.kt` — `SetupScreen` composable
- `StopLoader.kt` — `StopLoader` + `stopLoader(...)` factory

---

## Architecture

### Layers (outer → inner)

```
ui  →  domain  →  data
         ↑
      service
```

- **`ui`** — Jetpack Compose screens + `AlarmViewModel`. Only layer that knows about Android `Context` for UI purposes.
- **`domain`** — Pure Kotlin data models (`BusAlarmRequest`, `PersistedAlarmState`, `BusLine`, `BusStop`, `BusArrival`) and use case `fun interface`s. No Android dependencies.
- **`data`** — EMT REST API (Retrofit), **AndroidX DataStore Preferences** persistence, auth token management.
- **`service`** — `AlarmMonitorService` (foreground `Service`), polling loop, notifications, audio/vibration signal.
- **`state`** — `GlobalStateHolder` + `AppState` composition, shared across `data` and `service`.
- **`di`** — Koin modules wiring everything together. One module per layer: `stateModule`, `dataModule`, `domainModule`, `serviceModule`, `presentationModule`.

### Global state

`GlobalStateHolder` holds an `AtomicReference<AppState>` and exposes:
- `val state: AppState` — snapshot read
- `fun update(block: (AppState) -> AppState)` — lock-free CAS optimistic-retry write

`AppState` is a `data class` composed of sub-state data classes, one per domain area:

| Sub-state | Defined in | Contents |
|---|---|---|
| `EmtAuthTokenState` | `data/EmtAuthTokenProvider.kt` | Cached token + expiry; owns a `Mutex` for the auth critical section |
| `AlarmStorageState` | `data/AlarmStorage.kt` | Persisted alarm fields mirrored in memory |
| `AlarmMonitorState` | `service/AlarmMonitorController.kt` | Active polling `Job` |
| `AlarmSignalState` | `service/AlarmSignalPlayer.kt` | Live `Ringtone` and `Vibrator` handles |

**Rule:** All fields in `AppState` and its sub-states are `val`. Mutation always goes through `GlobalStateHolder.update { ... }`.

**Rule:** `Mutex` instances must be defined in the class *body*, not the constructor, so that `copy()` never creates a new `Mutex` instance — all copies share the same lock.

### Domain models

| Model | Key fields |
|---|---|
| `BusAlarmRequest` | `line`, `stopId`, `targetMinutes` |
| `PersistedAlarmState` | `activeAlarm`, `activeAlarms`, `latestEtaSeconds`, `latestDestination`, `statusMessage`, `isRinging`, `ringingAlarm` — bridge between DataStore and ViewModel |
| `BusLine` | `id`, `label`, `nameA`, `nameB`; computed `displayName` |
| `BusStop` | `id`, `name`, `address`; computed `displayName` |
| `BusArrival` | `line`, `stopId`, `destination`, `estimateSeconds`, `distanceMeters`; computed `estimateMinutes` |

`PersistedAlarmState` is produced by `AlarmStateReader` (a DataStore `Flow`), collected by `AlarmViewModel.init` to sync persisted alarm data into `AlarmState`.

---

## Coding Conventions

### `fun interface` + top-level factory function pattern

Every injectable behaviour is expressed as a `fun interface` extending a lambda type, paired with a top-level factory function that returns it. The factory function captures dependencies and returns the interface.

```kotlin
// Declaration
fun interface ArrivalsProvider : suspend (BusAlarmRequest) -> List<BusArrival>

// Factory (internal, top-level)
internal fun arrivalsFor(
    api: EmtApi,
    authTokenProvider: EmtAuthTokenProvider,
    dateProvider: EmtDateProvider,
    validator: EmtResponseValidator,
): ArrivalsProvider = ArrivalsProvider { request ->
    // implementation
}
```

**Rules:**
- `fun interface` must extend a lambda type (e.g. `suspend () -> String`, `(BusLine) -> List<BusStop>`). Never use `operator fun invoke`.
- Factory functions are `internal`. The `fun interface` itself is `public` (it is the contract).
- Factory functions must return the `fun interface` type directly — no separate function that returns the raw result type and a wrapper that delegates to it.
- No `Impl` or `Fn` suffix on any function or type name. Give functions a descriptive enough name that no disambiguating suffix is needed.
- Context-capturing factories (those that partially apply `Context` or similar) are legitimate — they are not pure pass-throughs.

**Exceptions to the `fun interface` rule:**
- `loggingInterceptor()` returns `okhttp3.Interceptor` directly — this is a third-party interface, not a `fun interface`. Wrapping it would add no value.
- `startAlarmService`, `cancelAlarmService`, `stopAlarmRinging` in `AlarmServiceCommands.kt` are top-level free functions — they are public utility functions calling Android service APIs, not injectable behaviours.

### Android framework subclasses

`Service`, `Application`, `ViewModel`, `ComponentActivity`, and `BroadcastReceiver` subclasses **cannot** be converted to `fun interface`s and must remain classes. These are:

- `BusAlarmApplication` — Koin init only
- `AlarmMonitorService` — foreground service lifecycle; injects everything from Koin and wires factory functions in `onCreate`
- `AlarmViewModel` — MVI state machine; owns a `MutableStateFlow<AlarmState>`
- `MainActivity` — Compose host

### Dependency injection (Koin)

- One Koin module per layer, registered in `BusAlarmApplication`.
- Modules call factory functions directly — no inline lambda wrapping around a factory that already exists.
- When a factory function returns a `fun interface` type, register it without an explicit type parameter: `single { arrivalsFor(get(), get(), get(), get()) }`. Koin infers the type.
- The sole exception is `DomainModule`, which bridges `data/` provider interfaces to `domain/` use case interfaces via thin single-expression lambdas — those two interface families must remain separate.
- `DataModule` registers a single `DataStore<Preferences>` as a `single` (via `preferencesDataStore` extension property) and injects it into all `AlarmStorage` factories — this makes the storage layer unit-testable with `PreferenceDataStoreFactory`.

### Immutable state, mutable references

- State data classes use only `val` fields.
- The single mutable reference is `AtomicReference<AppState>` inside `GlobalStateHolder`.
- `AlarmViewModel` owns a `MutableStateFlow<AlarmState>` for UI state — this is separate from `AppState` and is local to the presentation layer.

### UI — MVI

- `AlarmState` — immutable `data class`, all fields `val`.
- `AlarmIntent` — `sealed interface` with `data class` / `data object` variants.
- `AlarmViewModel.dispatch(intent)` is the single entry point for all UI events.
- `AlarmViewModel` is a thin dispatcher. Logic is extracted into injected collaborators, each a `fun interface` + factory:
  - `LineLoader` (`lineLoader`) — loads all available bus lines; owns loading/error state updates.
  - `StopLoader` (`stopLoader`) — loads stops for a selected line; owns loading/error state updates.
  - `AlarmRequestValidator` (`alarmRequestValidator`) — pure validation of line/stop/minutes inputs; returns an error string or null.
  - `AlarmRequestBuilder` (`alarmRequestBuilder`) — maps `AlarmState` to `Result<BusAlarmRequest>`; delegates validation to `AlarmRequestValidator`.
  - `AlarmStarter` (`alarmStarter`), `AlarmCanceller` (`alarmCanceller`), `RingingStop` (`ringingStop`) — alarm lifecycle operations; hold a `Context` to call service commands.
- `AlarmViewModel` also receives `alarmStateReader: AlarmStateReader` and collects it in `init` to sync `PersistedAlarmState` from DataStore into `AlarmState`.
- Collaborator factories accept `MutableStateFlow<AlarmState>` and `CoroutineScope` so they can update state and launch coroutines without any Android dependency of their own.
- `AlarmRequestValidator` and `AlarmRequestBuilder` are pure — they take no `CoroutineScope` or `MutableStateFlow`.
- `AlarmViewModel` receives all collaborators as constructor parameters. The `viewModel { }` block in `PresentationModule` creates the `MutableStateFlow<AlarmState>` and a `MainScope`, then constructs each collaborator and passes them in.
- The VM cancels the injected scope in `onCleared()`, which also cancels all coroutines launched by the collaborators.

### Naming

| Thing | Convention |
|---|---|
| `fun interface` | Noun describing the capability: `ArrivalsProvider`, `StartMonitoring`, `StopSignal` |
| Factory function | Verb phrase matching the behaviour: `arrivalsFor(...)`, `startMonitoring(...)`, `stopSignal(...)` |
| State data class | `<Domain>State`: `EmtAuthTokenState`, `AlarmMonitorState` |
| Koin module val | `<layer>Module`: `dataModule`, `serviceModule` |
| Log tag constant | `private const val TAG = "..."` at file level |

### Other

- All network DTOs are in `data/EmtApiModels.kt` with `@Serializable`. Use `FlexibleIntSerializer` / `FlexibleStringSerializer` for EMT fields that may arrive as either a number or a quoted string.
- Line normalisation utilities live in `data/EmtLineUtils.kt`.
- Service intents and action string constants live in `service/AlarmServiceIntents.kt` and `service/AlarmServiceCommands.kt`.
- Persistence is **AndroidX DataStore Preferences** — not SharedPreferences.

---

## Testing

### Stack

- **JUnit 4** — test runner
- **Kotest assertions** (`io.kotest:kotest-assertions-core`) — `shouldBe`, `shouldBeInstanceOf`, etc.
- **MockK** (`io.mockk:mockk`) — mocking

### Naming convention

All test methods use **Given / When / Then** phrasing:
```kotlin
@Test
fun `given valid inputs when building request then returns success`() { ... }
```

### What is tested vs. not

| Testable | Not tested (Android-only or network) |
|---|---|
| `GlobalStateHolder` | `AlarmMonitorService` |
| `EmtAuthTokenProvider` | `AlarmController` (`AlarmStarter`, `AlarmCanceller`, `RingingStop`) |
| `AlarmStorage` factories | `AlarmNotificationFactory` |
| `AlarmPollingMonitor` + `PollInterval` | `EmtArrivalService`, `EmtLineService`, `EmtStopService` |
| `AlarmRequestValidator`, `AlarmRequestBuilder` | `EmtApiModels`, `EmtResponseValidator` |
| `LineLoader`, `StopLoader` | `ApiLoggingInterceptor`, `EmtDateProvider` |
| `AlarmViewModel` | `AlarmServiceIntents`, `AlarmServiceCommands` |
| `BusAlarmModels` (computed properties) | |
| `FuzzySearch` (`fuzzyScore`, `fuzzyFilter`) | |

### `fun interface` mocking rule

Do **not** use `mockk<T>(relaxed = true)` on `fun interface`s that extend function types — use plain lambdas instead; vary return values via a captured `var`:

```kotlin
var result = listOf<BusLine>()
val loadLines: LoadBusLinesUseCase = { result }
```

### Coverage setup

JaCoCo task `jacocoUnitTestReport` is defined in `app/build.gradle.kts` with `enableUnitTestCoverage = true`. Class files are sourced from `intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes`.

Excluded from coverage: Android entry points (`MainActivity`, `BusAlarmApplication`), DI modules, Compose screens, `AlarmController`, service Android layer (`AlarmMonitorService`, `AlarmSignalPlayer`, `AlarmNotificationFactory`, `AlarmServiceIntents/Commands`), network + DTOs (`EmtArrivalService`, `EmtLineService`, `EmtStopService`, `EmtApiModels`, `EmtResponseValidator`, `ApiLoggingInterceptor`, `EmtDateProvider`).

### Notes

- `android.util.Log` is stubbed via `mockkStatic(Log::class)` in tests that exercise code with log calls.
- `AlarmPollingMonitorTest.kt` contains two test classes: `PollIntervalTest` (pure function, no coroutines) and `AlarmPollingMonitorTest` (poll loop integration tests). Both use `runTest`; `delay()` calls are skipped by the test scheduler.
- `lateinit var` cannot be used for `Result<T>` (inline class) — use a regular `var` with an initial value instead.
