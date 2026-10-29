package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Raw DTOs from Open Food Facts API v2
 */
@JsonClass(generateAdapter = true)
data class OpenFoodFactsResponse(
    @Json(name = "status") val status: Int? = 0,
    @Json(name = "code") val code: String? = null,
    @Json(name = "status_verbose") val statusVerbose: String? = null,
    @Json(name = "product") val product: ProductDto? = null
)

@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "code") val code: String? = null,
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "product_name_en") val productNameEn: String? = null,
    @Json(name = "brands") val brands: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "image_front_url") val imageFrontUrl: String? = null,
    @Json(name = "image_front_small_url") val imageFrontSmallUrl: String? = null,
    @Json(name = "nutriscore_grade") val nutriscoreGrade: String? = null,
    @Json(name = "nova_group") val novaGroup: Int? = null,
    @Json(name = "ecoscore_grade") val ecoscoreGrade: String? = null,
    @Json(name = "ingredients_text") val ingredientsText: String? = null,
    @Json(name = "ingredients_text_en") val ingredientsTextEn: String? = null,
    @Json(name = "additives_tags") val additivesTags: List<String>? = null,
    @Json(name = "additives_original_tags") val additivesOriginalTags: List<String>? = null,
    @Json(name = "allergens_tags") val allergensTags: List<String>? = null,
    @Json(name = "allergens_hierarchy") val allergensHierarchy: List<String>? = null,
    @Json(name = "traces_tags") val tracesTags: List<String>? = null,
    @Json(name = "ingredients_analysis_tags") val ingredientsAnalysisTags: List<String>? = null,
    @Json(name = "quantity") val quantity: String? = null,
    @Json(name = "serving_size") val servingSize: String? = null,
    @Json(name = "nutriments") val nutriments: NutrimentsDto? = null
)

@JsonClass(generateAdapter = true)
data class NutrimentsDto(
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double? = null,
    @Json(name = "energy_100g") val energy100g: Double? = null,
    @Json(name = "fat_100g") val fat100g: Double? = null,
    @Json(name = "saturated-fat_100g") val saturatedFat100g: Double? = null,
    @Json(name = "carbohydrates_100g") val carbohydrates100g: Double? = null,
    @Json(name = "sugars_100g") val sugars100g: Double? = null,
    @Json(name = "fiber_100g") val fiber100g: Double? = null,
    @Json(name = "proteins_100g") val proteins100g: Double? = null,
    @Json(name = "salt_100g") val salt100g: Double? = null,
    @Json(name = "sodium_100g") val sodium100g: Double? = null
)

/**
 * Domain Models for Clean App UI & Logic
 */
data class Product(
    val barcode: String,
    val name: String,
    val brand: String,
    val imageUrl: String?,
    val nutriscoreGrade: String?, // "a", "b", "c", "d", "e"
    val novaGroup: Int?, // 1, 2, 3, 4
    val ecoscoreGrade: String?, // "a", "b", "c", "d", "e"
    val ingredientsText: String?,
    val additives: List<AdditiveDetail>,
    val allergens: List<String>,
    val traces: List<String>,
    val palmOilStatus: PalmOilStatus,
    val veganStatus: DietStatus,
    val vegetarianStatus: DietStatus,
    val nutriments: NutrimentsInfo,
    val quantity: String?,
    val servingSize: String?,
    val scannedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isOfflineCached: Boolean = false
)

enum class PalmOilStatus {
    FREE,
    CONTAINS,
    MAY_CONTAIN,
    UNKNOWN
}

enum class DietStatus {
    SUITABLE,
    NOT_SUITABLE,
    MAYBE,
    UNKNOWN
}

enum class AdditiveRisk {
    SAFE,
    MODERATE,
    HIGH
}

data class AdditiveDetail(
    val code: String, // e.g. "E322"
    val name: String, // e.g. "Lecithins"
    val risk: AdditiveRisk,
    val purpose: String, // e.g. "Emulsifier"
    val warning: String? = null
)

data class NutrimentsInfo(
    val energyKcal: Double? = null,
    val fat: Double? = null,
    val saturatedFat: Double? = null,
    val carbs: Double? = null,
    val sugars: Double? = null,
    val fiber: Double? = null,
    val proteins: Double? = null,
    val salt: Double? = null
)

enum class WarningSeverity {
    DANGER, // Red (matches allergen or forbidden diet)
    CAUTION, // Orange (NOVA 4, Palm oil, Moderate additive, high sugar)
    INFO // Blue/Neutral
}

data class HealthWarning(
    val title: String,
    val message: String,
    val severity: WarningSeverity,
    val iconType: String // "allergen", "nova", "palmoil", "additive", "nutrition"
)

data class UserProfile(
    val monitoredAllergens: Set<String> = setOf("Milk", "Gluten", "Peanuts"),
    val isVegan: Boolean = false,
    val isVegetarian: Boolean = false,
    val isGlutenFree: Boolean = false,
    val customAvoidKeywords: Set<String> = emptySet(),
    val avoidPalmOil: Boolean = true,
    val avoidNova4UltraProcessed: Boolean = true,
    val avoidHighRiskAdditives: Boolean = true,
    val isSupporter: Boolean = false,
    val successfulOperationsCount: Int = 0,
    val installTimestamp: Long = 0L,
    val bannerDismissedTimestamp: Long = 0L,
    val dialogDismissedTimestamp: Long = 0L
) {
    val shouldShowHomeBanner: Boolean
        get() {
            if (isSupporter) return false
            val now = System.currentTimeMillis()
            val daysSinceInstall = if (installTimestamp > 0) (now - installTimestamp) / (1000 * 60 * 60 * 24) else 3 // Default eligible if first launch
            val cooldownOver = (now - bannerDismissedTimestamp) > (14L * 24 * 60 * 60 * 1000)
            return successfulOperationsCount >= 3 && daysSinceInstall >= 2 && cooldownOver
        }

    val shouldTriggerAutoPrompt: Boolean
        get() {
            if (isSupporter) return false
            val now = System.currentTimeMillis()
            val cooldownOver = (now - dialogDismissedTimestamp) > (30L * 24 * 60 * 60 * 1000)
            return successfulOperationsCount >= 7 && cooldownOver
        }
}

