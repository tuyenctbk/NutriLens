package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BillingConnectionState
import com.example.data.BillingEvent
import com.example.data.BillingUiState
import com.example.ui.components.DonationBillingHelper
import com.example.ui.components.DonationTier
import com.example.ui.components.QrCodeGenerator
import com.example.ui.components.findActivity
import com.example.ui.theme.semanticSafeColors
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportDevelopmentScreen(
    isSupporter: Boolean,
    billingState: BillingUiState,
    billingEvents: SharedFlow<BillingEvent>,
    onRequestGooglePlayDonation: (DonationTier) -> Unit,
    onRestorePurchases: () -> Unit,
    onBack: () -> Unit,
    hostActivity: Activity? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolvedActivity = remember(hostActivity, context) {
        hostActivity ?: context.findActivity()
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(billingEvents) {
        billingEvents.collect { event ->
            val message = when (event) {
                is BillingEvent.PurchaseSucceeded -> {
                    val label = event.tier?.let { context.getString(it.labelRes) }
                        ?: context.getString(R.string.donation_active_supporter)
                    context.getString(R.string.donation_processed_toast, label)
                }
                is BillingEvent.PurchasePending -> {
                    context.getString(R.string.billing_purchase_pending)
                }
                is BillingEvent.PurchaseCancelled -> {
                    context.getString(R.string.billing_purchase_cancelled)
                }
                is BillingEvent.RestoreResult -> {
                    if (event.restored) {
                        context.getString(R.string.billing_restore_success)
                    } else {
                        context.getString(R.string.billing_restore_none)
                    }
                }
                is BillingEvent.BillingMessage -> {
                    if (event.formatArg != null) {
                        context.getString(event.messageRes, event.formatArg)
                    } else {
                        context.getString(event.messageRes)
                    }
                }
                is BillingEvent.BillingError -> {
                    context.getString(R.string.billing_generic_error, event.debugMessage)
                }
            }
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("support_development_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.support_screen_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.support_screen_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("support_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWide = maxWidth >= 720.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 960.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SupportMissionHeaderCard(isSupporter = isSupporter)

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            GooglePlayBillingCard(
                                billingState = billingState,
                                onSelectTier = onRequestGooglePlayDonation,
                                onRestorePurchases = onRestorePurchases
                            )
                            KofiSupportCard(hostActivity = resolvedActivity)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PayPalSupportCard(hostActivity = resolvedActivity)
                        }
                    }
                } else {
                    GooglePlayBillingCard(
                        billingState = billingState,
                        onSelectTier = onRequestGooglePlayDonation,
                        onRestorePurchases = onRestorePurchases
                    )
                    PayPalSupportCard(hostActivity = resolvedActivity)
                    KofiSupportCard(hostActivity = resolvedActivity)
                }

                Text(
                    text = stringResource(R.string.support_footer_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SupportMissionHeaderCard(isSupporter: Boolean) {
    val (safeBg, safeFg, _) = semanticSafeColors()
    val containerColor = if (isSupporter) {
        safeBg
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("support_mission_card")
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSupporter) safeFg.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSupporter) Icons.Filled.Verified else Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = if (isSupporter) safeFg else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSupporter) {
                        stringResource(R.string.donation_active_supporter)
                    } else {
                        stringResource(R.string.donation_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSupporter) safeFg else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isSupporter) {
                        stringResource(R.string.donation_thank_you)
                    } else {
                        stringResource(R.string.donation_subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GooglePlayBillingCard(
    billingState: BillingUiState,
    onSelectTier: (DonationTier) -> Unit,
    onRestorePurchases: () -> Unit
) {
    val (safeBg, safeFg, _) = semanticSafeColors()
    val statusLabel = when (billingState.connectionState) {
        BillingConnectionState.CONNECTED -> stringResource(R.string.billing_status_ready)
        BillingConnectionState.CONNECTING -> stringResource(R.string.billing_status_connecting)
        BillingConnectionState.DISCONNECTED,
        BillingConnectionState.UNAVAILABLE -> stringResource(R.string.billing_status_unavailable)
    }
    val statusBg = when (billingState.connectionState) {
        BillingConnectionState.CONNECTED -> safeBg
        BillingConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondaryContainer
        BillingConnectionState.DISCONNECTED,
        BillingConnectionState.UNAVAILABLE -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val statusFg = when (billingState.connectionState) {
        BillingConnectionState.CONNECTED -> safeFg
        BillingConnectionState.CONNECTING -> MaterialTheme.colorScheme.onSecondaryContainer
        BillingConnectionState.DISCONNECTED,
        BillingConnectionState.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("support_google_play_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.donation_google_play),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (billingState.connectionState == BillingConnectionState.CONNECTED) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = statusFg,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusFg,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.support_google_play_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            val isBusy = billingState.processingTier != null

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BillingTierButton(
                    tier = DonationTier.ESPRESSO,
                    billingState = billingState,
                    isPrimary = true,
                    enabled = !isBusy,
                    onClick = { onSelectTier(DonationTier.ESPRESSO) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("support_play_tier_espresso")
                )
                BillingTierButton(
                    tier = DonationTier.COFFEE,
                    billingState = billingState,
                    isPrimary = true,
                    enabled = !isBusy,
                    onClick = { onSelectTier(DonationTier.COFFEE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("support_play_tier_coffee")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BillingTierButton(
                    tier = DonationTier.MEAL,
                    billingState = billingState,
                    isPrimary = false,
                    enabled = !isBusy,
                    onClick = { onSelectTier(DonationTier.MEAL) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("support_play_tier_meal")
                )
                BillingTierButton(
                    tier = DonationTier.PATRON,
                    billingState = billingState,
                    isPrimary = false,
                    enabled = !isBusy,
                    onClick = { onSelectTier(DonationTier.PATRON) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("support_play_tier_patron")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onRestorePurchases,
                    enabled = !billingState.isRestoring && !isBusy,
                    modifier = Modifier.testTag("support_restore_purchases_button")
                ) {
                    if (billingState.isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.support_restore_purchases),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun BillingTierButton(
    tier: DonationTier,
    billingState: BillingUiState,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isThisProcessing = billingState.processingTier == tier
    val rawLabel = stringResource(tier.labelRes)
    val baseTitle = remember(rawLabel) {
        rawLabel.substringBefore("(").trim().ifBlank { rawLabel }
    }
    val displayPrice = billingState.displayPriceFor(tier)

    val buttonContent: @Composable () -> Unit = {
        if (isThisProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = if (isPrimary) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = baseTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayPrice,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }

    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            modifier = modifier.heightIn(min = 54.dp)
        ) {
            buttonContent()
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            modifier = modifier.heightIn(min = 54.dp)
        ) {
            buttonContent()
        }
    }
}

@Composable
private fun PayPalSupportCard(hostActivity: Activity?) {
    val context = LocalContext.current
    val presets: List<Int?> = listOf(3, 5, 10, null)
    var selectedAmount by remember { mutableStateOf<Int?>(5) }
    val paypalUrl = remember(selectedAmount) { DonationBillingHelper.buildPayPalUrl(selectedAmount) }
    val qrMatrix = remember(paypalUrl) { QrCodeGenerator.encode(paypalUrl) }
    val clipLabel = stringResource(R.string.donation_paypal_clip_label)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("support_paypal_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.donation_paypal),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.support_paypal_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { amount ->
                    FilterChip(
                        selected = selectedAmount == amount,
                        onClick = { selectedAmount = amount },
                        label = {
                            Text(
                                text = if (amount != null) "$$amount" else stringResource(R.string.donation_any_amount),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("support_paypal_preset_${amount ?: "any"}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(176.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .testTag("support_paypal_qr")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Canvas(modifier = Modifier.size(148.dp)) {
                        val modules = qrMatrix.size
                        val cellSize = size.width / modules
                        for (r in 0 until modules) {
                            for (c in 0 until modules) {
                                if (qrMatrix[r][c]) {
                                    drawRect(
                                        color = Color(0xFF0A1F18),
                                        topLeft = Offset(c * cellSize, r * cellSize),
                                        size = Size(cellSize + 0.5f, cellSize + 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
            ) {
                Text(
                    text = paypalUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        DonationBillingHelper.openExternalDonationLink(
                            context = context,
                            hostActivity = hostActivity,
                            url = paypalUrl,
                            clipLabel = clipLabel
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("support_paypal_open_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.donation_open_paypal),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        DonationBillingHelper.copyToClipboard(context, clipLabel, paypalUrl)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("support_paypal_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.donation_copy_link),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun KofiSupportCard(hostActivity: Activity?) {
    val context = LocalContext.current
    val kofiUrl = DonationBillingHelper.KOFI_URL
    val clipLabel = stringResource(R.string.donation_kofi_clip_label)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("support_kofi_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocalCafe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.donation_kofi),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.donation_kofi_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        DonationBillingHelper.openExternalDonationLink(
                            context = context,
                            hostActivity = hostActivity,
                            url = kofiUrl,
                            clipLabel = clipLabel
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF29ABE2)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("support_kofi_open_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.donation_kofi_button),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        DonationBillingHelper.copyToClipboard(context, clipLabel, kofiUrl)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("support_kofi_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.donation_copy_link),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
