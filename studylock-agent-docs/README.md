# StudyLock — Agent Documentation Pack

StudyLock is an Android-first study commitment and focus-enforcement application. This repository documentation is the product and engineering source of truth. Read this file, then `AGENTS.md`, then the relevant documents in `docs/` before making changes.

## Product in one sentence
StudyLock automatically plans and guides one protected 120-minute study session on each study weekday, manages syllabus coverage and exam preparation as academic information arrives, and supports an ADHD-friendly focus experience.

## Current status
- Product requirements: substantially specified.
- Academic planning rules: specified, with adjustable parameters clearly identified.
- Progressive academic data handling: specified.
- Proposed native Android architecture: documented as the recommended baseline, subject to the feasibility and architecture gates below.
- UI/UX: requirements and a draft screen inventory exist, but visual direction and final screen flows still require owner review.
- Lockdown and monitoring: feasibility must be demonstrated on real target devices before strong enforcement claims are made.
- No implementation is authorized until the owner approves the open decisions and mobile-first UI/UX direction.

## Read order
1. [`AGENTS.md`](AGENTS.md)
2. `docs/00-product-brief.md`
3. `docs/01-product-requirements.md`
4. `docs/02-academic-intelligence-engine.md`
5. `docs/03-progressive-academic-data.md`
6. `docs/04-examination-model.md`
7. `docs/05-focus-session.md`
8. `docs/06-lockdown-and-monitoring.md`
9. `docs/07-adhd-ux-principles.md`
10. `docs/08-technical-architecture.md`
11. `docs/09-data-model-and-integrity.md`
12. `docs/10-security-and-privacy.md`
13. `docs/11-ui-screen-inventory.md`
14. `docs/12-testing-and-acceptance.md`
15. `docs/13-implementation-roadmap.md`
16. `docs/14-open-decisions.md`
17. `docs/15-developer-workstation.md`

## Authority and conflict resolution
- Confirmed owner decisions in these documents override agent assumptions.
- `14-open-decisions.md` is the list of unresolved decisions. Do not silently turn proposals into confirmed requirements.
- If documents conflict, stop and report the conflict. Do not pick whichever option is easiest to code.
- Keep historical learning records immutable. Replanning changes future assignments, not past facts.
- Never invent academic topics, official dates, examination structures, permissions, or Android capabilities.

## Recommended first agent task
Do a documentation-only consistency review. Return:
- conflicting or missing requirements;
- Android API feasibility risks;
- proposed technical decisions with reasons;
- questions requiring owner approval;
- no source-code changes.
