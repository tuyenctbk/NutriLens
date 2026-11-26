package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.HouseholdProfile
import com.example.model.UserProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ALLERGENS = stringSetPreferencesKey("monitored_allergens")
        val IS_VEGAN = booleanPreferencesKey("is_vegan")
        val IS_VEGETARIAN = booleanPreferencesKey("is_vegetarian")
        val IS_GLUTEN_FREE = booleanPreferencesKey("is_gluten_free")
        val CUSTOM_KEYWORDS = stringSetPreferencesKey("custom_avoid_keywords")
        val AVOID_PALM_OIL = booleanPreferencesKey("avoid_palm_oil")
        val AVOID_NOVA_4 = booleanPreferencesKey("avoid_nova_4")
        val AVOID_HIGH_RISK_ADDITIVES = booleanPreferencesKey("avoid_high_risk_additives")
        val IS_SUPPORTER = booleanPreferencesKey("is_supporter")
        val OPERATIONS_COUNT = intPreferencesKey("operations_count")
        val APP_LAUNCH_COUNT = intPreferencesKey("app_launch_count")
        val AUTO_PROMPT_COUNT = intPreferencesKey("auto_prompt_count")
        val INSTALL_TIMESTAMP = longPreferencesKey("install_timestamp")
        val BANNER_DISMISSED_TIME = longPreferencesKey("banner_dismissed_time")
        val DIALOG_DISMISSED_TIME = longPreferencesKey("dialog_dismissed_time")
        val PROFILES_JSON = stringPreferencesKey("household_profiles_json")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val PANTRY_REMINDERS_ENABLED = booleanPreferencesKey("pantry_reminders_enabled")
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val profileListType = Types.newParameterizedType(List::class.java, HouseholdProfile::class.java)
    private val profileListAdapter = moshi.adapter<List<HouseholdProfile>>(profileListType)

    private fun resolveProfiles(preferences: Preferences): List<HouseholdProfile> {
        val rawJson = preferences[PreferencesKeys.PROFILES_JSON]
        if (!rawJson.isNullOrBlank()) {
            try {
                val parsed = profileListAdapter.fromJson(rawJson)
                if (!parsed.isNullOrEmpty()) return parsed
            } catch (_: Exception) {
                // Fallback to legacy single profile migration
            }
        }
        // Migrate existing user settings seamlessly into the Personal preset
        val presets = HouseholdProfile.defaultPresets().toMutableList()
        val migratedPersonal = HouseholdProfile(
            id = HouseholdProfile.PERSONAL_ID,
            name = "Personal",
            monitoredAllergens = (preferences[PreferencesKeys.ALLERGENS] ?: setOf("Milk", "Gluten", "Peanuts")).toList(),
            isVegan = preferences[PreferencesKeys.IS_VEGAN] ?: false,
            isVegetarian = preferences[PreferencesKeys.IS_VEGETARIAN] ?: false,
            isGlutenFree = preferences[PreferencesKeys.IS_GLUTEN_FREE] ?: false,
            customAvoidKeywords = (preferences[PreferencesKeys.CUSTOM_KEYWORDS] ?: emptySet()).toList(),
            avoidPalmOil = preferences[PreferencesKeys.AVOID_PALM_OIL] ?: true,
            avoidNova4UltraProcessed = preferences[PreferencesKeys.AVOID_NOVA_4] ?: true,
            avoidHighRiskAdditives = preferences[PreferencesKeys.AVOID_HIGH_RISK_ADDITIVES] ?: true,
            isBuiltIn = true
        )
        presets[0] = migratedPersonal
        return presets
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { preferences ->
        val profiles = resolveProfiles(preferences)
        val activeId = preferences[PreferencesKeys.ACTIVE_PROFILE_ID] ?: HouseholdProfile.PERSONAL_ID
        val active = profiles.find { it.id == activeId } ?: profiles.first()

        UserProfile(
            monitoredAllergens = active.monitoredAllergens.toSet(),
            isVegan = active.isVegan,
            isVegetarian = active.isVegetarian,
            isGlutenFree = active.isGlutenFree,
            customAvoidKeywords = active.customAvoidKeywords.toSet(),
            avoidPalmOil = active.avoidPalmOil,
            avoidNova4UltraProcessed = active.avoidNova4UltraProcessed,
            avoidHighRiskAdditives = active.avoidHighRiskAdditives,
            isSupporter = preferences[PreferencesKeys.IS_SUPPORTER] ?: false,
            successfulOperationsCount = preferences[PreferencesKeys.OPERATIONS_COUNT] ?: 0,
            appLaunchCount = preferences[PreferencesKeys.APP_LAUNCH_COUNT] ?: 0,
            autoPromptCount = preferences[PreferencesKeys.AUTO_PROMPT_COUNT] ?: 0,
            installTimestamp = preferences[PreferencesKeys.INSTALL_TIMESTAMP] ?: 0L,
            bannerDismissedTimestamp = preferences[PreferencesKeys.BANNER_DISMISSED_TIME] ?: 0L,
            dialogDismissedTimestamp = preferences[PreferencesKeys.DIALOG_DISMISSED_TIME] ?: 0L,
            activeProfileId = active.id,
            activeProfileName = active.name,
            savedProfiles = profiles,
            pantryRemindersEnabled = preferences[PreferencesKeys.PANTRY_REMINDERS_ENABLED] ?: true
        )
    }

    private suspend fun updateActiveProfile(transform: (HouseholdProfile) -> HouseholdProfile) {
        context.dataStore.edit { preferences ->
            val profiles = resolveProfiles(preferences).toMutableList()
            val activeId = preferences[PreferencesKeys.ACTIVE_PROFILE_ID] ?: HouseholdProfile.PERSONAL_ID
            val index = profiles.indexOfFirst { it.id == activeId }.takeIf { it >= 0 } ?: 0
            val updated = transform(profiles[index])
            profiles[index] = updated
            preferences[PreferencesKeys.PROFILES_JSON] = profileListAdapter.toJson(profiles)
            preferences[PreferencesKeys.ACTIVE_PROFILE_ID] = updated.id
            // Mirror active fields to legacy keys for backward compatibility
            preferences[PreferencesKeys.ALLERGENS] = updated.monitoredAllergens.toSet()
            preferences[PreferencesKeys.IS_VEGAN] = updated.isVegan
            preferences[PreferencesKeys.IS_VEGETARIAN] = updated.isVegetarian
            preferences[PreferencesKeys.IS_GLUTEN_FREE] = updated.isGlutenFree
            preferences[PreferencesKeys.CUSTOM_KEYWORDS] = updated.customAvoidKeywords.toSet()
            preferences[PreferencesKeys.AVOID_PALM_OIL] = updated.avoidPalmOil
            preferences[PreferencesKeys.AVOID_NOVA_4] = updated.avoidNova4UltraProcessed
            preferences[PreferencesKeys.AVOID_HIGH_RISK_ADDITIVES] = updated.avoidHighRiskAdditives
        }
    }

    suspend fun selectProfile(profileId: String) {
        context.dataStore.edit { preferences ->
            val profiles = resolveProfiles(preferences)
            val target = profiles.find { it.id == profileId } ?: return@edit
            preferences[PreferencesKeys.PROFILES_JSON] = profileListAdapter.toJson(profiles)
            preferences[PreferencesKeys.ACTIVE_PROFILE_ID] = target.id
            preferences[PreferencesKeys.ALLERGENS] = target.monitoredAllergens.toSet()
            preferences[PreferencesKeys.IS_VEGAN] = target.isVegan
            preferences[PreferencesKeys.IS_VEGETARIAN] = target.isVegetarian
            preferences[PreferencesKeys.IS_GLUTEN_FREE] = target.isGlutenFree
            preferences[PreferencesKeys.CUSTOM_KEYWORDS] = target.customAvoidKeywords.toSet()
            preferences[PreferencesKeys.AVOID_PALM_OIL] = target.avoidPalmOil
            preferences[PreferencesKeys.AVOID_NOVA_4] = target.avoidNova4UltraProcessed
            preferences[PreferencesKeys.AVOID_HIGH_RISK_ADDITIVES] = target.avoidHighRiskAdditives
        }
    }

    suspend fun createProfile(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        context.dataStore.edit { preferences ->
            val profiles = resolveProfiles(preferences).toMutableList()
            val newProfile = HouseholdProfile(
                id = "profile_${UUID.randomUUID().toString().take(8)}",
                name = cleanName,
                monitoredAllergens = emptyList(),
                isVegan = false,
                isVegetarian = false,
                isGlutenFree = false,
                customAvoidKeywords = emptyList(),
                avoidPalmOil = true,
                avoidNova4UltraProcessed = true,
                avoidHighRiskAdditives = true,
                isBuiltIn = false
            )
            profiles.add(newProfile)
            preferences[PreferencesKeys.PROFILES_JSON] = profileListAdapter.toJson(profiles)
            preferences[PreferencesKeys.ACTIVE_PROFILE_ID] = newProfile.id
        }
    }

    suspend fun deleteProfile(profileId: String) {
        context.dataStore.edit { preferences ->
            val profiles = resolveProfiles(preferences).toMutableList()
            val target = profiles.find { it.id == profileId } ?: return@edit
            if (target.isBuiltIn || profiles.size <= 1) return@edit
            profiles.removeAll { it.id == profileId }
            val nextActive = if (preferences[PreferencesKeys.ACTIVE_PROFILE_ID] == profileId) {
                profiles.first().id
            } else {
                preferences[PreferencesKeys.ACTIVE_PROFILE_ID] ?: profiles.first().id
            }
            preferences[PreferencesKeys.PROFILES_JSON] = profileListAdapter.toJson(profiles)
            preferences[PreferencesKeys.ACTIVE_PROFILE_ID] = nextActive
        }
    }

    suspend fun setPantryRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PANTRY_REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun recordAppLaunch() {
        context.dataStore.edit { preferences ->
            if (preferences[PreferencesKeys.INSTALL_TIMESTAMP] == null) {
                preferences[PreferencesKeys.INSTALL_TIMESTAMP] = System.currentTimeMillis()
            }
            val currentLaunches = preferences[PreferencesKeys.APP_LAUNCH_COUNT] ?: 0
            preferences[PreferencesKeys.APP_LAUNCH_COUNT] = currentLaunches + 1
        }
    }

    suspend fun incrementOperationsCount(): Int {
        var newCount = 0
        context.dataStore.edit { preferences ->
            newCount = (preferences[PreferencesKeys.OPERATIONS_COUNT] ?: 0) + 1
            preferences[PreferencesKeys.OPERATIONS_COUNT] = newCount
            if (preferences[PreferencesKeys.INSTALL_TIMESTAMP] == null) {
                preferences[PreferencesKeys.INSTALL_TIMESTAMP] = System.currentTimeMillis()
            }
        }
        return newCount
    }

    suspend fun markAsSupporter(supporter: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SUPPORTER] = supporter
        }
    }

    suspend fun dismissHomeBanner() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BANNER_DISMISSED_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun recordAutoPromptShown() {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.AUTO_PROMPT_COUNT] ?: 0
            preferences[PreferencesKeys.AUTO_PROMPT_COUNT] = current + 1
            preferences[PreferencesKeys.DIALOG_DISMISSED_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun dismissAutoPromptDialog() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DIALOG_DISMISSED_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun toggleAllergen(allergen: String, isMonitored: Boolean) {
        updateActiveProfile { profile ->
            val updated = profile.monitoredAllergens.toMutableSet()
            if (isMonitored) updated.add(allergen) else updated.remove(allergen)
            profile.copy(monitoredAllergens = updated.toList())
        }
    }

    suspend fun setVegan(isVegan: Boolean) {
        updateActiveProfile { it.copy(isVegan = isVegan) }
    }

    suspend fun setVegetarian(isVegetarian: Boolean) {
        updateActiveProfile { it.copy(isVegetarian = isVegetarian) }
    }

    suspend fun setGlutenFree(isGlutenFree: Boolean) {
        updateActiveProfile { it.copy(isGlutenFree = isGlutenFree) }
    }

    suspend fun addCustomKeyword(keyword: String) {
        val clean = keyword.trim()
        if (clean.isBlank()) return
        updateActiveProfile { profile ->
            val updated = profile.customAvoidKeywords.toMutableSet()
            updated.add(clean)
            profile.copy(customAvoidKeywords = updated.toList())
        }
    }

    suspend fun removeCustomKeyword(keyword: String) {
        updateActiveProfile { profile ->
            val updated = profile.customAvoidKeywords.toMutableSet()
            updated.remove(keyword)
            profile.copy(customAvoidKeywords = updated.toList())
        }
    }

    suspend fun setAvoidPalmOil(avoid: Boolean) {
        updateActiveProfile { it.copy(avoidPalmOil = avoid) }
    }

    suspend fun setAvoidNova4(avoid: Boolean) {
        updateActiveProfile { it.copy(avoidNova4UltraProcessed = avoid) }
    }

    suspend fun setAvoidHighRiskAdditives(avoid: Boolean) {
        updateActiveProfile { it.copy(avoidHighRiskAdditives = avoid) }
    }

    suspend fun resetToDefaults() {
        updateActiveProfile { profile ->
            val preset = HouseholdProfile.defaultPresets().find { it.id == profile.id }
            preset ?: profile.copy(
                monitoredAllergens = listOf("Milk", "Gluten", "Peanuts"),
                isVegan = false,
                isVegetarian = false,
                isGlutenFree = false,
                customAvoidKeywords = emptyList(),
                avoidPalmOil = true,
                avoidNova4UltraProcessed = true,
                avoidHighRiskAdditives = true
            )
        }
    }
}
