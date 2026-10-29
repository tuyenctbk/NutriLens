package com.example.data

import com.example.model.AdditiveDetail
import com.example.model.AdditiveRisk
import com.example.model.DietStatus
import com.example.model.HealthWarning
import com.example.model.PalmOilStatus
import com.example.model.Product
import com.example.model.UserProfile
import com.example.model.WarningSeverity
import java.util.Locale

object HealthAnalyzer {

    // Common standard allergen mapping
    val KNOWN_ALLERGENS = listOf(
        "Milk" to listOf("milk", "lait", "dairy", "whey", "casein", "lactose", "butter", "cheese", "cream", "en:milk"),
        "Gluten" to listOf("gluten", "wheat", "blé", "barley", "orge", "rye", "seigle", "oats", "avoine", "spelt", "en:gluten"),
        "Peanuts" to listOf("peanut", "arachide", "cacahuète", "groundnut", "en:peanuts"),
        "Tree Nuts" to listOf("nuts", "nut", "hazelnut", "noisette", "almond", "amande", "walnut", "noix", "cashew", "pistachio", "pecan", "en:nuts"),
        "Soybeans" to listOf("soy", "soja", "soybean", "soya", "tofu", "edamame", "en:soybeans"),
        "Eggs" to listOf("egg", "oeuf", "albumin", "lysozyme", "en:eggs"),
        "Fish" to listOf("fish", "poisson", "salmon", "tuna", "cod", "anchovy", "en:fish"),
        "Crustaceans" to listOf("crustacean", "crab", "shrimp", "lobster", "prawn", "crevette", "en:crustaceans"),
        "Sesame" to listOf("sesame", "sésame", "tahini", "en:sesame-seeds"),
        "Mustard" to listOf("mustard", "moutarde", "en:mustard"),
        "Celery" to listOf("celery", "céleri", "en:celery"),
        "Lupin" to listOf("lupin", "lupine", "en:lupin"),
        "Molluscs" to listOf("mollusc", "mollusk", "clam", "mussel", "oyster", "squid", "octopus", "en:molluscs"),
        "Sulphites" to listOf("sulphite", "sulfite", "sulfur", "dioxyde de soufre", "e220", "e221", "e222", "e223", "e224", "en:sulphur-dioxide-and-sulphites")
    )

