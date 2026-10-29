package com.example.data

import com.example.model.AdditiveDetail
import com.example.model.AdditiveRisk

object AdditivesDatabase {
    // Curated database of food additives (E-numbers) with health risks and descriptions
    private val database = mapOf(
        // High Risk / Controversial Additives
        "e102" to AdditiveDetail("E102", "Tartrazine", AdditiveRisk.HIGH, "Artificial Yellow Dye", "Linked to hyperactivity in children and allergic reactions."),
        "e110" to AdditiveDetail("E110", "Sunset Yellow FCF", AdditiveRisk.HIGH, "Artificial Orange Dye", "Associated with allergies and hyperactivity. Restricted in parts of EU."),
        "e122" to AdditiveDetail("E122", "Azorubine / Carmoisine", AdditiveRisk.HIGH, "Artificial Red Dye", "May cause allergic and intolerant reactions, asthma symptoms."),
        "e124" to AdditiveDetail("E124", "Ponceau 4R", AdditiveRisk.HIGH, "Artificial Red Dye", "Classified as possible carcinogen in some jurisdictions."),
        "e129" to AdditiveDetail("E129", "Allura Red AC", AdditiveRisk.HIGH, "Artificial Red Dye", "Banned in several EU countries; linked to gut inflammation."),
        "e133" to AdditiveDetail("E133", "Brilliant Blue FCF", AdditiveRisk.MODERATE, "Synthetic Blue Dye", "Poorly absorbed in gut; avoid if sensitive to synthetic colors."),
        "e150d" to AdditiveDetail("E150d", "Ammonia Caramel", AdditiveRisk.MODERATE, "Brown Colorant", "May contain 4-MEI byproduct produced during high temperature caramelization."),
        "e211" to AdditiveDetail("E211", "Sodium Benzoate", AdditiveRisk.HIGH, "Preservative", "Can form benzene (carcinogen) when combined with Vitamin C (ascorbic acid)."),
        "e220" to AdditiveDetail("E220", "Sulphur Dioxide", AdditiveRisk.HIGH, "Preservative / Antioxidant", "Known potent respiratory allergen and asthma trigger."),
        "e249" to AdditiveDetail("E249", "Potassium Nitrite", AdditiveRisk.HIGH, "Curing Agent / Preservative", "Precursor to carcinogenic nitrosamines in processed meats."),
        "e250" to AdditiveDetail("E250", "Sodium Nitrite", AdditiveRisk.HIGH, "Curing Agent / Preservative", "Precursor to carcinogenic nitrosamines in cured meats."),
        "e251" to AdditiveDetail("E251", "Sodium Nitrate", AdditiveRisk.HIGH, "Preservative", "Converted to nitrites in the body; WHO recommends minimizing."),
        "e252" to AdditiveDetail("E252", "Potassium Nitrate", AdditiveRisk.HIGH, "Preservative", "Used in cured sausages and salted meats. Nitrosamine risk."),
        "e310" to AdditiveDetail("E310", "Propyl Gallate", AdditiveRisk.MODERATE, "Antioxidant", "May cause stomach and skin irritation in sensitive people."),
        "e320" to AdditiveDetail("E320", "BHA (Butylated Hydroxyanisole)", AdditiveRisk.HIGH, "Synthetic Antioxidant", "Suspected endocrine disruptor and possible human carcinogen."),
        "e321" to AdditiveDetail("E321", "BHT (Butylated Hydroxytoluene)", AdditiveRisk.HIGH, "Synthetic Antioxidant", "Chemical antioxidant linked to allergic and metabolic concerns."),
        "e407" to AdditiveDetail("E407", "Carrageenan", AdditiveRisk.MODERATE, "Thickener & Stabilizer", "Seaweed extract linked to gut inflammation and digestive distress."),
        "e450" to AdditiveDetail("E450", "Diphosphates", AdditiveRisk.MODERATE, "Emulsifier / Stabilizer", "Excessive dietary phosphorus intake affects bone and vascular health."),
        "e451" to AdditiveDetail("E451", "Triphosphates", AdditiveRisk.MODERATE, "Mineral Salt", "Phosphate additive that may harm kidney and cardiovascular health."),
        "e452" to AdditiveDetail("E452", "Polyphosphates", AdditiveRisk.MODERATE, "Emulsifying Salt", "Inorganic phosphates absorbed rapidly by the body."),
        "e621" to AdditiveDetail("E621", "Monosodium Glutamate (MSG)", AdditiveRisk.MODERATE, "Flavor Enhancer", "Excitotoxin for sensitive individuals, causes headaches or flushing in some."),
        "e950" to AdditiveDetail("E950", "Acesulfame K", AdditiveRisk.MODERATE, "Artificial Sweetener", "Calorie-free intense sweetener; debates on gut microbiome impact."),
        "e951" to AdditiveDetail("E951", "Aspartame", AdditiveRisk.HIGH, "Artificial Sweetener", "Classified as possibly carcinogenic to humans (IARC Group 2B). Contains phenylalanine."),
        "e952" to AdditiveDetail("E952", "Cyclamate", AdditiveRisk.HIGH, "Artificial Sweetener", "Banned in the US; high dosage concerns in animal studies."),
        "e954" to AdditiveDetail("E954", "Saccharin", AdditiveRisk.MODERATE, "Artificial Sweetener", "Older artificial sweetener; can leave bitter aftertaste and affect gut flora."),
        "e955" to AdditiveDetail("E955", "Sucralose", AdditiveRisk.MODERATE, "Artificial Sweetener", "Chlorinated sugar derivative; may alter microbiome balance at high doses."),

        // Common Safe / Natural Additives
        "e100" to AdditiveDetail("E100", "Curcumin", AdditiveRisk.SAFE, "Natural Color", "Extracted from turmeric roots. Natural antioxidant."),
        "e101" to AdditiveDetail("E101", "Riboflavin (Vitamin B2)", AdditiveRisk.SAFE, "Natural Color / Vitamin", "Essential B-vitamin, safe."),
        "e140" to AdditiveDetail("E140", "Chlorophylls", AdditiveRisk.SAFE, "Natural Green Pigment", "Safe plant pigment."),
        "e160a" to AdditiveDetail("E160a", "Carotenes", AdditiveRisk.SAFE, "Natural Color / Provitamin A", "Safe plant-derived orange pigment."),
        "e162" to AdditiveDetail("E162", "Beetroot Red (Betanin)", AdditiveRisk.SAFE, "Natural Red Color", "Extracted from red beets, safe."),
        "e300" to AdditiveDetail("E300", "Ascorbic Acid (Vitamin C)", AdditiveRisk.SAFE, "Antioxidant / Vitamin", "Safe and beneficial antioxidant."),
        "e306" to AdditiveDetail("E306", "Tocopherols (Vitamin E)", AdditiveRisk.SAFE, "Natural Antioxidant", "Safe fat-soluble vitamin."),
        "e322" to AdditiveDetail("E322", "Lecithins", AdditiveRisk.SAFE, "Natural Emulsifier", "Derived from soybeans, sunflower, or egg yolks. Note: check source if allergic."),
        "e330" to AdditiveDetail("E330", "Citric Acid", AdditiveRisk.SAFE, "Acidity Regulator", "Found naturally in citrus fruits, universally used."),
        "e406" to AdditiveDetail("E406", "Agar Agar", AdditiveRisk.SAFE, "Plant Gelling Agent", "Natural seaweed gelatin alternative."),
        "e410" to AdditiveDetail("E410", "Locust Bean Gum", AdditiveRisk.SAFE, "Natural Thickener", "From carob seeds, safe dietary fiber."),
        "e412" to AdditiveDetail("E412", "Guar Gum", AdditiveRisk.SAFE, "Natural Thickener", "Extracted from guar beans, safe plant fiber."),
        "e415" to AdditiveDetail("E415", "Xanthan Gum", AdditiveRisk.SAFE, "Stabilizer & Thickener", "Natural fermentation product, widely recognized as safe."),
        "e440" to AdditiveDetail("E440", "Pectins", AdditiveRisk.SAFE, "Natural Gelling Agent", "From apple and citrus peels, safe soluble fiber."),
        "e500" to AdditiveDetail("E500", "Sodium Carbonates (Baking Soda)", AdditiveRisk.SAFE, "Raising Agent", "Classic kitchen leavening agent, safe.")
    )

