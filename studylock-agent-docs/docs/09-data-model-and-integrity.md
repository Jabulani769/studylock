# 09 — Data Model and Integrity

## Main entities
- `AcademicYear`
- `Semester`
- `Module`
- `ModuleOutline` / `AcademicSource`
- `Topic`
- `TopicAlias` or `LearningTopicMatch`
- `Exam`
- `ExamSection`
- `ExamQuestionRule`
- `StudyAvailability`
- `CalendarException`
- `StudySession`
- `SessionActivity`
- `LearningEvent`
- `Assessment`
- `RevisionEvent`
- `ScheduleVersion`
- `ScheduledAssignment`
- `SchedulingDecision`
- `PermissionState`
- `EnforcementEvent`

Exact schema is to be designed and reviewed before implementation.

## Important fields and relationships
### Semester
- stable ID, academic year, label;
- start date and source/status;
- phase configuration;
- timezone;
- created/updated timestamps.

### Module
- stable ID, semester ID, code, name;
- outline status/source;
- ordering and optional workload estimate.

### Topic
- stable ID, module ID, title, lecturer sequence;
- source and source status;
- optional difficulty/duration estimate;
- lifecycle status (`ACTIVE`, `SUPERSEDED`, `REMOVED`);
- coverage phase/target mapping.

### Exam
- stable ID, module ID, exam type;
- date/time/duration with source and status;
- structure and topic coverage;
- source document reference.

### Learning records
- stable ID, topic ID, session ID;
- event type, timestamp, duration, assessment evidence;
- understanding/mastery evidence;
- never overwrite historical evidence when a topic title changes.

### Schedule
- schedule version, input data version, generated timestamp;
- date, session duration, selected module/topic/activity;
- reason codes and explanation;
- state and replacement/supersession relation.

## Data integrity rules
- Use stable UUID/ULID identifiers; never use topic title as primary key.
- Enforce foreign keys and appropriate unique constraints.
- Preserve lecturer order independently from display sorting.
- Use transactions for schedule generation and data reconciliation.
- Reprocessing the same imported outline must be idempotent.
- Do not duplicate a daily session.
- Do not delete learning history when topics are renamed, merged or superseded.
- Keep schedule versions and decision rationale for auditability.
- An externally interrupted or externally timed-out session preserves the unfinished topic and completed progress; topics are not marked complete or mastered merely because the commitment timer expired.
- Use explicit timezone-aware date/time types; store instants consistently.
- Use database migrations and test upgrades from prior versions.
- Export/backup must have a documented restore test.

## Matching prior learning to a new outline
When a syllabus topic arrives after a student has already studied similar material:
- suggest candidate matches using title, module, notes and sequence context;
- show match confidence and why it was suggested;
- require confirmation for ambiguous matches;
- link existing learning events to the confirmed topic;
- do not duplicate learning events or claim mastery from a title match alone.
