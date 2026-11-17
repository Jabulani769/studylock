# 13 — Implementation Roadmap

Implementation begins only after the owner approves the open decisions and UI direction.

## Phase 0 — Feasibility and decisions
Deliverables:
- confirm endsem interpretation;
- choose supported Android versions and distribution method;
- decide monitoring scope and enforcement mode;
- test platform capabilities on real device;
- approve architecture and data model;
- approve screen inventory and visual direction.
Exit: no critical P0 product or platform ambiguity.

## Phase 1 — Project foundation
- create native Kotlin Android project;
- configure Gradle/version catalog and CI;
- establish modules and dependency direction;
- add Room and migration framework;
- add clock/time-zone abstractions;
- add baseline tests and documentation.
Exit: clean build, unit tests and database smoke tests.

## Phase 2 — Academic data foundation
- semester/module/topic records;
- progressive information status and source tracking;
- outline entry/import review;
- stable IDs and duplicate handling;
- prior-learning matching workflow.
Exit: PADM scenarios and data-integrity tests pass.

## Phase 3 — Academic Intelligence Engine
- capacity calculator;
- semester phase logic;
- half-topic midsem target;
- module balancing and prioritization;
- schedule versioning and explanations;
- rolling horizon and infeasibility reporting.
Exit: AIE simulations and acceptance criteria pass.

## Phase 4 — Learning and exam model
- learning records and assessments;
- spaced revision;
- exam structure and dates;
- generic exam preparation before timetable;
- date-specific exam optimization.
Exit: exam and revision scenarios pass.

## Phase 5 — Focus session
- session state machine;
- 120-minute session composition;
- breaks, interruption and recovery;
- persistence across process death/reboot.
Exit: focus acceptance tests pass.

## Phase 6 — Lockdown implementation
Only implement the approach that passed Phase 0 feasibility and policy review. Keep adapters isolated. Include degraded states and emergency exit.
Exit: physical-device evidence for each advertised restriction.

## Phase 7 — UI implementation
Only after UI approval. Implement onboarding, dashboard, session, academic data, exams, progress and settings in small vertical slices.
Exit: owner review and accessibility/usability checks.

## Phase 8 — Hardening and release
- backups/export/restore;
- migration and upgrade testing;
- battery/performance review;
- privacy review;
- device compatibility testing;
- release signing and distribution plan.
Exit: release-candidate checklist complete.

## Agent task style
Prefer one focused task per prompt, with explicit files, acceptance criteria and tests. Avoid broad “build the whole app” prompts. Report only changed paths, checks, blockers and evidence.
