package studylock.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "semesters",
    primaryKeys = ["id"],
    indices = [Index(value = ["academic_year", "label"], unique = true)]
)
data class SemesterEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "academic_year") val academicYear: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "start_date") val startDate: Long?,
    @ColumnInfo(name = "timezone") val timezone: String = "UTC",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "modules",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["code", "semester_id"], unique = true)
    ]
)
data class ModuleEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "semester_id") val semesterId: String,
    @ColumnInfo(name = "code") val code: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "outline_status") val outlineStatus: String = "NOT_PROVIDED",
    @ColumnInfo(name = "workload_estimate_minutes") val workloadEstimateMinutes: Int? = null,
    @ColumnInfo(name = "status") val status: String = "ACTIVE"
)

@Entity(
    tableName = "topics",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = ModuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["module_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["module_id"]),
        Index(value = ["lecturer_sequence", "module_id"], unique = true),
        Index(value = ["title", "module_id"])
    ]
)
data class TopicEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "module_id") val moduleId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "lecturer_sequence") val lecturerSequence: Int,
    @ColumnInfo(name = "source_status") val sourceStatus: String = "NOT_PROVIDED",
    @ColumnInfo(name = "difficulty_estimate") val difficultyEstimate: Int? = null,
    @ColumnInfo(name = "duration_estimate_minutes") val durationEstimateMinutes: Int? = null,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String = "ACTIVE"
)

@Entity(
    tableName = "academic_sources",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["entity_type", "entity_id"]),
        Index(value = ["status"])
    ]
)
data class AcademicSourceEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "source_name") val sourceName: String? = null,
    @ColumnInfo(name = "received_date") val receivedDate: Long? = null,
    @ColumnInfo(name = "effective_date") val effectiveDate: Long? = null,
    @ColumnInfo(name = "entered_by") val enteredBy: String? = null,
    @ColumnInfo(name = "status") val status: String = "NOT_PROVIDED",
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "module_outlines",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = ModuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["module_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AcademicSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["module_id"]),
        Index(value = ["source_id"])
    ]
)
data class ModuleOutlineEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "module_id") val moduleId: String,
    @ColumnInfo(name = "source_id") val sourceId: String?,
    @ColumnInfo(name = "status") val status: String = "NOT_PROVIDED",
    @ColumnInfo(name = "lecturer_order_preserved") val lecturerOrderPreserved: Boolean = true,
    @ColumnInfo(name = "imported_at") val importedAt: Long? = null,
    @ColumnInfo(name = "source_document") val sourceDocument: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null
)

@Entity(
    tableName = "study_availability",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["semester_id", "day_of_week"], unique = true)
    ]
)
data class StudyAvailabilityEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "semester_id") val semesterId: String,
    @ColumnInfo(name = "day_of_week") val dayOfWeek: Int,
    @ColumnInfo(name = "available") val available: Boolean = true,
    @ColumnInfo(name = "preferred_start_time_minutes") val preferredStartTimeMinutes: Int? = null,
    @ColumnInfo(name = "commitment_minutes") val commitmentMinutes: Int = 120
)

@Entity(
    tableName = "calendar_exceptions",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["semester_id", "date"], unique = true)
    ]
)
data class CalendarExceptionEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "semester_id") val semesterId: String,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "reason") val reason: String,
    @ColumnInfo(name = "is_unavailable") val isUnavailable: Boolean = true
)

@Entity(
    tableName = "topic_aliases",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["topic_id"]),
        Index(value = ["alias_title"])
    ]
)
data class TopicAliasEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "topic_id") val topicId: String,
    @ColumnInfo(name = "alias_title") val aliasTitle: String,
    @ColumnInfo(name = "match_confidence") val matchConfidence: Double = 0.0,
    @ColumnInfo(name = "confirmed") val confirmed: Boolean = false
)

@Entity(
    tableName = "learning_topic_matches",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["existing_topic_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["new_topic_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["existing_topic_id"]),
        Index(value = ["new_topic_id"]),
        Index(value = ["existing_topic_id", "new_topic_id"], unique = true)
    ]
)
data class LearningTopicMatchEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "existing_topic_id") val existingTopicId: String?,
    @ColumnInfo(name = "new_topic_id") val newTopicId: String?,
    @ColumnInfo(name = "match_confidence") val matchConfidence: Double,
    @ColumnInfo(name = "match_reason") val matchReason: String?,
    @ColumnInfo(name = "requires_confirmation") val requiresConfirmation: Boolean = true,
    @ColumnInfo(name = "confirmed") val confirmed: Boolean = false,
    @ColumnInfo(name = "confirmed_at") val confirmedAt: Long?
)

