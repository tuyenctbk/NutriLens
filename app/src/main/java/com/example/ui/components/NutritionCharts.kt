package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import com.example.ui.theme.Nova3
import com.example.ui.theme.Nova4
import kotlin.math.roundToInt

// Vivid accessible chart palette aligned with NutriLens botanical theme
val ChartCarbColor = Color(0xFFF59E0B)     // Warm Amber
val ChartSugarColor = Color(0xFFEA580C)    // Deep Orange
val ChartFatColor = Color(0xFFEF4444)      // Coral Crimson
val ChartSatFatColor = Color(0xFFB91C1C)   // Deep Crimson
val ChartProteinColor = Color(0xFF10B981)  // Botanical Emerald
val ChartFiberColor = Color(0xFF0EA5E9)    // Clear Sky Blue

enum class MacroMetricMode {
    GRAMS,
    CALORIES
}

enum class MultiProductChartMode {
    STACKED_MACROS,
    CALORIES
}

data class MacroEnergyBreakdown(
    val carbsGrams: Double,
    val fatGrams: Double,
    val proteinGrams: Double,
    val fiberGrams: Double,
    val sugarsGrams: Double,
    val satFatGrams: Double,
    val carbsKcal: Double,
    val fatKcal: Double,
    val proteinKcal: Double,
    val totalGrams: Double,
    val totalMacroKcal: Double,
    val reportedKcal: Double
)

object NutritionMath {
    fun computeMacroBreakdown(nutriments: NutrimentsInfo): MacroEnergyBreakdown {
        val carbs = (nutriments.carbs ?: 0.0).coerceAtLeast(0.0)
        val fat = (nutriments.fat ?: 0.0).coerceAtLeast(0.0)
        val protein = (nutriments.proteins ?: 0.0).coerceAtLeast(0.0)
        val fiber = (nutriments.fiber ?: 0.0).coerceAtLeast(0.0)
        val sugars = (nutriments.sugars ?: 0.0).coerceAtLeast(0.0).coerceAtMost(carbs.coerceAtLeast(nutriments.sugars ?: 0.0))
        val satFat = (nutriments.saturatedFat ?: 0.0).coerceAtLeast(0.0).coerceAtMost(fat.coerceAtLeast(nutriments.saturatedFat ?: 0.0))

        // Atwater general factors: 4 kcal/g carbs, 9 kcal/g fat, 4 kcal/g protein
        val carbsKcal = carbs * 4.0
        val fatKcal = fat * 9.0
        val proteinKcal = protein * 4.0
        val totalGrams = carbs + fat + protein
        val totalMacroKcal = carbsKcal + fatKcal + proteinKcal
        val reportedKcal = nutriments.energyKcal ?: totalMacroKcal

        return MacroEnergyBreakdown(
            carbsGrams = carbs,
            fatGrams = fat,
            proteinGrams = protein,
            fiberGrams = fiber,
            sugarsGrams = sugars,
            satFatGrams = satFat,
            carbsKcal = carbsKcal,
            fatKcal = fatKcal,
            proteinKcal = proteinKcal,
            totalGrams = totalGrams,
            totalMacroKcal = totalMacroKcal,
            reportedKcal = reportedKcal
        )
    }

    data class PortfolioAverages(
        val avgKcal: Double,
        val avgCarbs: Double,
        val avgFat: Double,
        val avgProtein: Double
    )

    fun computePortfolioAverages(products: List<Product>): PortfolioAverages {
        if (products.isEmpty()) return PortfolioAverages(0.0, 0.0, 0.0, 0.0)
        val count = products.size.toDouble()
        return PortfolioAverages(
            avgKcal = products.sumOf { it.nutriments.energyKcal ?: 0.0 } / count,
            avgCarbs = products.sumOf { it.nutriments.carbs ?: 0.0 } / count,
            avgFat = products.sumOf { it.nutriments.fat ?: 0.0 } / count,
            avgProtein = products.sumOf { it.nutriments.proteins ?: 0.0 } / count
        )
    }
}

/**
 * Portfolio KPI Summary Strip across all scanned Open Food Facts items.
 */
