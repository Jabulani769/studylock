package studylock.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import studylock.app.enforcement.EnforcementController
import studylock.focus.SessionState

/**
 * Keeps a visible countdown notification while a focus session is running.
 * Time itself is derived from the persisted monotonic anchor, so the service
 * (and the app process) can die without losing elapsed time.
 */
class SessionTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitor: Job? = null
    private var sawActiveSession = false

    private val container get() = (application as StudyLockApplication).container
    private val enforcement: EnforcementController get() = container.enforcement
    private var quietModeAppliedByService = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification("Starting session…", ongoing = true))
        // Re-apply after process death; the ViewModel applies it on normal start.
        quietModeAppliedByService = enforcement.applyQuietMode()
        monitor?.cancel()
        monitor = scope.launch { monitorSession() }
        return START_STICKY
    }

    override fun onDestroy() {
        monitor?.cancel()
        if (quietModeAppliedByService) enforcement.restoreQuietMode()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun monitorSession() {
        var pollCount = 0
        while (currentCoroutineContext().isActive) {
            val session = runCatching { container.sessionRepository.getCurrent() }.getOrNull()
            if (session == null) {
                val latest = runCatching {
                    container.sessionRepository.getHistory(1).firstOrNull()
                }.getOrNull()
                if (shouldNotifyCompletion(session, latest, sawActiveSession)) {
                    postCompletionNotification()
                }
                stopSelf()
                return
            }
            if (session.state == SessionState.COMPLETED) {
                postCompletionNotification()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            if (session.state != SessionState.ACTIVE) {
                stopSelf()
                return
            }
            sawActiveSession = true

            val clock = container.clock
            val remaining = session.remainingSeconds(clock.millis, clock.elapsedRealtimeMillis)
            if (remaining <= 0L) {
                val completed = container.focusStateMachine.complete(session)
                runCatching { container.sessionRepository.save(completed) }
                postCompletionNotification()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            // Approved monitoring only: foreground package identity and time
            // window. No notification content, keystrokes or screen capture.
            // UsageStats and permission checks are expensive on some OEMs, so
            // they run far less often than the 5-second countdown refresh.
            pollCount++
            if (pollCount % 4 == 0) {
                enforcement.noteForeground(enforcement.foregroundPackage(clock.millis))
            }
            if (pollCount % 12 == 0) {
                // Detect revocation of a previously granted control.
                enforcement.snapshot(sessionActive = true)
            }

            updateNotification(
                "${remaining.asClockString()} remaining · Open StudyLock to pause",
                ongoing = true
            )
            delay(5_000L)
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Study session timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the remaining time for an active study session."
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun notification(text: String, ongoing: Boolean): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Study session")
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(text: String, ongoing: Boolean) {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        NotificationManagerCompat.from(this).notify(
            NOTIFICATION_ID,
            notification(text, ongoing)
        )
    }

    private fun postCompletionNotification() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        NotificationManagerCompat.from(this).notify(
            COMPLETION_NOTIFICATION_ID,
            notification(
                "Session complete. Open StudyLock to review and record learning evidence.",
                ongoing = false
            )
        )
    }

    companion object {
        private const val CHANNEL_ID = "study_session_timer"
        private const val NOTIFICATION_ID = 41
        private const val COMPLETION_NOTIFICATION_ID = 42

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SessionTimerService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SessionTimerService::class.java))
        }
    }
}
