package studylock.app.ui.screens.exams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import studylock.app.AppUiState
import studylock.app.ExamWithSections
import studylock.app.StudyExamType
import studylock.app.ui.components.EmptyState
import studylock.app.ui.components.PrimaryActionButton
import studylock.app.ui.components.SectionHeading
import studylock.app.ui.components.StatusChip
import studylock.app.ui.components.StudyLockCard
import studylock.app.ui.theme.Spacing
import studylock.domain.DataStatus

@Composable
fun ExamsScreen(
    state: AppUiState,
    onAddExam: (moduleId: UUID, type: StudyExamType, date: LocalDate?, status: DataStatus) -> Unit,
    onOpenAcademics: () -> Unit,
    onDeleteExam: (UUID) -> Unit
) {
    var showAddExam by remember { mutableStateOf(false) }
    var examToDelete by remember { mutableStateOf<ExamWithSections?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
    ) {
        if (state.modules.isEmpty()) {
            EmptyState(
                title = "Add modules before exams",
                body = "An exam belongs to a registered module. Add at least one module first.",
                actionLabel = "Open Academics",
                onAction = onOpenAcademics
            )
        } else {

            StudyLockCard {
                Text(
                    text = "Exam information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Structures and dates are kept separately. A date is only used when you record it. StudyLock never invents an official date.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
    
            Spacer(Modifier.height(Spacing.xxl))
            SectionHeading(
                title = "Exams",
                supporting = "Midsem and endsem structures are stored per module."
            )
            Spacer(Modifier.height(Spacing.md))
    
            if (state.exams.isEmpty()) {
                EmptyState(
                    title = "No exam information yet",
                    body = "You can record the confirmed structure now, even if the date has not been released.",
                    actionLabel = "Add an exam",
                    onAction = { showAddExam = true }
                )
            } else {
                state.exams
                    .sortedBy { it.exam.dateTime ?: java.time.LocalDateTime.MAX }
                    .forEach { exam ->
                        ExamCard(
                            exam = exam,
                            state = state,
                            onRemove = { examToDelete = exam }
                        )
                        Spacer(Modifier.height(Spacing.md))
                    }
                PrimaryActionButton(
                    text = "Add another exam",
                    onClick = { showAddExam = true }
                )
            }
    
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = "Accurate dates can change prioritisation later. An estimated date is labelled as estimated and is not treated as official.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xl))
        }
    }

    if (showAddExam) {
        AddExamDialog(
            state = state,
            onDismiss = { showAddExam = false },
            onConfirm = { moduleId, type, date, status ->
                onAddExam(moduleId, type, date, status)
                showAddExam = false
            }
        )
    }
    if (examToDelete != null) {
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            title = { Text("Remove this exam?") },
            text = { Text("The stored exam structure and date will be removed from your plan.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExam(examToDelete!!.exam.id)
                    examToDelete = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { examToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ExamCard(
    exam: ExamWithSections,
    state: AppUiState,
    onRemove: () -> Unit
) {
    val module = state.module(exam.exam.moduleId)
    val type = StudyExamType.entries.firstOrNull { it.stored == exam.exam.examType }
    StudyLockCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type?.label ?: exam.exam.examType,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = module?.let { it.code ?: it.name } ?: "Module removed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(text = dateStatusLabel(exam.exam.dateStatus))
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = examDateLabel(exam),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (exam.exam.dateTime != null) {
            Text(
                text = "Date only. Exam time is not recorded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = state.examStructureLabel(exam.exam),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onRemove) {
            Text("Remove exam", color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddExamDialog(
    state: AppUiState,
    onDismiss: () -> Unit,
    onConfirm: (UUID, StudyExamType, LocalDate?, DataStatus) -> Unit
) {
    var selectedModuleId by remember { mutableStateOf(state.modules.firstOrNull()?.module?.id) }
    var selectedType by remember { mutableStateOf(StudyExamType.ENDSEM) }
    var dateKnown by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(DataStatus.ESTIMATED) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add exam") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "Module",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    state.modules.forEach { moduleWithTopics ->
                        FilterChip(
                            selected = selectedModuleId == moduleWithTopics.module.id,
                            onClick = { selectedModuleId = moduleWithTopics.module.id },
                            label = { Text(moduleWithTopics.module.code ?: moduleWithTopics.module.name) }
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Exam",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    StudyExamType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label) }
                        )
                    }
                }
                Text(
                    text = when (selectedType) {
                        StudyExamType.MIDSEM ->
                            "3 compulsory questions · 20 + 20 + 10 · 50 marks"
                        StudyExamType.ENDSEM ->
                            "Section A: 2 compulsory × 20 · Section B: choose 3 of 5 × 20 · 100 marks"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "I know the date",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Leave this off while the timetable is unknown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dateKnown,
                        onCheckedChange = {
                            dateKnown = it
                            if (it) showDatePicker = true
                        }
                    )
                }

                if (dateKnown) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(
                            selectedDateMillis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                                    .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
                            } ?: "Choose a date"
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        FilterChip(
                            selected = selectedStatus == DataStatus.ESTIMATED,
                            onClick = { selectedStatus = DataStatus.ESTIMATED },
                            label = { Text("Estimated") }
                        )
                        FilterChip(
                            selected = selectedStatus == DataStatus.VERIFIED,
                            onClick = { selectedStatus = DataStatus.VERIFIED },
                            label = { Text("Verified") }
                        )
                    }
                }

                if (error != null) {
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val moduleId = selectedModuleId
                    if (moduleId == null) {
                        error = "Choose a module."
                    } else if (dateKnown && selectedDateMillis == null) {
                        error = "Choose the date or turn the date switch off."
                    } else {
                        val date = selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        val status = if (date == null) DataStatus.NOT_PROVIDED else selectedStatus
                        onConfirm(moduleId, selectedType, date, status)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis = pickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) { Text("Use date") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun examDateLabel(exam: ExamWithSections): String {
    val dateTime = exam.exam.dateTime
    return if (dateTime == null) {
        "Date not provided"
    } else {
        dateTime.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
    }
}

private fun dateStatusLabel(status: DataStatus): String = when (status) {
    DataStatus.VERIFIED -> "Verified date"
    DataStatus.RECEIVED -> "Date received"
    DataStatus.ESTIMATED -> "Estimated date"
    DataStatus.CONFLICTING -> "Conflicting date"
    DataStatus.SUPERSEDED -> "Date superseded"
    DataStatus.NOT_PROVIDED -> "No date"
}