@Composable
fun PortfolioMacroKpiStrip(
    products: List<Product>,
    modifier: Modifier = Modifier
) {
    val averages = remember(products) { NutritionMath.computePortfolioAverages(products) }

    val kcalUnit = stringResource(R.string.unit_kcal)
    val gramsUnit = stringResource(R.string.unit_g)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_portfolio_kpi_strip"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiMiniCard(
            title = stringResource(R.string.dashboard_kpi_avg_kcal),
            value = "${averages.avgKcal.roundToInt()}",
            unit = kcalUnit,
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        KpiMiniCard(
            title = stringResource(R.string.dashboard_kpi_avg_carbs),
            value = "%.1f".format(averages.avgCarbs),
            unit = gramsUnit,
            accentColor = ChartCarbColor,
            modifier = Modifier.weight(1f)
        )
        KpiMiniCard(
            title = stringResource(R.string.dashboard_kpi_avg_fat),
            value = "%.1f".format(averages.avgFat),
            unit = gramsUnit,
            accentColor = ChartFatColor,
            modifier = Modifier.weight(1f)
        )
        KpiMiniCard(
            title = stringResource(R.string.dashboard_kpi_avg_protein),
            value = "%.1f".format(averages.avgProtein),
            unit = gramsUnit,
            accentColor = ChartProteinColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KpiMiniCard(
    title: String,
    value: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Interactive Macronutrient Breakdown Card with Weight (g) vs Caloric Share (kcal) toggle,
 * tap-to-inspect animated Donut Chart, and sub-macro stacked composition bar.
 */
@Composable
fun InteractiveMacroBreakdownCard(
    nutriments: NutrimentsInfo,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(MacroMetricMode.GRAMS) }
    // -1 = Overview, 0 = Carbs, 1 = Fat, 2 = Protein
    var selectedSlice by remember { mutableIntStateOf(-1) }

    val breakdown = remember(nutriments) { NutritionMath.computeMacroBreakdown(nutriments) }

    val carbsVal = if (mode == MacroMetricMode.GRAMS) breakdown.carbsGrams else breakdown.carbsKcal
    val fatVal = if (mode == MacroMetricMode.GRAMS) breakdown.fatGrams else breakdown.fatKcal
    val proteinVal = if (mode == MacroMetricMode.GRAMS) breakdown.proteinGrams else breakdown.proteinKcal
    val totalVal = (carbsVal + fatVal + proteinVal).coerceAtLeast(0.01)
    val hasData = (carbsVal + fatVal + proteinVal) > 0.0

    val carbsPct = if (hasData) ((carbsVal / totalVal) * 100).roundToInt() else 0
    val fatPct = if (hasData) ((fatVal / totalVal) * 100).roundToInt() else 0
    val proteinPct = if (hasData) (100 - carbsPct - fatPct).coerceAtLeast(0) else 0

    val animatedCarbsSweep by animateFloatAsState(
        targetValue = if (hasData) (carbsVal / totalVal * 360f).toFloat() else 0f,
        animationSpec = tween(680, easing = FastOutSlowInEasing),
        label = "macro_carbs_sweep"
    )
    val animatedFatSweep by animateFloatAsState(
        targetValue = if (hasData) (fatVal / totalVal * 360f).toFloat() else 0f,
        animationSpec = tween(680, easing = FastOutSlowInEasing),
        label = "macro_fat_sweep"
    )
    val animatedProteinSweep by animateFloatAsState(
        targetValue = if (hasData) (proteinVal / totalVal * 360f).toFloat() else 0f,
        animationSpec = tween(680, easing = FastOutSlowInEasing),
        label = "macro_protein_sweep"
    )

    val carbStrokeDp by animateDpAsState(
        targetValue = if (selectedSlice == 0) 21.dp else 15.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "carb_stroke"
    )
    val fatStrokeDp by animateDpAsState(
        targetValue = if (selectedSlice == 1) 21.dp else 15.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fat_stroke"
    )
    val proteinStrokeDp by animateDpAsState(
        targetValue = if (selectedSlice == 2) 21.dp else 15.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "protein_stroke"
    )

    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
    val gramsUnit = stringResource(R.string.unit_g)
    val kcalUnit = stringResource(R.string.unit_kcal)
    val unitSuffix = if (mode == MacroMetricMode.GRAMS) gramsUnit else kcalUnit

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_macro_donut_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Mode Switcher Pills: Grams (g) vs Calories (kcal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = mode == MacroMetricMode.GRAMS,
                    onClick = { mode = MacroMetricMode.GRAMS },
                    label = { Text(stringResource(R.string.dashboard_mode_weight), fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("dashboard_mode_grams")
                )
                FilterChip(
                    selected = mode == MacroMetricMode.CALORIES,
                    onClick = { mode = MacroMetricMode.CALORIES },
                    label = { Text(stringResource(R.string.dashboard_mode_calories), fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("dashboard_mode_calories")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Interactive Donut Canvas
                Box(
                    modifier = Modifier
                        .size(134.dp)
                        .clickable { selectedSlice = -1 },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(134.dp)) {
                        val maxStroke = 21.dp.toPx()
                        val diameter = size.minDimension - maxStroke
                        val topLeft = Offset(maxStroke / 2f, maxStroke / 2f)
                        val arcSize = Size(diameter, diameter)

                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = 14.dp.toPx())
                        )

                        var startAngle = -90f
                        val gap = if (hasData) 2.5f else 0f

                        if (animatedCarbsSweep > 1f) {
                            drawArc(
                                color = ChartCarbColor.copy(alpha = if (selectedSlice == -1 || selectedSlice == 0) 1f else 0.35f),
                                startAngle = startAngle,
                                sweepAngle = (animatedCarbsSweep - gap).coerceAtLeast(0.5f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = carbStrokeDp.toPx(), cap = StrokeCap.Round)
                            )
                            startAngle += animatedCarbsSweep
                        }

                        if (animatedFatSweep > 1f) {
                            drawArc(
                                color = ChartFatColor.copy(alpha = if (selectedSlice == -1 || selectedSlice == 1) 1f else 0.35f),
                                startAngle = startAngle,
                                sweepAngle = (animatedFatSweep - gap).coerceAtLeast(0.5f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = fatStrokeDp.toPx(), cap = StrokeCap.Round)
                            )
                            startAngle += animatedFatSweep
                        }

                        if (animatedProteinSweep > 1f) {
                            drawArc(
                                color = ChartProteinColor.copy(alpha = if (selectedSlice == -1 || selectedSlice == 2) 1f else 0.35f),
                                startAngle = startAngle,
                                sweepAngle = (animatedProteinSweep - gap).coerceAtLeast(0.5f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = proteinStrokeDp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Dynamic Center Readout
                    AnimatedContent(
                        targetState = Pair(selectedSlice, mode),
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                        label = "donut_center"
                    ) { (slice, currentMode) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            when (slice) {
                                0 -> {
                                    Text(
                                        text = "$carbsPct%",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = ChartCarbColor
                                    )
                                    Text(
                                        text = stringResource(R.string.macro_carbs),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                1 -> {
                                    Text(
                                        text = "$fatPct%",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = ChartFatColor
                                    )
                                    Text(
                                        text = stringResource(R.string.macro_fats),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                2 -> {
                                    Text(
                                        text = "$proteinPct%",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = ChartProteinColor
                                    )
                                    Text(
                                        text = stringResource(R.string.macro_proteins),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                else -> {
                                    val centerMain = if (currentMode == MacroMetricMode.GRAMS) {
                                        "${breakdown.totalGrams.roundToInt()}$gramsUnit"
                                    } else {
                                        "${breakdown.reportedKcal.roundToInt()}"
                                    }
                                    Text(
                                        text = centerMain,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (currentMode == MacroMetricMode.GRAMS) {
                                            stringResource(R.string.macros)
                                        } else {
                                            stringResource(R.string.unit_kcal_per_100g)
                                        },
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Interactive Tap-to-Inspect Legend Tiles
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InteractiveMacroLegendTile(
                        label = stringResource(R.string.macro_carbs),
                        detail = "${stringResource(R.string.nutri_sugars)}: ${"%.1f".format(breakdown.sugarsGrams)}$gramsUnit",
                        valueText = "${"%.1f".format(carbsVal)}$unitSuffix",
                        percentage = carbsPct,
                        color = ChartCarbColor,
                        selected = selectedSlice == 0,
                        onClick = { selectedSlice = if (selectedSlice == 0) -1 else 0 }
                    )
                    InteractiveMacroLegendTile(
                        label = stringResource(R.string.macro_fats),
                        detail = "${stringResource(R.string.nutri_sat_fat)}: ${"%.1f".format(breakdown.satFatGrams)}$gramsUnit",
                        valueText = "${"%.1f".format(fatVal)}$unitSuffix",
                        percentage = fatPct,
                        color = ChartFatColor,
                        selected = selectedSlice == 1,
                        onClick = { selectedSlice = if (selectedSlice == 1) -1 else 1 }
                    )
                    InteractiveMacroLegendTile(
                        label = stringResource(R.string.macro_proteins),
                        detail = "${stringResource(R.string.nutri_fiber)}: ${"%.1f".format(breakdown.fiberGrams)}$gramsUnit",
                        valueText = "${"%.1f".format(proteinVal)}$unitSuffix",
                        percentage = proteinPct,
                        color = ChartProteinColor,
                        selected = selectedSlice == 2,
                        onClick = { selectedSlice = if (selectedSlice == 2) -1 else 2 }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-Macro Composition Stacked Strip (Sugars vs Other Carbs vs Sat Fat vs Unsat Fat vs Protein vs Fiber)
            Text(
                text = stringResource(R.string.dashboard_sub_macro_strip_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val otherCarbs = (breakdown.carbsGrams - breakdown.sugarsGrams).coerceAtLeast(0.0)
            val unsatFat = (breakdown.fatGrams - breakdown.satFatGrams).coerceAtLeast(0.0)
            val stripTotal = (breakdown.sugarsGrams + otherCarbs + breakdown.satFatGrams + unsatFat + breakdown.proteinGrams + breakdown.fiberGrams)
                .coerceAtLeast(0.1)

            val segments = listOf(
                breakdown.sugarsGrams / stripTotal to ChartSugarColor,
                otherCarbs / stripTotal to ChartCarbColor,
                breakdown.satFatGrams / stripTotal to ChartSatFatColor,
                unsatFat / stripTotal to ChartFatColor.copy(alpha = 0.72f),
                breakdown.proteinGrams / stripTotal to ChartProteinColor,
                breakdown.fiberGrams / stripTotal to ChartFiberColor
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(trackColor)
            ) {
                var xOffset = 0f
                val gapPx = 1.5.dp.toPx()
                segments.forEach { (fraction, color) ->
                    val segWidth = (size.width * fraction.toFloat()).coerceAtLeast(0f)
                    if (segWidth > 1f) {
                        drawRect(
                            color = color,
                            topLeft = Offset(xOffset, 0f),
                            size = Size((segWidth - gapPx).coerceAtLeast(1f), size.height)
                        )
                        xOffset += segWidth
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniLegendDot(stringResource(R.string.nutri_sugars), ChartSugarColor)
                MiniLegendDot(stringResource(R.string.dashboard_complex_carbs), ChartCarbColor)
                MiniLegendDot(stringResource(R.string.nutri_sat_fat), ChartSatFatColor)
                MiniLegendDot(stringResource(R.string.macro_proteins), ChartProteinColor)
                MiniLegendDot(stringResource(R.string.nutri_fiber), ChartFiberColor)
            }
        }
    }
}

@Composable
private fun InteractiveMacroLegendTile(
    label: String,
    detail: String,
    valueText: String,
    percentage: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 1.25.dp else 0.5.dp,
                color = if (selected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = detail,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = valueText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                    maxLines = 1
                )
                Text(
                    text = "$percentage%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MiniLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * Interactive Multi-Product Macronutrient & Calorie Bar Chart (native Compose Canvas).
 * Displays up to 8 scanned products side-by-side with gridlines, animated stacked macro bars
 * (Carbs + Fat + Protein) or Calorie bars, and tap-to-select cross-filtering.
 */
@Composable
fun MultiProductMacroBarChartCard(
    products: List<Product>,
    selectedBarcode: String?,
    onSelectProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var chartMode by remember { mutableStateOf(MultiProductChartMode.STACKED_MACROS) }
    var barsRevealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { barsRevealed = true }
    val chartProducts = remember(products) { products.take(8) }

    val maxScale = remember(chartProducts, chartMode) {
        when (chartMode) {
            MultiProductChartMode.STACKED_MACROS -> {
                chartProducts.maxOfOrNull {
                    (it.nutriments.carbs ?: 0.0) + (it.nutriments.fat ?: 0.0) + (it.nutriments.proteins ?: 0.0)
                }?.coerceAtLeast(20.0) ?: 100.0
            }
            MultiProductChartMode.CALORIES -> {
                chartProducts.maxOfOrNull {
                    it.nutriments.energyKcal ?: 0.0
                }?.coerceAtLeast(100.0) ?: 500.0
            }
        }
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_multi_product_chart")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = chartMode == MultiProductChartMode.STACKED_MACROS,
                    onClick = { chartMode = MultiProductChartMode.STACKED_MACROS },
                    label = { Text(stringResource(R.string.dashboard_chart_stacked_macros), fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("chart_mode_stacked_macros")
                )
                FilterChip(
                    selected = chartMode == MultiProductChartMode.CALORIES,
                    onClick = { chartMode = MultiProductChartMode.CALORIES },
                    label = { Text(stringResource(R.string.dashboard_chart_calories), fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("chart_mode_calories")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Y-axis legend & unit hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dashboard_tap_bar_hint),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (chartMode == MultiProductChartMode.STACKED_MACROS) {
                        stringResource(R.string.chart_max_grams, maxScale.roundToInt())
                    } else {
                        stringResource(R.string.chart_max_kcal, maxScale.roundToInt())
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart area with horizontal gridlines & scrollable interactive bars
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background horizontal dashed gridlines
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                ) {
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    listOf(0.05f, 0.35f, 0.65f, 0.96f).forEach { frac ->
                        val y = size.height * frac
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (frac < 0.95f) dashEffect else null
                        )
                    }
                }

                val gramsUnit = stringResource(R.string.unit_g)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartProducts.forEach { product ->
                        val isSelected = product.barcode == selectedBarcode
                        val carbs = (product.nutriments.carbs ?: 0.0).coerceAtLeast(0.0)
                        val fat = (product.nutriments.fat ?: 0.0).coerceAtLeast(0.0)
                        val protein = (product.nutriments.proteins ?: 0.0).coerceAtLeast(0.0)
                        val totalMacros = carbs + fat + protein
                        val kcal = (product.nutriments.energyKcal ?: 0.0).coerceAtLeast(0.0)

                        val topValueLabel = if (chartMode == MultiProductChartMode.STACKED_MACROS) {
                            "${totalMacros.roundToInt()}$gramsUnit"
                        } else {
                            "${kcal.roundToInt()}"
                        }

                        val animScale by animateFloatAsState(
                            targetValue = if (barsRevealed) 1f else 0f,
                            animationSpec = tween(680, easing = FastOutSlowInEasing),
                            label = "bar_anim_${product.barcode}"
                        )

                        val carbsRatio by animateFloatAsState(
                            targetValue = if (chartMode == MultiProductChartMode.STACKED_MACROS) {
                                (carbs / maxScale).toFloat().coerceIn(0f, 1f)
                            } else {
                                0f
                            },
                            animationSpec = tween(650, easing = FastOutSlowInEasing),
                            label = "bar_c_${product.barcode}"
                        )
                        val fatRatio by animateFloatAsState(
                            targetValue = if (chartMode == MultiProductChartMode.STACKED_MACROS) {
                                (fat / maxScale).toFloat().coerceIn(0f, 1f)
                            } else {
                                0f
                            },
                            animationSpec = tween(650, easing = FastOutSlowInEasing),
                            label = "bar_f_${product.barcode}"
                        )
                        val proteinRatio by animateFloatAsState(
                            targetValue = if (chartMode == MultiProductChartMode.STACKED_MACROS) {
                                (protein / maxScale).toFloat().coerceIn(0f, 1f)
                            } else {
                                0f
                            },
                            animationSpec = tween(650, easing = FastOutSlowInEasing),
                            label = "bar_p_${product.barcode}"
                        )
                        val kcalRatio by animateFloatAsState(
                            targetValue = if (chartMode == MultiProductChartMode.CALORIES) {
                                (kcal / maxScale).toFloat().coerceIn(0f, 1f)
                            } else {
                                0f
                            },
                            animationSpec = tween(650, easing = FastOutSlowInEasing),
                            label = "bar_k_${product.barcode}"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                                    else Color.Transparent
                                )
                                .clickable { onSelectProduct(product.barcode) }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .testTag("dashboard_bar_${product.barcode}")
                        ) {
                            Text(
                                text = topValueLabel,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Canvas(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(132.dp)
                            ) {
                                val barW = size.width
                                val maxH = size.height * animScale

                                if (chartMode == MultiProductChartMode.STACKED_MACROS) {
                                    val pH = maxH * proteinRatio
                                    val fH = maxH * fatRatio
                                    val cH = maxH * carbsRatio
                                    var bottomY = size.height

                                    if (pH > 1f) {
                                        drawRoundRect(
                                            color = ChartProteinColor,
                                            topLeft = Offset(0f, bottomY - pH),
                                            size = Size(barW, pH),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )
                                        bottomY -= pH
                                    }
                                    if (fH > 1f) {
                                        drawRoundRect(
                                            color = ChartFatColor,
                                            topLeft = Offset(0f, bottomY - fH),
                                            size = Size(barW, fH),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )
                                        bottomY -= fH
                                    }
                                    if (cH > 1f) {
                                        drawRoundRect(
                                            color = ChartCarbColor,
                                            topLeft = Offset(0f, bottomY - cH),
                                            size = Size(barW, cH),
                                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                        )
                                    }
                                } else {
                                    val kH = maxH * kcalRatio
                                    if (kH > 1f) {
                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    if (isSelected) primaryColor else ChartCarbColor,
                                                    if (isSelected) primaryColor.copy(alpha = 0.65f) else ChartFatColor.copy(alpha = 0.85f)
                                                )
                                            ),
                                            topLeft = Offset(0f, size.height - kH),
                                            size = Size(barW, kH),
                                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = product.name,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (chartMode == MultiProductChartMode.STACKED_MACROS) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
                ) {
                    MiniLegendDot(stringResource(R.string.macro_carbs), ChartCarbColor)
                    MiniLegendDot(stringResource(R.string.macro_fats), ChartFatColor)
                    MiniLegendDot(stringResource(R.string.macro_proteins), ChartProteinColor)
                }
            }
        }
    }
}

/**
 * Visual NOVA Processing Distribution Card with animated segmented horizontal chart.
 */
@Composable
fun NovaDistributionBarChartCard(
    products: List<Product>,
    modifier: Modifier = Modifier
) {
    val total = products.size.coerceAtLeast(1)
    val cleanCount = products.count { it.novaGroup in 1..2 }
    val processedCount = products.count { it.novaGroup == 3 }
    val ultraCount = products.count { it.novaGroup == 4 }
    val unratedCount = (products.size - cleanCount - processedCount - ultraCount).coerceAtLeast(0)

    val cleanPct = (cleanCount * 100) / total
    val processedPct = (processedCount * 100) / total
    val ultraPct = (ultraCount * 100) / total

    val cleanFrac by animateFloatAsState(
        targetValue = cleanCount.toFloat() / total.toFloat(),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "nova_clean"
    )
    val processedFrac by animateFloatAsState(
        targetValue = processedCount.toFloat() / total.toFloat(),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "nova_proc"
    )
    val ultraFrac by animateFloatAsState(
        targetValue = ultraCount.toFloat() / total.toFloat(),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "nova_ultra"
    )
    val unratedColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_nova_chart_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Segmented animated bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(unratedColor.copy(alpha = 0.35f))
            ) {
                var x = 0f
                val gap = 2.dp.toPx()
                listOf(
                    cleanFrac to Nova1,
                    processedFrac to Nova3,
                    ultraFrac to Nova4
                ).forEach { (frac, color) ->
                    val w = size.width * frac
                    if (w > 1f) {
                        drawRect(
                            color = color,
                            topLeft = Offset(x, 0f),
                            size = Size((w - gap).coerceAtLeast(1f), size.height)
                        )
                        x += w
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            NovaSummaryRow(
                title = stringResource(R.string.clean_minimal_foods),
                subtitle = stringResource(R.string.history_products_ratio_nova, cleanCount, total, "1-2"),
                percent = "$cleanPct%",
                color = Nova1
            )
            Spacer(modifier = Modifier.height(10.dp))
            NovaSummaryRow(
                title = stringResource(R.string.processed_food),
                subtitle = stringResource(R.string.history_products_ratio_nova, processedCount, total, "3"),
                percent = "$processedPct%",
                color = Nova3
            )
            Spacer(modifier = Modifier.height(10.dp))
            NovaSummaryRow(
                title = stringResource(R.string.ultra_processed_foods),
                subtitle = stringResource(R.string.history_products_ratio_nova, ultraCount, total, "4"),
                percent = "$ultraPct%",
                color = Nova4
            )
            if (unratedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${stringResource(R.string.processing_unrated)}: $unratedCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NovaSummaryRow(
    title: String,
    subtitle: String,
    percent: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = percent,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = color
        )
    }
}
