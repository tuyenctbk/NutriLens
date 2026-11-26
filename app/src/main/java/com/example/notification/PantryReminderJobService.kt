package com.example.notification

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.model.PantryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Native Android JobService running a lightweight daily background check for Pantry items
 * expiring within 3 days. Uses JobScheduler (no exact-alarm permission required).
 */
class PantryReminderJobService : JobService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        serviceScope.launch {
            try {
                PantryReminderScheduler.checkAndNotifyExpiringItems(applicationContext)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        serviceScope.cancel()
        return true
    }
}

object PantryReminderScheduler {
    private const val JOB_ID = 7701
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    private const val THREE_DAYS_MS = 3L * ONE_DAY_MS

    fun syncSchedule(context: Context, enabled: Boolean) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
        if (!enabled) {
            scheduler.cancel(JOB_ID)
            return
        }

        val component = ComponentName(context, PantryReminderJobService::class.java)
        val jobInfo = JobInfo.Builder(JOB_ID, component)
            .setPeriodic(ONE_DAY_MS)
            .setRequiresBatteryNotLow(true)
            .build()

        try {
            scheduler.schedule(jobInfo)
        } catch (_: Exception) {
            // Gracefully ignore if job scheduling is restricted on specific OEM builds
        }
    }

    suspend fun checkAndNotifyExpiringItems(context: Context, forceCheck: Boolean = false): Int {
        val appContext = context.applicationContext
        val prefs = UserPreferencesRepository(appContext)
        val profile = prefs.userProfileFlow.first()
        if (!forceCheck && !profile.pantryRemindersEnabled) return 0

        val dao = AppDatabase.getDatabase(appContext).pantryDao()
        val now = System.currentTimeMillis()
        val windowEnd = now + THREE_DAYS_MS

        val expiringItems = dao.getUnconsumedSync()
            .filter { it.expirationDate in now..windowEnd }
            .map { entity ->
                PantryItem(
                    barcode = entity.barcode,
                    name = entity.name,
                    brand = entity.brand,
                    imageUrl = entity.imageUrl,
                    quantity = entity.quantity,
                    addedAt = entity.addedAt,
                    expirationDate = entity.expirationDate,
                    isConsumed = entity.isConsumed
                )
            }

        if (expiringItems.isNotEmpty()) {
            SafetyNotificationManager.sendPantryExpirationNotification(context, expiringItems)
        }
        return expiringItems.size
    }
}
