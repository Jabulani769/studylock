package studylock.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.ZonedDateTime

/**
 * Inexact weekday reminder alarms (D-21). Each alarm schedules the next
 * weekday when it fires, so weekends never produce a mandatory session.
 */
class ReminderScheduler(private val context: Context) {

    fun reschedule(enabled: Boolean, startMinutes: Int) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = pendingIntent()
        manager.cancel(pendingIntent)
        if (!enabled) return
        val trigger = ReminderTime.nextWeekdayTrigger(ZonedDateTime.now(), startMinutes)
        manager.setWindow(
            AlarmManager.RTC_WAKEUP,
            trigger.toInstant().toEpochMilli(),
            WINDOW_MILLIS,
            pendingIntent
        )
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val REQUEST_CODE = 73
        const val WINDOW_MILLIS = 10 * 60 * 1000L
    }
}
