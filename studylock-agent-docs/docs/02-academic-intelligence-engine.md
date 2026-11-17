# 02 — Academic Intelligence Engine (AIE)

## Responsibility
AIE is a pure domain subsystem. It decides what to study, when, for how long, when to revisit it, how to balance modules and how to recover when plans change. It must not depend on Android UI, Activity, Service, Room entities or permission APIs.

## Inputs
- semester and calendar records;
- registered modules and ordered topics;
- known exam structures and confirmed/estimated dates;
- session availability and exceptions;
- topic progress and learning evidence;
- revision queue and assessment results;
- previous schedule versions and completed assignments.

## Outputs
- a rolling schedule of proposed sessions;
- selected module, topic and activity;
- time allocation within 120 minutes;
- revision events;
- feasibility/risk warnings;
- explanation/reason codes;
- schedule version and source-data version.

## Midsem coverage rule
For each module with `T` syllabus topics:
`midsem_target = ceil(T / 2)`.
Examples: 6→3, 8→4, 9→5, 10→5, 11→6, 12→6.
Default selection is the first `midsem_target` topics in lecturer-provided order. If official paper coverage conflicts, flag the discrepancy and ask the owner/student to confirm the applicable scope. Never silently replace the chosen rule.

## Semester phases
1. **Pre-midsem coverage:** aim for the first-half target in every module, while maintaining revision and assessment.
2. **Midsem exam period:** prioritize papers by actual dates when supplied; retain awareness of other modules.
3. **Post-midsem coverage:** prioritize remaining/second-half topics and continue revision of earlier material.
4. **Endsem preparation:** prioritize upcoming papers, weak topics, comprehensive revision and practice.

When dates are unknown, phase selection must be conservative and operate in rolling weekly mode. Do not infer an official exam date from a typical semester calendar.

## Capacity calculation
Count eligible weekdays in the planning horizon. Exclude:
- Saturdays and Sundays;
- user-entered unavailable days and confirmed holidays;
- dates where an exam or other confirmed commitment makes the normal session impractical.
Do not exclude a day merely because the app lacks a timetable.
Calculate realistic new-learning capacity separately from revision and assessment capacity. A 120-minute session is not 120 minutes of new syllabus coverage.

If remaining work exceeds available capacity:
- quantify the shortfall and assumptions;
- identify the most exam-critical content;
- reduce optional revision before essential coverage;
- show the risk and ask for a decision when a policy conflict exists;
- never claim full coverage when infeasible.

## Topic and module prioritization
Apply hard eligibility rules first:
- phase and midsem/endsem coverage policy;
- topic availability and dependencies;
- due revision and exam constraints;
- rest days and user availability.
Then score eligible work using transparent factors:
- examination urgency;
- coverage urgency;
- learning weakness;
- revision due status;
- estimated difficulty/workload;
- module neglect.
A previous draft weighting (25/25/20/15/10/5) is only a starting hypothesis, not an approved scientific model. Keep weights configurable and validate with simulation before locking them.

Do not let topic count alone determine fairness. Use estimated workload where available. Missing difficulty or duration must not prevent planning; use clearly marked defaults and update estimates from actual session evidence.

## Spaced revision
Initial configurable intervals may be 1, 3, 7, 14 and 30 days after learning. Treat these as defaults to evaluate, not universal scientific guarantees.
- weak performance → earlier and more targeted review;
- strong repeated recall → gradually extend intervals;
- repeated failure → change the activity, not simply repeat the same prompt;
- exam proximity → increase relevant retrieval practice.
Activities may include active recall, short quizzes, explaining without notes, worked problems, practical tasks and mistake review.

## Session composition
One primary module and one primary objective per session. Revision and assessment can include other topics/modules when useful, but avoid excessive context switching.
Illustrative allocation only: new learning 55–70 min, revision 20–30 min, practice/assessment 15–25 min, breaks/transitions 5–15 min. The total must equal 120 minutes and should adapt to the student's needs and task type.

## Recovery and recalculation
- Missed session: record as missed; preserve unfinished work; redistribute future assignments without automatic double workload.
- Difficult topic: keep it in progress; schedule continuation or a different learning activity.
- Date change: recalculate future plan; preserve completed history.
- New topic: insert according to lecturer order; retain existing topic IDs and learning links.
- Removed topic: mark superseded/removed; do not erase historical learning records.
- All targets completed early: schedule consolidation, revision and assessment, not fabricated topics.
- No feasible plan: expose the shortfall and options.

## Interruption-aware assessment

The AIE receives interruption records when the elapsed commitment timer runs down, including cases where the timer expires while the student is outside the app. Interruption time is assessed as a variable, not as a fixed failure to penalise:

- Count interruptions and interruption time as a proportion of the 120-minute session.
- Do not use arbitrary fixed-minute thresholds or automatically treat every interruption as a missed learning opportunity.
- Consider the interruption duration alongside the actual learning progress and the remaining incomplete work for the topic.
- Determine whether additional learning or spaced revision is necessary based on the combined signal.

Session expiry due to external circumstances does not retroactively complete or mark mastered a topic the student did not finish; the unfinished topic and completed progress are preserved and made available to the AIE.

## Repeat and revision phase

After priority syllabus coverage is completed — including pre-midsem first-half coverage, post-midsem coverage and endsem preparation covering remaining and second-half topics — unfinished or weakly learned material is scheduled into a dedicated repeat/revision phase rather than displacing ongoing priority syllabus assignments. Repeating unfinished topics does not displace current priority syllabus assignments, and weak material continues receiving revised practice spaced out from the initial learning interval.

## Determinism and explainability
Given identical inputs, configuration and clock, the scheduler should produce identical output. If random selection is later used for revision variety, inject a seeded random source and record the seed.
Every assignment must include reason codes such as `MIDSEM_COVERAGE_GAP`, `REVISION_DUE`, `LOW_RECALL_SCORE`, `EXAM_APPROACHING`, `MODULE_BEHIND`, or `CONTINUATION_REQUIRED`.

## Core pipeline
1. Load a consistent snapshot of academic data and history.
2. Validate data and source statuses.
3. Calculate eligible session capacity.
4. Determine current academic phase and applicable goals.
5. Build coverage gaps and due revision queue.
6. Filter hard constraints.
7. Score and balance eligible work.
8. Build a 120-minute session.
9. Persist a versioned schedule transactionally.
10. Explain decisions.
11. Record outcomes and update future revision.
12. Recalculate future horizon when meaningful input changes.

## Required simulations
Test odd/even topic counts, unequal module workload, late outlines, no exam dates, late timetable, date changes, missed sessions, weak topics, multiple exams, insufficient capacity and topic additions/removals. Compare fairness, target completion, revision load and daily feasibility.