@Entity(
    tableName = "exams",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = ModuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["module_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["module_id"]),
        Index(value = ["date_time", "module_id"]),
        Index(value = ["date_status"])
    ]
)
data class ExamEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "module_id") val moduleId: String,
    @ColumnInfo(name = "exam_type") val examType: String,
    @ColumnInfo(name = "date_time") val dateTime: Long?,
    @ColumnInfo(name = "date_status") val dateStatus: String = "NOT_PROVIDED",
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int? = null,
    @ColumnInfo(name = "marks_available") val marksAvailable: Int? = null,
    @ColumnInfo(name = "timezone") val timezone: String = "UTC",
    @ColumnInfo(name = "source_document") val sourceDocument: String? = null,
    @ColumnInfo(name = "verification_status") val verificationStatus: String = "NOT_PROVIDED"
)

@Entity(
    tableName = "exam_sections",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["exam_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["exam_id"])]
)
data class ExamSectionEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "exam_id") val examId: String,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "questions_offered") val questionsOffered: Int,
    @ColumnInfo(name = "questions_compulsory") val questionsCompulsory: Int,
    @ColumnInfo(name = "questions_to_answer") val questionsToAnswer: Int,
    @ColumnInfo(name = "marks_per_question") val marksPerQuestion: Int
)

@Entity(
    tableName = "exam_question_rules",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = ExamSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["section_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["section_id"])]
)
data class ExamQuestionRuleEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "section_id") val sectionId: String,
    @ColumnInfo(name = "topic_coverage") val topicCoverage: String?,
    @ColumnInfo(name = "is_compulsory") val isCompulsory: Boolean = false
)

@Entity(
    tableName = "study_sessions",
    primaryKeys = ["id"],
    indices = [Index(value = ["module_id"]), Index(value = ["topic_id"])]
)
data class StudySessionEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "module_id") val moduleId: String,
    @ColumnInfo(name = "topic_id") val topicId: String?,
    @ColumnInfo(name = "state") val state: String,
    @ColumnInfo(name = "start_time_millis") val startTimeMillis: Long,
    @ColumnInfo(name = "end_time_millis") val endTimeMillis: Long?,
    @ColumnInfo(name = "elapsed_active_minutes") val elapsedActiveMinutes: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "learning_events",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["topic_id"]),
        Index(value = ["session_id"])
    ]
)
data class LearningEventEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "topic_id") val topicId: String,
    @ColumnInfo(name = "session_id") val sessionId: String?,
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "timestamp_millis") val timestampMillis: Long,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
    @ColumnInfo(name = "evidence") val evidence: String?
)

@Entity(
    tableName = "focus_sessions",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["state"]),
        Index(value = ["start_time_millis"]),
        Index(value = ["module_id"])
    ]
)
data class FocusSessionEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "state") val state: String,
    @ColumnInfo(name = "module_id") val moduleId: String?,
    @ColumnInfo(name = "topic_id") val topicId: String?,
    @ColumnInfo(name = "target_minutes") val targetMinutes: Int,
    @ColumnInfo(name = "start_time_millis") val startTimeMillis: Long?,
    @ColumnInfo(name = "end_time_millis") val endTimeMillis: Long?,
    @ColumnInfo(name = "elapsed_active_minutes") val elapsedActiveMinutes: Int,
    @ColumnInfo(name = "elapsed_active_seconds") val elapsedActiveSeconds: Int = 0,
    @ColumnInfo(name = "break_count") val breakCount: Int,
    @ColumnInfo(name = "interruption_count") val interruptionCount: Int,
    @ColumnInfo(name = "interruption_seconds") val interruptionSeconds: Int = 0,
    @ColumnInfo(name = "learning_evidence") val learningEvidence: String? = null,
    @ColumnInfo(name = "reason") val reason: String?,
    @ColumnInfo(name = "segment_start_elapsed_millis") val segmentStartElapsedMillis: Long? = null,
    @ColumnInfo(name = "last_updated_millis") val lastUpdatedMillis: Long
)

@Entity(
    tableName = "session_interruptions",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["session_id"])]
)
data class SessionInterruptionEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "started_at_millis") val startedAtMillis: Long,
    @ColumnInfo(name = "ended_at_millis") val endedAtMillis: Long?,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int,
    @ColumnInfo(name = "reason") val reason: String
)
