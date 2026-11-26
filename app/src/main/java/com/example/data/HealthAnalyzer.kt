package com.example.data

import com.example.R
import com.example.model.AdditiveDetail
import com.example.model.AdditiveRisk
import com.example.model.DietStatus
import com.example.model.HealthWarning
import com.example.model.PalmOilStatus
import com.example.model.Product
import com.example.model.UserProfile
import com.example.model.WarningSeverity
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object HealthAnalyzer {

    private val wordBoundaryRegexCache = ConcurrentHashMap<String, Regex>()

    private fun wordRegex(term: String): Regex =
        wordBoundaryRegexCache.getOrPut(term) { Regex("\\b${Regex.escape(term)}\\b") }

    // Common standard allergen mapping (includes both singular & plural forms for accurate word-boundary matching)
    val KNOWN_ALLERGENS = listOf(
        "Milk" to listOf("milk", "lait", "dairy", "whey", "casein", "lactose", "butter", "cheese", "cream", "yogurt", "yoghurt", "en:milk"),
        "Gluten" to listOf("gluten", "wheat", "blé", "barley", "orge", "rye", "seigle", "oats", "oat", "avoine", "spelt", "en:gluten"),
        "Peanuts" to listOf("peanut", "peanuts", "arachide", "arachides", "cacahuète", "cacahuètes", "groundnut", "groundnuts", "en:peanuts"),
        "Tree Nuts" to listOf("tree nuts", "tree nut", "nuts", "nut", "hazelnut", "hazelnuts", "noisette", "noisettes", "almond", "almonds", "amande", "amandes", "walnut", "walnuts", "noix", "cashew", "cashews", "pistachio", "pistachios", "pecan", "pecans", "macadamia", "brazil nut", "brazil nuts", "pine nut", "pine nuts", "en:nuts"),
        "Soybeans" to listOf("soy", "soja", "soybean", "soybeans", "soya", "tofu", "edamame", "en:soybeans"),
        "Eggs" to listOf("egg", "eggs", "oeuf", "oeufs", "œuf", "œufs", "albumin", "lysozyme", "en:eggs"),
        "Fish" to listOf("fish", "poisson", "poissons", "salmon", "tuna", "cod", "anchovy", "anchovies", "sardine", "sardines", "mackerel", "en:fish"),
        "Crustaceans" to listOf("crustacean", "crustaceans", "crab", "crabs", "shrimp", "shrimps", "lobster", "lobsters", "prawn", "prawns", "crevette", "crevettes", "en:crustaceans"),
        "Sesame" to listOf("sesame", "sésame", "tahini", "en:sesame-seeds"),
        "Mustard" to listOf("mustard", "moutarde", "en:mustard"),
        "Celery" to listOf("celery", "céleri", "celeriac", "en:celery"),
        "Lupin" to listOf("lupin", "lupine", "lupins", "en:lupin"),
        "Molluscs" to listOf("mollusc", "molluscs", "mollusk", "mollusks", "clam", "clams", "mussel", "mussels", "oyster", "oysters", "squid", "octopus", "en:molluscs"),
        "Sulphites" to listOf("sulphite", "sulphites", "sulfite", "sulfites", "sulfur dioxide", "sulphur dioxide", "dioxyde de soufre", "e220", "e221", "e222", "e223", "e224", "en:sulphur-dioxide-and-sulphites")
    )

    private fun tagMatchesSynonyms(tag: String, synonyms: List<String>, allergenName: String): Boolean {
        val sanitizedTag = sanitizeIngredientsForAllergen(tag, allergenName)
        return synonyms.any { s ->
            sanitizedTag == s || sanitizedTag.contains(wordRegex(s))
        }
    }

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
            val sanitizedIngredients = sanitizeIngredientsForAllergen(ingredientsLower, allergenName)

            val inAllergens = productAllergensLower.any { tag -> tagMatchesSynonyms(tag, synonyms, allergenName) }
            val inIngredients = synonyms.any { s -> sanitizedIngredients.contains(wordRegex(s)) }

            if (inAllergens || inIngredients) {
                matchedAllergens.add(allergenName)
            } else {
                val inTraces = productTracesLower.any { tag -> tagMatchesSynonyms(tag, synonyms, allergenName) }
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
                    iconType = "allergen",
                    titleRes = R.string.warning_allergen_title,
                    messageRes = R.string.warning_allergen_msg,
                    allergenArgs = matchedAllergens.toList()
                )
            )
        }

        if (matchedTraces.isNotEmpty()) {
            warnings.add(
                HealthWarning(
                    title = "Possible Allergen Traces",
                    message = "May contain cross-contamination traces of: ${matchedTraces.joinToString(", ")}.",
                    severity = WarningSeverity.CAUTION,
                    iconType = "allergen",
                    titleRes = R.string.warning_traces_title,
                    messageRes = R.string.warning_traces_msg,
                    allergenArgs = matchedTraces.toList()
                )
            )
        }

        // 2. Gluten-Free Preference
        if (profile.isGlutenFree) {
            val glutenSynonyms = listOf("gluten", "wheat", "blé", "barley", "orge", "rye", "seigle", "oats", "oat", "avoine", "spelt", "en:gluten")
            val hasGlutenTag = productAllergensLower.any { tag -> tagMatchesSynonyms(tag, glutenSynonyms, "Gluten") }
            val hasGlutenText = glutenSynonyms.any { s -> ingredientsLower.contains(wordRegex(s)) }

            if (hasGlutenTag || hasGlutenText) {
                warnings.add(
                    HealthWarning(
                        title = "Gluten Detected (Not Gluten-Free)",
                        message = "Contains wheat, gluten, barley, or rye ingredients, violating your Strict Gluten-Free profile.",
                        severity = WarningSeverity.DANGER,
                        iconType = "allergen",
                        titleRes = R.string.warning_gluten_title,
                        messageRes = R.string.warning_gluten_msg
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
                val joinedKeywords = matchedKeywords.joinToString(", ")
                warnings.add(
                    HealthWarning(
                        title = "Custom Avoid Keyword Flagged",
                        message = "Found ingredient match for your custom avoid list: $joinedKeywords.",
                        severity = WarningSeverity.CAUTION,
                        iconType = "additive",
                        titleRes = R.string.warning_custom_keyword_title,
                        messageRes = R.string.warning_custom_keyword_msg,
                        messageArgs = listOf(joinedKeywords)
                    )
                )
            }
        }

        // 4. Vegan / Vegetarian Preference
        if (profile.isVegan) {
            val effectiveVeganStatus = when {
                product.veganStatus != DietStatus.UNKNOWN -> product.veganStatus
                product.vegetarianStatus == DietStatus.NOT_SUITABLE -> DietStatus.NOT_SUITABLE
                else -> DietStatus.UNKNOWN
            }
            when (effectiveVeganStatus) {
                DietStatus.NOT_SUITABLE -> {
                    warnings.add(
                        HealthWarning(
                            title = "Not Vegan Suitable",
                            message = "This product contains animal-derived ingredients or dairy/egg components.",
                            severity = WarningSeverity.DANGER,
                            iconType = "vegan",
                            titleRes = R.string.warning_not_vegan_title,
                            messageRes = R.string.warning_not_vegan_msg
                        )
                    )
                }
                DietStatus.MAYBE -> {
                    warnings.add(
                        HealthWarning(
                            title = "May Not Be Vegan",
                            message = "Some ingredients or additives might be sourced from animal origins.",
                            severity = WarningSeverity.CAUTION,
                            iconType = "vegan",
                            titleRes = R.string.warning_maybe_vegan_title,
                            messageRes = R.string.warning_maybe_vegan_msg
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
                            iconType = "vegetarian",
                            titleRes = R.string.warning_not_vegetarian_title,
                            messageRes = R.string.warning_not_vegetarian_msg
                        )
                    )
                }
                DietStatus.MAYBE -> {
                    warnings.add(
                        HealthWarning(
                            title = "Check Vegetarian Status",
                            message = "Contains ingredients that may be animal-derived.",
                            severity = WarningSeverity.CAUTION,
                            iconType = "vegetarian",
                            titleRes = R.string.warning_maybe_vegetarian_title,
                            messageRes = R.string.warning_maybe_vegetarian_msg
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
                    iconType = "nova",
                    titleRes = R.string.warning_nova4_title,
                    messageRes = R.string.warning_nova4_msg
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
                            iconType = "palmoil",
                            titleRes = R.string.warning_palm_oil_title,
                            messageRes = R.string.warning_palm_oil_msg
                        )
                    )
                }
                PalmOilStatus.MAY_CONTAIN -> {
                    warnings.add(
                        HealthWarning(
                            title = "May Contain Palm Oil",
                            message = "Vegetable oil source is unspecified or may derive from palm plantations.",
                            severity = WarningSeverity.CAUTION,
                            iconType = "palmoil",
                            titleRes = R.string.warning_may_contain_palm_oil_title,
                            messageRes = R.string.warning_may_contain_palm_oil_msg
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
                        iconType = "additive",
                        titleRes = R.string.warning_additives_title,
                        messageRes = R.string.warning_additives_msg,
                        messageArgs = listOf(names)
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
                        iconType = "nutrition",
                        titleRes = R.string.warning_high_sugar_title,
                        messageRes = R.string.warning_high_sugar_msg,
                        messageArgs = listOf(sugars)
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
                        iconType = "nutrition",
                        titleRes = R.string.warning_high_salt_title,
                        messageRes = R.string.warning_high_salt_msg,
                        messageArgs = listOf(salt)
                    )
                )
            }
        }

        return warnings
    }

    private val E_CODE_REGEX = Regex("\\b[eE]\\s?([1-9][0-9]{2}[a-i]?)\\b")

    private val NAMED_ADDITIVE_PATTERNS = listOf(
        "aspartame" to "e951",
        "acesulfame" to "e950",
        "sucralose" to "e955",
        "saccharin" to "e954",
        "cyclamate" to "e952",
        "monosodium glutamate" to "e621",
        "sodium benzoate" to "e211",
        "sodium nitrite" to "e250",
        "potassium nitrite" to "e249",
        "sodium nitrate" to "e251",
        "sulphur dioxide" to "e220",
        "sulfur dioxide" to "e220",
        "tartrazine" to "e102",
        "allura red" to "e129",
        "sunset yellow" to "e110",
        "brilliant blue" to "e133",
        "carrageenan" to "e407",
        "lecithin" to "e322",
        "citric acid" to "e330",
        "xanthan gum" to "e415",
        "guar gum" to "e412",
        "pectin" to "e440",
        "ascorbic acid" to "e300",
        "caramel color" to "e150d",
        "ammonia caramel" to "e150d"
    )

    /**
     * Parses raw ingredient label text (from OCR or manual paste) to detect E-numbers and common named additives.
     */
    fun extractAdditivesFromText(ingredientsText: String): List<AdditiveDetail> {
        if (ingredientsText.isBlank()) return emptyList()
        val foundCodes = linkedSetOf<String>()

        E_CODE_REGEX.findAll(ingredientsText).forEach { match ->
            val normalized = "e" + match.groupValues[1].lowercase(Locale.ROOT)
            foundCodes.add(normalized)
        }

        val lower = ingredientsText.lowercase(Locale.ROOT)
        NAMED_ADDITIVE_PATTERNS.forEach { (keyword, eCode) ->
            if (lower.contains(keyword)) {
                foundCodes.add(eCode)
            }
        }

        return foundCodes.map { AdditivesDatabase.resolve(it) }
    }

    /**
     * Builds a complete domain Product from raw ingredient label text so the full HealthAnalyzer
     * safety engine can evaluate items missing from Open Food Facts.
     */
    fun buildSyntheticProductFromIngredients(
        name: String,
        brand: String,
        ingredientsText: String,
        barcode: String? = null,
        imageUri: String? = null,
        defaultName: String = "Custom Label Scan",
        defaultBrand: String = "Ingredient Label Analyzer"
    ): Product {
        val cleanText = ingredientsText.trim()
        val lower = cleanText.lowercase(Locale.ROOT)

        val detectedAllergens = KNOWN_ALLERGENS.mapNotNull { (allergenName, synonyms) ->
            val sanitized = sanitizeIngredientsForAllergen(lower, allergenName)
            val matched = synonyms.any { s -> sanitized.contains(wordRegex(s)) }
            if (matched) allergenName else null
        }

        val additives = extractAdditivesFromText(cleanText)

        val palmOilStatus = when {
            lower.contains("palm oil") || lower.contains("palmolein") || lower.contains("palm kernel") || lower.contains("palm fat") -> PalmOilStatus.CONTAINS
            lower.contains("vegetable oil") || lower.contains("vegetable fat") -> PalmOilStatus.MAY_CONTAIN
            cleanText.isNotBlank() -> PalmOilStatus.FREE
            else -> PalmOilStatus.UNKNOWN
        }

        val nonVegKeywords = listOf(
            "beef", "pork", "chicken", "turkey", "meat", "fish", "salmon", "tuna",
            "shrimp", "shrimps", "prawn", "prawns", "gelatin", "gelatine", "lard",
            "anchovy", "anchovies", "sardine", "sardines", "rennet"
        )
        val nonVeganKeywords = nonVegKeywords + listOf(
            "milk", "butter", "cheese", "cream", "whey", "casein", "lactose",
            "egg", "eggs", "honey", "yogurt", "yoghurt"
        )
        val dairySanitizedLower = sanitizeIngredientsForAllergen(lower, "Milk")

        val vegetarianStatus = when {
            nonVegKeywords.any { lower.contains(wordRegex(it)) } -> DietStatus.NOT_SUITABLE
            cleanText.isNotBlank() -> DietStatus.SUITABLE
            else -> DietStatus.UNKNOWN
        }

        val veganStatus = when {
            nonVeganKeywords.any { dairySanitizedLower.contains(wordRegex(it)) } -> DietStatus.NOT_SUITABLE
            cleanText.isNotBlank() -> DietStatus.SUITABLE
            else -> DietStatus.UNKNOWN
        }

        val estimatedNova = when {
            additives.any { it.risk == AdditiveRisk.HIGH } || additives.size >= 3 ||
                lower.contains("high fructose") || lower.contains("glucose-fructose") ||
                lower.contains("artificial flavor") || lower.contains("artificial flavour") ||
                lower.contains("modified starch") -> 4
            additives.isNotEmpty() || lower.contains("sugar") -> 3
            cleanText.isNotBlank() -> 1
            else -> null
        }

        val finalBarcode = barcode?.takeIf { it.isNotBlank() } ?: "LABEL-${System.currentTimeMillis()}"

        return Product(
            barcode = finalBarcode,
            name = name.trim().ifBlank { defaultName },
            brand = brand.trim().ifBlank { defaultBrand },
            imageUrl = imageUri,
            nutriscoreGrade = null,
            novaGroup = estimatedNova,
            ecoscoreGrade = null,
            ingredientsText = cleanText,
            additives = additives,
            allergens = detectedAllergens,
            traces = emptyList(),
            palmOilStatus = palmOilStatus,
            veganStatus = veganStatus,
            vegetarianStatus = vegetarianStatus,
            nutriments = com.example.model.NutrimentsInfo(),
            quantity = null,
            servingSize = null,
            isOfflineCached = true
        )
    }

    /**
     * Filters and ranks healthier swap candidates tailored to the active household dietary profile.
     * Excludes the target product itself and any candidate that triggers DANGER warnings.
     */
    fun rankHealthierAlternatives(
        target: Product,
        candidates: List<Product>,
        profile: UserProfile,
        limit: Int = 5
    ): List<Product> {
        val targetWarnings = evaluate(target, profile)
        val targetDangerCount = targetWarnings.count { it.severity == WarningSeverity.DANGER }
        val targetTotalWarnings = targetWarnings.size
        val targetScore = qualityScore(target)
        val targetCategories = target.categoriesTags.map { it.lowercase(Locale.ROOT) }.toSet()

        return candidates
            .distinctBy { it.barcode }
            .filter { candidate -> candidate.barcode != target.barcode }
            .mapNotNull { candidate ->
                val warnings = evaluate(candidate, profile)
                val dangerCount = warnings.count { it.severity == WarningSeverity.DANGER }
                if (dangerCount > 0) return@mapNotNull null

                val candidateScore = qualityScore(candidate)
                val isStrictlyBetter = candidateScore > targetScore ||
                    warnings.size < targetTotalWarnings ||
                    targetDangerCount > 0

                if (!isStrictlyBetter) return@mapNotNull null

                val candidateCats = candidate.categoriesTags.map { it.lowercase(Locale.ROOT) }.toSet()
                val categoryOverlap = if (targetCategories.isNotEmpty()) {
                    targetCategories.intersect(candidateCats).size
                } else {
                    0
                }

                // Weighted rank: category relevance first, then nutritional quality & clean warnings
                val rankPoints = (categoryOverlap * 45) +
                    (candidateScore * 8) -
                    (warnings.size * 12) +
                    (if (candidate.palmOilStatus == PalmOilStatus.FREE) 6 else 0)

                candidate to rankPoints
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun qualityScore(product: Product): Int {
        val nutriPoints = when (product.nutriscoreGrade?.lowercase(Locale.ROOT)) {
            "a" -> 5
            "b" -> 4
            "c" -> 3
            "d" -> 2
            "e" -> 1
            else -> 2
        }
        val novaPoints = when (product.novaGroup) {
            1 -> 4
            2 -> 3
            3 -> 2
            4 -> 0
            else -> 1
        }
        val additivePenalty = product.additives.count { it.risk == AdditiveRisk.HIGH } * 2
        return nutriPoints + novaPoints - additivePenalty
    }

    private val NON_DAIRY_PHRASES = listOf(
        "cocoa butter", "cacao butter", "shea butter", "peanut butter",
        "almond butter", "cashew butter", "hazelnut butter", "sunflower butter",
        "coconut butter", "nut butter", "apple butter", "cream of tartar",
        "coconut milk", "almond milk", "oat milk", "soy milk", "soya milk",
        "rice milk", "hemp milk", "cashew milk", "coconut cream"
    )

    private fun sanitizeIngredientsForAllergen(ingredientsLower: String, allergenName: String): String {
        if (!allergenName.equals("Milk", ignoreCase = true) || ingredientsLower.isBlank()) {
            return ingredientsLower
        }
        var sanitized = ingredientsLower
        for (phrase in NON_DAIRY_PHRASES) {
            if (sanitized.contains(phrase)) {
                sanitized = sanitized.replace(phrase, " ")
            }
        }
        return sanitized
    }
}
