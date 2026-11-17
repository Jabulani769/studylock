# 05 — Focus Session Specification

## Commitment
One continuous 120-minute session on each eligible weekday. It is not two or more separate sessions accumulated across the day. Weekends are full rest days.

## Session states
Suggested state model:
`PLANNED → READY → ACTIVE → COMPLETED`
Alternative terminal/intermediate states: `MISSED`, `INTERRUPTED`, `ABANDONED`, `NEEDS_RECOVERY`.
Define exact transition rules before implementation. Never silently convert a missed or interrupted session into a completed one.

## Session content
- One primary module.
- One primary topic or continuation objective.
- Revision and assessment activities as scheduled.
- Structured breaks and transitions included in the 120 minutes.
- A visible progress indicator and clear next action.
- Minimal, non-distracting visual and audio feedback.

## Time and evidence
- Persist start time, end time, elapsed active time, break intervals and interruptions.
- Use a monotonic elapsed-time source while a session is running; handle process death and reboot.
- Wall-clock changes must not create extra time or erase history.
- Time elapsed is not proof of reading, understanding or mastery.
- A phone untouched while the student reads a physical textbook is not automatically distraction.
- Topic completion requires explicit learning evidence or assessment rules, not timer expiry.

## Interruption and recovery
- Restore the correct task after app/process interruption.
- Offer an attention reset without shame or score penalty.
- Explain what happened and the next action.
- Do not automatically add missed minutes to the next day.
- Resume the session where the student stopped; each interruption is recorded separately so interruption time cannot be counted twice later.
- Emergency exit must remain possible.
- If lockdown permission or enforcement becomes unavailable, clearly show degraded mode and record the event; do not pretend the device remained locked.

## Session completion and summary
A completed session ends automatically when the elapsed 120-minute commitment timer reaches zero, whether or not the student was inside the app at that moment. The summary must contain:
- assigned topic
- completed progress
- elapsed time
- number of interruptions and total interruption time
- learning status, as `studied`, `understood`, or `mastered`
- next scheduled topic

Never infer mastery from time spent alone.

## ADHD-friendly session behavior
- Display the current task, not a dense semester timetable.
- Use short instructions and one action at a time.
- Use calm transitions and predictable break prompts.
- Avoid streak loss, punitive language, flashing warnings and excessive notifications.
- Allow the user to mark “I lost focus” and resume without restarting the session.
- Do not use inactivity as a proxy for lack of effort.
