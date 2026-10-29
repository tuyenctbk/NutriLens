package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.DietStatus
import com.example.model.HealthWarning
import com.example.model.PalmOilStatus
import com.example.model.Product
import com.example.model.WarningSeverity
import com.example.ui.components.AdditiveChip
import com.example.ui.components.EcoScoreCard
import com.example.ui.components.NovaGroupCard
import com.example.ui.components.NutriScoreCard
import com.example.ui.theme.AllergenDanger
import com.example.ui.theme.AllergenWarning
import com.example.ui.theme.PalmOilWarning
import com.example.ui.theme.SafeGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    product: Product?,
    warnings: List<HealthWarning>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onToggleFavorite: (Product) -> Unit,
    onRefresh: () -> Unit,
    onOpenDonationDialog: () -> Unit = {},
    isSupporter: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = product?.name ?: "Product Analysis",
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (product != null) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("refresh_product_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh data")
                        }
                        IconButton(
                            onClick = { onToggleFavorite(product) },
                            modifier = Modifier.testTag("favorite_button")
                        ) {
                            Icon(
                                imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (product.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("loading_indicator")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing additives & allergens...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Consulting Open Food Facts & Room cache",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoFood,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Product Not Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = onBack) {
                                Text(stringResource(R.string.back_to_scanner))
                            }
                            Button(onClick = onRefresh) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }

                product != null -> {
                    ProductContent(
                        product = product,
                        warnings = warnings,
                        onOpenDonationDialog = onOpenDonationDialog,
                        isSupporter = isSupporter,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductContent(
    product: Product,
    warnings: List<HealthWarning>,
    onOpenDonationDialog: () -> Unit = {},
    isSupporter: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Hero Header Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Image
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!product.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(product.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = product.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.NoFood,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.brand,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "UPC/EAN: ${product.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!product.quantity.isNullOrBlank()) {
                        Text(
                            text = "Net: ${product.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Offline Room cached badge
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cached",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cached in Room DB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Personalized Safety & Dietary Verdict Banner
        PersonalizedVerdictBanner(warnings = warnings)

        Spacer(modifier = Modifier.height(16.dp))

        // Food Scores Trio (Nutri-Score, NOVA Group, Eco-Score)
        Text(
            text = "Food Safety & Quality Scores",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NutriScoreCard(
                grade = product.nutriscoreGrade,
                modifier = Modifier.weight(1.2f)
            )
            NovaGroupCard(
                group = product.novaGroup,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        EcoScoreCard(
            grade = product.ecoscoreGrade,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Dietary & Formulation Badges (Palm Oil, Vegan, Vegetarian)
        Text(
            text = "Dietary Profile & Palm Oil Check",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        DietaryFlagsRow(product = product)

        Spacer(modifier = Modifier.height(20.dp))

        // Food Additives Section (E-Numbers)
        Text(
            text = "Food Additives (${product.additives.size} detected)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (product.additives.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                product.additives.forEach { additive ->
                    AdditiveChip(additive = additive)
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SafeGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No chemical food additives detected in this product.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Ingredients Text
        Text(
            text = "Ingredients List",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = product.ingredientsText ?: "Ingredients not listed on Open Food Facts for this product.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Nutrition Table (per 100g)
        Text(
            text = "Nutritional Values (per 100g)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        NutritionTable(nutriments = product.nutriments)

        if (!isSupporter) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_scan_donation_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.post_scan_support_prompt),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenDonationDialog,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.support_dev_button), fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun PersonalizedVerdictBanner(warnings: List<HealthWarning>, modifier: Modifier = Modifier) {
    val hasDanger = warnings.any { it.severity == WarningSeverity.DANGER }
    val hasCaution = warnings.any { it.severity == WarningSeverity.CAUTION }

    val (bgColor, borderColor, title, headerIcon) = when {
        hasDanger -> Quadruple(Color(0xFFFFEBEE), AllergenDanger, "Personal Allergen / Diet Alert", Icons.Default.Warning)
        hasCaution -> Quadruple(Color(0xFFFFF3E0), AllergenWarning, "Health & Processing Warnings", Icons.Default.WarningAmber)
        else -> Quadruple(Color(0xFFE8F5E9), SafeGreen, "All Clear — Safe for Your Profile", Icons.Default.CheckCircle)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = headerIcon,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (warnings.isNotEmpty()) {
                warnings.forEach { warning ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontWeight = FontWeight.Bold,
                            color = borderColor,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Column {
                            Text(
                                text = warning.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = warning.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No allergen conflicts, forbidden diets, or sensitive additives detected based on your custom profile preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1B5E20)
                )
            }
        }
    }
}

@Composable
fun DietaryFlagsRow(product: Product, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Palm Oil Badge
        val (palmBg, palmText, palmLabel) = when (product.palmOilStatus) {
            PalmOilStatus.FREE -> Triple(Color(0xFFE8F5E9), SafeGreen, "Palm Oil Free")
            PalmOilStatus.CONTAINS -> Triple(Color(0xFFFFEBEE), PalmOilWarning, "Contains Palm Oil")
            PalmOilStatus.MAY_CONTAIN -> Triple(Color(0xFFFFF8E1), Color(0xFFE65100), "May Contain Palm")
            PalmOilStatus.UNKNOWN -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Palm Oil Unknown")
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palmBg,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PALM OIL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = palmText.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = palmLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = palmText,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Vegan Badge
        val (veganBg, veganText, veganLabel) = when (product.veganStatus) {
            DietStatus.SUITABLE -> Triple(Color(0xFFE8F5E9), SafeGreen, "100% Vegan")
            DietStatus.NOT_SUITABLE -> Triple(Color(0xFFFFEBEE), AllergenDanger, "Non-Vegan")
            DietStatus.MAYBE -> Triple(Color(0xFFFFF8E1), Color(0xFFE65100), "Maybe Vegan")
            DietStatus.UNKNOWN -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Vegan Unknown")
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = veganBg,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VEGAN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = veganText.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = veganLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = veganText,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Vegetarian Badge
        val (vegBg, vegText, vegLabel) = when (product.vegetarianStatus) {
            DietStatus.SUITABLE -> Triple(Color(0xFFE8F5E9), SafeGreen, "Vegetarian")
            DietStatus.NOT_SUITABLE -> Triple(Color(0xFFFFEBEE), AllergenDanger, "Non-Vegetarian")
            DietStatus.MAYBE -> Triple(Color(0xFFFFF8E1), Color(0xFFE65100), "Check Diet")
            DietStatus.UNKNOWN -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Diet Unknown")
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = vegBg,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VEGETARIAN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = vegText.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = vegLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = vegText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun NutritionTable(nutriments: com.example.model.NutrimentsInfo, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            NutritionRow("Calories", "${nutriments.energyKcal?.toInt() ?: "--"} kcal")
            NutritionRow("Total Fat", formatGrams(nutriments.fat), level = getTrafficLight(nutriments.fat, 3.0, 17.5))
            NutritionRow("Saturated Fat", formatGrams(nutriments.saturatedFat), level = getTrafficLight(nutriments.saturatedFat, 1.5, 5.0))
            NutritionRow("Carbohydrates", formatGrams(nutriments.carbs))
            NutritionRow("Sugars", formatGrams(nutriments.sugars), level = getTrafficLight(nutriments.sugars, 5.0, 22.5))
            NutritionRow("Fiber", formatGrams(nutriments.fiber))
            NutritionRow("Proteins", formatGrams(nutriments.proteins))
            NutritionRow("Salt", formatGrams(nutriments.salt), level = getTrafficLight(nutriments.salt, 0.3, 1.5))
        }
    }
}

@Composable
private fun NutritionRow(label: String, value: String, level: TrafficLight = TrafficLight.NONE) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (level != TrafficLight.NONE) {
                Spacer(modifier = Modifier.width(8.dp))
                val (color, text) = when (level) {
                    TrafficLight.LOW -> Pair(Color(0xFF2E7D32), "Low")
                    TrafficLight.MODERATE -> Pair(Color(0xFFEF6C00), "Med")
                    TrafficLight.HIGH -> Pair(Color(0xFFC62828), "High")
                    TrafficLight.NONE -> Pair(Color.Transparent, "")
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = text,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

private enum class TrafficLight { NONE, LOW, MODERATE, HIGH }

private fun getTrafficLight(value: Double?, lowThreshold: Double, highThreshold: Double): TrafficLight {
    if (value == null) return TrafficLight.NONE
    return when {
        value <= lowThreshold -> TrafficLight.LOW
        value > highThreshold -> TrafficLight.HIGH
        else -> TrafficLight.MODERATE
    }
}

private fun formatGrams(value: Double?): String {
    return if (value != null) "${"%.1f".format(value)} g" else "--"
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
