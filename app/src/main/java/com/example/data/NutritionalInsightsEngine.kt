package com.example.data

import com.example.model.NutrimentsInfo
import com.example.model.Product
import kotlin.math.roundToInt

enum class InsightTone {
    POSITIVE,
    BALANCED,
    CAUTION,
    WATCH_OUT
}

enum class InsightCategory {
    ENERGY,
    PROTEIN,
    CARBS_SUGAR,
    FATS,
    FIBER,
    SODIUM,
    BALANCE
}

enum class DietFitTag {
    HIGH_PROTEIN,
    HIGH_FIBER,
    LOW_SUGAR,
    LOW_CARB,
    HEART_SMART_FATS,
    ENERGY_DENSE,
    LIGHT_OPTION,
    BALANCED_MACROS
}

enum class InsightCode {
    HIGH_PROTEIN_SATIETY,
    LOW_PROTEIN_PAIRING,
    HIGH_SUGAR_SPIKE,
    LOW_SUGAR_CHOICE,
    COMPLEX_CARB_FUEL,
    HIGH_SAT_FAT,
    HEALTHY_FAT_RATIO,
    HIGH_FIBER_DIGESTION,
    LOW_FIBER_REFINED,
    HIGH_SODIUM_ALERT,
    HIGH_ENERGY_DENSITY,
    LIGHT_ENERGY_PROFILE,
    BALANCED_MACROS_OVERVIEW,
    INSUFFICIENT_MACRO_DATA
}

data class NutritionalInsight(
    val code: InsightCode,
    val category: InsightCategory,
    val tone: InsightTone,
    val metricBadge: String
)

data class NutritionalInsightsSummary(
    val balanceScore: Int,
    val dietFitTags: List<DietFitTag>,
    val insights: List<NutritionalInsight>,
    val carbsCaloriePct: Int,
    val fatCaloriePct: Int,
    val proteinCaloriePct: Int,
    val hasMacroData: Boolean
)

enum class PortfolioInsightCode {
    HIGH_SUGAR_BASKET,
    HIGH_SAT_FAT_BASKET,
    PROTEIN_RICH_BASKET,
    FIBER_RICH_BASKET,
    BALANCED_BASKET
}

data class PortfolioInsightSummary(
    val code: PortfolioInsightCode,
    val tone: InsightTone,
    val highlightValue: String
)

/**
 * Deterministic, offline-first macronutrient analysis engine that transforms
 * Open Food Facts / Room per-100g nutriments into practical dietary advice.
 */
object NutritionalInsightsEngine {

