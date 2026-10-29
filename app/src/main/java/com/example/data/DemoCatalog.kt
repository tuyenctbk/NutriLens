package com.example.data

import com.example.model.AdditiveDetail
import com.example.model.AdditiveRisk
import com.example.model.DietStatus
import com.example.model.NutrimentsInfo
import com.example.model.PalmOilStatus
import com.example.model.Product

object DemoCatalog {

    data class SampleItem(
        val barcode: String,
        val title: String,
        val subtitle: String,
        val category: String
    )

    val samples = listOf(
        SampleItem("3017620422003", "Nutella", "Ferrero • Palm Oil & Hazelnuts", "Spreads"),
        SampleItem("7622210449283", "Oreo Original", "Mondelez • Ultra-processed NOVA 4", "Biscuits"),
        SampleItem("5449000131805", "Coca-Cola Zero", "Coca-Cola • Aspartame & Caramel E150d", "Beverages"),
        SampleItem("7394376616037", "Oatly Barista Edition", "Oatly • 100% Vegan & Palm Oil Free", "Plant Milk"),
        SampleItem("8076809513753", "Barilla Whole Grain Penne", "Barilla • NOVA 1 Natural Clean", "Pasta"),
        SampleItem("0013000006408", "Heinz Tomato Ketchup", "Heinz • High Sugar Content", "Condiments")
    )

