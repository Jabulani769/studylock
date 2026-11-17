package studylock.app.ui.screens.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import studylock.app.AppUiState
import studylock.app.ui.components.DetailRow
import studylock.app.ui.components.EmptyState
import studylock.app.ui.components.SectionHeading
import studylock.app.ui.components.StatusChip
import studylock.app.ui.components.StudyLockCard
import studylock.app.ui.theme.Spacing
import studylock.focus.FocusSession
import studylock.focus.LearningEvidence
import studylock.focus.SessionState

@Composable
fun ProgressScreen(
    state: AppUiState,
    onOpenSummary: (FocusSession) -> Unit
) {
    val recorded = state.history
        .filter {
            it.state == SessionState.COMPLETED ||
                it.state == SessionState.EARLY_EXIT ||
                it.state == SessionState.INTERRUPTED
        }
        .sortedByDescending { it.endTime ?: it.lastUpdated }
    val completedCount = recorded.count { it.state == SessionState.COMPLETED }
    val totalMinutes = recorded.sumOf { it.elapsedActiveMinutes }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
    ) {
        SectionHeading(
            title = "Recorded sessions",
            supporting = "Time is evidence that a session happened. It is not proof of understanding."
        )
        Spacer(Modifier.height(Spacing.md))

        if (recorded.isEmpty()) {
            EmptyState(
                title = "No sessions recorded yet",
                body = "After your first focus session, its time and interruptions appear here. Nothing is counted as mastered from time alone."
            )
        } else {
            StudyLockCard {
                DetailRow(label = "Sessions finished", value = completedCount.toString())
                DetailRow(label = "Recorded study time", value = "$totalMinutes min")
                DetailRow(label = "All recorded sessions", value = recorded.size.toString())
            }
            Spacer(Modifier.height(Spacing.xl))

            recorded.forEachIndexed { index, session ->
                SessionHistoryRow(
                    session = session,
                    state = state,
                    onClick = { onOpenSummary(session) }
                )
                if (index != recorded.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.md),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
        StudyLockCard {
            Text(
                text = "Learning evidence",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Each session's interruptions and any learning status you chose are shown here. Studied, understood and mastered are never inferred from the timer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val assessments = state.learningEvents
            .filter { it.eventType == "ASSESSMENT" }
            .take(5)
        if (assessments.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xxl))
            SectionHeading(
                title = "Recall checks",
                supporting = "Your own results. Weak results raise revision priority later."
            )
            Spacer(Modifier.height(Spacing.md))
            StudyLockCard {
                assessments.forEach { event ->
                    val topicTitle = state.modules
                        .flatMap { it.topics }
                        .firstOrNull { it.id == event.topicId }
                        ?.title ?: "Topic"
                    DetailRow(label = topicTitle, value = event.evidence ?: "Not recorded")
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun SessionHistoryRow(
    session: FocusSession,
    state: AppUiState,
    onClick: () -> Unit
) {
    val module = state.module(session.moduleId)
    val topic = state.topic(session.moduleId, session.topicId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = topic?.title ?: "Session",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = module?.let { it.code ?: it.name } ?: "Module removed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = session.endTime?.let { instant ->
                    val zone = runCatching { ZoneId.of(state.timezone) }.getOrDefault(ZoneId.systemDefault())
                    instant.atZone(zone).format(
                        DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.getDefault())
                    )
                } ?: "Time not recorded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            StatusChip(text = stateLabel(session.state))
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = if (session.elapsedActiveSeconds > 0) {
                    "${session.elapsedActiveSeconds / 60} min"
                } else {
                    "${session.elapsedActiveMinutes} min"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (session.interruptionCount > 0) {
                Text(
                    text = "${session.interruptionCount} interruptions · ${
                        formatSeconds(session.interruptionSeconds)
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val evidence = session.learningEvidence
            if (evidence != null) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = evidence.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun stateLabel(state: SessionState): String = when (state) {
    SessionState.COMPLETED -> "Completed"
    SessionState.EARLY_EXIT -> "Ended early"
    SessionState.INTERRUPTED -> "Interrupted"
    SessionState.PAUSED -> "Paused"
    SessionState.ACTIVE -> "In progress"
    SessionState.IDLE -> "Not started"
}

private fun LearningEvidence.label(): String = when (this) {
    LearningEvidence.STUDIED -> "Studied"
    LearningEvidence.UNDERSTOOD -> "Understood"
    LearningEvidence.MASTERED -> "Mastered"
}

private fun formatSeconds(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return if (safe < 60) "${safe}s" else "${safe / 60}m ${safe % 60}s"
}
