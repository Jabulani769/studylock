package studylock.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import studylock.academic.engine.ScheduleRequest
import studylock.academic.engine.ScheduledAssignment
import studylock.app.enforcement.EnforcementController
import studylock.app.settings.SettingsRepository
import studylock.common.clock.StudyClock
import studylock.domain.AcademicRepository
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
import studylock.focus.EnforcementResolver
import studylock.focus.EnforcementState
import studylock.focus.FocusSession
import studylock.focus.FocusStateMachine
import studylock.focus.LearningEvidence
import studylock.focus.SessionConfig
import studylock.focus.SessionInterruption
import studylock.focus.SessionRepository
import studylock.focus.SessionState

data class ModuleWithTopics(
    val module: Module,
    val topics: List<Topic>
)

data class ExamWithSections(
    val exam: Exam,
    val sections: List<ExamSection>
)

data class TodayPlan(
    val date: LocalDate,
    val module: Module,
    val topic: Topic,
    val requiredMinutes: Int,
    val explanation: String
)

data class NextAssignment(
    val date: LocalDate,
    val module: Module,
    val topic: Topic
)

enum class StudyExamType(val stored: String, val label: String, val totalMarks: Int) {
    MIDSEM("MIDSEM", "Midsem", 50),
    ENDSEM("ENDSEM", "Endsem", 100)
}

data class NewModuleDraft(val name: String, val code: String?)

sealed interface TodayState {
    data object NeedsSemester : TodayState
    data object NeedsModules : TodayState
    data object NeedsTopics : TodayState
    data class RestDay(val next: NextAssignment?) : TodayState
    data class Unavailable(val message: String, val next: NextAssignment?) : TodayState
    data class Planned(val plan: TodayPlan, val next: NextAssignment?) : TodayState
    data class Active(val session: FocusSession) : TodayState
}

data class AppUiState(
    val isLoading: Boolean = true,
    val busy: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val semester: Semester? = null,
    val modules: List<ModuleWithTopics> = emptyList(),
    val availability: List<StudyAvailability> = emptyList(),
    val exams: List<ExamWithSections> = emptyList(),
    val history: List<FocusSession> = emptyList(),
    val activeSession: FocusSession? = null,
    val today: TodayState = TodayState.NeedsSemester,
    val todayDate: LocalDate? = null,
    val timezone: String = "UTC",
    val timestampMillis: Long = 0L,
    val monotonicMillis: Long = 0L,
    val lastSummary: FocusSession? = null,
    val summaryInterruptions: List<SessionInterruption> = emptyList(),
    val backups: List<String> = emptyList(),
    val learningEvents: List<LearningEvent> = emptyList(),
    val pendingTopicMatches: List<LearningTopicMatch> = emptyList(),
    val enforcementState: EnforcementState = EnforcementState.PERMISSION_REQUIRED,
    val enforcementDescription: String = "Setup needed to enable monitoring.",
    val leftAppCount: Int = 0,
    val message: String? = null
) {
    fun module(id: UUID?): Module? =
        modules.firstOrNull { it.module.id == id }?.module

    fun topic(moduleId: UUID?, topicId: UUID?): Topic? =
        modules.firstOrNull { it.module.id == moduleId }
            ?.topics
            ?.firstOrNull { it.id == topicId }

    fun topicTitle(topicId: UUID?): String? =
        modules.flatMap { it.topics }.firstOrNull { it.id == topicId }?.title

    fun examStructureLabel(exam: Exam): String {
        val sections = exams.firstOrNull { it.exam.id == exam.id }?.sections.orEmpty()
        if (sections.isEmpty()) return "Structure not provided"
        return sections.joinToString(" · ") { section ->
            val name = section.name ?: "Section"
            "$name: ${section.questionsToAnswer} x ${section.marksPerQuestion}"
        }
    }
}

