package studylock.app.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import studylock.app.enforcement.EnforcementController
import studylock.app.ui.components.PrimaryActionButton
import studylock.app.ui.components.StudyLockCard
import studylock.app.ui.theme.Spacing

/**
 * Asked on the first launch, before academic setup. Every permission is
 * optional: declining only turns off the feature that needs it. Usage access
 * and Do Not Disturb are special Android accesses, so the only honest way to
 * grant them is to open the system settings screen from here.
 */
@Composable
fun FirstRunPermissionsScreen(
    enforcement: EnforcementController,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var notificationsGranted by remember { mutableStateOf(notificationsAllowed(context)) }
    var usageGranted by remember { mutableStateOf(enforcement.usageAccessGranted()) }
    var dndGranted by remember { mutableStateOf(enforcement.notificationPolicyAccessGranted()) }

    LifecycleResumeEffect(Unit) {
        notificationsGranted = notificationsAllowed(context)
        usageGranted = enforcement.usageAccessGranted()
        dndGranted = enforcement.notificationPolicyAccessGranted()
        onPauseOrDispose { }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl)
        ) {
            Text(
                text = "StudyLock",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Permissions, asked once",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "StudyLock asks only for access it actually uses. You can decline any of these and change them later in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.lg))
            StudyLockCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                PermissionRow(
                    title = "Notifications",
                    description = "Shows the daily study reminder and the session timer. Notification content is never read.",
                    granted = notificationsGranted,
                    actionLabel = if (notificationsGranted) null else "Allow",
                    onAction = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
                HorizontalDivider()
                PermissionRow(
                    title = "Usage access",
                    description = "Lets StudyLock see which app is in front during a session. It does not read app content.",
                    granted = usageGranted,
                    actionLabel = if (usageGranted) "Change" else "Open settings",
                    onAction = enforcement::openUsageAccessSettings
                )
                HorizontalDivider()
                PermissionRow(
                    title = "Do Not Disturb access",
                    description = "Lets StudyLock silence non-alarm notifications during a session. Your phone's settings are not changed outside a session.",
                    granted = dndGranted,
                    actionLabel = if (dndGranted) "Change" else "Open settings",
                    onAction = enforcement::openNotificationPolicySettings
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "Nothing is uploaded. If you skip a permission, only the feature that needs it stays off.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xl))
            PrimaryActionButton(text = "Continue", onClick = onContinue)
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = if (granted) "Allowed" else "Not allowed",
            style = MaterialTheme.typography.labelSmall,
            color = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
