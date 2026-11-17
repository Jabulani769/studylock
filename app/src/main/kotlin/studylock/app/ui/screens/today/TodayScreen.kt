package studylock.app.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import studylock.app.R
import java.time.format.DateTimeFormatter
import java.util.Locale
import studylock.app.AppUiState
import studylock.app.NextAssignment
import studylock.app.TodayState
import studylock.app.asClockString
import studylock.app.remainingSeconds
import studylock.app.ui.components.DetailRow
import studylock.app.ui.components.EmptyState
import studylock.app.ui.components.HeroCard
import studylock.app.ui.components.PrimaryActionButton
import studylock.app.ui.components.SectionHeading
import studylock.app.ui.components.StatusChip
import studylock.app.ui.components.StudyLockCard
import studylock.app.ui.theme.Spacing
import studylock.focus.FocusSession
import studylock.focus.SessionState

@Composable
fun TodayScreen(
    state: AppUiState,
    onStartSession: () -> Unit,
    onResumeSession: () -> Unit,
    onOpenSetup: () -> Unit,
    onOpenAcademics: () -> Unit,
    onOpenSummary: () -> Unit,
    clockTick: StateFlow<Pair<Long, Long>>
) {
    // Only this screen reads the ticking clock; the rest of the app stays put.
    val tick by clockTick.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
    ) {
        val dateText = state.todayDate?.format(
            DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SL",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(
                    text = "Hello,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ready to study?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        if (dateText != null) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(Spacing.md))
        }

        HeroCard(
            title = "One clear plan\nfor today",
            badge = "120 min a day",
            containerColor = colorResource(R.color.sl_color_hero_blue),
            accentColor = colorResource(R.color.sl_color_hero_amber)
        ) {
            Text(
                text = "Your next session is chosen for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
        Spacer(Modifier.height(Spacing.xl))

        if (state.lastSummary != null) {
            SessionRecordedCard(
                session = state.lastSummary,
                onOpenSummary = onOpenSummary
            )
            Spacer(Modifier.height(Spacing.lg))
        }

        when (val today = state.today) {
            TodayState.NeedsSemester -> EmptyState(
                title = "Start with your semester",
                body = "Add the academic year and semester label once. You can add modules, outlines and dates as you receive them.",
                actionLabel = "Set up my semester",
                onAction = onOpenSetup
            )

            TodayState.NeedsModules -> EmptyState(
                title = "Add your first module",
                body = "StudyLock needs at least one registered module before it can choose what to study.",
                actionLabel = "Open Academics",
                onAction = onOpenAcademics
            )

            TodayState.NeedsTopics -> EmptyState(
                title = "Add topics to a module",
                body = "Add topics in lecturer order. Outlines can arrive later; the plan improves as you add information.",
                actionLabel = "Open Academics",
                onAction = onOpenAcademics
            )

            is TodayState.RestDay -> {
                RestDayCard(next = today.next)
            }

            is TodayState.Unavailable -> {
                StudyLockCard {
                    Text(
                        text = "Nothing scheduled today",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = today.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (today.next != null) {
                    Spacer(Modifier.height(Spacing.lg))
                    NextSessionCard(today.next)
                }
            }

            is TodayState.Planned -> {
                NextUpCard(
                    moduleName = today.plan.module.code ?: today.plan.module.name,
                    moduleFullName = today.plan.module.name,
                    topicTitle = today.plan.topic.title,
                    requiredMinutes = today.plan.requiredMinutes,
                    onStartSession = onStartSession
                )
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = "Why this topic?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "StudyLock chooses automatically from your current syllabus. It weighs topic difficulty and how long each topic has gone without study. You do not need to choose a topic.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (today.next != null) {
                    Spacer(Modifier.height(Spacing.lg))
                    NextSessionCard(today.next)
                }
            }

            is TodayState.Active -> {
                ActiveSessionCard(
                    session = today.session,
                    moduleName = state.module(today.session.moduleId)?.let { it.code ?: it.name }
                        ?: "Study session",
                    topicTitle = state.topic(today.session.moduleId, today.session.topicId)?.title,
                    nowMillis = if (tick.first == 0L) state.timestampMillis else tick.first,
                    monotonicMillis = if (tick.second == 0L) state.monotonicMillis else tick.second,
                    onResumeSession = onResumeSession
                )
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
        DataReadinessCard(state)
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun NextUpCard(
    moduleName: String,
    moduleFullName: String,
    topicTitle: String,
    requiredMinutes: Int,
    onStartSession: () -> Unit
) {
    SectionHeading(
        title = "My session",
        supporting = "Chosen automatically from your syllabus.",
        actionLabel = "See all",
        onAction = null
    )
    Spacer(Modifier.height(Spacing.md))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.sl_color_accent_primary)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colorResource(R.color.sl_color_media)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(Spacing.md))
                Column {
                    Text(
                        text = moduleName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = moduleFullName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            LinearProgressIndicator(
                progress = { 0.08f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.35f)
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = topicTitle,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = "$requiredMinutes min · one continuous session",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(Spacing.md))
            Button(
                onClick = onStartSession,
                modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF6C4CE0)
                )
            ) {
                Text("Start $requiredMinutes-minute session")
            }
        }
    }
}

@Composable
private fun ActiveSessionCard(
    session: FocusSession,
    moduleName: String,
    topicTitle: String?,
    nowMillis: Long,
    monotonicMillis: Long,
    onResumeSession: () -> Unit
) {
    val remaining = session.remainingSeconds(nowMillis, monotonicMillis)
    val progress = if (session.targetMinutes > 0) {
        (elapsedMinutes(session, nowMillis, monotonicMillis).toFloat() / session.targetMinutes)
            .coerceIn(0f, 1f)
    } else {
        0f
    }
    StudyLockCard {
        StatusChip(
            text = if (session.state == SessionState.PAUSED) "Paused" else "Session in progress",
            emphasized = true
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = topicTitle ?: "Continue your session",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = moduleName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = remaining.asClockString(),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "remaining",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.md))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface
        )
        Spacer(Modifier.height(Spacing.lg))
        PrimaryActionButton(
            text = if (session.state == SessionState.PAUSED) "Resume session" else "Return to session",
            onClick = onResumeSession
        )
    }
}

@Composable
private fun RestDayCard(next: NextAssignment?) {
    StudyLockCard {
        Text(
            text = "Rest day",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Nothing is required today. Weekends are part of the plan, not a gap in it.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (next != null) {
        Spacer(Modifier.height(Spacing.lg))
        NextSessionCard(next)
    }
}

@Composable
private fun NextSessionCard(next: NextAssignment) {
    StudyLockCard {
        Text(
            text = "Next session",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = next.topic.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "${next.module.code ?: next.module.name} · ${
                next.date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
            }",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionRecordedCard(
    session: FocusSession,
    onOpenSummary: () -> Unit
) {
    StudyLockCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Session recorded",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${session.elapsedActiveMinutes} min · ${summaryStateLabel(session.state)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onOpenSummary) { Text("View") }
        }
    }
}

@Composable
private fun DataReadinessCard(state: AppUiState) {
    val topicCount = state.modules.sumOf { it.topics.size }
    SectionHeading(
        title = "Your data",
        supporting = "StudyLock only plans from what you add. It does not invent topics or dates."
    )
    Spacer(Modifier.height(Spacing.md))
    StudyLockCard {
        DetailRow(
            label = "Semester",
            value = state.semester?.let { "${it.academicYear} · ${it.label}" } ?: "Not set"
        )
        DetailRow(label = "Modules", value = state.modules.size.toString())
        DetailRow(label = "Topics", value = topicCount.toString())
        DetailRow(label = "Exams", value = state.exams.size.toString())
        DetailRow(
            label = "Study days",
            value = state.availability
                .filter { it.available }
                .joinToString(", ") { dayShortName(it.dayOfWeek) }
                .ifEmpty { "Weekdays (default)" }
        )
    }
}

private fun elapsedMinutes(
    session: FocusSession,
    nowMillis: Long,
    monotonicMillis: Long
): Long =
    session.remainingSeconds(nowMillis, monotonicMillis).let { remaining ->
        (session.targetMinutes * 60L - remaining) / 60L
    }

private fun summaryStateLabel(state: SessionState): String = when (state) {
    SessionState.COMPLETED -> "Completed"
    SessionState.EARLY_EXIT -> "Ended early"
    SessionState.INTERRUPTED -> "Interrupted"
    SessionState.PAUSED -> "Paused"
    SessionState.ACTIVE -> "In progress"
    SessionState.IDLE -> "Not started"
}

private fun dayShortName(day: Int): String =
    java.time.DayOfWeek.of(day)
        .getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
