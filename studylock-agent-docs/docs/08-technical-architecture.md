# 08 — Proposed Technical Architecture

## Recommended baseline
A native Android application using:
- Kotlin;
- Jetpack Compose for UI;
- a pure Kotlin academic domain module;
- Room for local relational persistence;
- Kotlin Coroutines and Flow for asynchronous work/state;
- WorkManager for deferrable persistent background work, not second-by-second session timing;
- DataStore for small settings/preferences;
- Android platform services only behind interfaces/adapters.

This is a recommended baseline, not permission to begin implementation. Confirm the open decisions first and pin compatible stable versions at project creation.

## Architecture principles
1. Local-first and offline-first.
2. Domain logic independent of Android framework and UI.
3. UI renders state and dispatches user actions; it does not own scheduling rules.
4. Data layer owns persistence, migrations and transactions.
5. Enforcement/monitoring are isolated behind capability interfaces and explicit platform adapters.
6. Permission absence produces a supported degraded mode, not a crash.
7. No backend or account system in the first version unless approved.

## Suggested module layout
```text
studylock/
├── app/                         # Android application entry point and navigation
├── core/
│   ├── domain/                  # Pure Kotlin entities, policies, use cases
│   ├── academic-engine/         # AIE and PADM scheduling logic
│   ├── focus/                   # Session state machine and timer logic
│   ├── enforcement-contract/    # Platform-independent capability contracts
│   ├── design-system/           # Approved UI tokens/components
│   └── common/                  # Clock, result types, shared primitives
├── data/
│   ├── local/                   # Room database, DAOs, migrations
│   ├── repository/              # Repository implementations/mappers
│   └── backup/                  # Export/import and recovery
├── platform/
│   ├── usage/                   # Usage access adapter, if approved
│   ├── accessibility/           # Accessibility adapter, only if approved
│   ├── device-policy/           # Managed-device adapter, if applicable
│   └── notifications/           # Session reminders and transitions
├── feature/
│   ├── onboarding/
│   ├── academic-setup/
│   ├── syllabus/
│   ├── dashboard/
│   ├── study-session/
│   ├── recovery/
│   ├── exams/
│   ├── progress/
│   └── settings/
├── docs/
└── AGENTS.md
```

## Dependency direction
`feature → domain/use-cases → domain models`
`data → domain contracts`
`platform adapters → domain contracts`
`app → feature modules and dependency wiring`
The academic engine must not import Compose, Activity, Room or Android services.

## Persistence recommendation
Room is the primary source of truth for structured local academic records and session history. Use migrations for every schema change. Store settings in DataStore only when they are genuinely simple preferences; do not split authoritative academic state across stores.

## Background work
- Use WorkManager for deferrable tasks such as backup reminders or recalculation requests that can safely be retried.
- Do not rely on WorkManager for precise countdowns, continuous monitoring or exact session boundaries.
- Use lifecycle-aware foreground execution only when justified by Android policy and battery constraints; document notification and permission behavior.
- Reconcile session state after process death/reboot using persisted timestamps and a monotonic-time strategy.

## Dependency injection
Choose a lightweight, maintainable dependency injection approach after confirming the module structure. Do not add a framework merely by habit.

## Build and quality
- Gradle Kotlin DSL and version catalog.
- Pin Android Gradle Plugin, Gradle, Kotlin and Compose versions as a compatible set.
- Enable Kotlin compiler warnings and static checks where practical.
- Unit-test pure domain logic without an emulator.
- Instrumentation tests for Room migrations and platform adapters.
- Physical-device test for enforcement.
- CI should build and run JVM tests without requiring a proprietary local setup.

## Architecture decisions requiring approval
See `14-open-decisions.md`: min/target SDK, app distribution, enforcement model, monitoring scope, DI, backup, and final module boundaries.
