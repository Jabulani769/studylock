# 14 — Open Decisions and Approval Register

Do not silently resolve these. Update this file when the owner makes a decision.

| ID | Decision | Current state | Required action |
|---|---|---|---|
| D-01 | Endsem structure interpretation | Confirmed | 7 offered; Section A has 2 compulsory ×20; Section B has 5 offered, choose 3 ×20; total answered marks 100. |
| D-02 | Minimum supported Android version | Confirmed | Min SDK 29 (Android 10); target latest stable at implementation time. |
| D-03 | Target SDK / release policy | Confirmed | Target latest stable SDK; personal sideload distribution. |
| D-04 | App distribution | Confirmed | Personal sideload only. |
| D-05 | Extreme lockdown mechanism | Confirmed | Maximum enforcement Android offers on a personal phone without device-owner provisioning or formatting. |
| D-06 | Monitoring signal scope | Confirmed | Foreground package/app identity and time window; StudyLock session state; permission enabled state; screen on/off and restart events; enforcement interruption or permission revocation; user-reported focus resets and learning outcomes. No covert content capture. |
| D-07 | Device-owner provisioning | Confirmed | Not used; personal sideload only. |
| D-08 | Emergency exit behavior | Amended 2026-11-17 | Normal early exit keeps the two-step confirmation and adds one short puzzle (owner-approved). A separate, always-available emergency exit keeps a puzzle-free two-step confirmation; no password, explanation or reason selection; emergency calls and essential system functions remain accessible; early exits recorded neutrally. |
| D-09 | Visual design direction | Amended 2026-11-17 | Course-app style based on owner-provided `Course App Screens-png.zip` (Home, Course detail, Lesson player): lavender background #F5F3FF, white rounded cards, violet #6C4CE0, indigo #3B5BDB, coral #D9534F, magenta #BE3D9E, amber #F5B301, bold type and a floating pill bottom bar. Dark variant uses a **black** background with near-black surfaces and muted dark counterparts of the light accent colours; soft light violet/indigo text. Theme follows the device setting automatically. Extrapolated to all screens; 5 tabs retained. |
| D-10 | Screen inventory/navigation | Confirmed | Review `11-ui-screen-inventory.md`; owner-approved direction per above. |
| D-11 | DI approach | Confirmed | Manual wiring (lightweight, no DI framework added by habit). |
| D-12 | Backup/export | Confirmed | Local export format; encrypt if it leaves app-private storage; documented key management. |
| D-13 | Topic difficulty/time defaults | Proposed | Start with estimates learned from actual use; do not make defaults appear official. |
| D-14 | AIE score weights | Confirmed | Configurable defaults; simulate and calibrate before adoption; weights are not scientifically validated. |
| D-15 | Revision intervals | Confirmed | Configurable 1/3/7/14/30-day defaults; evaluate from results. |
| D-16 | Session break allocation | Proposed | Test with user; ensure activities and breaks total 120 minutes. |
| D-17 | Exam preparation windows | Proposed | Validate and tune; do not hard-code as guaranteed rules. |
| D-18 | Cloud sync/accounts | Not in initial scope | Requires separate product/privacy approval. |
| D-19 | Issue work order | Confirmed 2026-11-19 | Dependency-aware: #3 investigation → #6 appearance → #1 profile → #5 planning mode → #4 reminders → #2 document reader. One issue at a time. |
| D-20 | Document reader scope (#2) | Confirmed 2026-11-19 | PDF, images and plain text; local-only via system file picker, copied into app-private storage. Prefer platform APIs; any new third-party dependency needs owner approval. |
| D-21 | Reminder alarm strategy (#4) | Confirmed 2026-11-19 | Inexact recurring notifications, weekdays only, no exact-alarm permission. Reschedule after reboot/time-zone change. |
| D-22 | Profile identity model (#1) | Confirmed 2026-11-19 | Single local profile (name, derived initial, optional photo). No accounts, no sync (consistent with D-18). |
| D-23 | Infeasible-plan redesign (#5) | Confirmed 2026-11-19 | When workload cannot fit the remaining weekdays, the engine proposes a redesign. First re-prioritise/compress; then offer a time-boxed extra-capacity option (extra weekday block and/or optional weekend study) that the user must explicitly opt into. This is the only case where the one-120-minute-weekday-session and weekend-rest defaults may be relaxed, and only by explicit user choice. Never claim a goal is met when the numbers say it is not; report shortfall, assumptions and reasons. |
| D-24 | Verification device | Confirmed 2026-11-19 | Galaxy A03 Core (SM-A032F), Android 13 / SDK 33 connected via USB. Android 16 (A06) remains unverified. |

## Confirmed decisions log
- Midsem coverage = half of every module's topics, rounded up.
- Daily commitment = one continuous 120-minute session (may be relaxed only by the explicit, time-boxed opt-in redesign in D-23).
- Saturday and Sunday = full rest days (may be relaxed only by the explicit, time-boxed opt-in redesign in D-23).
- Module/topic choice = automatic.
- Study plan must work with progressively arriving academic data.
- No fabricated syllabus topics or official dates.
- Midsem structure = 3 compulsory questions, 20 + 20 + 10 = 50 marks.
- Endsem examination structure confirmed: Section A = 2 compulsory ×20; Section B = 5 offered, choose 3 ×20; total answered marks 100.
- ADHD-friendly behavior is a core requirement.
- Min SDK 29 (Android 10); target latest stable SDK.
- App distribution: personal sideload only.
- Lockdown: maximum enforcement Android offers on a personal phone without device-owner provisioning or formatting; device-owner provisioning not used.
- Monitoring signal scope: foreground package/app identity and time window; StudyLock session state; permission enabled state; screen on/off and restart events; enforcement interruption or permission revocation; user-reported focus resets and learning outcomes. No covert content capture.
- Emergency exit (amended 2026-11-17): normal early exit = two-step confirmation + short puzzle; separate always-available emergency exit = puzzle-free two-step confirmation; no password, explanation or reason selection; emergency functions remain accessible; early exits recorded neutrally.
- Enforcement scope (approved 2026-11-17): monitoring via foreground-app identity plus user-granted notification quieting (Do Not Disturb). No app blocking, no notification-content reading, no overlay/VPN/device-admin enforcement, no covert capture. Honest states only; no universal-blocking claim.
- Feasibility devices: Samsung Galaxy A03 Core (SM-A032F, Android 13 / SDK 33) connected; Samsung Galaxy A06 (Android 16) not yet tested, so Android 16 behaviour remains unverified.
- Visual design (amended 2026-11-17): light-first course-app style derived from the owner's reference PNGs; see amended D-09. Past olive/sage palette is superseded.
- DI approach: manual wiring (no DI framework by habit).
- Backup/export: local export format; encrypt if it leaves app-private storage; documented key management.
- AIE score weights: configurable defaults; simulate and calibrate before adoption.
- Revision intervals: configurable 1/3/7/14/30-day defaults; evaluate from results.

## Implementation notes (2026-11-16)
- Owner approved the decisions above and the screen inventory in
  `11-ui-screen-inventory.md` on 2026-11-16; the UI approval gate is resolved.
- Focus sessions persist interruption rows with reason and duration. A pause is
  recorded when it starts and finalised on resume or early exit, so interruption
  time cannot be counted twice.
- Learning evidence (`studied`, `understood`, `mastered`) is persisted only when
  the user explicitly selects it after a session. It is never inferred from
  elapsed time.
- The running timer uses a persisted monotonic anchor
  (`segment_start_elapsed_millis`) with a wall-clock fallback after reboot. A
  foreground service shows the countdown and updates it while the app is in the
  background; the two-step emergency exit remains available.
- Completed sessions can be reopened from Progress to review interruption detail
  and record or clear learning evidence after the summary has been dismissed.
- Parent session rows are saved with Room `@Upsert`, not SQLite `REPLACE`.
  `REPLACE` deletes and reinserts the row, which would cascade-delete the
  session's interruption history.
- The timer service posts a completion notification when it observes the session
  finish, including when the in-app ticker completed the session first. The
  session screen stops the service on pause; the service stops itself on
  completion or early exit.
- Every normal weekday assignment and started session use the same
  `commitment_minutes` (120 by default). A lower topic estimate changes priority
  but does not shorten the agreed continuous session.
- Module outlines can be pasted into Module Detail. Numbered and bulleted lines
  are parsed in order, duplicates are skipped, and an `AcademicSource` plus
  `ModuleOutline` record the information as user-provided (`RECEIVED`).
- A recall check entered after a session is stored as an explicit
  `LearningEvent`. It is never inferred from elapsed time. Weak results add a
  revision priority boost to future automatic topic selection; the timer alone
  does not mark learning.
- Local backups are JSON snapshots of all user tables written to the app's
  private storage (D-12). They never leave the phone, so no encryption or key
  management is involved. Restoring is blocked while a session is active and
  preserves existing history, assessments and decision records.

## Phase 6 implementation notes (2026-11-17)
- Enforcement is limited to the owner-approved scope: foreground-app identity
  monitoring (foreground service + UsageStats) and user-granted Do Not Disturb
  via `ACCESS_NOTIFICATION_POLICY`. Notification content is never read and no
  app is forcibly blocked.
- DND uses `INTERRUPTION_FILTER_ALARMS` during a session; the previous filter is
  saved locally and restored. Alarms remain audible and emergency calls are not
  blocked by DND. A second restore is a no-op so the original setting is kept.
- Enforcement state is one of `FULL_SUPPORTED`, `LIMITED`, `MONITORING_ONLY`,
  `PERMISSION_REQUIRED` or `ENFORCEMENT_INTERRUPTED`. The UI never shows a
  "locked" state when controls are inactive, and revocation is recorded.
- The normal "End session early" flow now has two confirmations plus one short
  deterministic arithmetic puzzle. A separate "Emergency exit" flow is always
  available with the puzzle-free two-step confirmation (amended D-08).
- Only the A03 Core (Android 13) was available for device checks. A06 (Android
  16) behaviour is explicitly unverified; no cross-device enforcement claim is
  made.

### Device evidence — Galaxy A03 Core (SM-A032F), Android 13 / SDK 33
- Setup: debug APK installed via `adb install -r`; USB debugging; no factory
  reset and no device-owner provisioning.
- Permission states: with neither special access granted, Settings showed
  `PERMISSION_REQUIRED`. After granting usage access and DND access via Settings
  (`appops set ... android:get_usage_stats allow`, `cmd notification allow_dnd`),
  the panel correctly showed `LIMITED` (no session active, so nothing claimed as
  fully supported).
- Notification quieting: `settings get global zen_mode` was `0` before a session
  and `3` (Alarms only) while a session was active. It returned to `0` after the
  emergency exit. Alarms remain audible; emergency calls are not affected by DND.
- Foreground monitoring: the session screen displayed the gentle "stepped away"
  nudge from locally recorded foreground transitions. No notification content,
  typed text or screen content was read.
- Exit flows: normal "End session early" required two confirmations plus a
  deterministic arithmetic puzzle; "Emergency exit" required two confirmations
  with no puzzle, and no explanation or reason selection. Early exits remain
  recorded neutrally.
- Logcat showed no StudyLock crashes during install, launch, setup, session
  start, DND application, both exit flows and DND restoration.
- Not verified on this device: reboot/force-stop/safe-mode/OEM battery-kill
  recovery, and Android 16 (Galaxy A06). No claim is made for those cases.

## CRUD and redesign notes (2026-11-17)
- Modules, topics, exams and semester details can now be renamed/edited and
  removed from the UI. Removal never destroys history: modules are archived via
  a `status` column (migration 6→7), and topics are marked `SUPERSEDED`. Exams
  are the only hard delete because no learning history links to them.
- Editing is blocked while a session is active for the affected module/topic.
- UI redesign follows the owner-approved reference style: light lavender
  background, white rounded cards, bold headings, colourful hero cards with
  geometric shapes, pill buttons and a floating five-tab bottom bar. Layout is
  extrapolated consistently across screens not present in the reference zip.
- Dark mode uses a true-black background with near-black surfaces, soft light
  accents and muted dark versions of the light theme's coloured cards. The app
  follows the device theme automatically via `isSystemInDarkTheme()` and
  `values-night` resources.
- Launcher icons are theme-aware: `Glossy Purple Education Lock Icon.png` for
  light mode and `Glossy Dark Lock Education Icon.png` for dark mode, selected
  through a `values/values-night` drawable alias. Note that launchers often
  cache an app icon at install time, so a theme change may not re-label the
  home-screen icon until the app is reinstalled or the launcher refreshes.
- Performance: the 1-second session clock now lives in a separate StateFlow
  collected only by the countdown screens, so it no longer recomposes the whole
  app. Independent refresh reads run concurrently, exam sections load in
  parallel, and the background service gates UsageStats/permission checks to
  every 20s/60s instead of every 5s. Prefs writes happen only when a value
  changes.

## GitHub issues 1–6 agreement (2026-11-19)
Owner and agent agreed to work through the six open GitHub issues one at a time,
in the order in D-19. Scope per issue:
- **#3 (Critical):** investigate overheating/lag with evidence from the connected
  A03 Core; fix only confirmed causes; no universal-blocking or performance
  claims.
- **#6 (Medium):** Appearance setting with System/Light/Dark, persisted and
  applied to the approved light/black dark palettes.
- **#1 (Medium):** single local profile — display name, name-derived initial,
  optional photo; no accounts or sync.
- **#5 (High):** planning-mode choice (recommended schedule vs existing
  routine), switchable later; infeasible workloads produce the opt-in redesign
  in D-23 with honest shortfall reporting; engine remains the automatic topic
  chooser and replanning never rewrites history.
- **#4 (High):** weekday study time in onboarding and settings, enable/disable,
  inexact recurring reminders (D-21), with deterministic next-trigger tests.
- **#2 (High):** local document reader for PDF, images and plain text (D-20),
  including page/scroll position restoration; no cloud.

Each issue follows the `AGENTS.md` workflow: acceptance criteria, smallest
complete change, tests, narrowest relevant test run, docs update, evidence
report. No commits or PRs unless the owner explicitly asks.

## Issue #3 investigation — overheating/lag (2026-11-19)
Device: Galaxy A03 Core (SM-A032F), Android 13 / SDK 33, USB connected.

### Evidence collected
- Cold/warm launches: `ActivityTaskManager: Displayed ... MainActivity: +2s997ms`
  and `+2s296ms`; `Choreographer: Skipped 39/54/131 frames` in one launch.
- `dumpsys gfxinfo` on the first launch: 1020 frames, 50.98% janky, 90th
  percentile 200 ms, 99th 1650 ms.
- `atrace` (`dalvik view am wm`) during startup: long main-thread events
  including `traversal` 1868 ms, multiple `JIT compiling ... (kind=Baseline)`
  blocks and `setCoreSettings` 1391 ms. Startup cost is shared between ART
  baseline-profile work and app main-thread work.
- Idle in the foreground after the fixes: 0 CPU ticks and 0 rendered frames
  over 10 s — no background drain while no session is running.

### Confirmed causes and fixes applied
- `AppViewModel.refreshInternal()` ran all data coordination, enforcement
  permission checks (AppOps/NotificationManager binder calls) and schedule
  generation on the Main dispatcher. It now dispatches to `Dispatchers.IO` via
  a single wrapper, so every caller (init, user actions, resume, ticker
  completion) is covered (`AppViewModel.kt`).
- `SessionRepositoryImpl` used `Dispatchers.Default` for blocking Room work;
  changed to `Dispatchers.IO` (`SessionRepositoryImpl.kt`).
- `EnforcementController.snapshot()` and `noteForeground()` issued up to four
  separate `SharedPreferences.apply()` calls; each now uses one batched edit
  while preserving interruption/grant-history semantics
  (`EnforcementController.kt`).
- `TodayScreen` and `SessionScreen` collected the 1-second tick at the top
  level, recomposing the whole screen every second for a 120-minute session.
  Collection is now isolated to the countdown block/card only.

### Verification and limits
- `JAVA_HOME=/home/jabu/jdks/jdk-21.0.5+11 ./gradlew :app:compileDebugKotlin
  :app:testDebugUnitTest :data:local:testDebugUnitTest --offline` — passed.
- Debug APK rebuilt and installed on the A03 Core; app launches and renders.
- Post-fix launch timings were 3.0 s, 2.8 s, 4.0 s and 5.3 s with 99–178
  skipped frames. The device was under heavy unrelated load during these runs
  (load average 6.92; `com.android.sys*` ~95% CPU, Spotify ~74% CPU,
  only ~71 MB free RAM). **No clean before/after speed-up is claimed** — a
  controlled re-test on an idle device (other apps stopped) is still needed.
- Android 16 (Galaxy A06) remains untested. No universal performance or
  overheating claim is made.

## Issue #6 — Appearance section (2026-11-19)
Added an Appearance section to the existing approved Settings screen with three
radio choices: System default, Light and Dark. The choice is stored in the
existing `studylock_settings` DataStore under `theme_preference` and falls back
to SYSTEM for missing/unknown values.

Files: `settings/ThemePreference.kt` (pure resolution + stored-value parsing),
`settings/SettingsRepository.kt`, `AppViewModel.kt` / `AppUiState`,
`ui/screens/settings/SettingsScreen.kt`, `MainActivity.kt`.

Important implementation detail: the approved palettes are Android colour
resources with `values-night` variants, and `colorResource()` reads the resource
configuration. Overriding only the Compose colour scheme left the app dark when
Light was chosen. `MainActivity` now provides a `LocalContext` whose `uiMode` is
overridden to match the resolved theme, as well as selecting the matching
Compose colour scheme and system-bar style.

Verification on the A03 Core:
- Live switching Light → Dark changed the whole UI immediately.
- Light survived `am force-stop` + relaunch (DataStore persistence).
- With System default selected, `cmd uimode night no` switched the app to light
  and `cmd uimode night auto` restored it; device setting restored to auto.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --offline` passed,
  including new `ThemePreferenceTest` cases.

Limitations: launcher icons are chosen by the launcher at install time from the
`values/values-night` alias, so a manual in-app theme change may not relabel the
home-screen icon until the launcher refreshes. Android 16 remains unverified.

## Issue #1 — User profile (2026-11-19)
Implemented a single local profile (D-22): editable display name, initial derived
from that name, optional profile picture, no account and no sync.

Files: `profile/ProfileIdentity.kt`, `profile/UserProfile.kt`,
`profile/ProfilePhotoStore.kt`, `settings/SettingsRepository.kt`,
`AppContainer.kt`, `AppViewModelFactory.kt`, `AppViewModel.kt` / `AppUiState`,
`ui/screens/settings/SettingsScreen.kt`, `ui/screens/today/TodayScreen.kt`,
`ui/StudyLockApp.kt`, plus `ProfileIdentityTest.kt`.

- The photo is copied from the system picker into `filesDir/profile/avatar.jpg`,
  downsampled to at most 512 px; no source URI is retained and nothing is
  uploaded. Only a file name is stored in DataStore.
- The Today greeting avatar now shows the real initial (or the stored photo)
  instead of the hard-coded "SL".
- Missing/blank names fall back to a neutral "?" initial and "Student" label;
  no identity is invented.
- Verification: `:app:compileDebugKotlin` and `:app:testDebugUnitTest` passed,
  including unicode and blank-name cases in `ProfileIdentityTest`. Debug APK
  built and installed successfully.
- Device UI walkthrough was not completed: during this session the phone
  repeatedly locked/fell asleep while in active use, so the profile editor
  could not be exercised reliably. It remains on the verification list.

## Issue #5 — planning mode and infeasible-plan redesign (2026-11-19)
Implemented the owner's D-23 requirement: when the workload does not fit the
remaining weekdays, the app proposes a redesign instead of silently claiming
the goal is met.

Engine (pure, testable, no Android):
- `SchedulerConfig` gained `routineMode`, `compressedLearningMinutesPerSession`,
  `useCompressedPlan`, `extraWeekdaySession` and `weekendStudy`.
- `CapacityCalculator.studySlots()` models a second weekday slot and weekend
  slots only when explicitly enabled; default behaviour is unchanged.
- `RedesignPlanner.propose()` checks, in order: current plan → compression →
  extra weekday block → weekend study. It reports the first option that closes
  the gap, or an honest infeasibility result with the remaining shortfall.
  No topics or dates are fabricated, and no success is claimed when short.
- `routineMode` forces the ROLLING phase: still one automatic session a day,
  without exam-driven phasing.
- Today selects the first assignment not already completed that day, so an
  accepted extra block shows as “session 2 of 2” rather than being lost.

App:
- Settings → Study plan offers “Follow the recommended plan” / “Stick to my
  routine”, switchable at any time, persisted in DataStore.
- When the recommended plan is infeasible, a Settings card shows the shortfall
  and the recommended redesign; it is applied only when the user taps it.
  Returning to the standard plan is always available.
- Today shows a gentle, non-punitive nudge linking to those options.
- Attempts that are still infeasible state plainly that the goal cannot be
  claimed as met.

Verification: `:core:academic-engine:test` (including new `RedesignPlannerTest`
and scheduler slot/routine tests) and the full `./gradlew test --offline` passed.
Device UI verification was not completed for the same reason as #1 (phone in
active use / repeatedly sleeping); it remains on the verification list.

## Issue #4 — Study reminders (2026-11-19)
- `reminders/ReminderTime.kt` computes the next weekday trigger from an
  injected `ZonedDateTime`; Friday evening rolls to Monday and weekends are
  skipped. `ReminderTimeTest` covers those cases deterministically.
- `ReminderScheduler` uses `AlarmManager.setWindow` (inexact, ~10-minute
  window, D-21) for the next weekday; `ReminderReceiver` posts a gentle
  notification and schedules the following weekday. `BootReceiver` restores
  reminders after reboot, time set, time-zone change and package replacement.
- Preferred time is asked in the final onboarding step and is editable in
  Settings → Reminders, with enable/disable and a notification-permission
  button for Android 13+.
- Manifest adds only `RECEIVE_BOOT_COMPLETED`; no exact-alarm permission.
- Verification: `:app:compileDebugKotlin` and `:app:testDebugUnitTest` passed,
  including `ReminderTimeTest`. Device alarm delivery was not exercised
  because the phone was in active use; it remains on the verification list.

## Issue #2 — local document reader (2026-11-19)
Added PDF, image and plain-text reading inside the app (D-20), reached from a
Settings → Study materials entry. No new bottom tab was added.

- `documents/DocumentStore.kt` copies an imported file into
  `filesDir/documents/`, keeps a small local index and stores only the last
  reading position. Documents are not uploaded and are not part of backups.
  Unsupported types and files over 25 MB are rejected.
- `documents/DocumentMeta.kt` holds the pure type detection and position
  clamping rules; `DocumentTypesTest` covers them.
- `ui/screens/documents/DocumentsScreen.kt` lists/imports/removes documents;
  `DocumentReaderScreen.kt` renders PDF pages with the platform `PdfRenderer`,
  shows images, and displays text with a remembered scroll position. Page and
  scroll positions are restored when a document is reopened.
- No third-party dependency was added.
- This extends the approved screen inventory with an in-app reader; it was
  explicitly agreed in D-20 rather than inferred from the original inventory.
- Verification: full `./gradlew test --offline` passed (including the new
  document tests); `:app:assembleDebug` built and the APK installed on the
  A03 Core. Interactive import/read verification remains on the list because
  the phone was in active use during this session.

## Crash fix — Settings tab (2026-11-19)
Reproduced on the A03 Core: tapping Settings crashed with
`IllegalStateException: No ActivityResultRegistryOwner was provided via
LocalActivityResultRegistryOwner` from `SettingsScreen`'s photo/permission
launchers. The `LocalContext` override introduced for manual themes replaced
the context used to locate the activity's result-registry owner.
Fix: `MainActivity` now provides `LocalActivityResultRegistryOwner provides this`
alongside the themed `LocalContext`. Verified on device: Settings opens and the
profile/appearance/reminders sections render without a crash; logcat shows no
fatal exception and the process stays alive. Full `./gradlew test --offline`
passed.

## Owner-requested profile UI refinement (2026-11-19)
Owner rejected the three-button profile editor. Replaced it with:
- avatar with a small camera badge at the bottom for choosing a photo;
- a pen icon on the right in normal view;
- edit mode with the name field and a single **Save profile** action that
  commits both the name and any pending photo, plus Cancel/Remove photo;
- after saving, controls collapse back to the avatar + pen view.
Verified on the A03 Core: view mode shows the photo badge and pen only; tapping
the pen shows the editor; typing a name and saving updates the displayed name
and returns to view mode; the camera badge opens the system picker without a
crash. The test name used during verification was restored to the owner's name.

## On-device acceptance results (2026-11-19, phone connected)
- **#1 profile:** name save, photo display, camera picker and pen/edit flow all
  verified on device. Resolved.
- **#2 documents:** imported `study-notes.txt` and `KA2002.pdf` through the
  system picker, read both in-app (text content and rendered PDF page shown),
  then removed both test documents and the pushed temp file. Resolved.
- **#4 reminders:** toggling on scheduled an inexact `RTC_WAKEUP` alarm for
  18:00 with a 10-minute window (`dumpsys alarm`); toggling off cancelled it.
  Setting was restored to off. Resolved.
- **#5 planning:** study-plan radios rendered; switching to "Stick to my
  routine" and back to "Follow the recommended plan" worked. Resolved for the
  mode-toggle behaviour; infeasibility cards still need data-driven exercise.
- **#3 re-measure:** three warm launches took 3.41 s, 2.61 s and 3.44 s with
  roughly 230–280 skipped frames, against pre-fix 2.30–3.00 s with 208–224
  skipped frames. Device load remained high (load average 7.12, Spotify and
  Bluetooth active). **No measured improvement can be claimed**; the structural
  main-thread fixes remain, but a quiet-device A/B test is still required.


## Owner-requested icon and document follow-ups (2026-11-20)
- **Icons:** the camera badge and the minus action now use local vector
  drawables (`res/drawable/ic_camera.xml`, `res/drawable/ic_remove.xml`)
  instead of an emoji or a newly downloaded icon pack. `material-icons-extended`
  could not be fetched from Google Maven in this environment, so the app keeps
  only `material-icons-core` (plus, pencil, navigation) and ships the two
  missing glyphs as tiny vectors. No new dependency was added.
- **Documents list:** the Read buttons are now compact pill buttons (40 dp)
  and the word "Remove" is replaced by a red minus icon on the right.
- **Settings access:** the Study materials section in Settings opens the
  documents screen directly; verified on the A03 Core.
- **PDF text:** `PdfRenderer` leaves unpainted pixels transparent. Against the
  dark theme those pixels stayed dark and black document text disappeared, so
  only image-like areas were visible. The renderer now erases the page bitmap
  to opaque white and the Compose container is white. Verified with
  `KA2002.pdf`: the full result letter (header, table, notes, signature) is
  now readable.
- **Topic documents:** each topic row has a "+" action that imports a file
  through the system picker and stores the topic's UUID in
  `DocumentMeta.topicId`. Linked documents are listed under the topic and
  open in the reader when tapped. Verified with a temporary module and topic
  ("Test"/"TopicOne") and a text file: the file appeared under the topic and
  opened correctly. The module, topic, document and pushed temp file were
  then deleted.
- **Removal wording:** visible "Remove" buttons in Academics, Exams,
  Documents and Settings are now minus icons; confirmation buttons say
  "Confirm" and dialog titles use "Delete ...?". Accessibility labels retain
  "Remove ..." so screen readers still describe the action.
- **Verification:** `:app:test --offline` passed, `:app:assembleDebug --offline`
  passed, and `git diff --check` is clean. The temporary semester, module,
  topic and documents created for the on-device checks were removed; app data
  was cleared afterwards so the device is back at the first-run screen.

## Reader navigation and first-run permissions (2026-11-20)
- **Reader navigation:** PDF pages now use two circular chevron icon buttons
  (`<` / `>`) pinned to the bottom-right corner instead of full-width "Previous
  page"/"Next page" buttons. They carry accessible labels and disable at the
  first/last page; for a one-page document they are hidden.
- **First-run permissions:** a new `FirstRunPermissionsScreen` is shown on the
  very first launch, before academic setup. It explains and offers grants for
  notifications (runtime dialog), usage access and Do Not Disturb access (the
  two Android special accesses must be granted through their system settings
  screens). Every permission is optional and statuses refresh when returning
  from system settings. The flag is stored in DataStore
  (`permissions_prompted`) so this happens once. Settings keeps entries for
  re-granting, but is no longer the only place to find them. The session
  screen's duplicate notification prompt was removed.
- **Simplified reminder UI:** both onboarding and Settings now use one compact
  `ReminderTimeSelector` row that opens the platform time picker. The two
  text fields, Save button and large "Allow notifications" button were removed
  from Settings; a small recovery link remains only if permission is still
  missing. The pinned Compose Material 3 version has no `TimePickerDialog`, so
  the platform dialog is used.
- **Verification before the phone became unavailable:** on a freshly cleared
  app the first-run permission screen appeared, the notification dialog was
  shown and granted, the status changed to "Allowed", the usage-access button
  opened the system settings screen and the status refreshed after returning,
  Continue advanced to academic onboarding, and the simplified Settings
  reminder row rendered correctly. The new time picker and the bottom-right
  PDF arrows were built successfully but were not exercised on device: the
  phone repeatedly dropped its USB connection and another app was brought to
  the foreground during those taps. They still need an on-device check.
- `./gradlew test --offline` passed and `git diff --check` is clean.
