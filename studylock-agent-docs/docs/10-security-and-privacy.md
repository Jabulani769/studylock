# 10 — Security and Privacy Requirements

## Principles
- Local-first; no remote transmission by default.
- Data minimization and purpose limitation.
- Explicit, contextual permission requests.
- Clear explanation of monitoring and enforcement.
- No covert collection.
- User retains access to academic records and backup/export.
- Secure handling of any sensitive local records.

## Data classification
### Academic personal data
Modules, syllabus topics, exam dates, assessments, learning history and notes. Treat as private user data.

### Device activity signals
Foreground application identity and time window, attempt to leave an active session, and distraction events with durations, recorded locally to support focus enforcement. Keep only what is required to provide the focus feature on an approved, limited monitoring scope; do not collect messages, passwords, typed text, private screen content, photos, microphone audio, contacts or call contents.

### Prohibited collection by default
Passwords, keystrokes, message bodies, typed text, private screen content, microphone audio, camera images, continuous screenshots, unrelated browsing contents, contact lists and call contents.

## Storage
- Use app-private storage.
- Do not log academic notes, exam records or sensitive activity details.
- Avoid storing raw event streams when aggregate/session-level facts are sufficient.
- Encrypt backups if they leave app-private storage; document key management.
- Never hardcode secrets or private signing material in source control.

## Permissions
For each permission/special access:
- name the Android capability;
- explain the exact feature requiring it;
- explain what data is observed;
- explain what is not collected;
- provide a setup guide;
- define app behavior if denied or revoked;
- allow the user to review current state.
Do not request all permissions at first launch. Request only when the relevant feature is introduced.

## Data lifecycle
Define retention and deletion behavior for academic data, device signals, backups and logs. Deleting a module should not silently erase linked learning history; use archive/supersede semantics where needed.

## Threat and abuse review
Consider:
- unauthorized access to device-local records;
- accidental data loss;
- malicious or compromised dependencies;
- permission misuse;
- false enforcement status;
- lockout preventing emergency access;
- backups exposed outside app-private storage.
