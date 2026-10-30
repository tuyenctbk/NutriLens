package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AdditiveDetail
import com.example.model.AdditiveRisk
import com.example.ui.theme.Nova1
import com.example.ui.theme.Nova2
import com.example.ui.theme.Nova3
import com.example.ui.theme.Nova4
import com.example.ui.theme.NutriScoreA
import com.example.ui.theme.NutriScoreB
import com.example.ui.theme.NutriScoreC
import com.example.ui.theme.NutriScoreD
import com.example.ui.theme.NutriScoreE

@Composable
fun NutriScoreCard(grade: String?, modifier: Modifier = Modifier) {
    val cleanGrade = grade?.lowercase()
    val letters = listOf("a", "b", "c", "d", "e")
    val colors = listOf(NutriScoreA, NutriScoreB, NutriScoreC, NutriScoreD, NutriScoreE)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.nutri_score),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (cleanGrade != null && cleanGrade in letters) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    letters.forEachIndexed { index, letter ->
                        val isSelected = letter == cleanGrade
                        val letterColor = colors[index]

                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 30.dp else 22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) letterColor else letterColor.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter.uppercase(),
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) Color.White else letterColor.copy(alpha = 0.9f),
                                fontSize = if (isSelected) 15.sp else 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                val desc = when (cleanGrade) {
                    "a" -> stringResource(R.string.optimal_nutritional_quality)
                    "b" -> stringResource(R.string.good_nutritional_quality)
                    "c" -> stringResource(R.string.moderate_nutritional_quality)
                    "d" -> stringResource(R.string.poor_nutritional_quality)
                    "e" -> stringResource(R.string.low_nutritional_quality)
                    else -> ""
                }
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = stringResource(R.string.not_calculated),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NovaGroupCard(group: Int?, modifier: Modifier = Modifier) {
    val (color, title, subtitle) = when (group) {
        1 -> Triple(Nova1, stringResource(R.string.group_1_title), stringResource(R.string.unprocessed_minimal))
        2 -> Triple(Nova2, stringResource(R.string.group_2_title), stringResource(R.string.processed_culinary))
        3 -> Triple(Nova3, stringResource(R.string.group_3_title), stringResource(R.string.processed_food))
        4 -> Triple(Nova4, stringResource(R.string.group_4_title), stringResource(R.string.ultra_processed))
        else -> Triple(Color.Gray, stringResource(R.string.group_unknown), stringResource(R.string.processing_unrated))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.nova_classification),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = group?.toString() ?: "?",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EcoScoreCard(grade: String?, modifier: Modifier = Modifier) {
    val cleanGrade = grade?.lowercase()
    val (color, desc) = when (cleanGrade) {
        "a" -> Pair(NutriScoreA, stringResource(R.string.very_low_eco_impact))
        "b" -> Pair(NutriScoreB, stringResource(R.string.low_eco_impact))
        "c" -> Pair(NutriScoreC, stringResource(R.string.moderate_eco_impact))
        "d" -> Pair(NutriScoreD, stringResource(R.string.high_eco_impact))
        "e" -> Pair(NutriScoreE, stringResource(R.string.very_high_eco_impact))
        else -> Pair(Color.Gray, stringResource(R.string.eco_score_unknown))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.eco_score),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = stringResource(R.string.eco_score),
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = cleanGrade?.uppercase() ?: "-",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdditiveChip(additive: AdditiveDetail, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }

    val (bg, textColor, borderC) = when (additive.risk) {
        AdditiveRisk.HIGH -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Color(0xFFEF9A9A))
        AdditiveRisk.MODERATE -> Triple(Color(0xFFFFF8E1), Color(0xFFEF6C00), Color(0xFFFFE082))
        AdditiveRisk.SAFE -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFFA5D6A7))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderC, RoundedCornerShape(8.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = additive.code,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = additive.name,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.9f)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = {
                Icon(
                    imageVector = if (additive.risk == AdditiveRisk.HIGH) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = textColor
                )
            },
            title = {
                Text(text = "${additive.code}: ${additive.name}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.category_label, additive.purpose),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.risk_level_label, additive.risk.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                    additive.warning?.let { warn ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = warn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
