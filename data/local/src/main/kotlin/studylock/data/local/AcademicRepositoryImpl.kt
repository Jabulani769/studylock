package studylock.data.local

import androidx.room.withTransaction
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import studylock.domain.AcademicRepository
import studylock.domain.AcademicSource
import studylock.domain.CalendarException
import studylock.domain.DataStatus
import studylock.domain.Exam
import studylock.domain.ExamSection
import studylock.domain.LearningEvent
import studylock.domain.LearningTopicMatch
import studylock.domain.Module
import studylock.domain.Semester
import studylock.domain.StudyAvailability
import studylock.domain.Topic

class AcademicRepositoryImpl(
    private val database: AppDatabase
) : AcademicRepository {

    private val semesterDao = database.semesterDao()
    private val moduleDao = database.moduleDao()
    private val topicDao = database.topicDao()
    private val availabilityDao = database.studyAvailabilityDao()
    private val examDao = database.examDao()
    private val examSectionDao = database.examSectionDao()
    private val academicSourceDao = database.academicSourceDao()
    private val moduleOutlineDao = database.moduleOutlineDao()
    private val learningEventDao = database.learningEventDao()
    private val calendarExceptionDao = database.calendarExceptionDao()
    private val topicMatchDao = database.learningTopicMatchDao()

    override suspend fun getSemester(): Semester? =
        semesterDao.getAll().firstOrNull()?.toDomain()

    override suspend fun saveSemester(semester: Semester) {
        semesterDao.insert(semester.toEntity())
    }

    override suspend fun getModules(semesterId: UUID): List<Module> =
        moduleDao.getBySemester(semesterId.toString()).map { it.toDomain() }

    override suspend fun getAllModules(): List<Module> =
        moduleDao.getAll().map { it.toDomain() }

    override suspend fun saveModule(module: Module) {
        moduleDao.insert(module.toEntity())
    }

    override suspend fun archiveModule(moduleId: UUID) {
        moduleDao.archive(moduleId.toString())
    }

    override suspend fun getTopics(moduleId: UUID): List<Topic> =
        topicDao.getByModule(moduleId.toString()).map { it.toDomain() }

    override suspend fun getAllTopics(): List<Topic> =
        topicDao.getAll().map { it.toDomain() }

    override suspend fun saveTopic(topic: Topic) {
        topicDao.insert(topic.toEntity())
    }

    override suspend fun archiveTopic(topicId: UUID) {
        topicDao.supersede(topicId.toString())
    }

    override suspend fun importOutline(moduleId: UUID, titles: List<String>) {
        database.withTransaction {
            val existing = topicDao.getByModule(moduleId.toString())
            val priorTopics = existing.toList()
            val known = priorTopics.map { normalizeTitle(it.title) }.toMutableSet()
            var sequence = priorTopics.maxOfOrNull { it.lecturerSequence } ?: 0

            titles.map { it.trim() }.filter { it.isNotEmpty() }.forEach { title ->
                val key = normalizeTitle(title)
                if (!known.add(key)) return@forEach
                sequence++
                val newTopic = Topic(
                    moduleId = moduleId,
                    title = title,
                    lecturerSequence = sequence,
                    sourceStatus = "RECEIVED"
                )
                topicDao.insert(newTopic.toEntity())

                // PADM: offer to map earlier learning to the newly imported topic.
                // Ambiguous matches require explicit confirmation before use.
                priorTopics.forEach { prior ->
                    val confidence = titleSimilarity(prior.title, title)
                    if (confidence > 0.0) {
                        topicMatchDao.insert(
                            LearningTopicMatchEntity(
                                id = UUID.randomUUID().toString(),
                                existingTopicId = prior.id,
                                newTopicId = newTopic.id.toString(),
                                matchConfidence = confidence,
                                matchReason = "Similar title to '${prior.title}'",
                                requiresConfirmation = confidence < 0.85,
                                confirmed = false,
                                confirmedAt = null
                            )
                        )
                    }
                }
            }

            val now = System.currentTimeMillis()
            val sourceId = UUID.randomUUID()
            academicSourceDao.insert(
                AcademicSourceEntity(
                    id = sourceId.toString(),
                    entityType = "MODULE_OUTLINE",
                    entityId = moduleId.toString(),
                    sourceName = "Imported outline",
                    receivedDate = now,
                    status = "RECEIVED"
                )
            )
            moduleOutlineDao.insert(
                ModuleOutlineEntity(
                    id = UUID.randomUUID().toString(),
                    moduleId = moduleId.toString(),
                    sourceId = sourceId.toString(),
                    status = "RECEIVED",
                    lecturerOrderPreserved = true,
                    importedAt = now
                )
            )
            moduleDao.getById(moduleId.toString())?.let { module ->
                moduleDao.update(module.copy(outlineStatus = "RECEIVED"))
            }
        }
    }

    override suspend fun getLearningEvents(): List<LearningEvent> =
        learningEventDao.getAll().map { entity ->
            LearningEvent(
                id = UUID.fromString(entity.id),
                topicId = UUID.fromString(entity.topicId),
                sessionId = entity.sessionId?.let(UUID::fromString),
                eventType = entity.eventType,
                timestamp = Instant.ofEpochMilli(entity.timestampMillis),
                durationMinutes = entity.durationMinutes,
                evidence = entity.evidence
            )
        }

    override suspend fun saveLearningEvent(event: LearningEvent) {
        learningEventDao.insert(
            LearningEventEntity(
                id = event.id.toString(),
                topicId = event.topicId.toString(),
                sessionId = event.sessionId?.toString(),
                eventType = event.eventType,
                timestampMillis = event.timestamp.toEpochMilli(),
                durationMinutes = event.durationMinutes,
                evidence = event.evidence
            )
        )
    }

    private fun normalizeTitle(title: String): String =
        title.trim().lowercase().replace(Regex("\\s+"), " ")

    /** Cheap, transparent similarity used only to propose candidate matches. */
    private fun titleSimilarity(a: String, b: String): Double {
        val left = normalizeTitle(a)
        val right = normalizeTitle(b)
        if (left == right) return 1.0
        if (left.length >= 4 && right.length >= 4 && (left in right || right in left)) return 0.7
        val leftTokens = left.split(" ").toSet()
        val rightTokens = right.split(" ").toSet()
        val intersection = leftTokens.intersect(rightTokens).size
        val union = leftTokens.union(rightTokens).size
        return if (union == 0) 0.0 else intersection.toDouble() / union.toDouble()
    }

    override suspend fun getAvailability(semesterId: UUID): List<StudyAvailability> =
        availabilityDao.getBySemester(semesterId.toString()).map { it.toDomain() }

    override suspend fun replaceAvailability(
        semesterId: UUID,
        availability: List<StudyAvailability>
    ) = database.withTransaction {
        availabilityDao.deleteBySemester(semesterId.toString())
        availability.forEach { availabilityDao.insert(it.toEntity()) }
    }

    override suspend fun getExams(): List<Exam> =
        examDao.getAll().map { it.toDomain() }

    override suspend fun getExamSections(examId: UUID): List<ExamSection> =
        examSectionDao.getByExam(examId.toString()).map { it.toDomain() }

    override suspend fun saveExam(exam: Exam, sections: List<ExamSection>) {
        database.withTransaction {
            examDao.insert(exam.toEntity())
            examSectionDao.insertAll(sections.map { it.toEntity() })
        }
    }

    override suspend fun deleteExam(examId: UUID) {
        database.withTransaction {
            examDao.getById(examId.toString())?.let { examDao.delete(it) }
        }
    }

    override suspend fun getCalendarExceptions(semesterId: UUID): List<CalendarException> =
        calendarExceptionDao.getBySemester(semesterId.toString()).map { entity ->
            CalendarException(
                id = UUID.fromString(entity.id),
                semesterId = semesterId,
                date = Instant.ofEpochMilli(entity.date).atZone(ZoneOffset.UTC).toLocalDate(),
                reason = entity.reason,
                isUnavailable = entity.isUnavailable
            )
        }

    override suspend fun getAcademicSources(entityType: String, entityId: UUID): List<AcademicSource> =
        academicSourceDao.getByEntity(entityType, entityId.toString()).map { it.toDomain() }

    override suspend fun saveAcademicSource(source: AcademicSource) {
        academicSourceDao.insert(source.toEntity())
    }

    override suspend fun getUnconfirmedTopicMatches(): List<LearningTopicMatch> =
        topicMatchDao.getUnconfirmed().map { it.toDomain() }

    override suspend fun confirmTopicMatch(matchId: UUID) {
        topicMatchDao.confirm(matchId.toString(), System.currentTimeMillis())
    }

    private fun AcademicSourceEntity.toDomain() = AcademicSource(
        id = UUID.fromString(id),
        entityType = entityType,
        entityId = UUID.fromString(entityId),
        sourceName = sourceName,
        receivedDate = receivedDate,
        effectiveDate = effectiveDate,
        enteredBy = enteredBy,
        status = DataStatus.valueOf(status),
        notes = notes,
        createdAt = createdAt
    )

    private fun AcademicSource.toEntity() = AcademicSourceEntity(
        id = id.toString(),
        entityType = entityType,
        entityId = entityId.toString(),
        sourceName = sourceName,
        receivedDate = receivedDate,
        effectiveDate = effectiveDate,
        enteredBy = enteredBy,
        status = status.name,
        notes = notes,
        createdAt = createdAt
    )

    private fun LearningTopicMatchEntity.toDomain() = LearningTopicMatch(
        id = UUID.fromString(id),
        existingTopicId = existingTopicId?.let(UUID::fromString),
        newTopicId = newTopicId?.let(UUID::fromString),
        matchConfidence = matchConfidence,
        matchReason = matchReason,
        requiresConfirmation = requiresConfirmation,
        confirmed = confirmed,
        confirmedAt = confirmedAt
    )

    private fun Semester.toEntity() = SemesterEntity(
        id = id.toString(),
        academicYear = academicYear,
        label = label,
        startDate = startDate?.toEpochDay(),
        timezone = timezone,
        createdAt = createdAt
    )

    private fun SemesterEntity.toDomain() = Semester(
        id = UUID.fromString(id),
        academicYear = academicYear,
        label = label,
        startDate = startDate?.let { LocalDate.ofEpochDay(it) },
        timezone = timezone,
        createdAt = createdAt
    )

    private fun Module.toEntity() = ModuleEntity(
        id = id.toString(),
        semesterId = semesterId.toString(),
        code = code,
        name = name,
        outlineStatus = outlineStatus,
        workloadEstimateMinutes = workloadEstimateMinutes,
        status = lifecycleStatus
    )

    private fun ModuleEntity.toDomain() = Module(
        id = UUID.fromString(id),
        semesterId = UUID.fromString(semesterId),
        code = code,
        name = name,
        outlineStatus = outlineStatus,
        workloadEstimateMinutes = workloadEstimateMinutes,
        lifecycleStatus = status
    )

    private fun Topic.toEntity() = TopicEntity(
        id = id.toString(),
        moduleId = moduleId.toString(),
        title = title,
        lecturerSequence = lecturerSequence,
        sourceStatus = sourceStatus,
        difficultyEstimate = difficultyEstimate,
        durationEstimateMinutes = durationEstimateMinutes,
        lifecycleStatus = lifecycleStatus
    )

    private fun TopicEntity.toDomain() = Topic(
        id = UUID.fromString(id),
        moduleId = UUID.fromString(moduleId),
        title = title,
        lecturerSequence = lecturerSequence,
        sourceStatus = sourceStatus,
        difficultyEstimate = difficultyEstimate,
        durationEstimateMinutes = durationEstimateMinutes,
        lifecycleStatus = lifecycleStatus
    )

    private fun StudyAvailability.toEntity() = StudyAvailabilityEntity(
        id = id.toString(),
        semesterId = semesterId.toString(),
        dayOfWeek = dayOfWeek,
        available = available,
        preferredStartTimeMinutes = preferredStartTimeMinutes,
        commitmentMinutes = commitmentMinutes
    )

    private fun StudyAvailabilityEntity.toDomain() = StudyAvailability(
        id = UUID.fromString(id),
        semesterId = UUID.fromString(semesterId),
        dayOfWeek = dayOfWeek,
        available = available,
        preferredStartTimeMinutes = preferredStartTimeMinutes,
        commitmentMinutes = commitmentMinutes
    )

    private fun Exam.toEntity() = ExamEntity(
        id = id.toString(),
        moduleId = moduleId.toString(),
        examType = examType,
        dateTime = dateTime?.atZone(ZoneId.of(timezone))?.toInstant()?.toEpochMilli(),
        dateStatus = dateStatus.name,
        durationMinutes = durationMinutes,
        marksAvailable = marksAvailable,
        timezone = timezone,
        sourceDocument = sourceDocument,
        verificationStatus = verificationStatus.name
    )

    private fun ExamEntity.toDomain() = Exam(
        id = UUID.fromString(id),
        moduleId = UUID.fromString(moduleId),
        examType = examType,
        dateTime = dateTime?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.of(timezone))
        },
        dateStatus = studylock.domain.DataStatus.valueOf(dateStatus),
        durationMinutes = durationMinutes,
        marksAvailable = marksAvailable,
        timezone = timezone,
        sourceDocument = sourceDocument,
        verificationStatus = studylock.domain.DataStatus.valueOf(verificationStatus)
    )

    private fun ExamSection.toEntity() = ExamSectionEntity(
        id = id.toString(),
        examId = examId.toString(),
        name = name,
        questionsOffered = questionsOffered,
        questionsCompulsory = questionsCompulsory,
        questionsToAnswer = questionsToAnswer,
        marksPerQuestion = marksPerQuestion
    )

    private fun ExamSectionEntity.toDomain() = ExamSection(
        id = UUID.fromString(id),
        examId = UUID.fromString(examId),
        name = name,
        questionsOffered = questionsOffered,
        questionsCompulsory = questionsCompulsory,
        questionsToAnswer = questionsToAnswer,
        marksPerQuestion = marksPerQuestion
    )
}
