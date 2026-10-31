package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.NutrimentsInfo

@Composable
fun NutritionalBarChart(
    nutriments: NutrimentsInfo,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("nutritional_bar_chart")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Title & Subtitle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.nutrition_chart_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.nutrition_chart_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calories Bar (Reference: 800 kcal max scale for 100g)
            val caloriesVal = nutriments.energyKcal ?: 0.0
            BarChartItem(
                label = stringResource(R.string.nutri_calories),
                displayValue = if (nutriments.energyKcal != null) "${caloriesVal.toInt()} kcal" else "--",
                fraction = (caloriesVal / 800.0).coerceIn(0.0, 1.0).toFloat(),
                barColor = Color(0xFFF57C00),
                icon = Icons.Default.LocalFireDepartment
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Total Fats Bar (Reference: 50g max scale for 100g)
            val fatVal = nutriments.fat ?: 0.0
            BarChartItem(
                label = stringResource(R.string.nutri_fat),
                displayValue = if (nutriments.fat != null) "${"%.1f".format(fatVal)} g" else "--",
                fraction = (fatVal / 50.0).coerceIn(0.0, 1.0).toFloat(),
                barColor = Color(0xFFE65100),
                icon = Icons.Default.Opacity
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Carbohydrates Bar (Reference: 100g max scale)
            val carbsVal = nutriments.carbs ?: 0.0
            BarChartItem(
                label = stringResource(R.string.nutri_carbs),
                displayValue = if (nutriments.carbs != null) "${"%.1f".format(carbsVal)} g" else "--",
                fraction = (carbsVal / 100.0).coerceIn(0.0, 1.0).toFloat(),
                barColor = Color(0xFF0288D1),
                icon = Icons.Default.Grain
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Proteins Bar (Reference: 40g max scale)
            val proteinVal = nutriments.proteins ?: 0.0
            BarChartItem(
                label = stringResource(R.string.nutri_protein),
                displayValue = if (nutriments.proteins != null) "${"%.1f".format(proteinVal)} g" else "--",
                fraction = (proteinVal / 40.0).coerceIn(0.0, 1.0).toFloat(),
                barColor = Color(0xFF2E7D32),
                icon = Icons.Default.FitnessCenter
            )
        }
    }
}

@Composable
private fun BarChartItem(
    label: String,
    displayValue: String,
    fraction: Float,
    barColor: Color,
    icon: ImageVector
) {
    var animationTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    val animatedFraction by animateFloatAsState(
        targetValue = if (animationTriggered) fraction else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "BarChartAnimation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress Track & Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(barColor.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction.coerceAtLeast(0.02f))
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor)
            )
        }
    }
}
