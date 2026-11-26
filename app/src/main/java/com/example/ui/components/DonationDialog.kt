package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.BillingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationDialog(
    isSupporter: Boolean,
    billingState: BillingUiState = BillingUiState(),
    onDismiss: () -> Unit,
    onRequestGooglePlayDonation: (DonationTier) -> Unit,
    hostActivity: Activity? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolvedActivity = remember(hostActivity, context) {
        hostActivity ?: context.findActivity()
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("donation_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.donation_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = stringResource(R.string.donation_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("donation_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (isSupporter) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.donation_thank_you),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.donation_tab_google_play), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        icon = { Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("donation_tab_play")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.donation_tab_paypal), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        icon = { Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("donation_tab_paypal")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(stringResource(R.string.donation_tab_kofi), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("donation_tab_kofi")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> GooglePlayTiersSection(
                        billingState = billingState,
                        onSelectTier = onRequestGooglePlayDonation
                    )
                    1 -> PayPalQrSection(hostActivity = resolvedActivity)
                    2 -> KofiSection(hostActivity = resolvedActivity)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun GooglePlayTiersSection(
    billingState: BillingUiState,
    onSelectTier: (DonationTier) -> Unit
) {
    val isBusy = billingState.processingTier != null
    val tier1 = "${stringResource(DonationTier.ESPRESSO.labelRes).substringBefore("(").trim()} (${billingState.displayPriceFor(DonationTier.ESPRESSO)})"
    val tier2 = "${stringResource(DonationTier.COFFEE.labelRes).substringBefore("(").trim()} (${billingState.displayPriceFor(DonationTier.COFFEE)})"
    val tier3 = "${stringResource(DonationTier.MEAL.labelRes).substringBefore("(").trim()} (${billingState.displayPriceFor(DonationTier.MEAL)})"
    val tier4 = "${stringResource(DonationTier.PATRON.labelRes).substringBefore("(").trim()} (${billingState.displayPriceFor(DonationTier.PATRON)})"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.donation_google_play),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSelectTier(DonationTier.ESPRESSO) },
                enabled = !isBusy,
                modifier = Modifier
                    .weight(1f)
                    .testTag("donation_tier_espresso"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (billingState.processingTier == DonationTier.ESPRESSO) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(tier1, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Button(
                onClick = { onSelectTier(DonationTier.COFFEE) },
                enabled = !isBusy,
                modifier = Modifier
                    .weight(1f)
                    .testTag("donation_tier_coffee"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (billingState.processingTier == DonationTier.COFFEE) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(tier2, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { onSelectTier(DonationTier.MEAL) },
                enabled = !isBusy,
                modifier = Modifier
                    .weight(1f)
                    .testTag("donation_tier_meal"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (billingState.processingTier == DonationTier.MEAL) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(tier3, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            FilledTonalButton(
                onClick = { onSelectTier(DonationTier.PATRON) },
                enabled = !isBusy,
                modifier = Modifier
                    .weight(1f)
                    .testTag("donation_tier_patron"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (billingState.processingTier == DonationTier.PATRON) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(tier4, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun PayPalQrSection(hostActivity: Activity?) {
    val context = LocalContext.current
    val presets: List<Int?> = listOf(3, 5, 10, null)
    var selectedAmount by remember { mutableStateOf<Int?>(5) }
    val paypalUrl = remember(selectedAmount) { DonationBillingHelper.buildPayPalUrl(selectedAmount) }
    val qrMatrix = remember(paypalUrl) { QrCodeGenerator.encode(paypalUrl) }
    val clipLabel = stringResource(R.string.donation_paypal_clip_label)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.donation_paypal),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { amount ->
                FilterChip(
                    selected = selectedAmount == amount,
                    onClick = { selectedAmount = amount },
                    label = {
                        Text(
                            text = if (amount != null) "$$amount" else stringResource(R.string.donation_any_amount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("paypal_preset_${amount ?: "any"}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Real ISO/IEC 18004 scannable QR code with standard white quiet zone
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier
                .size(164.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                .testTag("paypal_live_qr")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(14.dp)
            ) {
                Canvas(modifier = Modifier.size(136.dp)) {
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

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
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
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("paypal_open_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.donation_open_paypal), fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = {
                    DonationBillingHelper.copyToClipboard(context, clipLabel, paypalUrl)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("paypal_copy_button")
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.donation_copy_link), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun KofiSection(hostActivity: Activity?) {
    val context = LocalContext.current
    val kofiUrl = DonationBillingHelper.KOFI_URL
    val clipLabel = stringResource(R.string.donation_kofi_clip_label)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.donation_kofi),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.donation_kofi_card_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.donation_kofi_card_desc),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("kofi_open_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.donation_kofi_button), color = Color.White, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            DonationBillingHelper.copyToClipboard(context, clipLabel, kofiUrl)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("kofi_copy_button")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
