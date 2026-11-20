package studylock.app.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import studylock.app.reminders.ReminderTime
import studylock.app.ui.theme.Spacing

/**
 * One compact row that opens the platform time picker. Used by both onboarding
 * and Settings so the reminder has the same interaction everywhere.
 *
 * The platform dialog is used because the pinned Compose Material 3 version
 * does not yet provide TimePickerDialog.
 */
@Composable
fun ReminderTimeSelector(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    label: String = "Reminder time"
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            TimePickerDialog(
                context,
                { _, hour, minute -> onMinutesChange(hour * 60 + minute) },
                minutes / 60,
                minutes % 60,
                true
            ).show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("$label: ${ReminderTime.format(minutes)}")
        Spacer(Modifier.width(Spacing.sm))
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = "Change reminder time",
            modifier = Modifier.size(16.dp)
        )
    }
}
