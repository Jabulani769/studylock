# 06 — Lockdown and Monitoring Feasibility

## Goal
The owner wants the behavioural contract defined for a normal personal Android phone: reduced distraction exposure during a study session, transparent monitoring, and honest limits on what Android can enforce. Managed-device enrollment and device-owner provisioning are not prerequisites, and factory reset is not acceptable. The explicit Accessibility Service permission may be requested with a clear purpose and user consent, and enforcement claims must never imply universal blocking.

## Mandatory feasibility gate
Before implementing or promising enforcement, produce a device/API feasibility report covering:
- Android OS versions and OEMs targeted;
- app distribution method (personal sideload, managed deployment, Play Store);
- whether device-owner provisioning is acceptable;
- permissions and user setup steps;
- app blocking reliability across reboot, force-stop, safe mode and OEM battery management;
- emergency calling, accessibility, system settings and essential apps;
- policy compliance and user-consent requirements.

Test on the owner's actual phone and at least one additional representative device if broad compatibility is intended.

## Android capability distinctions
- **Usage access / UsageStatsManager:** can provide usage information with user-granted special access; it is not a universal app-blocking API.
- **AccessibilityService:** can observe/interact with accessibility events when enabled by the user. It is sensitive, policy-constrained and not a guaranteed or appropriate universal blocking mechanism. Do not use it for covert surveillance or unrelated content capture.
- **Device administration / DevicePolicyManager:** capabilities vary by policy and provisioning state. Strong Lock Task / kiosk controls generally require managed-device or device-owner/profile-owner setup; ordinary consumer app installation does not automatically grant these powers.
- **Overlay windows:** can be denied, bypassed or behave differently across OS/OEM versions. Never treat overlays as a secure enforcement boundary.
- **Notifications / Do Not Disturb:** require appropriate user-granted access and do not prevent every interruption.
- **VPN-based blocking:** has user-visible system implications, battery/network impact and privacy risks. Not a default choice.
- **App pinning / Lock Task:** test exact behavior and provisioning requirements; do not claim a normal app can silently lock arbitrary third-party apps.

## Monitoring scope
“Monitor everything” must be translated into a transparent, minimum-necessary signal list before implementation. Candidate signals for owner review:
- foreground package/app identity and time window;
- StudyLock session state;
- whether required permissions are enabled;
- screen on/off and device restart events;
- enforcement interruption or permission revocation;
- user-reported focus resets and learning outcomes.
Approved monitoring scope (local): detect foreground application changes; detect attempts to leave an active study session; record distraction events and their durations locally.
Do not collect or read: messages, passwords, typed text, private screen content, photos, microphone audio, contact lists or call contents.
Any proposed expansion requires a separate privacy and feasibility decision.

## Enforcement modes
Design explicit states, for example:
- `FULL_SUPPORTED`: all approved controls verified on this device;
- `LIMITED`: some controls unavailable;
- `MONITORING_ONLY`: signals available but blocking not reliable;
- `PERMISSION_REQUIRED`: setup incomplete;
- `ENFORCEMENT_INTERRUPTED`: user/system/OEM interrupted control.
The app must not display a “locked” success state when controls are not active.

Honesty rule: the application must never claim that Foreground App access, usage statistics, overlays, VPN, device-admin APIs or Lock Task Guarantee universal blocking. Actual enforcement depends on Android version, OEM battery management and user consent, and must be documented with device-specific evidence. When restrictions prevent a claimed control, the app must report a genuine degraded state and record the event.

## Escape and emergency handling
Emergency exit behaviour: a simple two-step confirmation before ending an active session early; no password, explanation, reason selection, puzzle or hold gesture. Emergency calls and essential system functions remain accessible throughout. Early exits are recorded neutrally and never punished or shamed.

## Acceptance evidence
For every claimed restriction, record:
- Android version and device;
- exact setup/permission state;
- test steps;
- expected and actual result;
- bypasses/failure cases;
- battery impact;
- limitations.
No “unbreakable” or “100% distraction-proof” claims.
