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

## Confirmed decisions log
- Midsem coverage = half of every module's topics, rounded up.
- Daily commitment = one continuous 120-minute session.
- Saturday and Sunday = full rest days.
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

