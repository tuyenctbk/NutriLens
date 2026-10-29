package com.example.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.model.HealthWarning
import com.example.model.Product
import com.example.model.WarningSeverity

object SafetyNotificationManager {

    const val CHANNEL_ID = "channel_dietary_safety_alerts"
    private const val CHANNEL_NAME = "Dietary & Allergen Safety Alerts"
    private const val CHANNEL_DESC = "Notifies when scanned food products contain ingredients matching your sensitive allergen or dietary profile."

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendDietaryAlertNotification(
        context: Context,
        product: Product,
        warnings: List<HealthWarning>
    ) {
        val criticalWarnings = warnings.filter { it.severity == WarningSeverity.DANGER || it.severity == WarningSeverity.CAUTION }
        if (criticalWarnings.isEmpty()) return

        createNotificationChannel(context)

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return

        // Build Intent to open app directly to Product Detail
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_BARCODE", product.barcode)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            product.barcode.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val primaryWarning = criticalWarnings.first()
        val title = "🚨 Dietary Alert: ${product.name}"
        val bodyText = "${primaryWarning.title}: ${primaryWarning.message}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_big_text, product.brand, bodyText)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            notificationManager.notify(product.barcode.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        }
    }
}
