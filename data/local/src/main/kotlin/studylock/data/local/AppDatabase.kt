package studylock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No schema changes in v2 yet; placeholder for future migrations
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS academic_sources (
                id TEXT NOT NULL,
                entity_type TEXT NOT NULL,
                entity_id TEXT NOT NULL,
                source_name TEXT,
                received_date INTEGER,
                effective_date INTEGER,
                entered_by TEXT,
                status TEXT NOT NULL DEFAULT 'NOT_PROVIDED',
                notes TEXT,
                created_at INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS module_outlines (
                id TEXT NOT NULL,
                module_id TEXT NOT NULL,
                source_id TEXT,
                status TEXT NOT NULL DEFAULT 'NOT_PROVIDED',
                lecturer_order_preserved INTEGER NOT NULL DEFAULT 1,
                imported_at INTEGER,
                source_document TEXT,
                notes TEXT,
                PRIMARY KEY(id),
                FOREIGN KEY(module_id) REFERENCES modules(id) ON DELETE CASCADE,
                FOREIGN KEY(source_id) REFERENCES academic_sources(id) ON DELETE SET NULL
            )
        """.trimIndent())

        database.execSQL("CREATE INDEX IF NOT EXISTS idx_module_outlines_module_id ON module_outlines(module_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_module_outlines_source_id ON module_outlines(source_id)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS study_availability (
                id TEXT NOT NULL,
                semester_id TEXT NOT NULL,
                day_of_week INTEGER NOT NULL,
                available INTEGER NOT NULL DEFAULT 1,
                preferred_start_time_minutes INTEGER,
                commitment_minutes INTEGER NOT NULL DEFAULT 120,
                PRIMARY KEY(id),
                FOREIGN KEY(semester_id) REFERENCES semesters(id) ON DELETE CASCADE
            )
        """.trimIndent())

        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_study_availability_semester_day ON study_availability(semester_id, day_of_week)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS calendar_exceptions (
                id TEXT NOT NULL,
                semester_id TEXT NOT NULL,
                date INTEGER NOT NULL,
                reason TEXT NOT NULL,
                is_unavailable INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY(id),
                FOREIGN KEY(semester_id) REFERENCES semesters(id) ON DELETE CASCADE
            )
        """.trimIndent())

        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_calendar_exceptions_semester_date ON calendar_exceptions(semester_id, date)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS topic_aliases (
                id TEXT NOT NULL,
                topic_id TEXT NOT NULL,
                alias_title TEXT NOT NULL,
                match_confidence REAL NOT NULL DEFAULT 0.0,
                confirmed INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(id),
                FOREIGN KEY(topic_id) REFERENCES topics(id) ON DELETE CASCADE
            )
        """.trimIndent())

        database.execSQL("CREATE INDEX IF NOT EXISTS idx_topic_aliases_topic_id ON topic_aliases(topic_id)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS learning_topic_matches (
                id TEXT NOT NULL,
                existing_topic_id TEXT,
                new_topic_id TEXT,
                match_confidence REAL NOT NULL,
                match_reason TEXT,
                requires_confirmation INTEGER NOT NULL DEFAULT 1,
                confirmed INTEGER NOT NULL DEFAULT 0,
                confirmed_at INTEGER,
                PRIMARY KEY(id),
                FOREIGN KEY(existing_topic_id) REFERENCES topics(id) ON DELETE SET NULL,
                FOREIGN KEY(new_topic_id) REFERENCES topics(id) ON DELETE SET NULL
            )
        """.trimIndent())

        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_learning_topic_matches_pair ON learning_topic_matches(existing_topic_id, new_topic_id)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS exams (
                id TEXT NOT NULL,
                module_id TEXT NOT NULL,
                exam_type TEXT NOT NULL,
                date_time INTEGER,
                date_status TEXT NOT NULL DEFAULT 'NOT_PROVIDED',
                duration_minutes INTEGER,
                marks_available INTEGER,
                timezone TEXT NOT NULL DEFAULT 'UTC',
                source_document TEXT,
                verification_status TEXT NOT NULL DEFAULT 'NOT_PROVIDED',
                PRIMARY KEY(id),
                FOREIGN KEY(module_id) REFERENCES modules(id) ON DELETE CASCADE
            )
        """.trimIndent())

        database.execSQL("CREATE INDEX IF NOT EXISTS idx_exams_module_id ON exams(module_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_exams_date_time ON exams(date_time)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS exam_sections (
                id TEXT NOT NULL,
                exam_id TEXT NOT NULL,
                name TEXT,
                questions_offered INTEGER NOT NULL,
                questions_compulsory INTEGER NOT NULL,
                questions_to_answer INTEGER NOT NULL,
                marks_per_question INTEGER NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(exam_id) REFERENCES exams(id) ON DELETE CASCADE
            )
        """.trimIndent())

        database.execSQL("CREATE INDEX IF NOT EXISTS idx_exam_sections_exam_id ON exam_sections(exam_id)")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS exam_question_rules (
                id TEXT NOT NULL,
                section_id TEXT NOT NULL,
                topic_coverage TEXT,
                is_compulsory INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(id),
                FOREIGN KEY(section_id) REFERENCES exam_sections(id) ON DELETE CASCADE
            )
        """.trimIndent())

        database.execSQL("CREATE INDEX IF NOT EXISTS idx_exam_question_rules_section_id ON exam_question_rules(section_id)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS focus_sessions (
                id TEXT NOT NULL,
                state TEXT NOT NULL,
                module_id TEXT,
                topic_id TEXT,
                target_minutes INTEGER NOT NULL,
                start_time_millis INTEGER,
                end_time_millis INTEGER,
                elapsed_active_minutes INTEGER NOT NULL DEFAULT 0,
                break_count INTEGER NOT NULL DEFAULT 0,
                interruption_count INTEGER NOT NULL DEFAULT 0,
                reason TEXT,
                last_updated_millis INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
        """.trimIndent())

        database.execSQL("CREATE INDEX IF NOT EXISTS idx_focus_sessions_state ON focus_sessions(state)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_focus_sessions_start_time ON focus_sessions(start_time_millis)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_focus_sessions_module_id ON focus_sessions(module_id)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE focus_sessions ADD COLUMN interruption_seconds INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE focus_sessions ADD COLUMN learning_evidence TEXT"
        )
        database.execSQL(
            "ALTER TABLE focus_sessions ADD COLUMN segment_start_elapsed_millis INTEGER"
        )
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS session_interruptions (
                id TEXT NOT NULL,
                session_id TEXT NOT NULL,
                started_at_millis INTEGER NOT NULL,
                ended_at_millis INTEGER,
                duration_seconds INTEGER NOT NULL,
                reason TEXT NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(session_id) REFERENCES focus_sessions(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_session_interruptions_session_id " +
                "ON session_interruptions(session_id)"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE focus_sessions ADD COLUMN elapsed_active_seconds INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "UPDATE focus_sessions SET elapsed_active_seconds = elapsed_active_minutes * 60"
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Archive modules instead of deleting them so linked learning history
        // and session records are never destroyed.
        database.execSQL(
            "ALTER TABLE modules ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'"
        )
    }
}

@Database(
    entities = [
        SemesterEntity::class,
        ModuleEntity::class,
        TopicEntity::class,
        AcademicSourceEntity::class,
        ModuleOutlineEntity::class,
        StudyAvailabilityEntity::class,
        CalendarExceptionEntity::class,
        TopicAliasEntity::class,
        LearningTopicMatchEntity::class,
        ExamEntity::class,
        ExamSectionEntity::class,
        ExamQuestionRuleEntity::class,
        StudySessionEntity::class,
        LearningEventEntity::class,
        FocusSessionEntity::class,
        SessionInterruptionEntity::class
    ],
    version = 7
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
    abstract fun moduleDao(): ModuleDao
    abstract fun topicDao(): TopicDao
    abstract fun academicSourceDao(): AcademicSourceDao
    abstract fun moduleOutlineDao(): ModuleOutlineDao
    abstract fun studyAvailabilityDao(): StudyAvailabilityDao
    abstract fun calendarExceptionDao(): CalendarExceptionDao
    abstract fun topicAliasDao(): TopicAliasDao
    abstract fun learningTopicMatchDao(): LearningTopicMatchDao
    abstract fun examDao(): ExamDao
    abstract fun examSectionDao(): ExamSectionDao
    abstract fun examQuestionRuleDao(): ExamQuestionRuleDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun learningEventDao(): LearningEventDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionInterruptionDao(): SessionInterruptionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studylock_database"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
