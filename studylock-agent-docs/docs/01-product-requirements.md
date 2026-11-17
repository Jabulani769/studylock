# 01 — Product Requirements

## Functional requirements

### Academic setup
- Create an academic year and semester.
- Add registered modules using name and/or module code.
- Record semester start as confirmed, estimated, or unknown.
- Record normal study availability and one 120-minute weekday commitment.
- Allow module outlines and topics to be added later.
- Import must preserve lecturer topic order and source attribution.
- Support manual correction and duplicate detection for imported topics.

### Planning
- Generate a rolling plan from currently known information.
- Automatically select the next module, topic and activity.
- Balance module workload; do not rotate mechanically if workload differs.
- Reserve time for revision, practice and assessment.
- Keep weekends free of mandatory study.
- Recalculate future assignments when relevant academic data changes.
- Explain each assignment in plain language.

### Learning records
- Record topic exposure, time spent, activities completed and assessment outcomes separately.
- Distinguish `not_started`, `in_progress`, `studied`, `understood` and `mastered`.
- Allow partial completion and continuation.
- Record confidence and evidence, but do not treat self-reported confidence as mastery.
- Preserve prior learning and allow it to be matched to a newly imported syllabus topic.

### Exams
- Store exam structures independently from exact exam dates.
- Support compulsory and optional question groups.
- Use known structures for generic preparation before timetable release.
- Activate date-specific prioritization only when reliable dates are supplied.
- Preserve completed paper history.

### Focus session
- Start one continuous 120-minute session on an eligible weekday.
- Present one primary objective and one immediate next action.
- Schedule breaks and activities inside the 120 minutes.
- The commitment timer tracks elapsed session time only; it does not imply the student studied actively for the full window. If the student leaves the app, the timer continues counting and the session resumes where it stopped, preserving the current topic and completed progress without restarting or discarding work. Each interruption is recorded separately so interruption time cannot double-count later learning.
- Session completion does not automatically complete the topic.
- When the elapsed timer expires, finalize the session, save progress, finalize interruption records, display a concise summary (assigned topic, completed progress, elapsed time, number of interruptions and total interruption time, learning status as `studied`, `understood`, or `mastered`, next scheduled topic), and restore normal phone use without requiring manual completion.
- Never infer mastery from time spent alone.

### Data and operation
- Work offline for core planning, session execution and recordkeeping.
- Persist all important state across process death and device restart.
- Handle time-zone and date changes without corrupting history.
- Provide data export/backup before any feature that could risk loss.

## Non-functional requirements
- Reliability: no duplicate daily sessions from retries or recalculation.
- Explainability: each assignment has one or more reason codes and a human-readable explanation.
- Accessibility: screen-reader labels, scalable text, adequate contrast, large touch targets.
- Performance: planning should complete quickly for a normal semester-sized dataset.
- Battery: no unnecessary continuous background polling.
- Privacy: collect the minimum signals needed for explicitly described features.
- Maintainability: pure domain logic, clear module boundaries, automated tests.
- Resilience: app restart, offline operation and delayed permissions must have defined behavior.

## Requirement priorities
- P0: academic data integrity, daily plan, 120-minute session, rest days, progressive outlines, learning history, deterministic scheduler, permission transparency.
- P1: exam optimization, adaptive spaced revision, focus enforcement feasibility, reports and recovery flows.
- P2: optional imports, advanced analytics, customization and cloud features (only if approved).

## Explicit product truth
StudyLock can measure app-level events and user-entered learning evidence. It cannot directly know whether a person is mentally attentive or reading a physical textbook. Any attention or study-quality estimate must be labeled as an estimate and must not punish the user for inactivity.
