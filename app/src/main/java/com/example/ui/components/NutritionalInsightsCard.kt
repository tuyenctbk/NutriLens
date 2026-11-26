package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DietFitTag
import com.example.data.InsightCode
import com.example.data.InsightTone
import com.example.data.NutritionalInsight
import com.example.data.NutritionalInsightsEngine
import com.example.data.PortfolioInsightCode
import com.example.model.NutrimentsInfo
import com.example.model.Product
import com.example.ui.theme.semanticCautionColors
import com.example.ui.theme.semanticDangerColors
import com.example.ui.theme.semanticSafeColors

/**
 * Displays actionable, localized dietary advice derived from a product's per-100g macronutrients.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionalInsightsCard(
    nutriments: NutrimentsInfo,
    modifier: Modifier = Modifier
) {
    val summary = remember(nutriments) { NutritionalInsightsEngine.analyze(nutriments) }
    val (safeBg, safeFg, _) = semanticSafeColors()
    val (cautionBg, cautionFg, _) = semanticCautionColors()
    val (dangerBg, dangerFg, _) = semanticDangerColors()

    val animatedScore by animateIntAsState(
        targetValue = summary.balanceScore,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "insight_balance_score"
    )

    val (scoreBg, scoreFg) = when {
        !summary.hasMacroData -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
        summary.balanceScore >= 75 -> safeBg to safeFg
        summary.balanceScore >= 50 -> cautionBg to cautionFg
        else -> dangerBg to dangerFg
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .testTag("nutritional_insights_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row + Macro Balance Score badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TipsAndUpdates,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.insights_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.insights_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (summary.hasMacroData) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = scoreBg,
                        modifier = Modifier.testTag("insights_balance_score_badge")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$animatedScore",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = scoreFg
                            )
                            Text(
                                text = stringResource(R.string.insights_balance_score_label),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scoreFg
                            )
                        }
                    }
                }
            }

            // Diet Fit Tags
            if (summary.dietFitTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    summary.dietFitTags.forEach { tag ->
                        DietFitTagPill(tag = tag)
                    }
                }
            }

            // Compact Caloric Split Bar (Carbs / Fat / Protein %)
            if (summary.hasMacroData) {
                Spacer(modifier = Modifier.height(12.dp))
                CaloricSplitBar(
                    carbsPct = summary.carbsCaloriePct,
                    fatPct = summary.fatCaloriePct,
                    proteinPct = summary.proteinCaloriePct
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actionable Dietary Advice Rows
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                summary.insights.forEach { insight ->
                    InsightAdviceRow(insight = insight)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.insights_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DietFitTagPill(tag: DietFitTag) {
    val (safeBg, safeFg, _) = semanticSafeColors()
    val (cautionBg, cautionFg, _) = semanticCautionColors()

    val (bg, fg, labelRes) = when (tag) {
        DietFitTag.HIGH_PROTEIN -> Triple(safeBg, safeFg, R.string.insight_tag_high_protein)
        DietFitTag.HIGH_FIBER -> Triple(safeBg, safeFg, R.string.insight_tag_high_fiber)
        DietFitTag.LOW_SUGAR -> Triple(safeBg, safeFg, R.string.insight_tag_low_sugar)
        DietFitTag.LOW_CARB -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, R.string.insight_tag_low_carb)
        DietFitTag.HEART_SMART_FATS -> Triple(safeBg, safeFg, R.string.insight_tag_heart_smart_fats)
        DietFitTag.ENERGY_DENSE -> Triple(cautionBg, cautionFg, R.string.insight_tag_energy_dense)
        DietFitTag.LIGHT_OPTION -> Triple(safeBg, safeFg, R.string.insight_tag_light_option)
        DietFitTag.BALANCED_MACROS -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, R.string.insight_tag_balanced)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CaloricSplitBar(
    carbsPct: Int,
    fatPct: Int,
    proteinPct: Int
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.insights_energy_split_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.insights_energy_split_values, carbsPct, fatPct, proteinPct),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(trackColor)
        ) {
            val total = (carbsPct + fatPct + proteinPct).coerceAtLeast(1).toFloat()
            val gap = 1.5.dp.toPx()
            var x = 0f

            val segments = listOf(
                (carbsPct / total) to ChartCarbColor,
                (fatPct / total) to ChartFatColor,
                (proteinPct / total) to ChartProteinColor
            )
            segments.forEach { (fraction, color) ->
                val w = size.width * fraction
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
    }
}

@Composable
private fun InsightAdviceRow(insight: NutritionalInsight) {
    val (safeBg, safeFg, _) = semanticSafeColors()
    val (cautionBg, cautionFg, _) = semanticCautionColors()
    val (dangerBg, dangerFg, _) = semanticDangerColors()

    val (iconBg, iconFg, icon) = when (insight.tone) {
        InsightTone.POSITIVE -> Triple(safeBg, safeFg, Icons.Filled.CheckCircle)
        InsightTone.BALANCED -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Filled.Info
        )
        InsightTone.CAUTION -> Triple(cautionBg, cautionFg, Icons.Filled.Lightbulb)
        InsightTone.WATCH_OUT -> Triple(dangerBg, dangerFg, Icons.Filled.Warning)
    }

    val (titleRes, bodyRes) = insightStringResources(insight.code)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.75.dp,
                color = iconFg.copy(alpha = 0.22f),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("insight_item_${insight.code.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconFg,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (insight.metricBadge != "--") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = iconBg
                        ) {
                            Text(
                                text = insight.metricBadge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = iconFg,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(bodyRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

/**
 * Compact portfolio-wide insight banner for the Dashboard summarizing overall scanned food trends.
 */
