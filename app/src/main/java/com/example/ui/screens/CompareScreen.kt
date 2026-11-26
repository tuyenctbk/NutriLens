package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.data.HealthAnalyzer
import com.example.model.DietStatus
import com.example.model.PalmOilStatus
import com.example.model.Product
import com.example.model.UserProfile
import com.example.model.WarningSeverity
import com.example.ui.components.localizedProfileName
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    leftProduct: Product?,
    rightProduct: Product?,
    availableProducts: List<Product>,
    userProfile: UserProfile,
    onSelectProduct: (isLeft: Boolean, product: Product) -> Unit,
    onSwapProducts: () -> Unit,
    onInspectProduct: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var pickingForLeft by remember { mutableStateOf<Boolean?>(null) }
    var swapSpinDegrees by remember { mutableStateOf(0f) }
    val animatedSwapRotation by animateFloatAsState(
        targetValue = swapSpinDegrees,
        animationSpec = tween(320),
        label = "compare_swap_rotation"
    )

    val pickerCandidates = remember(availableProducts) {
        availableProducts.distinctBy { it.barcode }
    }

    val leftWarnings = remember(leftProduct, userProfile) {
        leftProduct?.let { HealthAnalyzer.evaluate(it, userProfile) } ?: emptyList()
    }
    val rightWarnings = remember(rightProduct, userProfile) {
        rightProduct?.let { HealthAnalyzer.evaluate(it, userProfile) } ?: emptyList()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.compare_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val activeProfileDisplay = localizedProfileName(userProfile.activeProfileId, userProfile.activeProfileName)
                        Text(
                            text = stringResource(R.string.active_profile_label, activeProfileDisplay),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("compare_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
                actions = {
                    if (leftProduct != null && rightProduct != null) {
                        IconButton(
                            onClick = {
                                swapSpinDegrees += 180f
                                onSwapProducts()
                            },
                            modifier = Modifier.testTag("compare_swap_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = stringResource(R.string.compare_swap_cd),
                                modifier = Modifier.rotate(animatedSwapRotation)
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 900.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dual Product Header Selector Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    CompareProductSlotCard(
                        product = leftProduct,
                        slotLabel = stringResource(R.string.compare_item_a),
                        onChangeClick = { pickingForLeft = true },
                        onInspectClick = { leftProduct?.let { onInspectProduct(it.barcode) } },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("compare_slot_left")
                    )
                    CompareProductSlotCard(
                        product = rightProduct,
                        slotLabel = stringResource(R.string.compare_item_b),
                        onChangeClick = { pickingForLeft = false },
                        onInspectClick = { rightProduct?.let { onInspectProduct(it.barcode) } },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("compare_slot_right")
                    )
                }

                if (leftProduct != null && rightProduct != null) {
                    // Personalized Safety Verdict Showdown
                    CompareVerdictCard(
                        leftWarnings = leftWarnings,
                        rightWarnings = rightWarnings
                    )

                    // Quality Scores & Dietary Flags Table
                    CompareQualityScoresCard(
                        left = leftProduct,
                        right = rightProduct
                    )

                    // Nutritional Showdown per 100g
                    CompareNutritionCard(
                        left = leftProduct,
                        right = rightProduct
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.compare_pick_two_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // Product Picker Dialog
    val targetIsLeft = pickingForLeft
    if (targetIsLeft != null) {
        AlertDialog(
            onDismissRequest = { pickingForLeft = null },
            title = { Text(stringResource(R.string.compare_choose_product)) },
            text = {
                if (pickerCandidates.isEmpty()) {
                    Text(
                        text = stringResource(R.string.compare_no_products_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(pickerCandidates, key = { it.barcode }) { candidate ->
                        val isAlreadyOther = if (targetIsLeft) {
                            rightProduct?.barcode == candidate.barcode
                        } else {
                            leftProduct?.barcode == candidate.barcode
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !isAlreadyOther) {
                                    onSelectProduct(targetIsLeft, candidate)
                                    pickingForLeft = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!candidate.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(candidate.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = candidate.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                        )
                                    } else {
                                        Icon(Icons.Default.NoFood, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = candidate.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isAlreadyOther) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = candidate.brand,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                candidate.nutriscoreGrade?.let { grade ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = nutriScoreColor(grade)
                                    ) {
                                        Text(
                                            text = grade.uppercase(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingForLeft = null }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun CompareProductSlotCard(
    product: Product?,
    slotLabel: String,
    onChangeClick: () -> Unit,
    onInspectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = slotLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .clickable(enabled = product != null, onClick = onInspectClick),
                contentAlignment = Alignment.Center
            ) {
                if (!product?.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product?.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = product?.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                } else {
                    Icon(Icons.Default.NoFood, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product?.name ?: stringResource(R.string.compare_select_product),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            Text(
                text = product?.brand ?: "--",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            FilledTonalButton(
                onClick = onChangeClick,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.compare_change_button),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CompareVerdictCard(
    leftWarnings: List<com.example.model.HealthWarning>,
    rightWarnings: List<com.example.model.HealthWarning>
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.compare_profile_verdict),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VerdictPill(warnings = leftWarnings, modifier = Modifier.weight(1f))
                VerdictPill(warnings = rightWarnings, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun VerdictPill(
    warnings: List<com.example.model.HealthWarning>,
    modifier: Modifier = Modifier
) {
    val hasDanger = warnings.any { it.severity == WarningSeverity.DANGER }
    val hasCaution = warnings.any { it.severity == WarningSeverity.CAUTION }
    val (dangerBg, dangerFg, _) = semanticDangerColors()
    val (cautionBg, cautionFg, _) = semanticCautionColors()
    val (safeBg, safeFg, _) = semanticSafeColors()

    val (bg, fg, icon, label) = when {
        hasDanger -> Quadruple(dangerBg, dangerFg, Icons.Default.Warning, stringResource(R.string.compare_alerts_count, warnings.size))
        hasCaution -> Quadruple(cautionBg, cautionFg, Icons.Default.WarningAmber, stringResource(R.string.compare_cautions_count, warnings.size))
        else -> Quadruple(safeBg, safeFg, Icons.Default.CheckCircle, stringResource(R.string.compare_all_clear))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = fg,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompareQualityScoresCard(left: Product, right: Product) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.food_quality_scores),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            val leftNsRank = gradeRank(left.nutriscoreGrade)
            val rightNsRank = gradeRank(right.nutriscoreGrade)
            ComparisonMetricRow(
                label = stringResource(R.string.nutri_score),
                leftText = left.nutriscoreGrade?.uppercase() ?: "--",
                rightText = right.nutriscoreGrade?.uppercase() ?: "--",
                leftWins = leftNsRank != null && rightNsRank != null && leftNsRank < rightNsRank,
                rightWins = leftNsRank != null && rightNsRank != null && rightNsRank < leftNsRank,
                leftBadgeColor = left.nutriscoreGrade?.let { nutriScoreColor(it) },
                rightBadgeColor = right.nutriscoreGrade?.let { nutriScoreColor(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ComparisonMetricRow(
                label = stringResource(R.string.nova_classification),
                leftText = left.novaGroup?.let { stringResource(R.string.nova_generic, it) } ?: "--",
                rightText = right.novaGroup?.let { stringResource(R.string.nova_generic, it) } ?: "--",
                leftWins = left.novaGroup != null && right.novaGroup != null && left.novaGroup < right.novaGroup,
                rightWins = left.novaGroup != null && right.novaGroup != null && right.novaGroup < left.novaGroup,
                leftBadgeColor = left.novaGroup?.let { novaColor(it) },
                rightBadgeColor = right.novaGroup?.let { novaColor(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            val leftEcoRank = gradeRank(left.ecoscoreGrade)
            val rightEcoRank = gradeRank(right.ecoscoreGrade)
            ComparisonMetricRow(
                label = stringResource(R.string.eco_score),
                leftText = left.ecoscoreGrade?.uppercase() ?: "--",
                rightText = right.ecoscoreGrade?.uppercase() ?: "--",
                leftWins = leftEcoRank != null && rightEcoRank != null && leftEcoRank < rightEcoRank,
                rightWins = leftEcoRank != null && rightEcoRank != null && rightEcoRank < leftEcoRank,
                leftBadgeColor = left.ecoscoreGrade?.let { nutriScoreColor(it) },
                rightBadgeColor = right.ecoscoreGrade?.let { nutriScoreColor(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ComparisonMetricRow(
                label = stringResource(R.string.badge_palm_oil),
                leftText = palmOilLabel(left.palmOilStatus),
                rightText = palmOilLabel(right.palmOilStatus),
                leftWins = left.palmOilStatus == PalmOilStatus.FREE && right.palmOilStatus != PalmOilStatus.FREE,
                rightWins = right.palmOilStatus == PalmOilStatus.FREE && left.palmOilStatus != PalmOilStatus.FREE
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ComparisonMetricRow(
                label = stringResource(R.string.badge_vegan),
                leftText = dietLabel(left.veganStatus),
                rightText = dietLabel(right.veganStatus),
                leftWins = left.veganStatus == DietStatus.SUITABLE && right.veganStatus != DietStatus.SUITABLE,
                rightWins = right.veganStatus == DietStatus.SUITABLE && left.veganStatus != DietStatus.SUITABLE
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ComparisonMetricRow(
                label = stringResource(R.string.compare_additives),
                leftText = left.additives.size.toString(),
                rightText = right.additives.size.toString(),
                leftWins = left.additives.size < right.additives.size,
                rightWins = right.additives.size < left.additives.size
            )
        }
    }
}

@Composable
private fun CompareNutritionCard(left: Product, right: Product) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.nutritional_values),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            NutritionCompareRow(
                label = stringResource(R.string.nutri_calories),
                leftValue = left.nutriments.energyKcal,
                rightValue = right.nutriments.energyKcal,
                isCalories = true,
                higherIsBetter = false
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NutritionCompareRow(
                label = stringResource(R.string.nutri_sugars),
                leftValue = left.nutriments.sugars,
                rightValue = right.nutriments.sugars,
                isCalories = false,
                higherIsBetter = false
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NutritionCompareRow(
                label = stringResource(R.string.nutri_sat_fat),
                leftValue = left.nutriments.saturatedFat,
                rightValue = right.nutriments.saturatedFat,
                isCalories = false,
                higherIsBetter = false
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NutritionCompareRow(
                label = stringResource(R.string.nutri_salt),
                leftValue = left.nutriments.salt,
                rightValue = right.nutriments.salt,
                isCalories = false,
                higherIsBetter = false
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NutritionCompareRow(
                label = stringResource(R.string.nutri_protein),
                leftValue = left.nutriments.proteins,
                rightValue = right.nutriments.proteins,
                isCalories = false,
                higherIsBetter = true
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NutritionCompareRow(
                label = stringResource(R.string.nutri_fiber),
                leftValue = left.nutriments.fiber,
                rightValue = right.nutriments.fiber,
                isCalories = false,
                higherIsBetter = true
            )
        }
    }
}

@Composable
private fun ComparisonMetricRow(
    label: String,
    leftText: String,
    rightText: String,
    leftWins: Boolean,
    rightWins: Boolean,
    leftBadgeColor: Color? = null,
    rightBadgeColor: Color? = null
) {
    val (safeBg, safeFg, _) = semanticSafeColors()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            MetricValueChip(
                text = leftText,
                isWinner = leftWins,
                badgeColor = leftBadgeColor,
                safeBg = safeBg,
                safeFg = safeFg
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            MetricValueChip(
                text = rightText,
                isWinner = rightWins,
                badgeColor = rightBadgeColor,
                safeBg = safeBg,
                safeFg = safeFg
            )
        }
    }
}

@Composable
private fun MetricValueChip(
    text: String,
    isWinner: Boolean,
    badgeColor: Color?,
    safeBg: Color,
    safeFg: Color
) {
    when {
        badgeColor != null -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isWinner) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(safeFg)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Surface(shape = RoundedCornerShape(7.dp), color = badgeColor) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        isWinner -> {
            Surface(shape = RoundedCornerShape(8.dp), color = safeBg) {
                Text(
                    text = text,
                    color = safeFg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        else -> {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NutritionCompareRow(
    label: String,
    leftValue: Double?,
    rightValue: Double?,
    isCalories: Boolean,
    higherIsBetter: Boolean
) {
    val leftWins = leftValue != null && rightValue != null && leftValue != rightValue &&
        (if (higherIsBetter) leftValue > rightValue else leftValue < rightValue)
    val rightWins = leftValue != null && rightValue != null && leftValue != rightValue &&
        (if (higherIsBetter) rightValue > leftValue else rightValue < leftValue)

    val leftText = when {
        leftValue == null -> "--"
        isCalories -> stringResource(R.string.value_kcal_format, leftValue.toInt())
        else -> stringResource(R.string.value_grams_format, leftValue)
    }
    val rightText = when {
        rightValue == null -> "--"
        isCalories -> stringResource(R.string.value_kcal_format, rightValue.toInt())
        else -> stringResource(R.string.value_grams_format, rightValue)
    }

    ComparisonMetricRow(
        label = label,
        leftText = leftText,
        rightText = rightText,
        leftWins = leftWins,
        rightWins = rightWins
    )
}

@Composable
private fun palmOilLabel(status: PalmOilStatus): String = when (status) {
    PalmOilStatus.FREE -> stringResource(R.string.palm_oil_free)
    PalmOilStatus.CONTAINS -> stringResource(R.string.contains_palm_oil)
    PalmOilStatus.MAY_CONTAIN -> stringResource(R.string.may_contain_palm_oil)
    PalmOilStatus.UNKNOWN -> stringResource(R.string.palm_oil_unknown)
}

@Composable
private fun dietLabel(status: DietStatus): String = when (status) {
    DietStatus.SUITABLE -> stringResource(R.string.diet_vegan_100)
    DietStatus.NOT_SUITABLE -> stringResource(R.string.diet_non_vegan)
    DietStatus.MAYBE -> stringResource(R.string.diet_maybe_vegan)
    DietStatus.UNKNOWN -> stringResource(R.string.diet_vegan_unknown)
}

private fun gradeRank(grade: String?): Int? = when (grade?.lowercase(Locale.ROOT)) {
    "a" -> 1
    "b" -> 2
    "c" -> 3
    "d" -> 4
    "e" -> 5
    else -> null
}

private fun nutriScoreColor(grade: String): Color = when (grade.lowercase(Locale.ROOT)) {
    "a" -> NutriScoreA
    "b" -> NutriScoreB
    "c" -> NutriScoreC
    "d" -> NutriScoreD
    "e" -> NutriScoreE
    else -> Color.Gray
}

private fun novaColor(nova: Int): Color = when (nova) {
    1 -> Nova1
    2 -> Nova2
    3 -> Nova3
    4 -> Nova4
    else -> Color.Gray
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
