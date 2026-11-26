package com.example.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.example.R

/**
 * Safely unwraps nested ContextWrappers (such as Compose Dialog / ContextThemeWrapper)
 * to retrieve the hosting Activity without ever failing with
 * "Billing Activity requires an Activity session".
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class DonationTier(
    val skuId: String,
    val priceUsd: String,
    val labelRes: Int
) {
    ESPRESSO("nutrilens_supporter_espresso", "$1.99", R.string.donation_tier_1),
    COFFEE("nutrilens_supporter_coffee", "$4.99", R.string.donation_tier_2),
    MEAL("nutrilens_supporter_meal", "$9.99", R.string.donation_tier_3),
    PATRON("nutrilens_supporter_patron", "$19.99", R.string.donation_tier_4)
}

object DonationBillingHelper {

    const val PAYPAL_BASE_URL = "https://paypal.me/NutriLensApp"
    const val KOFI_URL = "https://ko-fi.com/nutrilens"

    fun buildPayPalUrl(amountUsd: Int?): String {
        return if (amountUsd != null && amountUsd > 0) {
            "$PAYPAL_BASE_URL/${amountUsd}USD"
        } else {
            PAYPAL_BASE_URL
        }
    }

    /**
     * Opens an external donation URL (PayPal / Ko-fi) using the unwrapped Activity session
     * or safe NEW_TASK flag, falling back to clipboard copy if no browser app is available.
     */
    fun openExternalDonationLink(
        context: Context,
        hostActivity: Activity?,
        url: String,
        clipLabel: String
    ) {
        val resolvedActivity = hostActivity ?: context.findActivity()
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        try {
            if (resolvedActivity != null && !resolvedActivity.isFinishing && !resolvedActivity.isDestroyed) {
                resolvedActivity.startActivity(intent)
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (_: Exception) {
            copyToClipboard(context, clipLabel, url)
        }
    }

    fun copyToClipboard(context: Context, label: String, url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, url))
        Toast.makeText(
            context,
            context.getString(R.string.donation_link_copied),
            Toast.LENGTH_SHORT
        ).show()
    }
}