    fun analyze(nutriments: NutrimentsInfo): NutritionalInsightsSummary {
        val carbs = nutriments.carbs?.coerceAtLeast(0.0)
        val sugars = nutriments.sugars?.coerceAtLeast(0.0)
        val fat = nutriments.fat?.coerceAtLeast(0.0)
        val satFat = nutriments.saturatedFat?.coerceAtLeast(0.0)
        val protein = nutriments.proteins?.coerceAtLeast(0.0)
        val fiber = nutriments.fiber?.coerceAtLeast(0.0)
        val salt = nutriments.salt?.coerceAtLeast(0.0)
        val kcal = nutriments.energyKcal?.coerceAtLeast(0.0)

        val hasAnyMacro = listOf(carbs, fat, protein, kcal).any { it != null && it > 0.0 }
        if (!hasAnyMacro) {
            return NutritionalInsightsSummary(
                balanceScore = 0,
                dietFitTags = emptyList(),
                insights = listOf(
                    NutritionalInsight(
                        code = InsightCode.INSUFFICIENT_MACRO_DATA,
                        category = InsightCategory.BALANCE,
                        tone = InsightTone.BALANCED,
                        metricBadge = "--"
                    )
                ),
                carbsCaloriePct = 0,
                fatCaloriePct = 0,
                proteinCaloriePct = 0,
                hasMacroData = false
            )
        }

        val carbsVal = carbs ?: 0.0
        val fatVal = fat ?: 0.0
        val proteinVal = protein ?: 0.0

        // Atwater caloric shares (4 / 9 / 4 kcal per gram)
        val carbsKcal = carbsVal * 4.0
        val fatKcal = fatVal * 9.0
        val proteinKcal = proteinVal * 4.0
        val rawMacroKcal = carbsKcal + fatKcal + proteinKcal
        val effectiveKcal = kcal ?: rawMacroKcal

        val (carbsPct, fatPct, proteinPct) = if (rawMacroKcal > 0.0) {
            val c = ((carbsKcal / rawMacroKcal) * 100.0).roundToInt().coerceIn(0, 100)
            val f = ((fatKcal / rawMacroKcal) * 100.0).roundToInt().coerceIn(0, 100)
            val p = (100 - c - f).coerceIn(0, 100)
            Triple(c, f, p)
        } else {
            Triple(0, 0, 0)
        }

        var score = 72
        val insights = mutableListOf<NutritionalInsight>()
        val tags = mutableListOf<DietFitTag>()

        // 1. Protein evaluation
        if (proteinVal >= 10.0 || (proteinVal >= 5.0 && proteinPct >= 20)) {
            score += 12
            tags.add(DietFitTag.HIGH_PROTEIN)
            insights.add(
                NutritionalInsight(
                    code = InsightCode.HIGH_PROTEIN_SATIETY,
                    category = InsightCategory.PROTEIN,
                    tone = InsightTone.POSITIVE,
                    metricBadge = "${formatOneDecimal(proteinVal)}g • $proteinPct% kcal"
                )
            )
        } else if (proteinVal < 3.0 && effectiveKcal >= 120.0) {
            score -= 6
            insights.add(
                NutritionalInsight(
                    code = InsightCode.LOW_PROTEIN_PAIRING,
                    category = InsightCategory.PROTEIN,
                    tone = InsightTone.CAUTION,
                    metricBadge = "${formatOneDecimal(proteinVal)}g / 100g"
                )
            )
        }

        // 2. Sugars & Carbohydrates evaluation
        if (sugars != null && sugars >= 15.0) {
            score -= if (sugars >= 22.5) 22 else 14
            insights.add(
                NutritionalInsight(
                    code = InsightCode.HIGH_SUGAR_SPIKE,
                    category = InsightCategory.CARBS_SUGAR,
                    tone = if (sugars >= 22.5) InsightTone.WATCH_OUT else InsightTone.CAUTION,
                    metricBadge = "${formatOneDecimal(sugars)}g / 100g"
                )
            )
        } else if (sugars != null && sugars <= 5.0 && (carbsVal > 0.0 || effectiveKcal >= 30.0)) {
            score += 8
            tags.add(DietFitTag.LOW_SUGAR)
            insights.add(
                NutritionalInsight(
                    code = InsightCode.LOW_SUGAR_CHOICE,
                    category = InsightCategory.CARBS_SUGAR,
                    tone = InsightTone.POSITIVE,
                    metricBadge = "${formatOneDecimal(sugars)}g / 100g"
                )
            )
        }

        if (carbsVal <= 10.0 && effectiveKcal >= 40.0) {
            tags.add(DietFitTag.LOW_CARB)
        } else if (carbsVal >= 20.0 && (sugars == null || sugars <= carbsVal * 0.25) && (fiber != null && fiber >= 3.0)) {
            score += 8
            insights.add(
                NutritionalInsight(
                    code = InsightCode.COMPLEX_CARB_FUEL,
                    category = InsightCategory.CARBS_SUGAR,
                    tone = InsightTone.POSITIVE,
                    metricBadge = "${formatOneDecimal(carbsVal)}g / 100g"
                )
            )
        }

        // 3. Dietary Fiber evaluation
        if (fiber != null && fiber >= 6.0) {
            score += 12
            tags.add(DietFitTag.HIGH_FIBER)
            insights.add(
                NutritionalInsight(
                    code = InsightCode.HIGH_FIBER_DIGESTION,
                    category = InsightCategory.FIBER,
                    tone = InsightTone.POSITIVE,
                    metricBadge = "${formatOneDecimal(fiber)}g / 100g"
                )
            )
        } else if (carbsVal >= 25.0 && fiber != null && fiber < 2.0) {
            score -= 8
            insights.add(
                NutritionalInsight(
                    code = InsightCode.LOW_FIBER_REFINED,
                    category = InsightCategory.FIBER,
                    tone = InsightTone.CAUTION,
                    metricBadge = "${formatOneDecimal(fiber)}g / 100g"
                )
            )
        }

        // 4. Fat & Saturated Fat quality evaluation
        if (satFat != null && satFat >= 5.0) {
            score -= if (satFat >= 10.0) 18 else 12
            insights.add(
                NutritionalInsight(
                    code = InsightCode.HIGH_SAT_FAT,
                    category = InsightCategory.FATS,
                    tone = if (satFat >= 10.0) InsightTone.WATCH_OUT else InsightTone.CAUTION,
                    metricBadge = "${formatOneDecimal(satFat)}g / 100g"
                )
            )
        } else if (fatVal >= 5.0 && satFat != null && satFat <= fatVal * 0.25) {
            score += 8
            tags.add(DietFitTag.HEART_SMART_FATS)
            insights.add(
                NutritionalInsight(
                    code = InsightCode.HEALTHY_FAT_RATIO,
                    category = InsightCategory.FATS,
                    tone = InsightTone.POSITIVE,
                    metricBadge = "${formatOneDecimal(satFat)}g / ${formatOneDecimal(fatVal)}g"
                )
            )
        }

        // 5. Salt / Sodium check
        if (salt != null && salt >= 1.5) {
            score -= 12
            insights.add(
                NutritionalInsight(
                    code = InsightCode.HIGH_SODIUM_ALERT,
                    category = InsightCategory.SODIUM,
                    tone = InsightTone.WATCH_OUT,
                    metricBadge = "${formatOneDecimal(salt)}g / 100g"
                )
            )
        }

        // 6. Caloric Density check
        if (effectiveKcal >= 400.0) {
            score -= 6
            tags.add(DietFitTag.ENERGY_DENSE)
            insights.add(
                NutritionalInsight(
                    code = InsightCode.HIGH_ENERGY_DENSITY,
                    category = InsightCategory.ENERGY,
                    tone = InsightTone.CAUTION,
                    metricBadge = "${effectiveKcal.roundToInt()} kcal"
                )
            )
        } else if (effectiveKcal in 1.0..120.0) {
            score += 6
            tags.add(DietFitTag.LIGHT_OPTION)
            insights.add(
                NutritionalInsight(
                    code = InsightCode.LIGHT_ENERGY_PROFILE,
                    category = InsightCategory.ENERGY,
                    tone = InsightTone.POSITIVE,
                    metricBadge = "${effectiveKcal.roundToInt()} kcal"
                )
            )
        }

        val finalScore = score.coerceIn(15, 100)

        if (insights.isEmpty() || (carbsPct in 30..65 && fatPct in 15..35 && proteinPct in 15..40)) {
            if (!tags.contains(DietFitTag.BALANCED_MACROS) && finalScore >= 70) {
                tags.add(0, DietFitTag.BALANCED_MACROS)
            }
            if (insights.isEmpty()) {
                insights.add(
                    NutritionalInsight(
                        code = InsightCode.BALANCED_MACROS_OVERVIEW,
                        category = InsightCategory.BALANCE,
                        tone = InsightTone.BALANCED,
                        metricBadge = "${effectiveKcal.roundToInt()} kcal"
                    )
                )
            }
        }

        // Prioritize the most impactful watch-outs and positive highlights (up to 5 items)
        val sortedInsights = insights
            .sortedBy {
                when (it.tone) {
                    InsightTone.WATCH_OUT -> 0
                    InsightTone.POSITIVE -> 1
                    InsightTone.CAUTION -> 2
                    InsightTone.BALANCED -> 3
                }
            }
            .take(5)

        return NutritionalInsightsSummary(
            balanceScore = finalScore,
            dietFitTags = tags.distinct().take(4),
            insights = sortedInsights,
            carbsCaloriePct = carbsPct,
            fatCaloriePct = fatPct,
            proteinCaloriePct = proteinPct,
            hasMacroData = true
        )
    }

