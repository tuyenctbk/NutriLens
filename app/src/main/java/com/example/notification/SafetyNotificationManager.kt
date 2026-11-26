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
import com.example.model.PantryItem
import com.example.model.Product
import com.example.model.WarningSeverity
import com.example.ui.components.localizedMessage
import com.example.ui.components.localizedTitle

object SafetyNotificationManager {

    const val CHANNEL_ID = "channel_dietary_safety_alerts"
    const val CHANNEL_PANTRY_EXPIRY = "channel_pantry_expiration_reminders"
    const val EXTRA_OPEN_TAB = "EXTRA_OPEN_TAB"
    const val TAB_PANTRY = "pantry"

    private const val PANTRY_NOTIFICATION_BASE_ID = 42000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val safetyChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_safety_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_safety_desc)
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(safetyChannel)

            val pantryChannel = NotificationChannel(
                CHANNEL_PANTRY_EXPIRY,
                context.getString(R.string.notif_channel_pantry_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_pantry_desc)
            }
            notificationManager.createNotificationChannel(pantryChannel)
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val primaryWarning = criticalWarnings.first()
        val title = context.getString(R.string.notification_dietary_alert_title, product.name)
        val bodyText = "${primaryWarning.localizedTitle(context)}: ${primaryWarning.localizedMessage(context)}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_big_text, product.brand, bodyText)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            notificationManager.notify(product.barcode.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Permission not granted on Android 13+
        }
    }

    @SuppressLint("MissingPermission")
    fun sendPantryExpirationNotification(
        context: Context,
        expiringItems: List<PantryItem>
    ) {
        if (expiringItems.isEmpty()) return
        createNotificationChannel(context)

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return

        val openPantryIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, TAB_PANTRY)
        }
        val openPantryPendingIntent = PendingIntent.getActivity(
            context,
            PANTRY_NOTIFICATION_BASE_ID,
            openPantryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val leadItem = expiringItems.first()
        val notificationId = PANTRY_NOTIFICATION_BASE_ID + (leadItem.barcode.hashCode() and 0x7FFF)

        val now = System.currentTimeMillis()
        val daysLeft = ((leadItem.expirationDate - now) / (1000L * 60L * 60L * 24L)).toInt().coerceAtLeast(0)

        val title = if (expiringItems.size == 1) {
            context.getString(R.string.pantry_notif_single_title, leadItem.name)
        } else {
            context.getString(R.string.pantry_notif_multi_title, expiringItems.size)
        }

        val body = if (expiringItems.size == 1) {
            if (daysLeft == 0) {
                context.getString(R.string.pantry_notif_expires_today, leadItem.brand)
            } else {
                context.getString(R.string.pantry_notif_expires_in_days, leadItem.brand, daysLeft)
            }
        } else {
            val previewNames = expiringItems.take(3).joinToString(", ") { it.name }
            context.getString(R.string.pantry_notif_multi_body, previewNames)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_PANTRY_EXPIRY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPantryPendingIntent)

        // Only attach quick "Mark Consumed" action for single-item alerts so it is unambiguous which item is updated
        if (expiringItems.size == 1) {
            val markConsumedIntent = Intent(context, PantryActionReceiver::class.java).apply {
                action = PantryActionReceiver.ACTION_MARK_CONSUMED
                putExtra(PantryActionReceiver.EXTRA_BARCODE, leadItem.barcode)
                putExtra(PantryActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val markConsumedPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                markConsumedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                0,
                context.getString(R.string.pantry_notif_mark_consumed),
                markConsumedPendingIntent
            )
        }

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted on Android 13+
        }
    }
}
