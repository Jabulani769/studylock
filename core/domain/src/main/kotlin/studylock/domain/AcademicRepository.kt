package studylock.domain

import java.util.UUID

/**
 * Local academic data contract. Implemented by the data layer so the UI and
 * domain stay independent of Room.
 */
interface AcademicRepository {
    suspend fun getSemester(): Semester?
    suspend fun saveSemester(semester: Semester)

    suspend fun getModules(semesterId: UUID): List<Module>
    suspend fun getAllModules(): List<Module>
    suspend fun saveModule(module: Module)
    /** Hides a mistaken module without deleting its linked history. */
    suspend fun archiveModule(moduleId: UUID)

    suspend fun getTopics(moduleId: UUID): List<Topic>
    suspend fun getAllTopics(): List<Topic>
    suspend fun saveTopic(topic: Topic)
    /** Marks a topic SUPERSEDED; learning events and past answers are preserved. */
    suspend fun archiveTopic(topicId: UUID)
    suspend fun importOutline(moduleId: UUID, titles: List<String>)
    suspend fun getLearningEvents(): List<LearningEvent>
    suspend fun saveLearningEvent(event: LearningEvent)

    suspend fun getAvailability(semesterId: UUID): List<StudyAvailability>
    suspend fun replaceAvailability(semesterId: UUID, availability: List<StudyAvailability>)

    suspend fun getExams(): List<Exam>
    suspend fun getExamSections(examId: UUID): List<ExamSection>
    suspend fun saveExam(exam: Exam, sections: List<ExamSection>)
    suspend fun deleteExam(examId: UUID)

    // Progressive academic data (PADM) support.
    suspend fun getCalendarExceptions(semesterId: UUID): List<CalendarException>
    suspend fun getAcademicSources(entityType: String, entityId: UUID): List<AcademicSource>
    suspend fun saveAcademicSource(source: AcademicSource)
    suspend fun getUnconfirmedTopicMatches(): List<LearningTopicMatch>
    suspend fun confirmTopicMatch(matchId: UUID)
}