class AppViewModel(
    private val academicRepository: AcademicRepository,
    private val sessionRepository: SessionRepository,
    private val focusStateMachine: FocusStateMachine,
    private val scheduler: studylock.academic.engine.AcademicScheduler,
    private val settingsRepository: SettingsRepository,
    private val backupRepository: studylock.data.local.BackupRepository,
    private val enforcement: EnforcementController,
    private val clock: StudyClock
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    /**
     * The running clock is kept separate from UI state: a once-per-second tick
     * must not recompose every screen in the app. Only the session countdown
     * reads this flow.
     */
    private val _clockTick = MutableStateFlow(0L to 0L)
    val clockTick: StateFlow<Pair<Long, Long>> = _clockTick.asStateFlow()

    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            val onboardingCompleted = runCatching {
                settingsRepository.onboardingCompleted.first()
            }.getOrDefault(false)
            _uiState.update { it.copy(onboardingCompleted = onboardingCompleted) }
            refreshInternal()
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshInternal() }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun finishOnboarding(
        academicYear: String,
        label: String,
        modules: List<NewModuleDraft>,
        availableDays: Set<Int>
    ) {
        viewModelScope.launch {
            val year = academicYear.trim()
            val semesterLabel = label.trim()
            if (year.isEmpty() || semesterLabel.isEmpty()) {
                setMessage("Enter an academic year and semester label.")
                return@launch
            }
            _uiState.update { it.copy(busy = true, message = null) }
            try {
                val existing = academicRepository.getSemester()
                val semester = (existing ?: Semester(
                    academicYear = year,
                    label = semesterLabel,
                    timezone = clock.zoneId.id
                )).copy(
                    academicYear = year,
                    label = semesterLabel,
                    timezone = clock.zoneId.id
                )
                academicRepository.saveSemester(semester)

                val existingModules = academicRepository.getAllModules()
                    .filter { it.semesterId == semester.id }
                modules.forEach { draft ->
                    val name = draft.name.trim()
                    if (name.isEmpty()) return@forEach
                    val duplicate = existingModules.any { it.name.equals(name, ignoreCase = true) }
                    if (!duplicate) {
                        academicRepository.saveModule(
                            Module(
                                semesterId = semester.id,
                                code = draft.code?.trim()?.ifBlank { null },
                                name = name
                            )
                        )
                    }
                }

                val availability = (1..7).map { day ->
                    StudyAvailability(
                        semesterId = semester.id,
                        dayOfWeek = day,
                        available = day in availableDays && day <= 5,
                        commitmentMinutes = 120
                    )
                }
                academicRepository.replaceAvailability(semester.id, availability)
                // Refresh before flipping the onboarding flag so the first frame
                // of the main shell already has the saved semester and modules.
                refreshInternal()
                settingsRepository.setOnboardingCompleted(true)
                _uiState.update { it.copy(onboardingCompleted = true, busy = false) }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.update {
                    it.copy(busy = false, message = "Could not save your setup. Try again.")
                }
            }
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            _uiState.update { it.copy(onboardingCompleted = true) }
            refreshInternal()
        }
    }

    fun addModule(name: String, code: String?) {
        viewModelScope.launch {
            val semester = _uiState.value.semester
            if (semester == null) {
                setMessage("Set up a semester first.")
                return@launch
            }
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                setMessage("Enter a module name.")
                return@launch
            }
            if (_uiState.value.modules.any { it.module.name.equals(trimmedName, ignoreCase = true) }) {
                setMessage("That module is already in your list.")
                return@launch
            }
            try {
                academicRepository.saveModule(
                    Module(
                        semesterId = semester.id,
                        code = code?.trim()?.ifBlank { null },
                        name = trimmedName
                    )
                )
                refreshInternal()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                setMessage("Could not add that module.")
            }
        }
    }

    fun addTopic(moduleId: UUID, title: String) {
        viewModelScope.launch {
            val module = _uiState.value.module(moduleId)
            if (module == null) {
                setMessage("That module no longer exists.")
                return@launch
            }
            val trimmed = title.trim()
            if (trimmed.isEmpty()) {
                setMessage("Enter a topic title.")
                return@launch
            }
            val existingTopics = _uiState.value.modules
                .firstOrNull { it.module.id == moduleId }
                ?.topics
                .orEmpty()
            if (existingTopics.any { it.title.equals(trimmed, ignoreCase = true) }) {
                setMessage("That topic is already in this module.")
                return@launch
            }
            try {
                val nextSequence = (existingTopics.maxOfOrNull { it.lecturerSequence } ?: 0) + 1
                academicRepository.saveTopic(
                    Topic(
                        moduleId = moduleId,
                        title = trimmed,
                        lecturerSequence = nextSequence,
                        sourceStatus = "RECEIVED"
                    )
                )
                if (module.outlineStatus == "NOT_PROVIDED") {
                    academicRepository.saveModule(module.copy(outlineStatus = "RECEIVED"))
                }
                refreshInternal()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                setMessage("Could not add that topic.")
            }
        }
    }

    fun setAvailabilityDay(dayOfWeek: Int, available: Boolean) {
        if (dayOfWeek !in 1..5) return
        viewModelScope.launch {
            val semester = _uiState.value.semester ?: return@launch
            val current = _uiState.value.availability.ifEmpty {
                defaultAvailability(semester.id)
            }
            val updated = current.map { entry ->
                if (entry.dayOfWeek == dayOfWeek) entry.copy(available = available) else entry
            }
            try {
                academicRepository.replaceAvailability(semester.id, updated)
                refreshInternal()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                setMessage("Could not update study days.")
            }
        }
    }

    fun addExam(
        moduleId: UUID,
        examType: StudyExamType,
        date: LocalDate?,
        dateStatus: DataStatus
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, message = null) }
            val examId = UUID.randomUUID()
            val resolvedStatus = if (date == null) DataStatus.NOT_PROVIDED else dateStatus
            val exam = Exam(
                id = examId,
                moduleId = moduleId,
                examType = examType.stored,
                dateTime = date?.atStartOfDay(),
                dateStatus = resolvedStatus,
                marksAvailable = examType.totalMarks,
                timezone = clock.zoneId.id,
                verificationStatus = resolvedStatus
            )
            val sections = when (examType) {
                StudyExamType.MIDSEM -> listOf(
                    section(examId, "Question 1", 1, 1, 1, 20),
                    section(examId, "Question 2", 1, 1, 1, 20),
                    section(examId, "Question 3", 1, 1, 1, 10)
                )
                StudyExamType.ENDSEM -> listOf(
                    section(examId, "Section A", 2, 2, 2, 20),
                    section(examId, "Section B", 5, 0, 3, 20)
                )
            }
            try {
                academicRepository.saveExam(exam, sections)
                _uiState.update { it.copy(busy = false) }
                refreshInternal()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.update {
                    it.copy(busy = false, message = "Could not save that exam.")
                }
            }
        }
    }

    fun renameModule(moduleId: UUID, name: String, code: String?) {
        viewModelScope.launch {
            val module = _uiState.value.module(moduleId)
            if (module == null) {
                setMessage("That module no longer exists.")
                return@launch
            }
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                setMessage("Enter a module name.")
                return@launch
            }
            if (_uiState.value.modules.any {
                    it.module.id != moduleId && it.module.name.equals(trimmed, ignoreCase = true)
                }
            ) {
                setMessage("That module is already in your list.")
                return@launch
            }
            runCatching {
                academicRepository.saveModule(
                    module.copy(name = trimmed, code = code?.trim()?.ifBlank { null })
                )
            }.onSuccess {
                setMessage("Module updated.")
                refreshInternal()
            }.onFailure {
                setMessage("Could not update that module.")
            }
        }
    }

    fun removeModule(moduleId: UUID) {
        viewModelScope.launch {
            if (_uiState.value.activeSession?.moduleId == moduleId) {
                setMessage("End the active session before removing this module.")
                return@launch
            }
            if (_uiState.value.module(moduleId) == null) {
                setMessage("That module no longer exists.")
                return@launch
            }
            runCatching {
                academicRepository.archiveModule(moduleId)
            }.onSuccess {
                setMessage("Module removed from your plan. Past records are kept on this phone.")
                refreshInternal()
            }.onFailure {
                setMessage("Could not remove that module.")
            }
        }
    }

    fun renameTopic(topicId: UUID, title: String) {
        viewModelScope.launch {
            val entry = _uiState.value.modules.flatMap { it.topics }
                .firstOrNull { it.id == topicId }
            if (entry == null) {
                setMessage("That topic no longer exists.")
                return@launch
            }
            val trimmed = title.trim()
            if (trimmed.isEmpty()) {
                setMessage("Enter a topic title.")
                return@launch
            }
            val siblings = _uiState.value.modules
                .firstOrNull { it.module.id == entry.moduleId }
                ?.topics.orEmpty()
            if (siblings.any { it.id != topicId && it.title.equals(trimmed, ignoreCase = true) }) {
                setMessage("That topic is already in this module.")
                return@launch
            }
            runCatching {
                academicRepository.saveTopic(entry.copy(title = trimmed))
            }.onSuccess {
                setMessage("Topic renamed.")
                refreshInternal()
            }.onFailure {
                setMessage("Could not rename that topic.")
            }
        }
    }

    fun removeTopic(topicId: UUID) {
        viewModelScope.launch {
            val topic = _uiState.value.modules.flatMap { it.topics }
                .firstOrNull { it.id == topicId }
            if (topic == null) {
                setMessage("That topic no longer exists.")
                return@launch
            }
            if (_uiState.value.activeSession?.topicId == topicId) {
                setMessage("End the active session before removing this topic.")
                return@launch
            }
            runCatching {
                academicRepository.archiveTopic(topicId)
            }.onSuccess {
                setMessage("Topic removed from your plan. Past learning records are kept.")
                refreshInternal()
            }.onFailure {
                setMessage("Could not remove that topic.")
            }
        }
    }

    fun deleteExam(examId: UUID) {
        viewModelScope.launch {
            runCatching {
                academicRepository.deleteExam(examId)
            }.onSuccess {
                setMessage("Exam information removed.")
                refreshInternal()
            }.onFailure {
                setMessage("Could not remove that exam.")
            }
        }
    }

    fun editSemester(year: String, label: String) {
        viewModelScope.launch {
            val semester = _uiState.value.semester
            if (semester == null) {
                setMessage("Set up a semester first.")
                return@launch
            }
            val trimmedYear = year.trim()
            val trimmedLabel = label.trim()
            if (trimmedYear.isEmpty() || trimmedLabel.isEmpty()) {
                setMessage("Enter both the academic year and semester label.")
                return@launch
            }
            runCatching {
                academicRepository.saveSemester(
                    semester.copy(academicYear = trimmedYear, label = trimmedLabel)
                )
            }.onSuccess {
                setMessage("Semester updated.")
                refreshInternal()
            }.onFailure {
                setMessage("Could not update that semester.")
            }
        }
    }

    fun startSession() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.activeSession != null) return@launch
            val plan = (state.today as? TodayState.Planned)?.plan ?: return@launch
            val started = focusStateMachine.start(
                FocusSession(),
                SessionConfig(
                    moduleId = plan.module.id,
                    topicId = plan.topic.id,
                    targetMinutes = plan.requiredMinutes
                )
            )
            sessionRepository.save(started)
            enforcement.startSession()
            enforcement.applyQuietMode()
            _uiState.update {
                it.copy(
                    activeSession = started,
                    timestampMillis = clock.millis,
                    monotonicMillis = clock.elapsedRealtimeMillis
                )
            }
            startTicker()
        }
    }

    fun pauseSession(reason: String = "user_pause") {
        viewModelScope.launch {
            val session = _uiState.value.activeSession ?: return@launch
            if (session.state != SessionState.ACTIVE) return@launch
            val paused = focusStateMachine.pause(session).copy(
                interruptionCount = session.interruptionCount + 1
            )
            enforcement.restoreQuietMode()
            sessionRepository.save(paused)
            sessionRepository.saveInterruption(
                SessionInterruption(
                    sessionId = session.id,
                    startedAt = clock.instant,
                    reason = reason
                )
            )
            stopTicker()
            _uiState.update {
                it.copy(
                    activeSession = paused,
                    timestampMillis = clock.millis,
                    monotonicMillis = clock.elapsedRealtimeMillis
                )
            }
        }
    }

    fun resumeSession() {
        viewModelScope.launch {
            val session = _uiState.value.activeSession ?: return@launch
            if (session.state != SessionState.PAUSED) return@launch
            var current = session
            val open = sessionRepository.getOpenInterruption(session.id)
            if (open != null) {
                val closed = closeInterruption(open)
                current = current.copy(
                    interruptionSeconds = current.interruptionSeconds + closed.durationSeconds
                )
            }
            val resumed = focusStateMachine.resume(current)
            enforcement.applyQuietMode()
            sessionRepository.save(resumed)
            _uiState.update {
                it.copy(
                    activeSession = resumed,
                    timestampMillis = clock.millis,
                    monotonicMillis = clock.elapsedRealtimeMillis
                )
            }
            startTicker()
        }
    }

    fun endSessionEarly() {
        viewModelScope.launch {
            var session = _uiState.value.activeSession ?: return@launch
            if (session.state != SessionState.ACTIVE && session.state != SessionState.PAUSED) {
                return@launch
            }
            stopTicker()
            val open = sessionRepository.getOpenInterruption(session.id)
            if (open != null) {
                val closed = closeInterruption(open)
                session = session.copy(
                    interruptionSeconds = session.interruptionSeconds + closed.durationSeconds
                )
            }
            val exited = focusStateMachine.earlyExit(session, confirmed = true)
            enforcement.restoreQuietMode()
            sessionRepository.save(exited)
            _uiState.update {
                it.copy(
                    activeSession = null,
                    lastSummary = exited,
                    summaryInterruptions = sessionRepository.getInterruptions(exited.id),
                    timestampMillis = clock.millis,
                    monotonicMillis = clock.elapsedRealtimeMillis
                )
            }
            refreshInternal()
        }
    }

    fun setLearningEvidence(evidence: LearningEvidence?) {
        viewModelScope.launch {
            val summary = _uiState.value.lastSummary ?: return@launch
            val updated = summary.copy(learningEvidence = evidence)
            sessionRepository.save(updated)
            _uiState.update { state ->
                state.copy(
                    lastSummary = updated,
                    history = state.history.map {
                        if (it.id == updated.id) updated else it
                    }
                )
            }
        }
    }

    fun openSummary(sessionId: UUID) {
        viewModelScope.launch {
            val session = sessionRepository.getById(sessionId) ?: return@launch
            _uiState.update {
                it.copy(
                    lastSummary = session,
                    summaryInterruptions = sessionRepository.getInterruptions(session.id)
                )
            }
        }
    }

    fun dismissSummary() {
        _uiState.update { it.copy(lastSummary = null, summaryInterruptions = emptyList()) }
    }

    fun importOutline(moduleId: UUID, text: String) {
        viewModelScope.launch {
            val titles = parseOutline(text)
            if (titles.isEmpty()) {
                setMessage("No topic titles found in that text.")
                return@launch
            }
            runCatching {
                academicRepository.importOutline(moduleId, titles)
            }.onSuccess {
                setMessage("Imported ${titles.size} outline line(s). Duplicates were skipped.")
                refreshInternal()
            }.onFailure {
                setMessage("Could not import that outline.")
            }
        }
    }

    /** PADM: confirm an ambiguous prior-learning match after the student reviews it. */
    fun confirmTopicMatch(matchId: UUID) {
        viewModelScope.launch {
            runCatching {
                academicRepository.confirmTopicMatch(matchId)
            }.onSuccess {
                setMessage("Prior-learning match confirmed.")
                refreshInternal()
            }.onFailure {
                setMessage("Could not confirm that match.")
            }
        }
    }

    fun openUsageAccessSettings() = enforcement.openUsageAccessSettings()

    fun openNotificationPolicySettings() = enforcement.openNotificationPolicySettings()

    fun acknowledgeEnforcementInterruption() {
        viewModelScope.launch {
            enforcement.acknowledgeInterruption()
            refreshInternal()
        }
    }

    fun recordAssessment(topicId: UUID, correct: Int, total: Int) {
        viewModelScope.launch {
            if (total <= 0 || correct !in 0..total) {
                setMessage("Enter a valid recall result.")
                return@launch
            }
            runCatching {
                academicRepository.saveLearningEvent(
                    LearningEvent(
                        topicId = topicId,
                        eventType = "ASSESSMENT",
                        timestamp = Instant.ofEpochMilli(clock.millis),
                        evidence = "$correct/$total"
                    )
                )
            }.onSuccess {
                setMessage("Recall result saved. It will influence future revision priority.")
                refreshInternal()
            }.onFailure {
                setMessage("Could not save the recall result.")
            }
        }
    }

    private suspend fun recordStudyEvent(session: FocusSession) {
        val topicId = session.topicId ?: return
        runCatching {
            academicRepository.saveLearningEvent(
                LearningEvent(
                    topicId = topicId,
                    sessionId = session.id,
                    eventType = "STUDY",
                    timestamp = Instant.ofEpochMilli(clock.millis),
                    durationMinutes = session.elapsedActiveSeconds / 60,
                    evidence = session.learningEvidence?.name
                )
            )
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            runCatching {
                backupRepository.exportBackup(clock.millis)
            }.onSuccess {
                setMessage("Backup saved on this phone: $it")
                refreshInternal()
            }.onFailure {
                setMessage("Could not create the backup.")
            }
        }
    }

    fun restoreBackup(fileName: String) {
        viewModelScope.launch {
            if (_uiState.value.activeSession != null) {
                setMessage("Pause or end the current session before restoring a backup.")
                return@launch
            }
            runCatching {
                backupRepository.restoreBackup(fileName)
            }.onSuccess {
                dismissSummary()
                setMessage("Backup restored.")
                refreshInternal()
            }.onFailure {
                setMessage("Could not restore that backup.")
            }
        }
    }

    private suspend fun closeInterruption(open: SessionInterruption): SessionInterruption {
        val now = clock.instant
        val duration = ((now.toEpochMilli() - open.startedAt.toEpochMilli()) / 1000L)
            .toInt()
            .coerceAtLeast(0)
        val closed = open.copy(endedAt = now, durationSeconds = duration)
        sessionRepository.saveInterruption(closed)
        return closed
    }

    private suspend fun refreshInternal() {
        try {
            val semester = academicRepository.getSemester()
            // Independent reads run concurrently so navigation and refresh do
            // not wait for a long chain of sequential database queries.
            coroutineScope {
            val allModulesDeferred = async { academicRepository.getAllModules() }
            val allTopicsDeferred = async { academicRepository.getAllTopics() }
            val eventsDeferred = async { academicRepository.getLearningEvents() }
            val matchesDeferred = async { academicRepository.getUnconfirmedTopicMatches() }
            val historyDeferred = async { sessionRepository.getHistory(50) }
            val backupsDeferred = async { backupRepository.listBackups() }
            val currentDeferred = async { sessionRepository.getCurrent() }
            val availabilityDeferred = async {
                semester?.let { academicRepository.getAvailability(it.id) } ?: emptyList()
            }
            val exceptionsDeferred = async {
                semester?.let { academicRepository.getCalendarExceptions(it.id) } ?: emptyList()
            }

            val allModules = allModulesDeferred.await()
            val scopedModules = if (semester == null) {
                emptyList()
            } else {
                allModules.filter { it.semesterId == semester.id }
            }
            val allTopics = allTopicsDeferred.await()
            val moduleIds = scopedModules.map { it.id }.toSet()
            val scopedTopics = allTopics.filter { it.moduleId in moduleIds }
            val moduleViews = scopedModules
                .sortedBy { it.name.lowercase() }
                .map { module ->
                    ModuleWithTopics(
                        module = module,
                        topics = scopedTopics
                            .filter { it.moduleId == module.id }
                            .sortedBy { it.lecturerSequence }
                    )
                }

            val availability = semanticAvailability(semester, availabilityDeferred.await())

            val exams: List<ExamWithSections> = if (semester == null) {
                emptyList()
            } else {
                val found = academicRepository.getExams()
                    .filter { exam -> moduleIds.contains(exam.moduleId) }
                val sectionLists = found.map { exam ->
                    async { academicRepository.getExamSections(exam.id) }
                }.awaitAll()
                found.zip(sectionLists).map { (exam, sections) ->
                    ExamWithSections(exam = exam, sections = sections)
                }
            }

            val calendarExceptions = exceptionsDeferred.await()

            val history = historyDeferred.await()
            val learningEvents = eventsDeferred.await()
            val pendingTopicMatches = matchesDeferred.await()
            val backups = backupsDeferred.await()
            var activeSession = currentDeferred.await()
            var summary = _uiState.value.lastSummary

            if (activeSession != null &&
                activeSession.state == SessionState.ACTIVE &&
                activeSession.remainingSeconds(clock.millis, clock.elapsedRealtimeMillis) <= 0L
            ) {
                stopTicker()
                val completed = focusStateMachine.complete(activeSession)
                enforcement.restoreQuietMode()
                sessionRepository.save(completed)
                recordStudyEvent(completed)
                summary = completed
                activeSession = null
            }

            val summaryInterruptions = summary?.let { sessionRepository.getInterruptions(it.id) }
                ?: emptyList()

            val enforcementState = enforcement.state(activeSession?.state == SessionState.ACTIVE)
            val enforcementDescription = EnforcementResolver.describe(enforcementState)
            val leftAppCount = enforcement.leftAppCount()

            val zone = semesterZone(semester)
            val today = clock.instant.atZone(zone).toLocalDate()
            val todayState = when {
                semester == null -> TodayState.NeedsSemester
                activeSession != null -> TodayState.Active(activeSession)
                else -> buildTodayState(
                    semester, moduleViews, availability, today, learningEvents,
                    exams.map { it.exam }, calendarExceptions
                )
            }

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    busy = false,
                    semester = semester,
                    modules = moduleViews,
                    availability = availability,
                    exams = exams,
                    history = history,
                    backups = backups,
                    learningEvents = learningEvents,
                    pendingTopicMatches = pendingTopicMatches,
                    activeSession = activeSession,
                    today = todayState,
                    todayDate = today,
                    timezone = zone.id,
                    timestampMillis = clock.millis,
                    monotonicMillis = clock.elapsedRealtimeMillis,
                    lastSummary = summary,
                    summaryInterruptions = summaryInterruptions,
                    enforcementState = enforcementState,
                    enforcementDescription = enforcementDescription,
                    leftAppCount = leftAppCount
                )
            }

            if (activeSession != null && activeSession.state == SessionState.ACTIVE) {
                startTicker()
            } else {
                stopTicker()
            }
            } // end of concurrent data load
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            android.util.Log.e("StudyLock", "Could not load local data", exception)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    busy = false,
                    message = "Could not load your local data."
                )
            }
        }
    }

    private suspend fun buildTodayState(
        semester: Semester,
        modules: List<ModuleWithTopics>,
        availability: List<StudyAvailability>,
        today: LocalDate,
        learningEvents: List<LearningEvent>,
        exams: List<Exam> = emptyList(),
        calendarExceptions: List<CalendarException> = emptyList()
    ): TodayState {
        if (modules.isEmpty()) return TodayState.NeedsModules
        if (modules.all { it.topics.isEmpty() }) return TodayState.NeedsTopics

        val startDate = semester.startDate
        if (startDate != null && today.isBefore(startDate)) {
            return TodayState.Unavailable(
                "Your plan starts on ${startDate.format(PLAN_DATE_FORMAT)}.",
                null
            )
        }

        val effectiveAvailability = availability.ifEmpty { defaultAvailability(semester.id) }
        val result = scheduler.generateSchedule(
            ScheduleRequest(
                semester = semester,
                modules = modules.map { it.module },
                topics = modules.flatMap { it.topics },
                availability = effectiveAvailability,
                calendarExceptions = calendarExceptions,
                clock = clock,
                startDate = today,
                endDate = today.plusDays(PLAN_HORIZON_DAYS),
                learningEvents = learningEvents,
                exams = exams
            )
        )
        val assignments = result.assignments
        val next = assignments
            .firstOrNull { LocalDate.parse(it.date).isAfter(today) }
            ?.toNextAssignment(modules)

        val dayOfWeek = today.dayOfWeek
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return TodayState.RestDay(next)
        }

        val availableToday = effectiveAvailability
            .firstOrNull { it.dayOfWeek == dayOfWeek.value }
            ?.available == true
        if (!availableToday) {
            return TodayState.Unavailable("No session is scheduled today.", next)
        }

        val assignment = assignments.firstOrNull { it.date == today.toString() }
            ?: return TodayState.Unavailable("No study assignment for today.", next)
        val module = modules.firstOrNull { it.module.id == assignment.moduleId }?.module
        val topic = assignment.topicId?.let { topicId ->
            modules.flatMap { it.topics }.firstOrNull { it.id == topicId }
        }
        if (module == null || topic == null) {
            return TodayState.Unavailable("The plan needs a module and topic.", next)
        }
        return TodayState.Planned(
            plan = TodayPlan(
                date = today,
                module = module,
                topic = topic,
                requiredMinutes = assignment.durationMinutes,
                explanation = assignment.explanation
            ),
            next = next
        )
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                val session = _uiState.value.activeSession ?: break
                if (session.state != SessionState.ACTIVE) break
                val now = clock.millis
                val monotonicNow = clock.elapsedRealtimeMillis
                if (session.remainingSeconds(now, monotonicNow) <= 0L) {
                    val completed = focusStateMachine.complete(session)
                    enforcement.restoreQuietMode()
                    sessionRepository.save(completed)
                    recordStudyEvent(completed)
                    _uiState.update {
                        it.copy(
                            activeSession = null,
                            lastSummary = completed,
                            summaryInterruptions = sessionRepository.getInterruptions(completed.id),
                            timestampMillis = now,
                            monotonicMillis = monotonicNow
                        )
                    }
                    ticker = null
                    refreshInternal()
                    break
                }
                // Localised tick: no whole-app state update, no re-scheduling,
                // no database work. Only the countdown composable recomposes.
                _clockTick.value = now to monotonicNow
            }
            ticker = null
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
        _clockTick.value = 0L to 0L
    }

    private fun semanticAvailability(
        semester: Semester?,
        stored: List<StudyAvailability>
    ): List<StudyAvailability> {
        if (semester == null) return emptyList()
        if (stored.isNotEmpty()) return stored
        return defaultAvailability(semester.id)
    }

    private fun defaultAvailability(semesterId: UUID): List<StudyAvailability> =
        (1..7).map { day ->
            StudyAvailability(
                semesterId = semesterId,
                dayOfWeek = day,
                available = day <= 5,
                commitmentMinutes = NORMAL_SESSION_MINUTES
            )
        }

    private fun semesterZone(semester: Semester?): ZoneId =
        semester?.timezone
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: clock.zoneId

    private fun ScheduledAssignment.toNextAssignment(
        modules: List<ModuleWithTopics>
    ): NextAssignment? {
        val module = modules.firstOrNull { it.module.id == moduleId }?.module ?: return null
        val topic = topicId?.let { topicId ->
            modules.flatMap { it.topics }.firstOrNull { it.id == topicId }
        } ?: return null
        return NextAssignment(
            date = LocalDate.parse(date),
            module = module,
            topic = topic
        )
    }

    private fun section(
        examId: UUID,
        name: String,
        offered: Int,
        compulsory: Int,
        toAnswer: Int,
        marks: Int
    ) = ExamSection(
        examId = examId,
        name = name,
        questionsOffered = offered,
        questionsCompulsory = compulsory,
        questionsToAnswer = toAnswer,
        marksPerQuestion = marks
    )

    private fun setMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private companion object {
        const val NORMAL_SESSION_MINUTES = 120
        const val PLAN_HORIZON_DAYS = 21L
        val PLAN_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    }
}
