package com.example

import com.example.data.AdditivesDatabase
import com.example.data.HealthAnalyzer
import com.example.model.AdditiveRisk
import com.example.model.DietStatus
import com.example.model.NutrimentsInfo
import com.example.model.PalmOilStatus
import com.example.model.Product
import com.example.model.UserProfile
import com.example.model.WarningSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutriLensUnitTest {

    private fun fixtureSpread() = Product(
        barcode = "3017620422003",
        name = "Hazelnut Spread",
        brand = "TestBrand",
        imageUrl = null,
        nutriscoreGrade = "e",
        novaGroup = 4,
        ecoscoreGrade = "d",
        ingredientsText = "Sugar, palm oil, hazelnuts (13%), skimmed milk powder (8.7%), fat-reduced cocoa (7.4%), emulsifier: lecithins (soya), vanillin.",
        additives = listOf(AdditivesDatabase.resolve("e322")),
        allergens = listOf("Milk", "Tree Nuts", "Soybeans"),
        traces = emptyList(),
        palmOilStatus = PalmOilStatus.CONTAINS,
        veganStatus = DietStatus.NOT_SUITABLE,
        vegetarianStatus = DietStatus.SUITABLE,
        nutriments = NutrimentsInfo(
            energyKcal = 539.0,
            fat = 30.9,
            saturatedFat = 10.6,
            carbs = 57.5,
            sugars = 56.3,
            fiber = 3.4,
            proteins = 6.3,
            salt = 0.107
        ),
        quantity = "400g",
        servingSize = "15g",
        categoriesTags = listOf("en:spreads", "en:sweet-spreads")
    )

    private fun fixtureWholeGrainPasta() = Product(
        barcode = "8076809513753",
        name = "Whole Grain Penne",
        brand = "TestMill",
        imageUrl = null,
        nutriscoreGrade = "a",
        novaGroup = 1,
        ecoscoreGrade = "a",
        ingredientsText = "Wholegrain durum wheat semolina, water.",
        additives = emptyList(),
        allergens = listOf("Gluten"),
        traces = emptyList(),
        palmOilStatus = PalmOilStatus.FREE,
        veganStatus = DietStatus.SUITABLE,
        vegetarianStatus = DietStatus.SUITABLE,
        nutriments = NutrimentsInfo(
            energyKcal = 350.0,
            fat = 2.5,
            saturatedFat = 0.5,
            carbs = 65.7,
            sugars = 3.5,
            fiber = 6.5,
            proteins = 13.0,
            salt = 0.01
        ),
        quantity = "500g",
        servingSize = "85g",
        categoriesTags = listOf("en:pastas")
    )

    private fun fixtureCookie() = Product(
        barcode = "7622210449283",
        name = "Chocolate Sandwich Cookie",
        brand = "TestBiscuits",
        imageUrl = null,
        nutriscoreGrade = "e",
        novaGroup = 4,
        ecoscoreGrade = "d",
        ingredientsText = "Wheat flour, sugar, palm oil, fat-reduced cocoa powder, soya lecithin.",
        additives = listOf(AdditivesDatabase.resolve("e322"), AdditivesDatabase.resolve("e500")),
        allergens = listOf("Gluten", "Soybeans"),
        traces = emptyList(),
        palmOilStatus = PalmOilStatus.CONTAINS,
        veganStatus = DietStatus.SUITABLE,
        vegetarianStatus = DietStatus.SUITABLE,
        nutriments = NutrimentsInfo(
            energyKcal = 476.0,
            fat = 20.0,
            saturatedFat = 5.4,
            carbs = 68.0,
            sugars = 38.0,
            fiber = 2.7,
            proteins = 5.0,
            salt = 0.73
        ),
        quantity = "154g",
        servingSize = "22g",
        categoriesTags = listOf("en:biscuits")
    )

    private fun fixtureOatDrink() = Product(
        barcode = "7394376616037",
        name = "Organic Oat Drink",
        brand = "TestOats",
        imageUrl = null,
        nutriscoreGrade = "b",
        novaGroup = 2,
        ecoscoreGrade = "a",
        ingredientsText = "Water, organic oats (10%), sea salt.",
        additives = emptyList(),
        allergens = emptyList(),
        traces = emptyList(),
        palmOilStatus = PalmOilStatus.FREE,
        veganStatus = DietStatus.SUITABLE,
        vegetarianStatus = DietStatus.SUITABLE,
        nutriments = NutrimentsInfo(
            energyKcal = 45.0,
            fat = 1.5,
            saturatedFat = 0.2,
            carbs = 6.7,
            sugars = 4.1,
            fiber = 0.8,
            proteins = 1.0,
            salt = 0.1
        ),
        quantity = "1L",
        servingSize = "200ml",
        categoriesTags = listOf("en:plant-based-beverages")
    )

    private fun fixtureRiceCakes() = Product(
        barcode = "5410041001204",
        name = "Organic Brown Rice Cakes",
        brand = "TestGrains",
        imageUrl = null,
        nutriscoreGrade = "a",
        novaGroup = 1,
        ecoscoreGrade = "a",
        ingredientsText = "Organic wholegrain brown rice (99%), sea salt.",
        additives = emptyList(),
        allergens = emptyList(),
        traces = emptyList(),
        palmOilStatus = PalmOilStatus.FREE,
        veganStatus = DietStatus.SUITABLE,
        vegetarianStatus = DietStatus.SUITABLE,
        nutriments = NutrimentsInfo(
            energyKcal = 387.0,
            fat = 2.8,
            saturatedFat = 0.6,
            carbs = 81.5,
            sugars = 0.8,
            fiber = 3.6,
            proteins = 7.9,
            salt = 0.35
        ),
        quantity = "130g",
        servingSize = "14g",
        categoriesTags = listOf("en:biscuits", "en:rice-cakes")
    )

    private fun fixtureProducts() = listOf(
        fixtureSpread(),
        fixtureWholeGrainPasta(),
        fixtureCookie(),
        fixtureOatDrink(),
        fixtureRiceCakes()
    )

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
    fun testAllergenAndPalmOilAlert() {
        val spread = fixtureSpread()

        // User monitoring Milk and avoiding palm oil
        val profile = UserProfile(
            monitoredAllergens = setOf("Milk"),
            avoidPalmOil = true,
            avoidNova4UltraProcessed = true
        )

        val warnings = HealthAnalyzer.evaluate(spread, profile)

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
        val pasta = fixtureWholeGrainPasta()

        // User not allergic to gluten, avoiding palm oil and NOVA 4
        val profile = UserProfile(
            monitoredAllergens = setOf("Peanuts"),
            avoidPalmOil = true,
            avoidNova4UltraProcessed = true,
            avoidHighRiskAdditives = true
        )

        val warnings = HealthAnalyzer.evaluate(pasta, profile)
        // Pasta has no peanuts, no palm oil, NOVA 1, no high risk additives
        val dangers = warnings.filter { it.severity == WarningSeverity.DANGER }
        assertTrue("No dangers should be present for clean whole grain pasta", dangers.isEmpty())
    }

    @Test
    fun testScanHistoryFilteringAndPastIngredientChecks() {
        val products = fixtureProducts()
        assertTrue("Test fixtures should be available", products.isNotEmpty())

        val cookie = products.find { it.barcode == "7622210449283" }
        assertNotNull(cookie)
        assertEquals(4, cookie?.novaGroup)
        assertEquals(PalmOilStatus.CONTAINS, cookie?.palmOilStatus)
        assertTrue(cookie?.allergens?.any { it.contains("Gluten") } == true)

        val oatDrink = products.find { it.barcode == "7394376616037" }
        assertNotNull(oatDrink)
        assertEquals(PalmOilStatus.FREE, oatDrink?.palmOilStatus)
        assertEquals(DietStatus.SUITABLE, oatDrink?.veganStatus)
    }

    @Test
    fun testGlutenFreeAndCustomKeywordsEvaluation() {
        val pasta = fixtureWholeGrainPasta()

        val profile = UserProfile(
            isGlutenFree = true,
            customAvoidKeywords = setOf("semolina")
        )

        val warnings = HealthAnalyzer.evaluate(pasta, profile)

        val glutenWarn = warnings.find { it.title.contains("Gluten Detected") }
        assertNotNull("Strict Gluten-Free setting should flag wheat pasta", glutenWarn)

        val customWarn = warnings.find { it.title.contains("Custom Avoid Keyword") }
        assertNotNull("Custom avoid keyword 'semolina' should be flagged", customWarn)
    }

    @Test
    fun testDashboardNutritionalDailyValueCalculations() {
        val spread = fixtureSpread()

        val sugarGrams = spread.nutriments.sugars ?: 0.0
        val satFatGrams = spread.nutriments.saturatedFat ?: 0.0

        // Daily recommended limits: 50g sugar, 20g saturated fat
        val sugarPct = (sugarGrams / 50.0) * 100
        val satFatPct = (satFatGrams / 20.0) * 100

        assertTrue("High-sugar spread exceeds 100% of WHO recommended daily sugar limit", sugarPct > 100)
        assertTrue("Spread supplies over 50% of daily saturated fat limit", satFatPct > 50)
    }

    @Test
    fun testSafetyNotificationChannelConstants() {
        assertEquals("channel_dietary_safety_alerts", com.example.notification.SafetyNotificationManager.CHANNEL_ID)
        assertEquals("channel_pantry_expiration_reminders", com.example.notification.SafetyNotificationManager.CHANNEL_PANTRY_EXPIRY)
    }

    @Test
    fun testIngredientLabelAnalyzerExtractsAdditivesAndAllergens() {
        val rawLabel = "Carbonated water, caramel color E150d, aspartame, sodium benzoate E211, skimmed milk powder, palm oil"
        val product = HealthAnalyzer.buildSyntheticProductFromIngredients(
            name = "Test Drink",
            brand = "LabelTest",
            ingredientsText = rawLabel
        )

        val additiveCodes = product.additives.map { it.code }.toSet()
        assertTrue("Should detect E150d", additiveCodes.any { it.equals("E150d", ignoreCase = true) })
        assertTrue("Should detect E951 (Aspartame)", additiveCodes.contains("E951"))
        assertTrue("Should detect E211 (Sodium Benzoate)", additiveCodes.contains("E211"))
        assertTrue("Should detect Milk allergen", product.allergens.contains("Milk"))
        assertEquals(PalmOilStatus.CONTAINS, product.palmOilStatus)
        assertEquals(4, product.novaGroup)

        val warnings = HealthAnalyzer.evaluate(product, UserProfile(monitoredAllergens = setOf("Milk")))
        assertTrue(warnings.any { it.severity == WarningSeverity.DANGER && it.iconType == "allergen" })
        assertTrue(warnings.any { it.iconType == "additive" })
    }

    @Test
    fun testHouseholdProfileDefaultPresets() {
        val presets = com.example.model.HouseholdProfile.defaultPresets()
        assertEquals(3, presets.size)
        val kids = presets.find { it.id == com.example.model.HouseholdProfile.KIDS_NUT_FREE_ID }
        assertNotNull(kids)
        assertTrue(kids!!.monitoredAllergens.contains("Peanuts"))
        assertTrue(kids.monitoredAllergens.contains("Tree Nuts"))

        val gf = presets.find { it.id == com.example.model.HouseholdProfile.GLUTEN_FREE_ID }
        assertNotNull(gf)
        assertTrue(gf!!.isGlutenFree)
    }

    @Test
    fun testHealthierAlternativesExcludesAllergenicCandidates() {
        val cookie = fixtureCookie()
        val nutFreeProfile = UserProfile(
            monitoredAllergens = setOf("Tree Nuts", "Gluten"),
            isGlutenFree = true
        )
        val ranked = HealthAnalyzer.rankHealthierAlternatives(
            target = cookie,
            candidates = fixtureProducts(),
            profile = nutFreeProfile
        )
        assertTrue(ranked.isNotEmpty())
        // Ensure no gluten or tree-nut product slips into recommendations
        ranked.forEach { alt ->
            val warnings = HealthAnalyzer.evaluate(alt, nutFreeProfile)
            assertTrue(
                "Suggested swap ${alt.name} must not have DANGER warnings",
                warnings.none { it.severity == WarningSeverity.DANGER }
            )
        }
    }

    @Test
    fun testMacroEnergyBreakdownAndPortfolioAverages() {
        val spread = fixtureSpread()
        val breakdown = com.example.ui.components.NutritionMath.computeMacroBreakdown(spread.nutriments)
        assertEquals(57.5 * 4.0, breakdown.carbsKcal, 0.01)
        assertEquals(30.9 * 9.0, breakdown.fatKcal, 0.01)
        assertEquals(6.3 * 4.0, breakdown.proteinKcal, 0.01)
        assertTrue(breakdown.totalMacroKcal > 500.0)

        val avg = com.example.ui.components.NutritionMath.computePortfolioAverages(fixtureProducts())
        assertTrue(avg.avgKcal > 0.0)
        assertTrue(avg.avgCarbs > 0.0)
    }

    @Test
    fun testQrCodeGeneratorProducesValidPayPalMatrix() {
        val url = com.example.ui.components.DonationBillingHelper.buildPayPalUrl(5)
        assertEquals("https://paypal.me/NutriLensApp/5USD", url)
        assertEquals("https://paypal.me/NutriLensApp", com.example.ui.components.DonationBillingHelper.buildPayPalUrl(null))

        val matrix = com.example.ui.components.QrCodeGenerator.encode(url)
        assertTrue("QR size must be at least 21x21", matrix.size >= 21)
        assertEquals(matrix.size, matrix[0].size)
        // Top-left 7x7 finder outer border corner must be dark
        assertTrue(matrix[0][0])
        assertTrue(matrix[0][6])
        assertTrue(matrix[6][0])
        // Center 3x3 of top-left finder must be dark
        assertTrue(matrix[3][3])
    }

    @Test
    fun testDonationTriggersRespectAntiAnnoyanceSafeguards() {
        val now = System.currentTimeMillis()
        val threeDaysAgo = now - (3L * 24 * 60 * 60 * 1000)

        // New user should NEVER see banner, milestone prompt, or post-scan support
        val freshUser = UserProfile(installTimestamp = now, successfulOperationsCount = 1, appLaunchCount = 1)
        assertTrue(!freshUser.shouldShowHomeBanner)
        assertTrue(!freshUser.shouldTriggerMilestonePrompt)
        assertTrue(!freshUser.shouldShowPostScanSupport)

        // Engaged user (3+ ops, 3+ launches, 2+ days) sees subtle banner, but NOT milestone dialog at 4 ops
        val engagedUser = UserProfile(installTimestamp = threeDaysAgo, successfulOperationsCount = 4, appLaunchCount = 3)
        assertTrue(engagedUser.shouldShowHomeBanner)
        assertTrue(!engagedUser.shouldTriggerMilestonePrompt)

        // Milestone user at 10 ops qualifies for milestone prompt (up to max 3 prompts ever)
        val milestoneUser = UserProfile(installTimestamp = threeDaysAgo, successfulOperationsCount = 10, appLaunchCount = 5, autoPromptCount = 0)
        assertTrue(milestoneUser.shouldTriggerMilestonePrompt)
        assertTrue(milestoneUser.shouldShowPostScanSupport)

        // Capped at 3 auto-prompts lifetime
        val cappedUser = milestoneUser.copy(autoPromptCount = 3)
        assertTrue(!cappedUser.shouldTriggerMilestonePrompt)

        // Active supporter never sees any prompts or banners permanently
        val supporter = milestoneUser.copy(isSupporter = true)
        assertTrue(!supporter.shouldShowHomeBanner)
        assertTrue(!supporter.shouldTriggerMilestonePrompt)
        assertTrue(!supporter.shouldShowPostScanSupport)
    }

    @Test
    fun testBillingRepositoryActivitySessionValidationAndPricing() {
        // Only a live, non-finishing, non-destroyed, at-least-STARTED Activity session is valid
        assertTrue(com.example.data.BillingRepository.isActivitySessionValid(isFinishing = false, isDestroyed = false, isAtLeastStarted = true))
        assertTrue(!com.example.data.BillingRepository.isActivitySessionValid(isFinishing = true, isDestroyed = false, isAtLeastStarted = true))
        assertTrue(!com.example.data.BillingRepository.isActivitySessionValid(isFinishing = false, isDestroyed = true, isAtLeastStarted = true))
        assertTrue(!com.example.data.BillingRepository.isActivitySessionValid(isFinishing = false, isDestroyed = false, isAtLeastStarted = false))

        // BillingUiState resolves Play localized price when present and falls back to default tier USD price
        val state = com.example.data.BillingUiState(
            localizedPrices = mapOf(com.example.ui.components.DonationTier.COFFEE.skuId to "€4.99")
        )
        assertEquals("€4.99", state.displayPriceFor(com.example.ui.components.DonationTier.COFFEE))
        assertEquals("$1.99", state.displayPriceFor(com.example.ui.components.DonationTier.ESPRESSO))
    }

    @Test
    fun testNutritionalInsightsEngineDietaryAdvice() {
        // 1. High-sugar spread: high sugar, high saturated fat, calorie dense -> lower balance score + watch-out insights
        val spread = fixtureSpread()
        val spreadSummary = com.example.data.NutritionalInsightsEngine.analyze(spread.nutriments)
        assertTrue(spreadSummary.hasMacroData)
        assertTrue("High-sugar calorie-dense spread should score under 55", spreadSummary.balanceScore < 55)
        assertTrue(spreadSummary.insights.any { it.code == com.example.data.InsightCode.HIGH_SUGAR_SPIKE })
        assertTrue(spreadSummary.insights.any { it.code == com.example.data.InsightCode.HIGH_SAT_FAT })
        assertTrue(spreadSummary.dietFitTags.contains(com.example.data.DietFitTag.ENERGY_DENSE))

        // 2. Whole grain pasta: high protein, high fiber, low sugar, complex carbs -> high balance score
        val pasta = fixtureWholeGrainPasta()
        val pastaSummary = com.example.data.NutritionalInsightsEngine.analyze(pasta.nutriments)
        assertTrue(pastaSummary.hasMacroData)
        assertTrue("Whole grain pasta should score >= 80", pastaSummary.balanceScore >= 80)
        assertTrue(pastaSummary.dietFitTags.contains(com.example.data.DietFitTag.HIGH_PROTEIN))
        assertTrue(pastaSummary.dietFitTags.contains(com.example.data.DietFitTag.HIGH_FIBER))
        assertTrue(pastaSummary.insights.any { it.code == com.example.data.InsightCode.HIGH_PROTEIN_SATIETY })

        // 3. Empty nutriments -> graceful fallback with INSUFFICIENT_MACRO_DATA
        val emptySummary = com.example.data.NutritionalInsightsEngine.analyze(NutrimentsInfo())
        assertTrue(!emptySummary.hasMacroData)
        assertEquals(com.example.data.InsightCode.INSUFFICIENT_MACRO_DATA, emptySummary.insights.first().code)

        // 4. Portfolio trend analysis across test fixtures
        val portfolio = com.example.data.NutritionalInsightsEngine.analyzePortfolio(fixtureProducts())
        assertNotNull(portfolio)
        assertTrue(portfolio!!.highlightValue.isNotBlank())
    }

    @Test
    fun testPlantBasedNonDairyPhrasesRemainVeganAndDairyFree() {
        val rawLabel = "Organic wholegrain oats, oat milk, cocoa butter, peanut butter, coconut cream, sea salt"
        val product = HealthAnalyzer.buildSyntheticProductFromIngredients(
            name = "Plant Protein Bar",
            brand = "GreenHarvest",
            ingredientsText = rawLabel
        )

        assertTrue("Non-dairy plant phrases must not trigger Milk allergen", !product.allergens.contains("Milk"))
        assertEquals(com.example.model.DietStatus.SUITABLE, product.veganStatus)
        assertEquals(com.example.model.DietStatus.SUITABLE, product.vegetarianStatus)

        val warnings = HealthAnalyzer.evaluate(
            product,
            UserProfile(monitoredAllergens = setOf("Milk"), isVegan = true)
        )
        assertTrue("Vegan dairy-free bar should produce zero DANGER warnings", warnings.none { it.severity == WarningSeverity.DANGER })
    }

    @Test
    fun testPluralAllergensDetectedAndTreeNutsDoesNotFalseTriggerOnPeanuts() {
        val peanutOnlyProduct = Product(
            barcode = "111000111",
            name = "Roasted Salted Peanuts",
            brand = "NutCo",
            imageUrl = null,
            nutriscoreGrade = "b",
            novaGroup = 2,
            ecoscoreGrade = "b",
            ingredientsText = "Roasted peanuts, sea salt.",
            additives = emptyList(),
            allergens = listOf("Peanuts"),
            traces = emptyList(),
            palmOilStatus = PalmOilStatus.FREE,
            veganStatus = DietStatus.SUITABLE,
            vegetarianStatus = DietStatus.SUITABLE,
            nutriments = NutrimentsInfo()
            , quantity = "200g", servingSize = "30g"
        )

        // Monitoring ONLY Tree Nuts must NOT false-trigger on Peanuts
        val treeNutWarnings = HealthAnalyzer.evaluate(
            peanutOnlyProduct,
            UserProfile(monitoredAllergens = setOf("Tree Nuts"))
        )
        assertTrue(
            "Peanuts must not trigger Tree Nuts false positive",
            treeNutWarnings.none { it.iconType == "allergen" }
        )

        // Plural "peanuts" in ingredients must trigger Peanuts allergen even when allergens tag list is empty
        val unlabeledPeanuts = peanutOnlyProduct.copy(allergens = emptyList())
        val peanutWarnings = HealthAnalyzer.evaluate(
            unlabeledPeanuts,
            UserProfile(monitoredAllergens = setOf("Peanuts"))
        )
        assertTrue(
            "Plural 'peanuts' in ingredients must trigger Peanuts DANGER alert",
            peanutWarnings.any { it.severity == WarningSeverity.DANGER && it.allergenArgs.contains("Peanuts") }
        )

        // Plural "eggs" and "almonds" in Ingredient Label Analyzer must be detected and marked non-vegan
        val eggAlmondCake = HealthAnalyzer.buildSyntheticProductFromIngredients(
            name = "Almond Sponge",
            brand = "Bakery",
            ingredientsText = "Almonds, whole eggs, sugar, salt"
        )
        assertTrue("Should detect Tree Nuts from plural 'almonds'", eggAlmondCake.allergens.contains("Tree Nuts"))
        assertTrue("Should detect Eggs from plural 'eggs'", eggAlmondCake.allergens.contains("Eggs"))
        assertEquals(DietStatus.NOT_SUITABLE, eggAlmondCake.veganStatus)
    }

    @Test
    fun testVeganProfileFlagsNonVegetarianWhenVeganStatusUnknown() {
        val meatProduct = Product(
            barcode = "222000222",
            name = "Chicken Broth",
            brand = "SoupCo",
            imageUrl = null,
            nutriscoreGrade = "b",
            novaGroup = 3,
            ecoscoreGrade = "c",
            ingredientsText = "Water, chicken stock, salt",
            additives = emptyList(),
            allergens = emptyList(),
            traces = emptyList(),
            palmOilStatus = PalmOilStatus.FREE,
            veganStatus = DietStatus.UNKNOWN,
            vegetarianStatus = DietStatus.NOT_SUITABLE,
            nutriments = NutrimentsInfo(),
            quantity = "500ml",
            servingSize = "250ml"
        )
        val warnings = HealthAnalyzer.evaluate(meatProduct, UserProfile(isVegan = true, monitoredAllergens = emptySet()))
        assertTrue(
            "Vegan profile must flag products with vegetarianStatus=NOT_SUITABLE even when veganStatus=UNKNOWN",
            warnings.any { it.severity == WarningSeverity.DANGER && it.iconType == "vegan" }
        )
    }

    @Test
    fun testNutritionalInsightsZeroGramsKcalOnlyDoesNotReport100PercentProtein() {
        val kcalOnly = NutrimentsInfo(energyKcal = 120.0)
        val summary = com.example.data.NutritionalInsightsEngine.analyze(kcalOnly)
        assertTrue(summary.hasMacroData)
        assertEquals(0, summary.carbsCaloriePct)
        assertEquals(0, summary.fatCaloriePct)
        assertEquals(0, summary.proteinCaloriePct)
    }

    @Test
    fun testAdditivesDatabaseHandlesNonEnglishPrefixesAndSubLetters() {
        val frenchLecithin = AdditivesDatabase.resolve("fr:e322i")
        assertEquals("E322I", frenchLecithin.code)
        assertEquals(AdditiveRisk.SAFE, frenchLecithin.risk)

        val germanAspartame = AdditivesDatabase.resolve("de:e951")
        assertEquals("E951", germanAspartame.code)
        assertEquals(AdditiveRisk.HIGH, germanAspartame.risk)
    }
}
