# 12 — Testing Strategy and Acceptance Criteria

## Test layers
1. Pure Kotlin unit tests for academic policies, AIE, PADM and focus state machine.
2. Property-based or parameterized tests for date/topic/capacity edge cases.
3. Room DAO and migration tests.
4. Repository integration tests.
5. Compose UI tests for core flows and accessibility semantics.
6. Android instrumentation tests for permissions and platform adapters.
7. Physical-device tests for lockdown, process death, reboot, battery and OEM behavior.
8. Manual usability testing of ADHD-friendly flows.

## Academic engine acceptance
- AIE-01: multiple modules remain independent.
- AIE-02: lecturer syllabus ordering is preserved.
- AIE-03: half-target rounds up correctly.
- AIE-04: no normal mandatory weekend sessions.
- AIE-05: no module is systematically neglected.
- AIE-06: module/topic selection is automatic.
- AIE-07: first-half target is prioritized before midsem.
- AIE-08: remaining/second-half topics are prioritized after midsem.
- AIE-09: completed topics can return for spaced revision.
- AIE-10: weak assessment evidence schedules earlier review.
- AIE-11: missed sessions trigger sensible future rebalancing.
- AIE-12: confirmed exam dates influence priority.
- AIE-13: date changes regenerate future plan only.
- AIE-14: normal session totals exactly 120 minutes.
- AIE-15: elapsed time alone cannot confer mastery.
- AIE-16: core planning works offline.
- AIE-17: history survives process/device restart.
- AIE-18: every assignment has an explanation/reason code.
- AIE-19: when the commitment timer expires while the student is outside the app, the session records the exact expiry timestamp, interruption duration and available context, preserves the unfinished topic and completed progress, and makes interruption information available to the AIE for adaptive assessment.
- AIE-20: after priority syllabus coverage is completed, unfinished or weakly learned material is scheduled into a dedicated repeat/revision phase without displacing current priority syllabus assignments.

## PADM acceptance
- Start planning without an official calendar.
- Start with incomplete module outlines.
- Add an outline in Week 2 or Week 4 without losing prior records.
- Match earlier learning to a new outline with confirmation for ambiguous matches.
- Receive a late outline and calculate whether remaining capacity is sufficient.
- Replace estimated dates with official dates and preserve history.
- Detect conflicting dates and request resolution.
- Continue generic preparation when exam structure is known but timetable is missing.
- Use exact exam priority when timetable arrives.
- Report when date is known but syllabus mapping is missing.

## Focus acceptance
- Session is one continuous 120-minute commitment.
- Breaks are included in the 120 minutes.
- Restart/reboot does not falsely complete or reset a session.
- Attention reset resumes correctly.
- Inactivity does not automatically count as distraction.
- Permission loss is visible and produces a truthful degraded state.
- Emergency exit remains possible.
- Interruptions are recorded separately per occurrence; returning the app resumes where the student stopped and interruption time is not counted twice.
- Ending a session early uses a simple two-step confirmation; the exit is recorded neutrally, emergency functions remain accessible, and mastery is never inferred from an early exit or from the elapsed timer reaching zero.
- The session summary produced on natural completion contains: assigned topic, completed progress, elapsed time, number of interruptions and total interruption time, learning status (studied, understood, or mastered), and next scheduled topic.

## Data integrity acceptance
- Re-importing the same outline creates no duplicate topics.
- Recalculation creates no duplicate daily assignments.
- Completed learning events remain unchanged.
- Topic rename/removal does not erase learning history.
- Schedule versions retain their input-data version and rationale.
- Database migrations preserve records.

## Edge-case matrix
- One topic, zero topics, odd/even topic counts.
- Missing or invalid semester dates.
- Midsem before semester start or endsem before midsem.
- Multiple exams on one day and overlapping times.
- Topic longer than expected.
- Completed topic with weak recall.
- Repeated failed assessments.
- One or many missed sessions.
- Exam date moved earlier/later.
- Outline added, reordered, merged or removed.
- All first-half topics completed early.
- Insufficient remaining weekday capacity.
- Offline restart, process death, reboot, timezone/date change.
- Permissions revoked mid-session.
- OEM kills background process.
- Commitment timer expires while the student is outside the app.

## Definition of release candidate
All P0 acceptance tests pass; no unresolved data-loss or emergency-access issue; enforcement claims match device test evidence; UI owner approval is recorded; known limitations are documented.
