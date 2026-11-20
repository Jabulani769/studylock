package studylock.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import studylock.app.NewModuleDraft
import studylock.app.ui.components.PrimaryActionButton
import studylock.app.ui.components.ReminderTimeSelector
import studylock.app.ui.components.SecondaryActionButton
import studylock.app.ui.components.StudyLockCard
import studylock.app.reminders.ReminderTime
import studylock.app.ui.theme.Spacing
import studylock.domain.Semester

private const val STEP_COUNT = 4

@Composable
fun OnboardingScreen(
    existingSemester: Semester?,
    initialAvailableDays: Set<Int>,
    busy: Boolean,
    onFinish: (academicYear: String, label: String, modules: List<NewModuleDraft>, availableDays: Set<Int>, reminderStartMinutes: Int) -> Unit,
    onSkip: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var academicYear by rememberSaveable {
        mutableStateOf(existingSemester?.academicYear ?: LocalDate.now().year.toString())
    }
    var semesterLabel by rememberSaveable {
        mutableStateOf(existingSemester?.label.orEmpty())
    }
    val draftSaver = listSaver<List<NewModuleDraft>, String>(
        save = { list -> list.flatMap { listOf(it.name, it.code.orEmpty()) } },
        restore = { saved ->
            saved.chunked(2).map { values ->
                NewModuleDraft(values[0], values.getOrNull(1)?.ifBlank { null })
            }
        }
    )
    var moduleDrafts by rememberSaveable(stateSaver = draftSaver) {
        mutableStateOf(emptyList<NewModuleDraft>())
    }
    var moduleName by rememberSaveable { mutableStateOf("") }
    var moduleCode by rememberSaveable { mutableStateOf("") }
    var newModuleError by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDaysSaver = listSaver<Set<Int>, Int>(
        save = { it.toList() },
        restore = { it.toSet() }
    )
    var selectedDays by rememberSaveable(stateSaver = selectedDaysSaver) {
        mutableStateOf(initialAvailableDays.ifEmpty { (1..5).toSet() })
    }
    var reminderStartMinutes by rememberSaveable { mutableIntStateOf(18 * 60) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl)
        ) {
            if (onCancel != null) {
                TextButton(onClick = onCancel) { Text("Close") }
            }
            Text(
                text = "StudyLock",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Step ${step + 1} of $STEP_COUNT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.sm))
            LinearProgressIndicator(
                progress = { (step + 1).toFloat() / STEP_COUNT },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(Spacing.xxl))

            when (step) {
                0 -> WelcomeStep()
                1 -> SemesterStep(
                    academicYear = academicYear,
                    onAcademicYearChange = { academicYear = it },
                    semesterLabel = semesterLabel,
                    onSemesterLabelChange = { semesterLabel = it }
                )
                2 -> ModulesStep(
                    moduleDrafts = moduleDrafts,
                    moduleName = moduleName,
                    onModuleNameChange = { moduleName = it },
                    moduleCode = moduleCode,
                    onModuleCodeChange = { moduleCode = it },
                    error = newModuleError,
                    onAdd = {
                        val name = moduleName.trim()
                        when {
                            name.isEmpty() -> newModuleError = "Enter a module name."
                            moduleDrafts.any { it.name.equals(name, ignoreCase = true) } ->
                                newModuleError = "That module is already in the list."
                            else -> {
                                moduleDrafts = moduleDrafts + NewModuleDraft(
                                    name,
                                    moduleCode.trim().ifBlank { null }
                                )
                                moduleName = ""
                                moduleCode = ""
                                newModuleError = null
                            }
                        }
                    },
                    onRemove = { draft -> moduleDrafts = moduleDrafts - draft }
                )
                else -> AvailabilityStep(
                    selectedDays = selectedDays,
                    onToggle = { day, selected ->
                        if (day <= 5) {
                            selectedDays = if (selected) selectedDays + day else selectedDays - day
                        }
                    },
                    reminderStartMinutes = reminderStartMinutes,
                    onReminderStartMinutesChange = { reminderStartMinutes = it }
                )
            }

            Spacer(Modifier.height(Spacing.xxl))

            when (step) {
                0 -> {
                    PrimaryActionButton(
                        text = "Set up my plan",
                        onClick = { step = 1 }
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    SecondaryActionButton(
                        text = "Look around first",
                        onClick = onSkip
                    )
                }
                1 -> {
                    PrimaryActionButton(
                        text = "Continue",
                        enabled = academicYear.isNotBlank() && semesterLabel.isNotBlank(),
                        onClick = { step = 2 }
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    TextButton(onClick = { step = 0 }) { Text("Back") }
                }
                2 -> {
                    PrimaryActionButton(
                        text = if (moduleDrafts.isEmpty()) "Skip for now" else "Continue",
                        onClick = { step = 3 }
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    TextButton(onClick = { step = 1 }) { Text("Back") }
                }
                else -> {
                    PrimaryActionButton(
                        text = if (busy) "Saving..." else "Finish setup",
                        enabled = !busy && selectedDays.isNotEmpty(),
                        onClick = {
                            onFinish(
                                academicYear,
                                semesterLabel,
                                moduleDrafts.toList(),
                                selectedDays.toSet(),
                                reminderStartMinutes
                            )
                        }
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    TextButton(onClick = { step = 2 }, enabled = !busy) { Text("Back") }
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun WelcomeStep() {
    Text(
        text = "One clear plan for the day.",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(Spacing.lg))
    Text(
        text = "StudyLock plans one continuous 120-minute study session on each study weekday. Saturday and Sunday are rest days.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.lg))
    Text(
        text = "It starts with what you know. Add modules now, then outlines and exam dates whenever you receive them.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.xl))
    StudyLockCard {
        Text(
            text = "What this app can and cannot do",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "This build keeps your data on this phone and does not use accounts, analytics or a server. With your permission it notes which app is in the foreground and can silence notifications during a session. It does not block apps or guarantee that every distraction is stopped, and it makes no universal enforcement claim.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SemesterStep(
    academicYear: String,
    onAcademicYearChange: (String) -> Unit,
    semesterLabel: String,
    onSemesterLabelChange: (String) -> Unit
) {
    Text(
        text = "Which semester is this?",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(Spacing.lg))
    OutlinedTextField(
        value = academicYear,
        onValueChange = onAcademicYearChange,
        label = { Text("Academic year") },
        placeholder = { Text("e.g. 2026") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(Spacing.md))
    OutlinedTextField(
        value = semesterLabel,
        onValueChange = onSemesterLabelChange,
        label = { Text("Semester label") },
        placeholder = { Text("e.g. Semester 2") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(Spacing.lg))
    Text(
        text = "An official start date is not required. Your plan starts today if no start date is known. You can correct the setup later.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ModulesStep(
    moduleDrafts: List<NewModuleDraft>,
    moduleName: String,
    onModuleNameChange: (String) -> Unit,
    moduleCode: String,
    onModuleCodeChange: (String) -> Unit,
    error: String?,
    onAdd: () -> Unit,
    onRemove: (NewModuleDraft) -> Unit
) {
    Text(
        text = "Add the modules you know.",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(Spacing.sm))
    Text(
        text = "Only add modules that are registered. Topics and outlines can arrive later.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.lg))
    OutlinedTextField(
        value = moduleName,
        onValueChange = onModuleNameChange,
        label = { Text("Module name") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(Spacing.md))
    OutlinedTextField(
        value = moduleCode,
        onValueChange = onModuleCodeChange,
        label = { Text("Module code (optional)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
    if (error != null) {
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    Spacer(Modifier.height(Spacing.md))
    SecondaryActionButton(text = "Add module", onClick = onAdd)

    if (moduleDrafts.isNotEmpty()) {
        Spacer(Modifier.height(Spacing.lg))
        StudyLockCard {
            moduleDrafts.forEachIndexed { index, draft ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = draft.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (draft.code != null) {
                            Text(
                                text = draft.code,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onRemove(draft) }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove ${draft.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (index != moduleDrafts.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                }
            }
        }
    }
}

@Composable
private fun AvailabilityStep(
    selectedDays: Set<Int>,
    onToggle: (day: Int, selected: Boolean) -> Unit,
    reminderStartMinutes: Int,
    onReminderStartMinutesChange: (Int) -> Unit
) {
    Text(
        text = "When can you usually study?",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(Spacing.sm))
    Text(
        text = "Pick the weekdays that normally work. Each study day gets one 120-minute session. Saturday and Sunday are rest days.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.lg))
    StudyLockCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        (1..7).forEach { day ->
            val isWeekend = day > 5
            val selected = day in selectedDays
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = selected,
                        enabled = !isWeekend,
                        onValueChange = { onToggle(day, it) }
                    )
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = dayName(day),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isWeekend) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = if (isWeekend) "Rest day" else "120-minute session",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = if (isWeekend) false else selected,
                    onCheckedChange = { onToggle(day, it) },
                    enabled = !isWeekend
                )
            }
        }
    }
    Spacer(Modifier.height(Spacing.lg))
    Text(
        text = "When should we remind you?",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = "A gentle weekday reminder at ${ReminderTime.format(reminderStartMinutes)}. " +
            "You can change or turn it off in Settings.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.md))
    ReminderTimeSelector(
        minutes = reminderStartMinutes,
        onMinutesChange = onReminderStartMinutesChange
    )
}

private fun dayName(day: Int): String =
    DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.getDefault())
