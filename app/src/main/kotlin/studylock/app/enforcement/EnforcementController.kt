package studylock.app.enforcement

import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import studylock.focus.EnforcementResolver
import studylock.focus.EnforcementSnapshot
import studylock.focus.EnforcementState

/**
 * Device adapter for the approved, limited enforcement scope (docs/06):
 * foreground-app monitoring plus user-granted notification quieting.
 *
 * It never blocks apps, never reads notification content, and never claims
 * universal enforcement. All state is local to this app.
 */
class EnforcementController(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val notificationManager: NotificationManager? =
        context.getSystemService(NotificationManager::class.java)
    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(UsageStatsManager::class.java)

    @Suppress("DEPRECATION")
    fun usageAccessGranted(): Boolean {
        return try {
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            appOps.checkOpNoThrow(
                "android:get_usage_stats", Process.myUid(), context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun notificationPolicyAccessGranted(): Boolean = try {
        notificationManager?.isNotificationPolicyAccessGranted == true
    } catch (e: Exception) {
        false
    }

    fun quietModeActive(): Boolean = try {
        notificationManager?.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    } catch (e: Exception) {
        false
    }

    /** Snapshot also records grant history so a later revocation is detectable. */
    fun snapshot(sessionActive: Boolean): EnforcementSnapshot {
        val usage = usageAccessGranted()
        val dnd = notificationPolicyAccessGranted()
        val hadUsage = prefs.getBoolean(KEY_HAD_USAGE, false)
        val hadDnd = prefs.getBoolean(KEY_HAD_DND, false)
        val revocation = sessionActive && ((!usage && hadUsage) || (!dnd && hadDnd))
        // One apply() instead of up to four on the low-end devices this runs on.
        if ((usage && !hadUsage) || (dnd && !hadDnd) || revocation) {
            prefs.edit().apply {
                if (usage && !hadUsage) putBoolean(KEY_HAD_USAGE, true)
                if (dnd && !hadDnd) putBoolean(KEY_HAD_DND, true)
                if (revocation) putBoolean(KEY_INTERRUPTED, true)
            }.apply()
        }
        return EnforcementSnapshot(
            usageAccessGranted = usage,
            notificationPolicyAccessGranted = dnd,
            quietModeActive = quietModeActive(),
            sessionActive = sessionActive,
            interrupted = interrupted()
        )
    }

    fun state(sessionActive: Boolean): EnforcementState =
        EnforcementResolver.resolve(snapshot(sessionActive))

    /** Alarms stay audible and emergency calls are unaffected by DND. */
    fun applyQuietMode(): Boolean {
        if (!notificationPolicyAccessGranted()) return false
        return try {
            if (!prefs.contains(KEY_PREVIOUS_FILTER)) {
                prefs.edit().putInt(
                    KEY_PREVIOUS_FILTER,
                    notificationManager?.currentInterruptionFilter
                        ?: NotificationManager.INTERRUPTION_FILTER_ALL
                ).apply()
            }
            notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun restoreQuietMode() {
        if (!notificationPolicyAccessGranted()) return
        // A second restore (for example ViewModel then service) must be a no-op,
        // otherwise the student's original DND setting would be overwritten.
        if (!prefs.contains(KEY_PREVIOUS_FILTER)) return
        val previous = prefs.getInt(KEY_PREVIOUS_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        try {
            notificationManager?.setInterruptionFilter(previous)
        } catch (e: Exception) {
            // Permission may have been revoked mid-session; degraded state is recorded.
        }
        prefs.edit().remove(KEY_PREVIOUS_FILTER).apply()
    }

    fun foregroundPackage(nowMillis: Long): String? {
        if (!usageAccessGranted()) return null
        return try {
            usageStatsManager
                ?.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    nowMillis - FOREGROUND_WINDOW_MS,
                    nowMillis
                )
                ?.maxByOrNull { it.lastTimeUsed }
                ?.packageName
        } catch (e: Exception) {
            null
        }
    }

    /** Counts distinct transitions away from StudyLock, not every poll. */
    fun noteForeground(packageName: String?) {
        if (packageName == null) return
        val previous = prefs.getString(KEY_LAST_FOREGROUND, null)
        if (previous == packageName) return
        prefs.edit().apply {
            putString(KEY_LAST_FOREGROUND, packageName)
            if (packageName != context.packageName) {
                putInt(KEY_LEFT_APP_COUNT, leftAppCount() + 1)
            }
        }.apply()
    }

    fun leftAppCount(): Int = prefs.getInt(KEY_LEFT_APP_COUNT, 0)

    fun startSession() {
        prefs.edit()
            .putInt(KEY_LEFT_APP_COUNT, 0)
            .putString(KEY_LAST_FOREGROUND, context.packageName)
            .putBoolean(KEY_INTERRUPTED, false)
            .apply()
    }

    fun noteInterruption() {
        prefs.edit().putBoolean(KEY_INTERRUPTED, true).apply()
    }

    fun interrupted(): Boolean = prefs.getBoolean(KEY_INTERRUPTED, false)

    fun acknowledgeInterruption() {
        prefs.edit().putBoolean(KEY_INTERRUPTED, false).apply()
    }

    fun usageAccessSettingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun notificationPolicySettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun openUsageAccessSettings() {
        context.startActivity(usageAccessSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openNotificationPolicySettings() {
        context.startActivity(
            notificationPolicySettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    companion object {
        private const val PREFS = "studylock_enforcement"
        private const val KEY_HAD_USAGE = "had_usage_access"
        private const val KEY_HAD_DND = "had_dnd_access"
        private const val KEY_PREVIOUS_FILTER = "previous_interruption_filter"
        private const val KEY_LAST_FOREGROUND = "last_foreground"
        private const val KEY_LEFT_APP_COUNT = "left_app_count"
        private const val KEY_INTERRUPTED = "interrupted"
        private const val FOREGROUND_WINDOW_MS = 2 * 60 * 1000L
    }
}
