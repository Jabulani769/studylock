package studylock.app.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import studylock.app.AppUiState
import studylock.app.TodayState
import studylock.app.ui.components.DetailRow
import studylock.app.ui.components.EmptyState
import studylock.app.ui.components.PrimaryActionButton
import studylock.app.ui.components.SecondaryActionButton
import studylock.app.ui.components.SectionHeading
import studylock.app.ui.components.StatusChip
import studylock.app.ui.components.StudyLockCard
import studylock.app.ui.theme.Spacing
import studylock.focus.LearningEvidence
import studylock.focus.SessionInterruption
import studylock.focus.SessionState

@Composable
fun SessionSummaryScreen(
    state: AppUiState,
    onSetLearningEvidence: (LearningEvidence?) -> Unit,
    onRecordAssessment: (correct: Int, total: Int) -> Unit,
    onDone: () -> Unit
) {
    var showRecall by remember { mutableStateOf(false) }
    var correctText by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    val session = state.lastSummary
    if (session == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl)
        ) {
            EmptyState(
                title = "No session to summarise",
                body = "Start a session from Today to record one."
            )
            Spacer(Modifier.height(Spacing.lg))
            PrimaryActionButton(text = "Back to Today", onClick = onDone)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl)
        ) {
            Text(
                text = if (session.state == SessionState.EARLY_EXIT) "Session ended early" else "Session recorded",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Your time has been saved. Time alone does not mark a topic as understood or mastered.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xl))

            val module = state.module(session.moduleId)
            val topic = state.topic(session.moduleId, session.topicId)
            StudyLockCard {
                StatusChip(
                    text = module?.code ?: module?.name ?: "Study session",
                    emphasized = true
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = topic?.title ?: "Session",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.lg))
                DetailRow(label = "Elapsed time", value = formatElapsed(session.elapsedActiveSeconds, session.elapsedActiveMinutes))
                DetailRow(
                    label = "Interruptions",
                    value = interruptionSummary(session.interruptionCount, session.interruptionSeconds)
                )
                DetailRow(
                    label = "Result",
                    value = if (session.state == SessionState.EARLY_EXIT) "Ended early" else "Completed"
                )
            }

            if (state.summaryInterruptions.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xl))
                SectionHeading(
                    title = "Interruption detail",
                    supporting = "Each pause is recorded separately. It is not counted twice."
                )
                Spacer(Modifier.height(Spacing.md))
                StudyLockCard {
                    state.summaryInterruptions.forEach { interruption ->
                        DetailRow(
                            label = interruptionLabel(interruption),
                            value = formatSeconds(interruption.durationSeconds)
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xxl))
            SectionHeading(
                title = "Learning status",
                supporting = "Recorded only when you choose it, never inferred from the timer."
            )
            Spacer(Modifier.height(Spacing.md))
            val recordedEvidence = session.learningEvidence
            StudyLockCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    LearningEvidence.entries.forEach { evidence ->
                        FilterChip(
                            selected = recordedEvidence == evidence,
                            onClick = {
                                onSetLearningEvidence(
                                    if (recordedEvidence == evidence) null else evidence
                                )
                            },
                            label = { Text(evidence.label()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = if (recordedEvidence == null) {
                        "Not recorded. Choose a status only if you have evidence such as notes, recall or practice. Tap a selected status again to clear it."
                    } else {
                        "Recorded by you: ${recordedEvidence.label()}. This is your explicit report, not a timer result."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val next = when (val today = state.today) {
                is TodayState.Planned -> today.next
                is TodayState.RestDay -> today.next
                is TodayState.Unavailable -> today.next
                else -> null
            }
            if (next != null) {
                Spacer(Modifier.height(Spacing.xxl))
                SectionHeading(title = "Next scheduled topic")
                Spacer(Modifier.height(Spacing.md))
                StudyLockCard {
                    Text(
                        text = next.topic.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${next.module.code ?: next.module.name} · ${
                            next.date.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM"))
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (session.topicId != null) {
                SecondaryActionButton(
                    text = "Record recall check",
                    onClick = { showRecall = true }
                )
                Spacer(Modifier.height(Spacing.sm))
            }
            PrimaryActionButton(text = "Back to Today", onClick = onDone)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Ending early is recorded neutrally. You can continue without a penalty.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xl))
        }
    }

    if (showRecall && session?.topicId != null) {
        val correct = correctText.toIntOrNull()
        val total = totalText.toIntOrNull()
        val valid = correct != null && total != null && total > 0 && correct in 0..total
        AlertDialog(
            onDismissRequest = { showRecall = false },
            title = { Text("Record recall check") },
            text = {
                Column {
                    Text(
                        text = "Enter how many questions you answered correctly from recall, without notes. This is your own result and is never inferred from time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.md))
                    OutlinedTextField(
                        value = correctText,
                        onValueChange = { correctText = it },
                        label = { Text("Correct") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = totalText,
                        onValueChange = { totalText = it },
                        label = { Text("Total questions") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        onRecordAssessment(correct!!, total!!)
                        correctText = ""
                        totalText = ""
                        showRecall = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRecall = false }) { Text("Cancel") }
            }
        )
    }
}

private fun LearningEvidence.label(): String = when (this) {
    LearningEvidence.STUDIED -> "Studied"
    LearningEvidence.UNDERSTOOD -> "Understood"
    LearningEvidence.MASTERED -> "Mastered"
}

private fun interruptionSummary(count: Int, seconds: Int): String {
    if (count == 0 && seconds == 0) return "None"
    return "$count · ${formatSeconds(seconds)}"
}

private fun interruptionLabel(interruption: SessionInterruption): String =
    when (interruption.reason) {
        "lost_focus" -> "Lost focus"
        "user_pause" -> "Paused"
        else -> interruption.reason.replace('_', ' ')
    }

private fun formatSeconds(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return if (safe < 60) {
        "${safe}s"
    } else {
        "${safe / 60}m ${safe % 60}s"
    }
}

private fun formatElapsed(seconds: Int, minutes: Int): String {
    val safe = if (seconds > 0) seconds else minutes.coerceAtLeast(0) * 60
    return if (safe < 60) "${safe}s" else "${safe / 60} min"
}