@Composable
fun PortfolioNutritionalInsightBanner(
    products: List<Product>,
    modifier: Modifier = Modifier
) {
    val portfolioInsight = remember(products) { NutritionalInsightsEngine.analyzePortfolio(products) }
        ?: return

    val (safeBg, safeFg, _) = semanticSafeColors()
    val (cautionBg, cautionFg, _) = semanticCautionColors()
    val (dangerBg, dangerFg, _) = semanticDangerColors()

    val (bg, fg) = when (portfolioInsight.tone) {
        InsightTone.POSITIVE -> safeBg to safeFg
        InsightTone.BALANCED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) to MaterialTheme.colorScheme.primary
        InsightTone.CAUTION -> cautionBg to cautionFg
        InsightTone.WATCH_OUT -> dangerBg to dangerFg
    }

    val messageRes = when (portfolioInsight.code) {
        PortfolioInsightCode.HIGH_SUGAR_BASKET -> R.string.portfolio_insight_high_sugar
        PortfolioInsightCode.HIGH_SAT_FAT_BASKET -> R.string.portfolio_insight_high_sat_fat
        PortfolioInsightCode.PROTEIN_RICH_BASKET -> R.string.portfolio_insight_protein_rich
        PortfolioInsightCode.FIBER_RICH_BASKET -> R.string.portfolio_insight_fiber_rich
        PortfolioInsightCode.BALANCED_BASKET -> R.string.portfolio_insight_balanced
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_portfolio_insight_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Insights,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.portfolio_insight_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = fg
                )
                Text(
                    text = stringResource(messageRes, portfolioInsight.highlightValue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun insightStringResources(code: InsightCode): Pair<Int, Int> = when (code) {
    InsightCode.HIGH_PROTEIN_SATIETY ->
        R.string.insight_high_protein_title to R.string.insight_high_protein_body
    InsightCode.LOW_PROTEIN_PAIRING ->
        R.string.insight_low_protein_title to R.string.insight_low_protein_body
    InsightCode.HIGH_SUGAR_SPIKE ->
        R.string.insight_high_sugar_title to R.string.insight_high_sugar_body
    InsightCode.LOW_SUGAR_CHOICE ->
        R.string.insight_low_sugar_title to R.string.insight_low_sugar_body
    InsightCode.COMPLEX_CARB_FUEL ->
        R.string.insight_complex_carb_title to R.string.insight_complex_carb_body
    InsightCode.HIGH_SAT_FAT ->
        R.string.insight_high_sat_fat_title to R.string.insight_high_sat_fat_body
    InsightCode.HEALTHY_FAT_RATIO ->
        R.string.insight_healthy_fat_title to R.string.insight_healthy_fat_body
    InsightCode.HIGH_FIBER_DIGESTION ->
        R.string.insight_high_fiber_title to R.string.insight_high_fiber_body
    InsightCode.LOW_FIBER_REFINED ->
        R.string.insight_low_fiber_title to R.string.insight_low_fiber_body
    InsightCode.HIGH_SODIUM_ALERT ->
        R.string.insight_high_sodium_title to R.string.insight_high_sodium_body
    InsightCode.HIGH_ENERGY_DENSITY ->
        R.string.insight_high_energy_title to R.string.insight_high_energy_body
    InsightCode.LIGHT_ENERGY_PROFILE ->
        R.string.insight_light_energy_title to R.string.insight_light_energy_body
    InsightCode.BALANCED_MACROS_OVERVIEW ->
        R.string.insight_balanced_title to R.string.insight_balanced_body
    InsightCode.INSUFFICIENT_MACRO_DATA ->
        R.string.insight_no_data_title to R.string.insight_no_data_body
}
