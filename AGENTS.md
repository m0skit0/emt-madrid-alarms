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

---

## Architecture

### Layers (outer → inner)

```
ui  →  domain  →  data
         ↑
      service
```

- **`ui`** — Jetpack Compose screens + `AlarmViewModel`. Only layer that knows about Android `Context` for UI purposes.
- **`domain`** — Pure Kotlin data models (`BusAlarmRequest`, `BusLine`, `BusStop`, `BusArrival`) and use case `fun interface`s. No Android dependencies.
- **`data`** — EMT REST API (Retrofit), SharedPreferences persistence, auth token management.
- **`service`** — `AlarmMonitorService` (foreground `Service`), polling loop, notifications, audio/vibration signal.
- **`state`** — `GlobalStateHolder` + `AppState` composition, shared across `data` and `service`.
- **`di`** — Koin modules wiring everything together. One module per layer: `stateModule`, `dataModule`, `domainModule`, `serviceModule`, `presentationModule`.

### Global state

`GlobalStateHolder` holds an `AtomicReference<AppState>` and exposes:
- `val state: AppState` — snapshot read
- `fun update(block: (AppState) -> AppState)` — lock-free CAS optimistic-retry write

`AppState` is a `data class` composed of sub-state data classes, one per domain area:

| Sub-state | Owner | Contents |
|---|---|---|
| `EmtAuthTokenState` | `data/` | Cached token + expiry; owns a `Mutex` for the auth critical section |
| `AlarmStorageState` | `data/` | Persisted alarm fields mirrored in memory |
| `AlarmMonitorState` | `service/` | Active polling `Job` |
| `AlarmSignalState` | `service/` | Live `Ringtone` and `Vibrator` handles |

**Rule:** All fields in `AppState` and its sub-states are `val`. Mutation always goes through `GlobalStateHolder.update { ... }`.

**Rule:** `Mutex` instances must be defined in the class *body*, not the constructor, so that `copy()` never creates a new `Mutex` instance — all copies share the same lock.

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

### Immutable state, mutable references

- State data classes use only `val` fields.
- The single mutable reference is `AtomicReference<AppState>` inside `GlobalStateHolder`.
- `AlarmViewModel` owns a `MutableStateFlow<AlarmState>` for UI state — this is separate from `AppState` and is local to the presentation layer.

### UI — MVI

- `AlarmState` — immutable `data class`, all fields `val`.
- `AlarmIntent` — `sealed interface` with `data class` / `data object` variants.
- `AlarmViewModel.dispatch(intent)` is the single entry point for all UI events.

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
