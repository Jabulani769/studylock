# Agent Instructions

## 1. Before doing work
- Read `README.md`, this file, and the documents relevant to the requested task.
- Treat the owner-approved requirements as binding.
- Check `docs/14-open-decisions.md` before implementing anything that depends on an unresolved decision.
- Do not assume a feature is feasible merely because an Android API exists.
- For Android platform behavior, verify against current official Android documentation and test on a physical device.

## 2. Product invariants
- The normal commitment is one continuous 120-minute session per eligible weekday.
- Saturday and Sunday are complete rest days: no mandatory study session.
- The academic engine chooses the module and topic automatically. Do not make manual topic selection the normal workflow.
- Study plans begin with partial academic information and improve as information arrives.
- Never fabricate topics, official dates, timetable details, or exam requirements.
- A time counter reaching zero does not prove that learning occurred.
- Preserve session history, assessment results, topic progress, and decision history when replanning.
- The product must be ADHD-friendly by design: low cognitive load, clear next action, gentle recovery, and no punitive overload.
- StudyLock must be transparent about what it monitors and what Android can actually enforce.

## 3. Safety, privacy and platform constraints
- Do not implement keylogging, covert recording, hidden screen capture, credential capture, or collection of unrelated personal content.
- Do not disguise permission requests or misrepresent why a permission is needed.
- Do not bypass Android security controls, OEM protections, or user consent.
- Do not claim that AccessibilityService, UsageStats, overlays, VPN, or device-admin APIs guarantee universal app blocking.
- Keep monitoring data local unless a separately approved, explicit sync feature is designed.
- Never make the app impossible to exit in an emergency. Provide a documented emergency/safety path, even if normal exits are deliberately difficult.
- Any enforcement method that could cause data loss, prevent emergency access, or violate Play policy requires explicit owner approval and a documented test.

## 4. Engineering rules
- Prefer small, reviewable changes with clear acceptance criteria.
- Keep the academic engine independent of Android UI and Android framework classes.
- Use deterministic, testable scheduling logic. Inject clock/time-zone dependencies; do not scatter direct system-clock calls through domain logic.
- Use stable IDs, database transactions, schema migrations, and idempotent operations.
- Recalculation must not duplicate sessions or rewrite completed history.
- Store scheduling rationale so every generated assignment can be explained.
- Avoid speculative abstractions and unnecessary dependencies.
- Do not add analytics, crash reporting, remote services, accounts, or cloud sync without approval.
- Pin compatible stable dependency versions; document why each dependency exists.
- Never commit secrets, signing keys, tokens, personal academic records, or local machine configuration.

## 5. Required workflow for each task
1. State the task and acceptance criteria.
2. Inspect only relevant files; preserve unrelated and uncommitted work.
3. Implement the smallest complete change.
4. Add or update tests.
5. Run the narrowest relevant tests, then required project checks.
6. Report changed paths, tests and results, remaining blockers, and evidence.
7. Do not claim tests passed if they were not run.

## 6. Git safety
Never run destructive or history-rewriting commands (`reset --hard`, `clean -fd`, force push, checkout-overwrite, stash/pop that risks conflicts, or deletion of untracked files) unless the owner explicitly authorizes the exact operation. Do not stage or commit unless explicitly requested. Preserve existing user changes.

## 7. UI gate
No frontend implementation until the owner has reviewed and approved:
- mobile-first screen inventory and navigation;
- design direction, typography, color, component style and interaction patterns;
- permission/onboarding explanations;
- lockdown states and emergency recovery behavior.

A draft inventory in `docs/11-ui-screen-inventory.md` is not approval.

## 8. Definition of done
A task is done only when its acceptance criteria are met, tests are run, relevant docs are updated, and known limitations are reported. A visually convincing screen is not evidence that enforcement or academic scheduling works.
