package com.example.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.AdditiveRisk
import com.example.model.HealthWarning
import com.example.model.HouseholdProfile
import java.util.Locale

fun allergenStringRes(canonicalKey: String): Int? = when (canonicalKey.trim().lowercase(Locale.ROOT)) {
    "milk" -> R.string.allergen_milk
    "gluten" -> R.string.allergen_gluten
    "peanuts" -> R.string.allergen_peanuts
    "tree nuts", "nuts" -> R.string.allergen_tree_nuts
    "soybeans", "soy", "soya" -> R.string.allergen_soybeans
    "eggs" -> R.string.allergen_eggs
    "fish" -> R.string.allergen_fish
    "crustaceans" -> R.string.allergen_crustaceans
    "sesame", "sesame seeds" -> R.string.allergen_sesame
    "mustard" -> R.string.allergen_mustard
    "celery" -> R.string.allergen_celery
    "lupin" -> R.string.allergen_lupin
    "molluscs" -> R.string.allergen_molluscs
    "sulphites", "sulfites", "sulphur dioxide and sulphites" -> R.string.allergen_sulphites
    else -> null
}

fun localizedAllergenName(context: Context, canonicalKey: String): String {
    val resId = allergenStringRes(canonicalKey)
    return if (resId != null) context.getString(resId) else canonicalKey
}

@Composable
fun localizedAllergenName(canonicalKey: String): String {
    val resId = allergenStringRes(canonicalKey)
    return if (resId != null) stringResource(resId) else canonicalKey
}

fun localizedProfileName(context: Context, profileId: String, fallbackName: String): String = when (profileId) {
    HouseholdProfile.PERSONAL_ID -> context.getString(R.string.profile_preset_personal)
    HouseholdProfile.KIDS_NUT_FREE_ID -> context.getString(R.string.profile_preset_kids_nut_free)
    HouseholdProfile.GLUTEN_FREE_ID -> context.getString(R.string.profile_preset_strict_gluten_free)
    else -> fallbackName
}

@Composable
fun localizedProfileName(profileId: String, fallbackName: String): String = when (profileId) {
    HouseholdProfile.PERSONAL_ID -> stringResource(R.string.profile_preset_personal)
    HouseholdProfile.KIDS_NUT_FREE_ID -> stringResource(R.string.profile_preset_kids_nut_free)
    HouseholdProfile.GLUTEN_FREE_ID -> stringResource(R.string.profile_preset_strict_gluten_free)
    else -> fallbackName
}

@Composable
fun localizedAdditiveRisk(risk: AdditiveRisk): String = when (risk) {
    AdditiveRisk.SAFE -> stringResource(R.string.risk_safe)
    AdditiveRisk.MODERATE -> stringResource(R.string.risk_moderate)
    AdditiveRisk.HIGH -> stringResource(R.string.risk_high)
}

fun HealthWarning.localizedTitle(context: Context): String {
    val resId = titleRes ?: return title
    return context.getString(resId)
}

fun HealthWarning.localizedMessage(context: Context): String {
    val resId = messageRes ?: return message
    return when {
        allergenArgs.isNotEmpty() -> {
            val localizedList = allergenArgs.joinToString(", ") { localizedAllergenName(context, it) }
            context.getString(resId, localizedList)
        }
        messageArgs.isNotEmpty() -> context.getString(resId, *messageArgs.toTypedArray())
        else -> context.getString(resId)
    }
}

@Composable
fun HealthWarning.localizedTitle(): String {
    return localizedTitle(LocalContext.current)
}

@Composable
fun HealthWarning.localizedMessage(): String {
    return localizedMessage(LocalContext.current)
}
