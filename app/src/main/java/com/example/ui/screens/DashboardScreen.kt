package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.theme.Nova1
import com.example.ui.theme.Nova2
import com.example.ui.theme.Nova3
import com.example.ui.theme.Nova4
import com.example.ui.theme.NutriScoreA
import com.example.ui.theme.NutriScoreB
import com.example.ui.theme.NutriScoreC
import com.example.ui.theme.NutriScoreD
import com.example.ui.theme.NutriScoreE

// Standard FDA / WHO Daily Recommended Intake Values (2,000 kcal reference diet)
private object DailyValues {
    const val SUGAR_MAX_G = 50.0 // WHO recommends <50g free sugars per day
    const val SAT_FAT_MAX_G = 20.0 // FDA Daily Value 20g
    const val SALT_MAX_G = 6.0 // WHO recommends <6g salt (2.3g sodium) per day
    const val FAT_MAX_G = 78.0
    const val CARBS_MAX_G = 275.0
    const val PROTEIN_GOAL_G = 50.0
    const val FIBER_GOAL_G = 28.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    products: List<Product>,
    onSelectProductForScan: () -> Unit,
    onProductClick: (String) -> Unit,
    onSeedSamples: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBarcode by remember(products) {
        mutableStateOf(products.firstOrNull()?.barcode ?: "")
    }

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
                            text = "Nutritional Profile Dashboard",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Native Jetpack Compose Canvas Visualizations",
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
                // Empty state card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_empty_card")
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
                            text = "No Scanned Food Data Available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scan grocery items or populate sample scans to visualize sugar, saturated fat, and salt levels relative to WHO daily recommended intake guidelines.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onSeedSamples,
                            modifier = Modifier.fillMaxWidth().testTag("dashboard_seed_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.populate_sample_scans))
                        }
                    }
                }
            } else {
                // Product selector carousel
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

                Spacer(modifier = Modifier.height(20.dp))

                if (activeProduct != null) {
                    // Active Product Header Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProductClick(activeProduct.barcode) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeProduct.brand,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeProduct.name,
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
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = stringResource(R.string.inspect),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Visualization 1: Daily Recommended Intake Progress Bar Gauges
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
                    Spacer(modifier = Modifier.height(12.dp))

                    DailyValuesGaugeCard(nutriments = activeProduct.nutriments)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Visualization 2: Native Compose Canvas Donut Chart for Macronutrients Ratio
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
                    Spacer(modifier = Modifier.height(12.dp))

                    MacroDonutChartCard(nutriments = activeProduct.nutriments)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Visualization 3: Overall Scan History Health Distribution
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
                    Spacer(modifier = Modifier.height(12.dp))

                    HistoryQualityGaugeCard(products = products)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DailyValuesGaugeCard(nutriments: NutrimentsInfo) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Sugar Gauge
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

            // Saturated Fat Gauge
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

            // Salt Gauge
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

            // Fiber Gauge (Positive Goal)
            val fiber = nutriments.fiber ?: 0.0
            val fiberPct = (fiber / DailyValues.FIBER_GOAL_G).toFloat()
            DailyValueProgressBar(
                label = stringResource(R.string.gauge_fiber),
                valueGrams = fiber,
                maxGrams = DailyValues.FIBER_GOAL_G,
                progressFraction = fiberPct,
                barColor = Color(0xFF1976D2),
                warningNote = null
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Protein Gauge (Positive Goal)
            val protein = nutriments.proteins ?: 0.0
            val proteinPct = (protein / DailyValues.PROTEIN_GOAL_G).toFloat()
            DailyValueProgressBar(
                label = stringResource(R.string.gauge_protein),
                valueGrams = protein,
                maxGrams = DailyValues.PROTEIN_GOAL_G,
                progressFraction = proteinPct,
                barColor = Color(0xFF388E3C),
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
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${"%.1f".format(valueGrams)}g / ${maxGrams.toInt()}g ($pctInt% DV)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = barColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress track canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val width = size.width
            val height = size.height

            val fillWidth = (width * animatedProgress.coerceAtMost(1.0f))

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(barColor.copy(alpha = 0.75f), barColor)
                ),
                size = Size(fillWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
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

@Composable
private fun MacroDonutChartCard(nutriments: NutrimentsInfo) {
    val carbs = nutriments.carbs ?: 0.0
    val fat = nutriments.fat ?: 0.0
    val protein = nutriments.proteins ?: 0.0
    val total = (carbs + fat + protein).coerceAtLeast(0.1)

    val carbsPct = (carbs / total * 100).toInt()
    val fatPct = (fat / total * 100).toInt()
    val proteinPct = (protein / total * 100).toInt()

    val carbColor = Color(0xFFFFA726)
    val fatColor = Color(0xFFEF5350)
    val proteinColor = Color(0xFF66BB6A)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Native Donut Canvas
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val strokeWidth = 18.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    var startAngle = -90f

                    // Carbs Arc
                    val carbsSweep = (carbs / total * 360f).toFloat()
                    drawArc(
                        color = carbColor,
                        startAngle = startAngle,
                        sweepAngle = carbsSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += carbsSweep

                    // Fat Arc
                    val fatSweep = (fat / total * 360f).toFloat()
                    drawArc(
                        color = fatColor,
                        startAngle = startAngle,
                        sweepAngle = fatSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += fatSweep

                    // Protein Arc
                    val proteinSweep = (protein / total * 360f).toFloat()
                    drawArc(
                        color = proteinColor,
                        startAngle = startAngle,
                        sweepAngle = proteinSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${total.toInt()}g",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Macros",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Legend
            Column(modifier = Modifier.weight(1f)) {
                MacroLegendRow(
                    label = "Carbohydrates",
                    valueGrams = carbs,
                    percentage = carbsPct,
                    color = carbColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                MacroLegendRow(
                    label = "Total Fats",
                    valueGrams = fat,
                    percentage = fatPct,
                    color = fatColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                MacroLegendRow(
                    label = "Proteins",
                    valueGrams = protein,
                    percentage = proteinPct,
                    color = proteinColor
                )
            }
        }
    }
}

@Composable
private fun MacroLegendRow(label: String, valueGrams: Double, percentage: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "${"%.1f".format(valueGrams)}g ($percentage%)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun HistoryQualityGaugeCard(products: List<Product>) {
    val total = products.size.coerceAtLeast(1)
    val nova4Count = products.count { it.novaGroup == 4 }
    val cleanCount = products.count { it.novaGroup in 1..2 }
    val palmOilCount = products.count { it.palmOilStatus == com.example.model.PalmOilStatus.CONTAINS }

    val cleanPct = (cleanCount * 100) / total
    val nova4Pct = (nova4Count * 100) / total

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Clean / Minimal Foods",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$cleanCount of $total products (NOVA 1-2)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$cleanPct%",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Nova1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ultra-Processed Foods",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$nova4Count of $total products (NOVA 4)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$nova4Pct%",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Nova4
                )
            }
        }
    }
}
