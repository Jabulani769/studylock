package studylock.app.settings

/** Issue #5: the user chooses whether the engine drives the plan. */
enum class PlanningMode {
    RECOMMENDED,
    ROUTINE;

    companion object {
        fun fromStored(value: String?): PlanningMode =
            entries.firstOrNull { it.name == value } ?: RECOMMENDED
    }
}

/** Only values the user has explicitly accepted are persisted (D-23). */
enum class RedesignChoice {
    NONE,
    COMPRESS,
    EXTRA_WEEKDAY,
    WEEKEND_AND_EXTRA;

    companion object {
        fun fromStored(value: String?): RedesignChoice =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}
