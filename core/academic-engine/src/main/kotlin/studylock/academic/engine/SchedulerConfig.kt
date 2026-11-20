package studylock.academic.engine

/**
 * Configurable scheduling parameters.
 *
 * These are starting hypotheses, not official or scientifically validated
 * rules. D-13 (difficulty/time) and D-17 (exam preparation windows) are still
 * "Proposed" decisions, so every value here is caller-overridable and the
 * generated rationale states when defaults were used. Nothing here is ever
 * presented to the student as an official academic date or requirement.
 */
data class SchedulerConfig(
    /** Clearly-marked default when a topic has no estimate (D-13). */
    val defaultDifficulty: Int = 5,
    val defaultDurationMinutes: Int = 60,
    /** Illustrative split: a 120-minute session is not 120 minutes of new learning. */
    val newLearningMinutesPerSession: Int = 70,
    /** Configurable exam approach window; not a guaranteed rule (D-17). */
    val examApproachWindowDays: Int = 14,
    val midsemApproachWindowDays: Int = 10,
    /** Days before/after a confirmed exam on which the normal session is impractical. */
    val examBlockWindowDays: Int = 0,
    /** Below this confidence a prior-learning match needs explicit confirmation. */
    val ambiguousMatchThreshold: Double = 0.85,
    /** Spaced-revision reference interval used for capacity estimation. */
    val revisionIntervalDays: Int = 7,
    val weights: ScoreWeights = ScoreWeights(),
    /** Previous schedule version; the new result is priorVersion + 1. */
    val priorVersion: Int = 1,
    /** Identifier of the academic-data snapshot used, for rationale/versioning. */
    val academicDataVersion: String = "",

    /** "Stick to my routine": one automatic session a day, no exam phasing. */
    val routineMode: Boolean = false,
    /** Owner-approved compression first step when the workload will not fit. */
    val compressedLearningMinutesPerSession: Int = 105,
    /** Set only after the user explicitly accepts the redesign. */
    val useCompressedPlan: Boolean = false,
    /** Time-boxed extra weekday block, explicit opt-in only (D-23). */
    val extraWeekdaySession: Boolean = false,
    /** Optional weekend study, explicit opt-in only (D-23). */
    val weekendStudy: Boolean = false
)

/**
 * Starting weighting hypothesis only (AIE: 25/25/20/15/10/5). Configurable;
 * must be simulated and calibrated before being treated as authoritative.
 */
data class ScoreWeights(
    val examinationUrgency: Double = 25.0,
    val coverageUrgency: Double = 25.0,
    val learningWeakness: Double = 20.0,
    val revisionDue: Double = 15.0,
    val difficultyWorkload: Double = 10.0,
    val moduleNeglect: Double = 5.0
)
