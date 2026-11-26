package com.example.notification

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.model.PantryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Native Android JobService running a lightweight daily background check for Pantry items
 * expiring within 3 days. Uses JobScheduler (no exact-alarm permission required).
 */
class PantryReminderJobService : JobService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        activeJob?.cancel()
        activeJob = serviceScope.launch {
            try {
                PantryReminderScheduler.checkAndNotifyExpiringItems(applicationContext)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        activeJob?.cancel()
        activeJob = null
        return true
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
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

        // Preserve existing periodic schedule so daily app opens do not reset the 24h timer
        if (scheduler.getPendingJob(JOB_ID) != null) {
            return
        }

        val component = ComponentName(context, PantryReminderJobService::class.java)
        val builder = JobInfo.Builder(JOB_ID, component)
            .setPeriodic(ONE_DAY_MS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setRequiresBatteryNotLow(true)
        }

        try {
            scheduler.schedule(builder.build())
        } catch (_: Exception) {
            // Gracefully ignore if job scheduling is restricted on specific OEM builds
        }
    }

    suspend fun checkAndNotifyExpiringItems(context: Context, forceCheck: Boolean = false): Int = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val prefs = UserPreferencesRepository(appContext)
        val profile = prefs.userProfileFlow.first()
        if (!forceCheck && !profile.pantryRemindersEnabled) return@withContext 0

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
            SafetyNotificationManager.sendPantryExpirationNotification(appContext, expiringItems)
        }
        expiringItems.size
    }
}
