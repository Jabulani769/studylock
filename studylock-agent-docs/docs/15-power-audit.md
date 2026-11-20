# Power / overheating audit — 2026-11-20

## Task and standard of evidence
Investigate whether StudyLock causes the reported overheating and battery drain.
Only code paths and measured device output are reported below. Where something
could not be measured, it is marked **not established**, not inferred.

Device: SM-A032F (Galaxy A03 Core), Android 13 / SDK 33. Build: current debug
APK from this working tree. The phone was in active use by the owner during
part of the audit, so a clean uninstall-vs-install thermal A/B test was not
possible.

## Findings by suspected cause

| Suspected cause | Evidence | Verdict |
|---|---|---|
| Accessibility Service | `grep -rn AccessibilityService app/src data core` returned no results. `AndroidManifest.xml` declares one Activity, two receivers and one non-accessibility Service (`SessionTimerService`). | Not implemented. |
| Foreground service consuming CPU while idle | `dumpsys activity services org.studylock.app` with no active session: `(nothing)`. After starting a session: exactly one `ServiceRecord` for `SessionTimerService`. The loop calls `stopSelf()` when no session exists (`SessionTimerService.kt:68-87`). | Runs only during an active session. |
| Millisecond timer loop | `SessionTimerService.kt:118` uses `delay(5_000L)`. `AppViewModel.kt:1239` uses `delay(1_000)` and is only started for an ACTIVE session. Time is derived from a persisted monotonic anchor (`SessionTimerService.kt:26-29`). | No millisecond loop. |
| Constant foreground-app polling / accessibility events | `SessionTimerService.kt:105-108` calls `foregroundPackage()` every 4th 5-second poll = **20 s**, and only while a session is active. It is absent when no session exists. | Bounded 20 s polling during sessions; not constant. |
| Database continuously written | `SessionTimerService.kt:67` reads `getCurrent()` every 5 s during a session; it is a read. `noteForeground()` writes SharedPreferences only when the package changes (`EnforcementController.kt:128-138`). `snapshot()` writes only on grant/revocation change (`EnforcementController.kt:53-75`). File timestamps below confirm no writes over 30 s. | No write storm observed. |
| Wake locks | No `WakeLock` reference anywhere in app code. `dumpsys power` showed no StudyLock wake lock; the only app-UID entry was `*dexopt*: 28ms partial (1 times)`, from installation/dexopt, and `UID u0a179: ACTIVE count=0`. | No app wake lock. |
| Infinite loops | Only `while(isActive)` with `delay`, a bounded `while(candidate...)` in `ReminderTime.nextWeekdayTrigger`, and the file-copy `while(true)` in `DocumentStore.import`. | No unbounded busy loop. |
| Excessive UI recomposition | 20 s of logcat during a steady active session produced **0** `Choreographer: Skipped` messages. CPU time over that 20 s rose 0.21 s. The 1 s tick is isolated to countdown composables (`AppViewModel.kt:1262-1264`). | No excessive recomposition observed. |
| Memory leak | `dumpsys meminfo` during a session: TOTAL PSS 42.6 MB → 53.6 MB → 51.1 MB over 20 s; Views 11, Activities 1, WebViews 0. | No demonstrated unbounded growth. |

## Measured device output

Idle app, foreground, 20 seconds:
- `top` before: `15707 ... S 0.0 ... 0:12.62 org.studylock.a+`
- `top` after: `15707 ... S 0.0 ... 0:12.62 org.studylock.a+`
- No CPU time accumulated; no services; no wake lock.

Active session, foreground, 20 seconds after startup settled:
- CPU time `1:13.37` → `1:13.58` = 0.21 s CPU in 20 s (about 1% of one core).
- Transient startup samples reached 13–16% for a few seconds.
- Background (session active, app not foreground) samples were mostly 0%, with
  a few 4–7% spikes.
- `Choreographer: Skipped` count over 20 s: 0.

Database / preferences during 30 s of active session:
- `studylock_database` mtime `06:07` unchanged; journal 0 bytes.
- `studylock_enforcement.xml` mtime `06:07` unchanged.

Historical battery stats (period start 2026-11-18; total run 1 d 14 h 30 m):
- `Proc org.studylock.app: CPU: 12s 990ms usr + 820ms krn; 21s 660ms fg`
- For comparison in the same report: WhatsApp `11636s usr + 3461s krn`,
  Spotify `7185s usr + 1097s krn`. StudyLock is not a top consumer in the
  recorded history.

Alarms: only one inexact `RTC_WAKEUP` for the weekday reminder when enabled;
no periodic app alarms. After audit cleanup, no app process, no services, no
wake locks and no app alarms remained.

## Conclusion
No pathological power behaviour was found: no accessibility service, no wake
locks, no millisecond loop, no continuous database writes, no frame-skip storm
and no demonstrated memory growth. Continuous work exists only during an
active study session, and steady-state CPU was about 1% of one core.

**Not established:** whether the phone heats specifically because of StudyLock
during long sessions. That requires a controlled A/B measurement with the
phone otherwise idle and screen-off temperature/battery sampling. The owner
was using the phone during this audit, so that comparison was not made. Any
claim that the app is or is not the sole cause of the heat would be a guess.

## Evidence-based follow-ups (not claims of a bug)
1. `SessionTimerService` polls the database every 5 s; replacing that with an
   event/flow-driven state change would remove even that small read.
2. The 20 s `queryUsageStats` call could be replaced with event-driven
   foreground detection if approved; adding an AccessibilityService is not
   recommended because it increases privacy surface and is not currently
   declared.
3. Gate the 1 s ticker on the session screen being visible; it already avoids
   whole-app state updates, but it can be stopped entirely while backgrounded.
4. Repeat the thermal A/B test with the phone idle: install → 30 min screen-off
   battery/temperature sample, uninstall → same sample, compare.

## Cleanup
Temporary semester, module and topic created to start a test session were
removed with `pm clear org.studylock.app`. The device is back at first run.
