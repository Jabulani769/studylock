# 03 — Progressive Academic Data Management (PADM)

## Purpose
PADM is an independent subsystem within AIE. It tracks what academic information is known, its reliability, what operations depend on it and how newly received information changes future plans.

## Real-world arrival pattern
- Semester begins.
- Module outlines commonly arrive after 1–2 weeks, sometimes later.
- Official semester calendar may arrive after teaching begins.
- Midsem timetable may arrive only 3–4 weeks before exams.
- Endsem timetable also arrives later.
- Exam structure may be known before exact exam dates.

These are typical examples, not fixed deadlines. Never create reminders or deadlines from these estimates unless the user confirms them.

## Information categories
### Essential to begin
- registered modules (names/codes);
- basic study availability;
- semester start estimate if known.
A useful rolling plan can begin with this information even when topic outlines are missing. If there are no known topics, use only legitimate user-provided learning materials or a clearly labeled setup/organization activity; do not invent syllabus topics.

### Progressive
- lecturer module outlines;
- ordered topic lists;
- topic difficulty and workload estimates;
- learning materials and coverage mapping.

### Examination-dependent
- official semester calendar;
- midsem/endsem dates and timetables;
- exact paper coverage;
- room, start time and duration.

### Known exam structures
- midsem and endsem structures described in `04-examination-model.md`; endsem interpretation remains pending owner confirmation.

## Data status
Every imported/entered academic record must carry one of:
- `NOT_PROVIDED`
- `ESTIMATED`
- `RECEIVED`
- `VERIFIED`
- `SUPERSEDED`
- `CONFLICTING`

Store source, received date, effective date (if applicable), who entered it, verification state and optional notes. Do not treat `RECEIVED` as `VERIFIED`.

## Hard versus soft dependencies
A hard dependency is required for a specific operation:
- syllabus-specific topic assignment requires a known topic;
- exact exam countdown requires a reliable exam date;
- date-specific exam priority requires module-to-date mapping.
A soft dependency improves quality but does not prevent operation:
- topic difficulty;
- estimated duration;
- holiday list;
- exam timetable before it is published;
- detailed question coverage.

The app must show which operation is limited by missing information, not block all planning.

## Operating modes
1. **Initial planning:** modules and availability known; rolling weekly plan; syllabus detail incomplete.
2. **Progressive planning:** outlines and official dates arrive; update coverage and capacity.
3. **Examination optimization:** reliable dates and structures available; date-specific prioritization.

## Reconciliation rules
- New outline: add topics with stable IDs and preserve lecturer order.
- Existing learning before outline: offer a matching workflow to map prior learning to newly imported topics. Require confirmation when match confidence is ambiguous.
- Late outline: calculate remaining sessions and realistic capacity; report infeasibility or risk.
- Official calendar replaces estimate: mark estimate `SUPERSEDED`, preserve source history, recalculate future plan.
- Conflicting dates: mark `CONFLICTING`; ask for clarification; do not choose silently.
- Timetable not published: generic coverage and revision continue.
- Timetable arrives: activate exam-specific sequencing; do not abandon other modules.
- Structure known but date unknown: use structure for generic practice, not countdowns.
- Date known but topic coverage unknown: date priority can be used, but exact syllabus mapping remains pending.

## Replanning safety
- Only future unstarted assignments may be freely replaced.
- In-progress sessions must be resolved explicitly.
- Completed sessions, answers, scores and learning events are immutable facts.
- Replanning is idempotent: processing the same update twice must not create duplicate topics or sessions.
- Every schedule version records which academic-data version it used.

## User-facing language
Use clear labels such as “Not provided”, “Estimated”, “Received — not verified”, “Verified”, “Replaced by official calendar”, and “Conflicting information”. Never display estimated dates as official.
