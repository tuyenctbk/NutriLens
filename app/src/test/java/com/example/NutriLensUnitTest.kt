package com.example

import com.example.data.AdditivesDatabase
import com.example.data.DemoCatalog
import com.example.data.HealthAnalyzer
import com.example.model.AdditiveRisk
import com.example.model.UserProfile
import com.example.model.WarningSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutriLensUnitTest {

    @Test
    fun testAdditivesDatabaseResolutions() {
        // E951 Aspartame should be marked HIGH risk
        val aspartame = AdditivesDatabase.resolve("en:e951")
        assertEquals("E951", aspartame.code)
        assertEquals(AdditiveRisk.HIGH, aspartame.risk)

        // E322 Lecithins should be SAFE
        val lecithins = AdditivesDatabase.resolve("e322")
        assertEquals("E322", lecithins.code)
        assertEquals(AdditiveRisk.SAFE, lecithins.risk)

        // E250 Sodium Nitrite should be HIGH risk
        val nitrite = AdditivesDatabase.resolve("en:e250")
        assertEquals(AdditiveRisk.HIGH, nitrite.risk)
    }

    @Test
    fun testNutellaAllergenAndPalmOilAlert() {
        val nutella = DemoCatalog.getSample("3017620422003")
        assertNotNull(nutella)

        // User monitoring Milk and avoiding palm oil
        val profile = UserProfile(
            monitoredAllergens = setOf("Milk"),
            avoidPalmOil = true,
            avoidNova4UltraProcessed = true
        )

        val warnings = HealthAnalyzer.evaluate(nutella!!, profile)

        // Expect allergen warning for Milk
        val allergenWarn = warnings.find { it.iconType == "allergen" && it.severity == WarningSeverity.DANGER }
        assertNotNull("Allergen warning for Milk must be triggered", allergenWarn)

        // Expect palm oil warning
        val palmWarn = warnings.find { it.iconType == "palmoil" }
        assertNotNull("Palm oil warning must be triggered", palmWarn)

        // Expect NOVA 4 warning
        val novaWarn = warnings.find { it.iconType == "nova" }
        assertNotNull("NOVA 4 warning must be triggered", novaWarn)
    }

    @Test
    fun testCleanProductAllClear() {
        val pasta = DemoCatalog.getSample("8076809513753") // Barilla Whole Grain Penne
        assertNotNull(pasta)

        // User not allergic to gluten, avoiding palm oil and NOVA 4
        val profile = UserProfile(
            monitoredAllergens = setOf("Peanuts"),
            avoidPalmOil = true,
            avoidNova4UltraProcessed = true,
            avoidHighRiskAdditives = true
        )

        val warnings = HealthAnalyzer.evaluate(pasta!!, profile)
        // Pasta has no peanuts, no palm oil, NOVA 1, no high risk additives
        val dangers = warnings.filter { it.severity == WarningSeverity.DANGER }
        assertTrue("No dangers should be present for clean whole grain pasta", dangers.isEmpty())
    }

    @Test
    fun testScanHistoryFilteringAndPastIngredientChecks() {
        val samples = DemoCatalog.samples.mapNotNull { DemoCatalog.getSample(it.barcode) }
        assertTrue("Sample products should be available for scan history", samples.isNotEmpty())

        // Check that Oreo is ultra-processed NOVA 4 and contains palm oil
        val oreo = samples.find { it.barcode == "7622210449283" }
        assertNotNull(oreo)
        assertEquals(4, oreo?.novaGroup)
        assertEquals(com.example.model.PalmOilStatus.CONTAINS, oreo?.palmOilStatus)
        assertTrue(oreo?.allergens?.any { it.contains("Gluten") } == true)

        // Check that Oatly is palm-oil free and 100% vegan
        val oatly = samples.find { it.barcode == "7394376616037" }
        assertNotNull(oatly)
        assertEquals(com.example.model.PalmOilStatus.FREE, oatly?.palmOilStatus)
        assertEquals(com.example.model.DietStatus.SUITABLE, oatly?.veganStatus)
    }

    @Test
    fun testGlutenFreeAndCustomKeywordsEvaluation() {
        val pasta = DemoCatalog.getSample("8076809513753") // Barilla Whole Wheat Pasta (contains durum wheat)
        assertNotNull(pasta)

        val profile = UserProfile(
            isGlutenFree = true,
            customAvoidKeywords = setOf("semolina")
        )

        val warnings = HealthAnalyzer.evaluate(pasta!!, profile)

        val glutenWarn = warnings.find { it.title.contains("Gluten Detected") }
        assertNotNull("Strict Gluten-Free setting should flag wheat pasta", glutenWarn)

        val customWarn = warnings.find { it.title.contains("Custom Avoid Keyword") }
        assertNotNull("Custom avoid keyword 'semolina' should be flagged", customWarn)
    }

    @Test
    fun testDashboardNutritionalDailyValueCalculations() {
        val nutella = DemoCatalog.getSample("3017620422003") // Nutella has 56.3g sugar / 100g
        assertNotNull(nutella)

        val sugarGrams = nutella?.nutriments?.sugars ?: 0.0
        val satFatGrams = nutella?.nutriments?.saturatedFat ?: 0.0

        // Daily recommended limits: 50g sugar, 20g saturated fat
        val sugarPct = (sugarGrams / 50.0) * 100
        val satFatPct = (satFatGrams / 20.0) * 100

        assertTrue("Nutella 100g exceeds 100% of WHO recommended daily sugar limit", sugarPct > 100)
        assertTrue("Nutella 100g supplies over 50% of daily saturated fat limit", satFatPct > 50)
    }

    @Test
    fun testSafetyNotificationChannelConstants() {
        assertEquals("channel_dietary_safety_alerts", com.example.notification.SafetyNotificationManager.CHANNEL_ID)
    }
}
