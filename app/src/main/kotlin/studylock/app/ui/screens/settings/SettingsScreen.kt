package studylock.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import studylock.app.AppUiState
import studylock.app.R
import studylock.academic.engine.RedesignOption
import studylock.app.BuildConfig
import studylock.app.profile.ProfilePhotoStore
import studylock.app.settings.PlanningMode
import studylock.app.settings.RedesignChoice
import studylock.app.profile.UserProfile
import studylock.app.settings.ThemePreference
import studylock.app.ui.components.PrimaryActionButton
import studylock.app.ui.components.ReminderTimeSelector
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
    onAcknowledgeInterruption: () -> Unit,
    onSetThemePreference: (ThemePreference) -> Unit,
    onSetReminderEnabled: (Boolean) -> Unit,
    onSetReminderStartMinutes: (Int) -> Unit,
    onSetPlanningMode: (PlanningMode) -> Unit,
    onSetRedesignChoice: (RedesignChoice) -> Unit,
    onSetProfileName: (String) -> Unit,
    onPickProfilePhoto: (Uri) -> Unit,
    onClearProfilePhoto: () -> Unit,
    onOpenDocuments: () -> Unit,
    photoStore: ProfilePhotoStore
) {
    var editingProfile by remember { mutableStateOf(false) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> pendingPhotoUri = uri }
    var nameText by remember(state.profile.displayName, editingProfile) {
        mutableStateOf(state.profile.displayName)
    }
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
    ) {
        SectionHeading(
            title = "Your profile",
            supporting = "Stored only on this phone. No account and no sync."
        )
        Spacer(Modifier.height(Spacing.md))
        StudyLockCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(
                    profile = state.profile,
                    photoStore = photoStore,
                    onPickPhoto = { photoPicker.launch("image/*") }
                )
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.profile.displayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your initial comes from this name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!editingProfile) {
                    IconButton(onClick = { editingProfile = true }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (editingProfile) {
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (pendingPhotoUri != null || state.profile.photoFileName != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = if (pendingPhotoUri != null) {
                            "New photo selected. It will be saved with the name."
                        } else {
                            "A photo is set. Tap the camera on the image to replace it."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                PrimaryActionButton(
                    text = "Save profile",
                    onClick = {
                        onSetProfileName(nameText)
                        pendingPhotoUri?.let { onPickProfilePhoto(it) }
                        pendingPhotoUri = null
                        editingProfile = false
                    }
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TextButton(onClick = {
                        pendingPhotoUri = null
                        editingProfile = false
                    }) {
                        Text("Cancel")
                    }
                    if (state.profile.photoFileName != null) {
                        IconButton(onClick = {
                            onClearProfilePhoto()
                            pendingPhotoUri = null
                            editingProfile = false
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_remove),
                                contentDescription = "Remove photo",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "The photo is copied into StudyLock's private storage. Nothing is uploaded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Spacing.xxl))
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
            title = "Appearance",
            supporting = "Choose how StudyLock looks on this phone."
        )
        Spacer(Modifier.height(Spacing.md))
        StudyLockCard {
            ThemePreference.entries.forEach { preference ->
                val (label, detail) = when (preference) {
                    ThemePreference.SYSTEM ->
                        "System default" to "Follows your phone's light or dark setting."
                    ThemePreference.LIGHT ->
                        "Light" to "Always use the light theme in StudyLock."
                    ThemePreference.DARK ->
                        "Dark" to "Always use the dark theme in StudyLock."
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetThemePreference(preference) }
                        .padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.themePreference == preference,
                        onClick = { onSetThemePreference(preference) }
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
        SectionHeading(
            title = "Reminders",
            supporting = "A gentle weekday nudge. Weekends stay free."
        )
        Spacer(Modifier.height(Spacing.md))
        StudyLockCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Study reminder",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = state.reminderEnabled,
                    onCheckedChange = onSetReminderEnabled
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "A gentle nudge on study weekdays. Reminders are inexact and may arrive a few minutes late.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.md))
            ReminderTimeSelector(
                minutes = state.reminderStartMinutes,
                onMinutesChange = onSetReminderStartMinutes
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Spacer(Modifier.height(Spacing.xs))
                TextButton(
                    onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                ) {
                    Text("Allow notifications")
                }
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
        SectionHeading(
            title = "Study plan",
            supporting = "You can switch approach at any time."
        )
        Spacer(Modifier.height(Spacing.md))
        StudyLockCard {
            listOf(
                PlanningMode.RECOMMENDED to (
                    "Follow the recommended plan" to
                        "StudyLock balances your modules toward midsem and endsem goals."
                    ),
                PlanningMode.ROUTINE to (
                    "Stick to my routine" to
                        "One automatic session a day, without an exam-driven timetable."
                    )
            ).forEach { (mode, labels) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetPlanningMode(mode) }
                        .padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.planningMode == mode,
                        onClick = { onSetPlanningMode(mode) }
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = labels.first,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = labels.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val proposal = state.redesignProposal
            if (state.planningMode == PlanningMode.RECOMMENDED &&
                proposal != null && proposal.needed
            ) {
                Spacer(Modifier.height(Spacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "This plan needs more time",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = proposal.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "About ${proposal.requiredSessions} session(s) needed, " +
                        "${proposal.availableSessions} available. Estimated topic times are " +
                        "defaults, not official requirements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (proposal.goalAchievable && proposal.recommendedOption != RedesignOption.NONE) {
                    Spacer(Modifier.height(Spacing.md))
                    PrimaryActionButton(
                        text = "Apply recommended redesign",
                        onClick = {
                            onSetRedesignChoice(RedesignChoice.valueOf(proposal.recommendedOption.name))
                        }
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "Only applies if you choose it. You can switch back any time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "No automatic option closes this gap. StudyLock will not claim " +
                            "the goal is met. Consider discussing the workload with your lecturer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (state.redesignChoice != RedesignChoice.NONE) {
                    Spacer(Modifier.height(Spacing.sm))
                    SecondaryActionButton(
                        text = "Return to the standard plan",
                        onClick = { onSetRedesignChoice(RedesignChoice.NONE) }
                    )
                }
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
        SectionHeading(
            title = "Study materials",
            supporting = "Read your documents inside StudyLock."
        )
        Spacer(Modifier.height(Spacing.md))
        StudyLockCard(modifier = Modifier.clickable(onClick = onOpenDocuments)) {
            SecondaryActionButton(
                text = "Open study materials",
                onClick = onOpenDocuments
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "PDFs, images and plain text stay on this phone in app-private storage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                text = "Appearance: ${state.themePreference.name.lowercase()}. Device time zone: ${state.timezone}",
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

/** Avatar with a small camera affordance for changing the local photo. */
@Composable
private fun ProfileAvatar(
    profile: UserProfile,
    photoStore: ProfilePhotoStore,
    onPickPhoto: () -> Unit
) {
    val bitmap = remember(profile.photoFileName) {
        photoStore.decode(profile.photoFileName)?.asImageBitmap()
    }
    Box(modifier = Modifier.size(64.dp)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(
                    text = profile.initial,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.BottomCenter)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onPickPhoto),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = "Change profile photo",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