    /**
     * Generates a concise portfolio-level dietary insight across all scanned products in Room.
     */
    fun analyzePortfolio(products: List<Product>): PortfolioInsightSummary? {
        if (products.isEmpty()) return null
        val count = products.size.toDouble()
        val avgSugars = products.sumOf { (it.nutriments.sugars ?: 0.0).coerceAtLeast(0.0) } / count
        val avgSatFat = products.sumOf { (it.nutriments.saturatedFat ?: 0.0).coerceAtLeast(0.0) } / count
        val avgProtein = products.sumOf { (it.nutriments.proteins ?: 0.0).coerceAtLeast(0.0) } / count
        val avgFiber = products.sumOf { (it.nutriments.fiber ?: 0.0).coerceAtLeast(0.0) } / count

        return when {
            avgSugars >= 15.0 -> PortfolioInsightSummary(
                code = PortfolioInsightCode.HIGH_SUGAR_BASKET,
                tone = InsightTone.WATCH_OUT,
                highlightValue = "${formatOneDecimal(avgSugars)}g"
            )
            avgSatFat >= 5.0 -> PortfolioInsightSummary(
                code = PortfolioInsightCode.HIGH_SAT_FAT_BASKET,
                tone = InsightTone.CAUTION,
                highlightValue = "${formatOneDecimal(avgSatFat)}g"
            )
            avgProtein >= 10.0 -> PortfolioInsightSummary(
                code = PortfolioInsightCode.PROTEIN_RICH_BASKET,
                tone = InsightTone.POSITIVE,
                highlightValue = "${formatOneDecimal(avgProtein)}g"
            )
            avgFiber >= 5.0 -> PortfolioInsightSummary(
                code = PortfolioInsightCode.FIBER_RICH_BASKET,
                tone = InsightTone.POSITIVE,
                highlightValue = "${formatOneDecimal(avgFiber)}g"
            )
            else -> PortfolioInsightSummary(
                code = PortfolioInsightCode.BALANCED_BASKET,
                tone = InsightTone.BALANCED,
                highlightValue = "${products.size}"
            )
        }
    }

    private fun formatOneDecimal(value: Double): String = "%.1f".format(value)
}
