package studylock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Upsert
    suspend fun insert(semester: SemesterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg semesters: SemesterEntity)

    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getById(id: String): SemesterEntity?

    @Query("SELECT * FROM semesters ORDER BY created_at DESC")
    suspend fun getAll(): List<SemesterEntity>

    @Query("SELECT * FROM semesters WHERE academic_year = :year AND label = :label")
    suspend fun getByYearAndLabel(year: String, label: String): SemesterEntity?

    @Delete
    suspend fun delete(semester: SemesterEntity)
}

@Dao
interface ModuleDao {
    @Upsert
    suspend fun insert(module: ModuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg modules: ModuleEntity)

    @Query("SELECT * FROM modules WHERE id = :id")
    suspend fun getById(id: String): ModuleEntity?

    @Query("SELECT * FROM modules WHERE semester_id = :semesterId AND status = 'ACTIVE' ORDER BY name")
    suspend fun getBySemester(semesterId: String): List<ModuleEntity>

    @Query("SELECT * FROM modules WHERE status = 'ACTIVE' ORDER BY name")
    suspend fun getAll(): List<ModuleEntity>

    @Query("UPDATE modules SET status = 'ARCHIVED' WHERE id = :id")
    suspend fun archive(id: String)

    @Query("SELECT * FROM modules WHERE code = :code AND semester_id = :semesterId")
    suspend fun getByCode(code: String?, semesterId: String): ModuleEntity?

    @Query("SELECT COUNT(*) FROM modules WHERE semester_id = :semesterId")
    suspend fun countBySemester(semesterId: String): Int

    @androidx.room.Update
    suspend fun update(module: ModuleEntity)

    @Delete
    suspend fun delete(module: ModuleEntity)
}

@Dao
interface TopicDao {
    @Upsert
    suspend fun insert(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg topics: TopicEntity)

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getById(id: String): TopicEntity?

    @Query("SELECT * FROM topics WHERE module_id = :moduleId ORDER BY lecturer_sequence")
    suspend fun getByModule(moduleId: String): List<TopicEntity>

    @Query("SELECT * FROM topics ORDER BY module_id, lecturer_sequence")
    suspend fun getAll(): List<TopicEntity>

    @Query("SELECT * FROM topics WHERE title = :title AND module_id = :moduleId")
    suspend fun getByTitleAndModule(title: String, moduleId: String): TopicEntity?

    @Query("SELECT COUNT(*) FROM topics WHERE module_id = :moduleId AND lifecycle_status = 'ACTIVE'")
    suspend fun countActiveByModule(moduleId: String): Int

    @Query("UPDATE topics SET lifecycle_status = 'SUPERSEDED' WHERE id = :id")
    suspend fun supersede(id: String)

    @Delete
    suspend fun delete(topic: TopicEntity)
}

@Dao
interface AcademicSourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: AcademicSourceEntity)

    @Query("SELECT * FROM academic_sources WHERE id = :id")
    suspend fun getById(id: String): AcademicSourceEntity?

    @Query("SELECT * FROM academic_sources WHERE entity_type = :entityType AND entity_id = :entityId ORDER BY created_at DESC")
    suspend fun getByEntity(entityType: String, entityId: String): List<AcademicSourceEntity>

    @Query("SELECT * FROM academic_sources WHERE status = :status")
    suspend fun getByStatus(status: String): List<AcademicSourceEntity>

    @Delete
    suspend fun delete(source: AcademicSourceEntity)
}

@Dao
interface ModuleOutlineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outline: ModuleOutlineEntity)

    @Query("SELECT * FROM module_outlines WHERE module_id = :moduleId")
    suspend fun getByModule(moduleId: String): List<ModuleOutlineEntity>

    @Query("SELECT * FROM module_outlines WHERE id = :id")
    suspend fun getById(id: String): ModuleOutlineEntity?

    @Query("SELECT * FROM module_outlines WHERE status = :status")
    suspend fun getByStatus(status: String): List<ModuleOutlineEntity>

    @Delete
    suspend fun delete(outline: ModuleOutlineEntity)
}

@Dao
interface StudyAvailabilityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(availability: StudyAvailabilityEntity)

    @Query("SELECT * FROM study_availability WHERE semester_id = :semesterId ORDER BY day_of_week")
    suspend fun getBySemester(semesterId: String): List<StudyAvailabilityEntity>

    @Query("DELETE FROM study_availability WHERE semester_id = :semesterId")
    suspend fun deleteBySemester(semesterId: String)

    @Query("SELECT * FROM study_availability WHERE semester_id = :semesterId AND day_of_week = :dayOfWeek")
    suspend fun getByDay(semesterId: String, dayOfWeek: Int): StudyAvailabilityEntity?

    @Delete
    suspend fun delete(availability: StudyAvailabilityEntity)
}

