package studylock.app.ui

object Routes {
    const val ONBOARDING = "onboarding"
    const val TODAY = "today"
    const val ACADEMICS = "academics"
    const val MODULE_DETAIL = "academics/module/{moduleId}"
    const val EXAMS = "exams"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val SESSION = "session"
    const val SUMMARY = "session/summary"
    const val DOCUMENTS = "documents"
    const val DOCUMENT_READER = "documents/{documentId}"

    fun moduleDetail(moduleId: String): String = "academics/module/$moduleId"
    fun documentReader(documentId: String): String = "documents/$documentId"
}
