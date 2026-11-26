package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.NutrimentsInfo
import com.example.model.Product
import com.example.ui.components.InteractiveMacroBreakdownCard
import com.example.ui.components.MultiProductMacroBarChartCard
import com.example.ui.components.NovaDistributionBarChartCard
import com.example.ui.components.NutritionalInsightsCard
import com.example.ui.components.PortfolioMacroKpiStrip
import com.example.ui.components.PortfolioNutritionalInsightBanner

// Standard FDA / WHO Daily Recommended Intake Values (2,000 kcal reference diet)
private object DailyValues {
    const val SUGAR_MAX_G = 50.0 // WHO recommends <50g free sugars per day
    const val SAT_FAT_MAX_G = 20.0 // FDA Daily Value 20g
    const val SALT_MAX_G = 6.0 // WHO recommends <6g salt (2.3g sodium) per day
    const val PROTEIN_GOAL_G = 50.0
    const val FIBER_GOAL_G = 28.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    products: List<Product>,
    onSelectProductForScan: () -> Unit,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Remember selected barcode across list updates (e.g. toggling favorites) instead of resetting
    var selectedBarcode by rememberSaveable { mutableStateOf("") }

    val activeProduct = remember(products, selectedBarcode) {
        products.find { it.barcode == selectedBarcode } ?: products.firstOrNull()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.dashboard_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (products.isEmpty()) {
                DashboardEmptyStateCard(
                    onScanClick = onSelectProductForScan
                )
            } else {
                // Top Portfolio KPI Averages across all scanned Open Food Facts items
                PortfolioMacroKpiStrip(products = products)

                Spacer(modifier = Modifier.height(12.dp))

                // Portfolio-level dietary insight across scanned food database items
                PortfolioNutritionalInsightBanner(products = products)

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.select_product_analyze),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    products.forEach { prod ->
                        val isSelected = prod.barcode == activeProduct?.barcode
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedBarcode = prod.barcode },
                            label = {
                                Text(
                                    text = prod.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("dashboard_chip_${prod.barcode}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activeProduct != null) {
                    ActiveProductSummaryBanner(
                        product = activeProduct,
                        onInspect = { onProductClick(activeProduct.barcode) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isWide = maxWidth >= 720.dp
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    MacroCompositionSection(nutriments = activeProduct.nutriments)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    NutritionalInsightsCard(nutriments = activeProduct.nutriments)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    DailyIntakeSection(nutriments = activeProduct.nutriments)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    MultiProductComparisonSection(
                                        products = products,
                                        selectedBarcode = activeProduct.barcode,
                                        onSelectProduct = { selectedBarcode = it }
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    HistoryQualitySection(products = products)
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                MacroCompositionSection(nutriments = activeProduct.nutriments)
                                Spacer(modifier = Modifier.height(20.dp))
                                NutritionalInsightsCard(nutriments = activeProduct.nutriments)
                                Spacer(modifier = Modifier.height(22.dp))
                                MultiProductComparisonSection(
                                    products = products,
                                    selectedBarcode = activeProduct.barcode,
                                    onSelectProduct = { selectedBarcode = it }
                                )
                                Spacer(modifier = Modifier.height(22.dp))
                                DailyIntakeSection(nutriments = activeProduct.nutriments)
                                Spacer(modifier = Modifier.height(22.dp))
                                HistoryQualitySection(products = products)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DashboardEmptyStateCard(
    onScanClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_empty_card")
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.dashboard_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.dashboard_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_open_scanner_button")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scan_a_product))
            }
        }
    }
}

@Composable
private fun ActiveProductSummaryBanner(
    product: Product,
    onInspect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onInspect)
            .testTag("dashboard_active_product_banner")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.nutritional_breakdown_serving),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = stringResource(R.string.inspect),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun MacroCompositionSection(nutriments: NutrimentsInfo) {
    Text(
        text = stringResource(R.string.macro_composition_title),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.macro_composition_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(10.dp))
    InteractiveMacroBreakdownCard(nutriments = nutriments)
}

@Composable
private fun MultiProductComparisonSection(
    products: List<Product>,
    selectedBarcode: String?,
    onSelectProduct: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.dashboard_multi_product_title),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.dashboard_multi_product_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(10.dp))
    MultiProductMacroBarChartCard(
        products = products,
        selectedBarcode = selectedBarcode,
        onSelectProduct = onSelectProduct
    )
}

@Composable
private fun DailyIntakeSection(nutriments: NutrimentsInfo) {
    Text(
        text = stringResource(R.string.daily_intake_title),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.daily_intake_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(10.dp))
    DailyValuesGaugeCard(nutriments = nutriments)
}

@Composable
private fun HistoryQualitySection(products: List<Product>) {
    Text(
        text = stringResource(R.string.history_quality_title),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.history_quality_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(10.dp))
    NovaDistributionBarChartCard(products = products)
}

@Composable
private fun DailyValuesGaugeCard(nutriments: NutrimentsInfo) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val sugars = nutriments.sugars ?: 0.0
            val sugarPct = (sugars / DailyValues.SUGAR_MAX_G).toFloat()
            DailyValueProgressBar(
                label = stringResource(R.string.gauge_sugars),
                valueGrams = sugars,
                maxGrams = DailyValues.SUGAR_MAX_G,
                progressFraction = sugarPct,
                barColor = if (sugars >= 22.5) Color(0xFFD32F2F) else if (sugars >= 5.0) Color(0xFFF57C00) else Color(0xFF388E3C),
                warningNote = if (sugars >= 22.5) stringResource(R.string.warning_sugar_exceeds) else null
            )

            Spacer(modifier = Modifier.height(14.dp))

            val satFat = nutriments.saturatedFat ?: 0.0
            val satFatPct = (satFat / DailyValues.SAT_FAT_MAX_G).toFloat()
            DailyValueProgressBar(
                label = stringResource(R.string.gauge_sat_fat),
                valueGrams = satFat,
                maxGrams = DailyValues.SAT_FAT_MAX_G,
                progressFraction = satFatPct,
                barColor = if (satFat >= 5.0) Color(0xFFD32F2F) else if (satFat >= 1.5) Color(0xFFF57C00) else Color(0xFF388E3C),
                warningNote = if (satFat >= 5.0) stringResource(R.string.warning_sat_fat_high) else null
            )

            Spacer(modifier = Modifier.height(14.dp))

            val salt = nutriments.salt ?: 0.0
            val saltPct = (salt / DailyValues.SALT_MAX_G).toFloat()
            DailyValueProgressBar(
                label = stringResource(R.string.gauge_salt),
                valueGrams = salt,
                maxGrams = DailyValues.SALT_MAX_G,
                progressFraction = saltPct,
                barColor = if (salt >= 1.5) Color(0xFFD32F2F) else if (salt >= 0.3) Color(0xFFF57C00) else Color(0xFF388E3C),
                warningNote = if (salt >= 1.5) stringResource(R.string.warning_salt_high) else null
            )

            Spacer(modifier = Modifier.height(14.dp))

            val fiber = nutriments.fiber ?: 0.0
            val fiberPct = (fiber / DailyValues.FIBER_GOAL_G).toFloat()
            DailyValueProgressBar(
                label = stringResource(R.string.gauge_fiber),
                valueGrams = fiber,
                maxGrams = DailyValues.FIBER_GOAL_G,
                progressFraction = fiberPct,
                barColor = Color(0xFF0EA5E9),
                warningNote = null
            )

            Spacer(modifier = Modifier.height(14.dp))

            val protein = nutriments.proteins ?: 0.0
            val proteinPct = (protein / DailyValues.PROTEIN_GOAL_G).toFloat()
            DailyValueProgressBar(
                label = stringResource(R.string.gauge_protein),
                valueGrams = protein,
                maxGrams = DailyValues.PROTEIN_GOAL_G,
                progressFraction = proteinPct,
                barColor = Color(0xFF10B981),
                warningNote = null
            )
        }
    }
}

@Composable
private fun DailyValueProgressBar(
    label: String,
    valueGrams: Double,
    maxGrams: Double,
    progressFraction: Float,
    barColor: Color,
    warningNote: String?
) {
    var animateStart by remember { mutableStateOf(false) }
    LaunchedEffect(progressFraction) { animateStart = true }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animateStart) progressFraction.coerceIn(0f, 1.5f) else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "dv_progress"
    )

    val pctInt = (progressFraction * 100).toInt()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.dv_percent_format, valueGrams, maxGrams.toInt(), pctInt),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = barColor,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(barColor.copy(alpha = 0.14f))
        ) {
            val fillWidth = size.width * animatedProgress.coerceAtMost(1.0f)
            if (fillWidth > 1f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(barColor.copy(alpha = 0.75f), barColor)
                    ),
                    size = Size(fillWidth, size.height),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        warningNote?.let { note ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = barColor
            )
        }
    }
}