    fun evaluate(product: Product, profile: UserProfile): List<HealthWarning> {
        val warnings = mutableListOf<HealthWarning>()

        // 1. Check Monitored Allergens
        val matchedAllergens = mutableListOf<String>()
        val matchedTraces = mutableListOf<String>()

        val productAllergensLower = product.allergens.map { it.lowercase(Locale.ROOT) }
        val productTracesLower = product.traces.map { it.lowercase(Locale.ROOT) }
        val ingredientsLower = (product.ingredientsText ?: "").lowercase(Locale.ROOT)

        for (allergenName in profile.monitoredAllergens) {
            val synonyms = KNOWN_ALLERGENS.find { it.first.equals(allergenName, ignoreCase = true) }?.second
                ?: listOf(allergenName.lowercase(Locale.ROOT))

            val inAllergens = productAllergensLower.any { tag -> synonyms.any { s -> tag.contains(s) } }
            val inIngredients = synonyms.any { s -> ingredientsLower.contains("\\b$s\\b".toRegex()) }

            if (inAllergens || inIngredients) {
                matchedAllergens.add(allergenName)
            } else {
                val inTraces = productTracesLower.any { tag -> synonyms.any { s -> tag.contains(s) } }
                if (inTraces) {
                    matchedTraces.add(allergenName)
                }
            }
        }

        if (matchedAllergens.isNotEmpty()) {
            warnings.add(
                HealthWarning(
                    title = "Allergen Alert Detected",
                    message = "Contains ${matchedAllergens.joinToString(", ")} which matches your sensitive allergen profile.",
                    severity = WarningSeverity.DANGER,
                    iconType = "allergen"
                )
            )
        }

        if (matchedTraces.isNotEmpty()) {
            warnings.add(
                HealthWarning(
                    title = "Possible Allergen Traces",
                    message = "May contain cross-contamination traces of: ${matchedTraces.joinToString(", ")}.",
                    severity = WarningSeverity.CAUTION,
                    iconType = "allergen"
                )
            )
        }

        // 2. Gluten-Free Preference
        if (profile.isGlutenFree) {
            val glutenSynonyms = listOf("gluten", "wheat", "blé", "barley", "orge", "rye", "seigle", "oats", "avoine", "spelt", "en:gluten")
            val hasGlutenTag = productAllergensLower.any { tag -> glutenSynonyms.any { s -> tag.contains(s) } }
            val hasGlutenText = glutenSynonyms.any { s -> ingredientsLower.contains("\\b$s\\b".toRegex()) }

            if (hasGlutenTag || hasGlutenText) {
                warnings.add(
                    HealthWarning(
                        title = "Gluten Detected (Not Gluten-Free)",
                        message = "Contains wheat, gluten, barley, or rye ingredients, violating your Strict Gluten-Free profile.",
                        severity = WarningSeverity.DANGER,
                        iconType = "allergen"
                    )
                )
            }
        }

        // 3. Custom Avoid Keywords Preference
        if (profile.customAvoidKeywords.isNotEmpty()) {
            val matchedKeywords = profile.customAvoidKeywords.filter { kw ->
                val cleanKw = kw.lowercase(Locale.ROOT)
                ingredientsLower.contains(cleanKw) ||
                    product.name.lowercase(Locale.ROOT).contains(cleanKw) ||
                    product.additives.any { it.code.lowercase(Locale.ROOT).contains(cleanKw) || it.name.lowercase(Locale.ROOT).contains(cleanKw) }
            }

            if (matchedKeywords.isNotEmpty()) {
                warnings.add(
                    HealthWarning(
                        title = "Custom Avoid Keyword Flagged",
                        message = "Found ingredient match for your custom avoid list: ${matchedKeywords.joinToString(", ")}.",
                        severity = WarningSeverity.CAUTION,
                        iconType = "additive"
                    )
                )
            }
        }

        // 4. Vegan / Vegetarian Preference
        if (profile.isVegan) {
            when (product.veganStatus) {
                DietStatus.NOT_SUITABLE -> {
                    warnings.add(
                        HealthWarning(
                            title = "Not Vegan Suitable",
                            message = "This product contains animal-derived ingredients or dairy/egg components.",
                            severity = WarningSeverity.DANGER,
                            iconType = "vegan"
                        )
                    )
                }
                DietStatus.MAYBE -> {
                    warnings.add(
                        HealthWarning(
                            title = "May Not Be Vegan",
                            message = "Some ingredients or additives might be sourced from animal origins.",
                            severity = WarningSeverity.CAUTION,
                            iconType = "vegan"
                        )
                    )
                }
                else -> {}
            }
        } else if (profile.isVegetarian) {
            when (product.vegetarianStatus) {
                DietStatus.NOT_SUITABLE -> {
                    warnings.add(
                        HealthWarning(
                            title = "Not Vegetarian",
                            message = "Contains meat, poultry, fish, gelatin, or animal rennet ingredients.",
                            severity = WarningSeverity.DANGER,
                            iconType = "vegetarian"
                        )
                    )
                }
                DietStatus.MAYBE -> {
                    warnings.add(
                        HealthWarning(
                            title = "Check Vegetarian Status",
                            message = "Contains ingredients that may be animal-derived.",
                            severity = WarningSeverity.CAUTION,
                            iconType = "vegetarian"
                        )
                    )
                }
                else -> {}
            }
        }

        // 3. Ultra-Processed Food (NOVA 4)
        if (profile.avoidNova4UltraProcessed && product.novaGroup == 4) {
            warnings.add(
                HealthWarning(
                    title = "Ultra-Processed (NOVA 4)",
                    message = "Industrial formulation with modified starches, flavorings, or industrial additives. Correlated with higher cardiovascular & metabolic risks.",
                    severity = WarningSeverity.CAUTION,
                    iconType = "nova"
                )
            )
        }

        // 4. Palm Oil Check
        if (profile.avoidPalmOil) {
            when (product.palmOilStatus) {
                PalmOilStatus.CONTAINS -> {
                    warnings.add(
                        HealthWarning(
                            title = "Contains Palm Oil",
                            message = "Contains palm oil or palmolein fractions (high in palmitic saturated fat).",
                            severity = WarningSeverity.CAUTION,
                            iconType = "palmoil"
                        )
                    )
                }
                PalmOilStatus.MAY_CONTAIN -> {
                    warnings.add(
                        HealthWarning(
                            title = "May Contain Palm Oil",
                            message = "Vegetable oil source is unspecified or may derive from palm plantations.",
                            severity = WarningSeverity.CAUTION,
                            iconType = "palmoil"
                        )
                    )
                }
                else -> {}
            }
        }

        // 5. Additives Sensitivity Check
        if (profile.avoidHighRiskAdditives) {
            val highRisk = product.additives.filter { it.risk == AdditiveRisk.HIGH }
            if (highRisk.isNotEmpty()) {
                val names = highRisk.joinToString(", ") { "${it.code} (${it.name})" }
                warnings.add(
                    HealthWarning(
                        title = "High-Risk Additives Found",
                        message = "Contains flagged additive(s): $names. Consider choosing a cleaner alternative.",
                        severity = WarningSeverity.CAUTION,
                        iconType = "additive"
                    )
                )
            }
        }

        // 6. Excessive Sugar / Salt thresholds
        product.nutriments.sugars?.let { sugars ->
            if (sugars >= 22.5) { // UK/EU standard high sugar threshold per 100g
                warnings.add(
                    HealthWarning(
                        title = "High Sugar Content",
                        message = "Contains ${"%.1f".format(sugars)}g of sugars per 100g (over 22.5% sugar by weight).",
                        severity = WarningSeverity.CAUTION,
                        iconType = "nutrition"
                    )
                )
            }
        }

        product.nutriments.salt?.let { salt ->
            if (salt >= 1.5) { // UK/EU standard high salt threshold per 100g
                warnings.add(
                    HealthWarning(
                        title = "High Salt Content",
                        message = "Contains ${"%.1f".format(salt)}g of salt per 100g (over 60% of recommended daily sodium intake).",
                        severity = WarningSeverity.CAUTION,
                        iconType = "nutrition"
                    )
                )
            }
        }

        return warnings
    }
}
