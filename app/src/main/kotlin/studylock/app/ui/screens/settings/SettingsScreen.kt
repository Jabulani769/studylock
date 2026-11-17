package studylock.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import studylock.app.AppUiState
import studylock.app.BuildConfig
import studylock.app.ui.components.PrimaryActionButton
import studylock.app.ui.components.SecondaryActionButton
import studylock.app.ui.components.SectionHeading
import studylock.app.ui.components.StudyLockCard
import studylock.app.ui.theme.Spacing

@Composable
fun SettingsScreen(
    state: AppUiState,
    onSetAvailabilityDay: (dayOfWeek: Int, available: Boolean) -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: (String) -> Unit,
    onOpenSetup: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenNotificationPolicy: () -> Unit,
    onAcknowledgeInterruption: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
    ) {
        SectionHeading(
            title = "Study commitment",
            supporting = "One continuous 120-minute session on each study weekday."
        )
        Spacer(Modifier.height(Spacing.md))
        StudyLockCard {
            Text(
                text = "Weekdays",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Turn off a weekday when you cannot normally study. Saturday and Sunday are always rest days.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.md))
            val availability = state.availability
            (1..7).forEachIndexed { index, day ->
                val entry = availability.firstOrNull { it.dayOfWeek == day }
                val isWeekend = day > 5
                val available = entry?.available == true && !isWeekend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = available,
                            enabled = !isWeekend && state.semester != null,
                            onValueChange = { onSetAvailabilityDay(day, it) }
                        )
                        .padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isWeekend) "Rest" else if (available) "120 min" else "Off",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = available,
                        onCheckedChange = { onSetAvailabilityDay(day, it) },
                        enabled = !isWeekend && state.semester != null
                    )
                }
                if (index != 6) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                }
            }
            if (state.semester == null) {
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "Set up a semester to change these days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
        SectionHeading(
            title = "Privacy",
            supporting = "Local-first and offline."
        )
        Spacer(Modifier.height(Spacing.md))
        StudyLockCard {
            Text(
                text = "What StudyLock stores",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Your semester, modules, topics, exam information and session records are stored in this app's private database on this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "This build has no accounts, analytics, crash reporting, cloud sync or network permission. Nothing is sent off the device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Spacing.xxl))
        SectionHeading(
            title = "Focus support",
            supporting = "Monitoring and notification quieting with honest limits."
        )
        Spacer(Modifier.height(Spacing.md))
        StudyLockCard {
            Text(
                text = state.enforcementState.name.replace('_', ' ').lowercase()
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = state.enforcementDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "During a session StudyLock notes which app is in the foreground and can silence non-alarm notifications with your permission. It does not block apps, read messages or typed text, capture your screen, or guarantee that every distraction is stopped.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.md))
            SecondaryActionButton(
                text = "Usage access for monitoring",
                onClick = onOpenUsageAccess
            )
            Spacer(Modifier.height(Spacing.sm))
            SecondaryActionButton(
                text = "Notification quieting access",
                onClick = onOpenNotificationPolicy
            )
            if (state.enforcementState == studylock.focus.EnforcementState.ENFORCEMENT_INTERRUPTED) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "A control was revoked or interrupted. StudyLock is showing its reduced state, not a false locked state.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(Spacing.sm))
                SecondaryActionButton(
                    text = "Acknowledge and continue",
                    onClick = onAcknowledgeInterruption
                )
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
        SectionHeading(
            title = "Backup",
            supporting = "Protect your records."
        )
        Spacer(Modifier.height(Spacing.md))
        StudyLockCard {
            Text(
                text = "Local backups",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Backups are JSON files in this app's private storage. They never leave the phone. Clearing app storage also removes them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.md))
            SecondaryActionButton(text = "Create backup", onClick = onCreateBackup)
            if (state.backups.isEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "No backups yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.backups.forEach { name ->
                    TextButton(onClick = { onRestoreBackup(name) }) {
                        Text(text = "Restore ${name.removeSuffix(".json")}")
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
        SectionHeading(title = "Setup")
        Spacer(Modifier.height(Spacing.md))
        PrimaryActionButton(
            text = "Review semester and modules",
            onClick = onOpenSetup
        )

        Spacer(Modifier.height(Spacing.xxl))
        StudyLockCard {
            Text(
                text = "StudyLock ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Dark theme by default. Device time zone: ${state.timezone}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Academic planning is local and deterministic. No topics, dates or exam requirements are invented.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}