    fun resolve(tag: String): AdditiveDetail {
        // Tag format often "en:e322" or "e322" or "en:e322i"
        val cleanTag = tag.trim().lowercase().removePrefix("en:")
        
        // Exact match
        database[cleanTag]?.let { return it }

        // Strip sub-letter like e322i -> e322
        val baseCode = cleanTag.filter { it.isDigit() || it == 'e' }
        database[baseCode]?.let { return it.copy(code = cleanTag.uppercase()) }

        // Formatted code
        val displayCode = cleanTag.uppercase()
        val eNum = cleanTag.removePrefix("e").toIntOrNull()

        val inferredRisk = when {
            cleanTag.startsWith("e1") && (cleanTag in setOf("e102", "e104", "e110", "e122", "e124", "e129")) -> AdditiveRisk.HIGH
            cleanTag.startsWith("e2") && (cleanTag in setOf("e249", "e250", "e251", "e252", "e211")) -> AdditiveRisk.HIGH
            cleanTag.startsWith("e9") && (cleanTag in setOf("e951", "e952", "e954")) -> AdditiveRisk.HIGH
            cleanTag.startsWith("e6") -> AdditiveRisk.MODERATE
            eNum != null && eNum in 100..199 -> AdditiveRisk.MODERATE // Colors
            eNum != null && eNum in 200..299 -> AdditiveRisk.MODERATE // Preservatives
            eNum != null && eNum in 600..699 -> AdditiveRisk.MODERATE // Flavor enhancers
            eNum != null && eNum in 900..999 -> AdditiveRisk.MODERATE // Sweeteners
            else -> AdditiveRisk.SAFE
        }

        val category = when {
            eNum != null && eNum in 100..199 -> "Food Coloring"
            eNum != null && eNum in 200..299 -> "Preservative"
            eNum != null && eNum in 300..399 -> "Antioxidant"
            eNum != null && eNum in 400..499 -> "Thickener / Emulsifier"
            eNum != null && eNum in 500..599 -> "Acidity Regulator"
            eNum != null && eNum in 600..699 -> "Flavor Enhancer"
            eNum != null && eNum in 900..999 -> "Sweetener / Glazing"
            else -> "Food Additive"
        }

        return AdditiveDetail(
            code = displayCode,
            name = "Additive $displayCode",
            risk = inferredRisk,
            purpose = category,
            warning = if (inferredRisk == AdditiveRisk.HIGH) "Controversial chemical additive" else null
        )
    }
}
