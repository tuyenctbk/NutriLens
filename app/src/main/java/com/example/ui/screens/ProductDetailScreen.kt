package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.DietStatus
import com.example.model.HealthWarning
import com.example.model.NutrimentsInfo
import com.example.model.PalmOilStatus
import com.example.model.Product
import com.example.model.WarningSeverity
import com.example.ui.components.AdditiveChip
import com.example.ui.components.EcoScoreCard
import com.example.ui.components.NovaGroupCard
import com.example.ui.components.NutriScoreCard
import com.example.ui.components.NutritionalBarChart
import com.example.ui.components.NutritionalInsightsCard
import com.example.ui.components.localizedMessage
import com.example.ui.components.localizedTitle
import com.example.ui.theme.Nova1
import com.example.ui.theme.Nova2
import com.example.ui.theme.Nova3
import com.example.ui.theme.Nova4
import com.example.ui.theme.NutriScoreA
import com.example.ui.theme.NutriScoreB
import com.example.ui.theme.NutriScoreC
import com.example.ui.theme.NutriScoreD
import com.example.ui.theme.NutriScoreE
import com.example.ui.theme.semanticCautionColors
import com.example.ui.theme.semanticDangerColors
import com.example.ui.theme.semanticSafeColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    product: Product?,
    warnings: List<HealthWarning>,
    betterAlternatives: List<Product> = emptyList(),
    isLoadingAlternatives: Boolean = false,
    activeProfileName: String = "",
    failedBarcode: String? = null,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onToggleFavorite: (Product) -> Unit,
    onRefresh: () -> Unit,
    onCompareClick: (String, String?) -> Unit = { _, _ -> },
    onAlternativeClick: (String) -> Unit = {},
    onOpenIngredientAnalyzer: (String?) -> Unit = {},
    onOpenDonationDialog: () -> Unit = {},
    showPostScanSupport: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val coroutineScope = rememberCoroutineScope()
    val favScale = remember { Animatable(1f) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = product?.name ?: stringResource(R.string.product_analysis),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.active_profile_label, activeProfileName),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
                actions = {
                    if (product != null) {
                        IconButton(
                            onClick = { onCompareClick(product.barcode, null) },
                            modifier = Modifier.testTag("compare_product_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = stringResource(R.string.compare_action_cd))
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("refresh_product_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_cd))
                        }
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    favScale.animateTo(1.35f, spring(stiffness = Spring.StiffnessHigh))
                                    favScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
                                }
                                onToggleFavorite(product)
                            },
                            modifier = Modifier.testTag("favorite_button")
                        ) {
                            Icon(
                                imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(R.string.favorite_cd),
                                tint = if (product.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.scale(favScale.value)
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
        val viewStateKey = when {
            isLoading -> "loading"
            errorMessage != null -> "error"
            product != null -> "product_${product.barcode}"
            else -> "empty"
        }

        AnimatedContent(
            targetState = viewStateKey,
            transitionSpec = {
                (fadeIn(tween(260, easing = FastOutSlowInEasing)) + slideInVertically(tween(260)) { it / 18 })
                    .togetherWith(fadeOut(tween(180)))
            },
            label = "product_detail_state_anim",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { stateKey ->
            when {
                stateKey == "loading" -> {
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
                            text = stringResource(R.string.analyzing_msg),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.consulting_db),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                stateKey == "error" && errorMessage != null -> {
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
                            text = stringResource(R.string.product_not_found),
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
                        Button(
                            onClick = { onOpenIngredientAnalyzer(failedBarcode) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fallback_analyze_label_button")
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.analyze_label_fallback_button))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = onBack) {
                                Text(stringResource(R.string.back_to_scanner))
                            }
                            OutlinedButton(onClick = onRefresh) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }

                product != null -> {
                    ProductContent(
                        product = product,
                        warnings = warnings,
                        betterAlternatives = betterAlternatives,
                        isLoadingAlternatives = isLoadingAlternatives,
                        onCompareClick = onCompareClick,
                        onAlternativeClick = onAlternativeClick,
                        onOpenDonationDialog = onOpenDonationDialog,
                        showPostScanSupport = showPostScanSupport,
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
    betterAlternatives: List<Product> = emptyList(),
    isLoadingAlternatives: Boolean = false,
    onCompareClick: (String, String?) -> Unit = { _, _ -> },
    onAlternativeClick: (String) -> Unit = {},
    onOpenDonationDialog: () -> Unit = {},
    showPostScanSupport: Boolean = false,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ProductHeroCard(product = product)
                    Spacer(modifier = Modifier.height(16.dp))
                    ProductScoresSection(product = product)
                    Spacer(modifier = Modifier.height(20.dp))
                    ProductDietarySection(product = product)
                    Spacer(modifier = Modifier.height(20.dp))
                    ProductAdditivesSection(product = product)
                    Spacer(modifier = Modifier.height(20.dp))
                    ProductIngredientsSection(product = product)
                    Spacer(modifier = Modifier.height(40.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    PersonalizedVerdictBanner(warnings = warnings)
                    if (betterAlternatives.isNotEmpty() || isLoadingAlternatives) {
                        Spacer(modifier = Modifier.height(20.dp))
                        BetterAlternativesSection(
                            currentBarcode = product.barcode,
                            alternatives = betterAlternatives,
                            isLoading = isLoadingAlternatives,
                            onAlternativeClick = onAlternativeClick,
                            onCompareClick = onCompareClick
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    NutritionalBarChart(nutriments = product.nutriments)
                    Spacer(modifier = Modifier.height(20.dp))
                    NutritionalInsightsCard(nutriments = product.nutriments)
                    Spacer(modifier = Modifier.height(20.dp))
                    ProductNutritionTableSection(nutriments = product.nutriments)
                    if (showPostScanSupport) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ProductDonationCard(onOpenDonationDialog = onOpenDonationDialog)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                ProductHeroCard(product = product)
                Spacer(modifier = Modifier.height(16.dp))
                PersonalizedVerdictBanner(warnings = warnings)
                if (betterAlternatives.isNotEmpty() || isLoadingAlternatives) {
                    Spacer(modifier = Modifier.height(16.dp))
                    BetterAlternativesSection(
                        currentBarcode = product.barcode,
                        alternatives = betterAlternatives,
                        isLoading = isLoadingAlternatives,
                        onAlternativeClick = onAlternativeClick,
                        onCompareClick = onCompareClick
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                ProductScoresSection(product = product)
                Spacer(modifier = Modifier.height(20.dp))
                ProductDietarySection(product = product)
                Spacer(modifier = Modifier.height(20.dp))
                ProductAdditivesSection(product = product)
                Spacer(modifier = Modifier.height(20.dp))
                ProductIngredientsSection(product = product)
                Spacer(modifier = Modifier.height(20.dp))
                NutritionalBarChart(nutriments = product.nutriments)
                Spacer(modifier = Modifier.height(20.dp))
                NutritionalInsightsCard(nutriments = product.nutriments)
                Spacer(modifier = Modifier.height(20.dp))
                ProductNutritionTableSection(nutriments = product.nutriments)
                if (showPostScanSupport) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ProductDonationCard(onOpenDonationDialog = onOpenDonationDialog)
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun BetterAlternativesSection(
    currentBarcode: String,
    alternatives: List<Product>,
    isLoading: Boolean,
    onAlternativeClick: (String) -> Unit,
    onCompareClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val (safeBg, safeFg, _) = semanticSafeColors()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.better_alternatives_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.better_alternatives_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading && alternatives.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.better_alternatives_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                alternatives.forEach { alt ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier
                            .width(210.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onAlternativeClick(alt.barcode) }
                            .testTag("better_alt_${alt.barcode}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!alt.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(alt.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = alt.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                        )
                                    } else {
                                        Icon(Icons.Default.NoFood, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alt.brand,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = alt.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                alt.nutriscoreGrade?.let { grade ->
                                    val nsColor = when (grade.lowercase()) {
                                        "a" -> NutriScoreA
                                        "b" -> NutriScoreB
                                        "c" -> NutriScoreC
                                        "d" -> NutriScoreD
                                        "e" -> NutriScoreE
                                        else -> Color.Gray
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = nsColor) {
                                        Text(
                                            text = stringResource(R.string.nutri_score_short_badge, grade.uppercase()),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                alt.novaGroup?.let { nova ->
                                    val novaCol = when (nova) {
                                        1 -> Nova1
                                        2 -> Nova2
                                        3 -> Nova3
                                        4 -> Nova4
                                        else -> Color.Gray
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = novaCol) {
                                        Text(
                                            text = stringResource(R.string.nova_generic, nova),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = safeBg) {
                                    Text(
                                        text = stringResource(R.string.better_alt_safe_badge),
                                        color = safeFg,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            FilledTonalButton(
                                onClick = { onCompareClick(currentBarcode, alt.barcode) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.compare_button_short), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductHeroCard(product: Product, modifier: Modifier = Modifier) {
    val (_, safeFg, _) = semanticSafeColors()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.upc_ean_label, product.barcode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!product.quantity.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.net_weight_label, product.quantity),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = stringResource(R.string.cached_room_badge),
                        tint = safeFg,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.cached_room_badge),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = safeFg
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductScoresSection(product: Product, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.food_quality_scores),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        NutriScoreCard(
            grade = product.nutriscoreGrade,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NovaGroupCard(
                group = product.novaGroup,
                modifier = Modifier.weight(1f)
            )
            EcoScoreCard(
                grade = product.ecoscoreGrade,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProductDietarySection(product: Product, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dietary_profile_check),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        DietaryFlagsRow(product = product)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductAdditivesSection(product: Product, modifier: Modifier = Modifier) {
    val (_, safeFg, _) = semanticSafeColors()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.food_additives_detected_count, product.additives.size),
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
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = safeFg
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.no_additives_detected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductIngredientsSection(product: Product, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.ingredients_list),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = product.ingredientsText ?: stringResource(R.string.ingredients_not_listed),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ProductNutritionTableSection(nutriments: NutrimentsInfo, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.nutritional_values),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        NutritionTable(nutriments = nutriments)
    }
}

@Composable
private fun ProductDonationCard(onOpenDonationDialog: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_scan_donation_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(18.dp)
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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

@Composable
fun PersonalizedVerdictBanner(warnings: List<HealthWarning>, modifier: Modifier = Modifier) {
    val hasDanger = warnings.any { it.severity == WarningSeverity.DANGER }
    val hasCaution = warnings.any { it.severity == WarningSeverity.CAUTION }

    val (dangerBg, dangerFg, dangerBorder) = semanticDangerColors()
    val (cautionBg, cautionFg, cautionBorder) = semanticCautionColors()
    val (safeBg, safeFg, safeBorder) = semanticSafeColors()

    val (bgColor, accentColor, borderColor, title, headerIcon) = when {
        hasDanger -> Quintuple(dangerBg, dangerFg, dangerBorder, stringResource(R.string.danger_verdict_title), Icons.Default.Warning)
        hasCaution -> Quintuple(cautionBg, cautionFg, cautionBorder, stringResource(R.string.caution_verdict_title), Icons.Default.WarningAmber)
        else -> Quintuple(safeBg, safeFg, safeBorder, stringResource(R.string.all_clear_verdict_title), Icons.Default.CheckCircle)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = headerIcon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
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
                            color = accentColor,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = warning.localizedTitle(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = warning.localizedMessage(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.all_clear_verdict_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun DietaryFlagsRow(product: Product, modifier: Modifier = Modifier) {
    val (dangerBg, dangerFg, _) = semanticDangerColors()
    val (cautionBg, cautionFg, _) = semanticCautionColors()
    val (safeBg, safeFg, _) = semanticSafeColors()
    val neutralBg = MaterialTheme.colorScheme.surfaceContainerLow
    val neutralFg = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val (palmBg, palmText, palmLabel) = when (product.palmOilStatus) {
            PalmOilStatus.FREE -> Triple(safeBg, safeFg, stringResource(R.string.palm_oil_free))
            PalmOilStatus.CONTAINS -> Triple(dangerBg, dangerFg, stringResource(R.string.contains_palm_oil))
            PalmOilStatus.MAY_CONTAIN -> Triple(cautionBg, cautionFg, stringResource(R.string.may_contain_palm_oil))
            PalmOilStatus.UNKNOWN -> Triple(neutralBg, neutralFg, stringResource(R.string.palm_oil_unknown))
        }

        DietaryFlagPill(
            header = stringResource(R.string.badge_palm_oil),
            label = palmLabel,
            bgColor = palmBg,
            textColor = palmText,
            modifier = Modifier.weight(1f)
        )

        val (veganBg, veganText, veganLabel) = when (product.veganStatus) {
            DietStatus.SUITABLE -> Triple(safeBg, safeFg, stringResource(R.string.diet_vegan_100))
            DietStatus.NOT_SUITABLE -> Triple(dangerBg, dangerFg, stringResource(R.string.diet_non_vegan))
            DietStatus.MAYBE -> Triple(cautionBg, cautionFg, stringResource(R.string.diet_maybe_vegan))
            DietStatus.UNKNOWN -> Triple(neutralBg, neutralFg, stringResource(R.string.diet_vegan_unknown))
        }

        DietaryFlagPill(
            header = stringResource(R.string.badge_vegan),
            label = veganLabel,
            bgColor = veganBg,
            textColor = veganText,
            modifier = Modifier.weight(1f)
        )

        val (vegBg, vegText, vegLabel) = when (product.vegetarianStatus) {
            DietStatus.SUITABLE -> Triple(safeBg, safeFg, stringResource(R.string.diet_vegetarian))
            DietStatus.NOT_SUITABLE -> Triple(dangerBg, dangerFg, stringResource(R.string.diet_non_vegetarian))
            DietStatus.MAYBE -> Triple(cautionBg, cautionFg, stringResource(R.string.diet_check))
            DietStatus.UNKNOWN -> Triple(neutralBg, neutralFg, stringResource(R.string.diet_vegetarian_unknown))
        }

        DietaryFlagPill(
            header = stringResource(R.string.badge_vegetarian),
            label = vegLabel,
            bgColor = vegBg,
            textColor = vegText,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DietaryFlagPill(
    header: String,
    label: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = header,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NutritionTable(nutriments: NutrimentsInfo, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val energyText = nutriments.energyKcal?.let {
                stringResource(R.string.value_kcal_format, it.toInt())
            } ?: "--"
            NutritionRow(stringResource(R.string.nutri_calories), energyText)
            NutritionRow(stringResource(R.string.nutri_fat), formatGrams(nutriments.fat), level = getTrafficLight(nutriments.fat, 3.0, 17.5))
            NutritionRow(stringResource(R.string.nutri_sat_fat), formatGrams(nutriments.saturatedFat), level = getTrafficLight(nutriments.saturatedFat, 1.5, 5.0))
            NutritionRow(stringResource(R.string.nutri_carbs), formatGrams(nutriments.carbs))
            NutritionRow(stringResource(R.string.nutri_sugars), formatGrams(nutriments.sugars), level = getTrafficLight(nutriments.sugars, 5.0, 22.5))
            NutritionRow(stringResource(R.string.nutri_fiber), formatGrams(nutriments.fiber))
            NutritionRow(stringResource(R.string.nutri_protein), formatGrams(nutriments.proteins))
            NutritionRow(stringResource(R.string.nutri_salt), formatGrams(nutriments.salt), level = getTrafficLight(nutriments.salt, 0.3, 1.5))
        }
    }
}

@Composable
private fun NutritionRow(label: String, value: String, level: TrafficLight = TrafficLight.NONE) {
    val (dangerBg, dangerFg, _) = semanticDangerColors()
    val (cautionBg, cautionFg, _) = semanticCautionColors()
    val (safeBg, safeFg, _) = semanticSafeColors()

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
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
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
                val (bg, fg, text) = when (level) {
                    TrafficLight.LOW -> Triple(safeBg, safeFg, stringResource(R.string.level_low))
                    TrafficLight.MODERATE -> Triple(cautionBg, cautionFg, stringResource(R.string.level_med))
                    TrafficLight.HIGH -> Triple(dangerBg, dangerFg, stringResource(R.string.level_high))
                    TrafficLight.NONE -> Triple(Color.Transparent, Color.Transparent, "")
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(bg)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = text,
                        color = fg,
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
        value >= highThreshold -> TrafficLight.HIGH
        else -> TrafficLight.MODERATE
    }
}

@Composable
private fun formatGrams(value: Double?): String {
    return if (value != null) stringResource(R.string.value_grams_format, value) else "--"
}

private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