    fun getSample(barcode: String): Product? {
        return when (barcode) {
            "3017620422003" -> Product(
                barcode = "3017620422003",
                name = "Nutella Hazelnut Spread with Cocoa",
                brand = "Ferrero",
                imageUrl = "https://images.openfoodfacts.org/images/products/301/762/042/2003/front_en.546.400.jpg",
                nutriscoreGrade = "e",
                novaGroup = 4,
                ecoscoreGrade = "d",
                ingredientsText = "Sugar, palm oil, hazelnuts (13%), skimmed milk powder (8.7%), fat-reduced cocoa (7.4%), emulsifier: lecithins (soya), vanillin.",
                additives = listOf(
                    AdditiveDetail("E322", "Lecithins (Soya)", AdditiveRisk.SAFE, "Emulsifier", "Derived from soybeans; check if allergic to soy.")
                ),
                allergens = listOf("Milk", "Nuts (Hazelnuts)", "Soybeans"),
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
                    fiber = 3.0,
                    proteins = 6.3,
                    salt = 0.107
                ),
                quantity = "400 g",
                servingSize = "15 g"
            )
            "7622210449283" -> Product(
                barcode = "7622210449283",
                name = "Oreo Original Sandwich Cookies",
                brand = "Mondelez",
                imageUrl = "https://images.openfoodfacts.org/images/products/762/221/044/9283/front_en.115.400.jpg",
                nutriscoreGrade = "e",
                novaGroup = 4,
                ecoscoreGrade = "d",
                ingredientsText = "Wheat flour, sugar, palm oil, rapeseed oil, fat-reduced cocoa powder 4.3%, wheat starch, glucose-fructose syrup, raising agents (ammonium carbonates, potassium carbonates, sodium carbonates), salt, emulsifier (soya lecithins), acidity regulator (sodium hydroxide), flavouring.",
                additives = listOf(
                    AdditiveDetail("E500", "Sodium Carbonates", AdditiveRisk.SAFE, "Raising Agent"),
                    AdditiveDetail("E501", "Potassium Carbonates", AdditiveRisk.SAFE, "Acidity Regulator"),
                    AdditiveDetail("E503", "Ammonium Carbonates", AdditiveRisk.SAFE, "Raising Agent"),
                    AdditiveDetail("E524", "Sodium Hydroxide", AdditiveRisk.SAFE, "Acidity Regulator"),
                    AdditiveDetail("E322", "Soya Lecithins", AdditiveRisk.SAFE, "Emulsifier")
                ),
                allergens = listOf("Gluten (Wheat)", "Soybeans"),
                traces = listOf("Milk"),
                palmOilStatus = PalmOilStatus.CONTAINS,
                veganStatus = DietStatus.SUITABLE,
                vegetarianStatus = DietStatus.SUITABLE,
                nutriments = NutrimentsInfo(
                    energyKcal = 474.0,
                    fat = 19.0,
                    saturatedFat = 5.2,
                    carbs = 68.0,
                    sugars = 38.0,
                    fiber = 2.7,
                    proteins = 5.4,
                    salt = 0.74
                ),
                quantity = "154 g",
                servingSize = "1 cookie (11 g)"
            )
            "5449000131805" -> Product(
                barcode = "5449000131805",
                name = "Coca-Cola Zero Sugar",
                brand = "The Coca-Cola Company",
                imageUrl = "https://images.openfoodfacts.org/images/products/544/900/013/1805/front_en.365.400.jpg",
                nutriscoreGrade = "b",
                novaGroup = 4,
                ecoscoreGrade = "b",
                ingredientsText = "Carbonated water, color (Caramel E150d), acid (phosphoric acid), sweeteners (aspartame, acesulfame K), natural flavorings including caffeine, acidity regulator (sodium citrates).",
                additives = listOf(
                    AdditiveDetail("E150d", "Ammonia Caramel", AdditiveRisk.MODERATE, "Artificial Colorant", "May contain 4-MEI byproducts from caramelization."),
                    AdditiveDetail("E338", "Phosphoric Acid", AdditiveRisk.MODERATE, "Acidity Regulator", "Inorganic acid; frequent high consumption linked to enamel & bone loss."),
                    AdditiveDetail("E950", "Acesulfame K", AdditiveRisk.MODERATE, "Artificial Sweetener", "High intensity zero-calorie synthetic sweetener."),
                    AdditiveDetail("E951", "Aspartame", AdditiveRisk.HIGH, "Artificial Sweetener", "Contains phenylalanine. Classified as possibly carcinogenic (IARC Group 2B)."),
                    AdditiveDetail("E331", "Sodium Citrates", AdditiveRisk.SAFE, "Acidity Regulator")
                ),
                allergens = emptyList(),
                traces = emptyList(),
                palmOilStatus = PalmOilStatus.FREE,
                veganStatus = DietStatus.SUITABLE,
                vegetarianStatus = DietStatus.SUITABLE,
                nutriments = NutrimentsInfo(
                    energyKcal = 0.3,
                    fat = 0.0,
                    saturatedFat = 0.0,
                    carbs = 0.0,
                    sugars = 0.0,
                    fiber = 0.0,
                    proteins = 0.0,
                    salt = 0.02
                ),
                quantity = "330 ml",
                servingSize = "250 ml"
            )
            "7394376616037" -> Product(
                barcode = "7394376616037",
                name = "Oat Drink Barista Edition",
                brand = "Oatly",
                imageUrl = "https://images.openfoodfacts.org/images/products/739/437/661/6037/front_en.188.400.jpg",
                nutriscoreGrade = "b",
                novaGroup = 3,
                ecoscoreGrade = "a",
                ingredientsText = "Oat base (water, oats 10%), rapeseed oil, acidity regulator (dipotassium phosphate), calcium carbonate, calcium phosphates, iodised salt, vitamins (D2, riboflavin, B12).",
                additives = listOf(
                    AdditiveDetail("E340ii", "Dipotassium Phosphate", AdditiveRisk.SAFE, "Acidity Regulator"),
                    AdditiveDetail("E170", "Calcium Carbonate", AdditiveRisk.SAFE, "Mineral Fortification"),
                    AdditiveDetail("E341", "Calcium Phosphates", AdditiveRisk.SAFE, "Mineral Salt")
                ),
                allergens = listOf("Gluten (Oats)"),
                traces = emptyList(),
                palmOilStatus = PalmOilStatus.FREE,
                veganStatus = DietStatus.SUITABLE,
                vegetarianStatus = DietStatus.SUITABLE,
                nutriments = NutrimentsInfo(
                    energyKcal = 59.0,
                    fat = 3.0,
                    saturatedFat = 0.3,
                    carbs = 6.6,
                    sugars = 4.0,
                    fiber = 0.8,
                    proteins = 1.0,
                    salt = 0.1
                ),
                quantity = "1 L",
                servingSize = "100 ml"
            )
            "8076809513753" -> Product(
                barcode = "8076809513753",
                name = "Whole Grain Penne Rigate",
                brand = "Barilla",
                imageUrl = "https://images.openfoodfacts.org/images/products/807/680/951/3753/front_en.112.400.jpg",
                nutriscoreGrade = "a",
                novaGroup = 1,
                ecoscoreGrade = "a",
                ingredientsText = "Whole durum wheat semolina, water.",
                additives = emptyList(),
                allergens = listOf("Gluten (Wheat)"),
                traces = listOf("Soybeans"),
                palmOilStatus = PalmOilStatus.FREE,
                veganStatus = DietStatus.SUITABLE,
                vegetarianStatus = DietStatus.SUITABLE,
                nutriments = NutrimentsInfo(
                    energyKcal = 350.0,
                    fat = 2.5,
                    saturatedFat = 0.5,
                    carbs = 64.0,
                    sugars = 3.5,
                    fiber = 8.0,
                    proteins = 13.0,
                    salt = 0.013
                ),
                quantity = "500 g",
                servingSize = "85 g"
            )
            "0013000006408" -> Product(
                barcode = "0013000006408",
                name = "Tomato Ketchup",
                brand = "Heinz",
                imageUrl = "https://images.openfoodfacts.org/images/products/001/300/000/6408/front_en.214.400.jpg",
                nutriscoreGrade = "d",
                novaGroup = 4,
                ecoscoreGrade = "b",
                ingredientsText = "Tomatoes (148g per 100g tomato ketchup), spirit vinegar, sugar, salt, spice and herb extracts (contains celery), spice.",
                additives = emptyList(),
                allergens = listOf("Celery"),
                traces = emptyList(),
                palmOilStatus = PalmOilStatus.FREE,
                veganStatus = DietStatus.SUITABLE,
                vegetarianStatus = DietStatus.SUITABLE,
                nutriments = NutrimentsInfo(
                    energyKcal = 102.0,
                    fat = 0.1,
                    saturatedFat = 0.1,
                    carbs = 23.2,
                    sugars = 22.8,
                    fiber = 1.0,
                    proteins = 1.2,
                    salt = 1.8
                ),
                quantity = "460 g",
                servingSize = "15 g"
            )
            else -> null
        }
    }
}