@Dao
interface CalendarExceptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exception: CalendarExceptionEntity)

    @Query("SELECT * FROM calendar_exceptions WHERE semester_id = :semesterId ORDER BY date")
    suspend fun getBySemester(semesterId: String): List<CalendarExceptionEntity>

    @Query("SELECT * FROM calendar_exceptions WHERE semester_id = :semesterId AND date = :date")
    suspend fun getByDate(semesterId: String, date: Long): CalendarExceptionEntity?

    @Delete
    suspend fun delete(exception: CalendarExceptionEntity)
}

@Dao
interface TopicAliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alias: TopicAliasEntity)

    @Query("SELECT * FROM topic_aliases WHERE topic_id = :topicId")
    suspend fun getByTopic(topicId: String): List<TopicAliasEntity>

    @Query("SELECT * FROM topic_aliases WHERE alias_title = :aliasTitle")
    suspend fun getByAliasTitle(aliasTitle: String): List<TopicAliasEntity>

    @Delete
    suspend fun delete(alias: TopicAliasEntity)
}

@Dao
interface LearningTopicMatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(match: LearningTopicMatchEntity)

    @Query("SELECT * FROM learning_topic_matches WHERE id = :id")
    suspend fun getById(id: String): LearningTopicMatchEntity?

    @Query("SELECT * FROM learning_topic_matches WHERE new_topic_id = :newTopicId")
    suspend fun getByNewTopic(newTopicId: String): List<LearningTopicMatchEntity>

    @Query("SELECT * FROM learning_topic_matches WHERE confirmed = 0")
    suspend fun getUnconfirmed(): List<LearningTopicMatchEntity>

    @Query("UPDATE learning_topic_matches SET confirmed = 1, confirmed_at = :confirmedAt WHERE id = :id")
    suspend fun confirm(id: String, confirmedAt: Long)

    @Delete
    suspend fun delete(match: LearningTopicMatchEntity)
}

@Dao
interface ExamDao {
    @Upsert
    suspend fun insert(exam: ExamEntity)

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getById(id: String): ExamEntity?

    @Query("SELECT * FROM exams WHERE module_id = :moduleId ORDER BY date_time")
    suspend fun getByModule(moduleId: String): List<ExamEntity>

    @Query("SELECT * FROM exams ORDER BY date_time")
    suspend fun getAll(): List<ExamEntity>

    @Query("SELECT * FROM exams WHERE exam_type = :examType")
    suspend fun getByType(examType: String): List<ExamEntity>

    @Query("SELECT * FROM exams WHERE date_status = :status")
    suspend fun getByDateStatus(status: String): List<ExamEntity>

    @Delete
    suspend fun delete(exam: ExamEntity)
}

@Dao
interface ExamSectionDao {
    @Upsert
    suspend fun insert(section: ExamSectionEntity)

    @Upsert
    suspend fun insertAll(sections: List<ExamSectionEntity>)

    @Query("SELECT * FROM exam_sections WHERE exam_id = :examId")
    suspend fun getByExam(examId: String): List<ExamSectionEntity>

    @Delete
    suspend fun delete(section: ExamSectionEntity)
}

@Dao
interface ExamQuestionRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: ExamQuestionRuleEntity)

    @Query("SELECT * FROM exam_question_rules WHERE section_id = :sectionId")
    suspend fun getBySection(sectionId: String): List<ExamQuestionRuleEntity>

    @Delete
    suspend fun delete(rule: ExamQuestionRuleEntity)
}

@Dao
interface StudySessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySessionEntity)

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun getById(id: String): StudySessionEntity?

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAll()
}

@Dao
interface LearningEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: LearningEventEntity)

    @Query("SELECT * FROM learning_events ORDER BY timestamp_millis DESC")
    suspend fun getAll(): List<LearningEventEntity>

    @Query("DELETE FROM learning_events")
    suspend fun deleteAll()
}

@Dao
interface SessionDao {
    @Upsert
    suspend fun insert(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun getById(id: String): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE state IN ('ACTIVE', 'PAUSED') ORDER BY last_updated_millis DESC LIMIT 1")
    suspend fun getActive(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions ORDER BY last_updated_millis DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<FocusSessionEntity>

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}

@Dao
interface SessionInterruptionDao {
    @Upsert
    suspend fun upsert(interruption: SessionInterruptionEntity)

    @Query("SELECT * FROM session_interruptions WHERE session_id = :sessionId ORDER BY started_at_millis")
    suspend fun getBySession(sessionId: String): List<SessionInterruptionEntity>

    @Query(
        "SELECT * FROM session_interruptions WHERE session_id = :sessionId " +
            "AND ended_at_millis IS NULL ORDER BY started_at_millis DESC LIMIT 1"
    )
    suspend fun getOpen(sessionId: String): SessionInterruptionEntity?

    @Query("DELETE FROM session_interruptions")
    suspend fun deleteAll()
}
