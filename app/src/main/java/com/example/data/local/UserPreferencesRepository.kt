package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

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
        val INSTALL_TIMESTAMP = longPreferencesKey("install_timestamp")
        val BANNER_DISMISSED_TIME = longPreferencesKey("banner_dismissed_time")
        val DIALOG_DISMISSED_TIME = longPreferencesKey("dialog_dismissed_time")
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { preferences ->
        val installTime = preferences[PreferencesKeys.INSTALL_TIMESTAMP] ?: System.currentTimeMillis()
        UserProfile(
            monitoredAllergens = preferences[PreferencesKeys.ALLERGENS] ?: setOf("Milk", "Gluten", "Peanuts"),
            isVegan = preferences[PreferencesKeys.IS_VEGAN] ?: false,
            isVegetarian = preferences[PreferencesKeys.IS_VEGETARIAN] ?: false,
            isGlutenFree = preferences[PreferencesKeys.IS_GLUTEN_FREE] ?: false,
            customAvoidKeywords = preferences[PreferencesKeys.CUSTOM_KEYWORDS] ?: emptySet(),
            avoidPalmOil = preferences[PreferencesKeys.AVOID_PALM_OIL] ?: true,
            avoidNova4UltraProcessed = preferences[PreferencesKeys.AVOID_NOVA_4] ?: true,
            avoidHighRiskAdditives = preferences[PreferencesKeys.AVOID_HIGH_RISK_ADDITIVES] ?: true,
            isSupporter = preferences[PreferencesKeys.IS_SUPPORTER] ?: false,
            successfulOperationsCount = preferences[PreferencesKeys.OPERATIONS_COUNT] ?: 0,
            installTimestamp = installTime,
            bannerDismissedTimestamp = preferences[PreferencesKeys.BANNER_DISMISSED_TIME] ?: 0L,
            dialogDismissedTimestamp = preferences[PreferencesKeys.DIALOG_DISMISSED_TIME] ?: 0L
        )
    }

    suspend fun incrementOperationsCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.OPERATIONS_COUNT] ?: 0
            preferences[PreferencesKeys.OPERATIONS_COUNT] = current + 1
            if (preferences[PreferencesKeys.INSTALL_TIMESTAMP] == null) {
                preferences[PreferencesKeys.INSTALL_TIMESTAMP] = System.currentTimeMillis()
            }
        }
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

    suspend fun dismissAutoPromptDialog() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DIALOG_DISMISSED_TIME] = System.currentTimeMillis()
        }
    }


    suspend fun toggleAllergen(allergen: String, isMonitored: Boolean) {
        context.dataStore.edit { preferences ->
            val current = (preferences[PreferencesKeys.ALLERGENS] ?: setOf("Milk", "Gluten", "Peanuts")).toMutableSet()
            if (isMonitored) {
                current.add(allergen)
            } else {
                current.remove(allergen)
            }
            preferences[PreferencesKeys.ALLERGENS] = current
        }
    }

    suspend fun setVegan(isVegan: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_VEGAN] = isVegan
        }
    }

    suspend fun setVegetarian(isVegetarian: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_VEGETARIAN] = isVegetarian
        }
    }

    suspend fun setGlutenFree(isGlutenFree: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_GLUTEN_FREE] = isGlutenFree
        }
    }

    suspend fun addCustomKeyword(keyword: String) {
        val clean = keyword.trim()
        if (clean.isBlank()) return
        context.dataStore.edit { preferences ->
            val current = (preferences[PreferencesKeys.CUSTOM_KEYWORDS] ?: emptySet()).toMutableSet()
            current.add(clean)
            preferences[PreferencesKeys.CUSTOM_KEYWORDS] = current
        }
    }

    suspend fun removeCustomKeyword(keyword: String) {
        context.dataStore.edit { preferences ->
            val current = (preferences[PreferencesKeys.CUSTOM_KEYWORDS] ?: emptySet()).toMutableSet()
            current.remove(keyword)
            preferences[PreferencesKeys.CUSTOM_KEYWORDS] = current
        }
    }

    suspend fun setAvoidPalmOil(avoid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AVOID_PALM_OIL] = avoid
        }
    }

    suspend fun setAvoidNova4(avoid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AVOID_NOVA_4] = avoid
        }
    }

    suspend fun setAvoidHighRiskAdditives(avoid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AVOID_HIGH_RISK_ADDITIVES] = avoid
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALLERGENS] = setOf("Milk", "Gluten", "Peanuts")
            preferences[PreferencesKeys.IS_VEGAN] = false
            preferences[PreferencesKeys.IS_VEGETARIAN] = false
            preferences[PreferencesKeys.IS_GLUTEN_FREE] = false
            preferences[PreferencesKeys.CUSTOM_KEYWORDS] = emptySet()
            preferences[PreferencesKeys.AVOID_PALM_OIL] = true
            preferences[PreferencesKeys.AVOID_NOVA_4] = true
            preferences[PreferencesKeys.AVOID_HIGH_RISK_ADDITIVES] = true
        }
    }
}
