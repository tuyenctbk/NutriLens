package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.semanticCautionColors
import com.example.ui.theme.semanticDangerColors
import com.example.ui.theme.semanticSafeColors

@Composable
fun NutriScoreCard(grade: String?, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    val cleanGrade = grade?.lowercase()
    val letters = listOf("a", "b", "c", "d", "e")
    val colors = listOf(NutriScoreA, NutriScoreB, NutriScoreC, NutriScoreD, NutriScoreE)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { showDialog = true },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.nutri_score),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (cleanGrade != null && cleanGrade in letters) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    letters.forEachIndexed { index, letter ->
                        val isSelected = letter == cleanGrade
                        val letterColor = colors[index]

                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 32.dp else 22.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isSelected) letterColor else letterColor.copy(alpha = 0.22f)),
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = stringResource(R.string.not_calculated),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.nutri_score),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.nutri_score_info_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.nutri_score_info_grades),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

@Composable
fun NovaGroupCard(group: Int?, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    val (color, title, subtitle) = when (group) {
        1 -> Triple(Nova1, stringResource(R.string.group_1_title), stringResource(R.string.unprocessed_minimal))
        2 -> Triple(Nova2, stringResource(R.string.group_2_title), stringResource(R.string.processed_culinary))
        3 -> Triple(Nova3, stringResource(R.string.group_3_title), stringResource(R.string.processed_food))
        4 -> Triple(Nova4, stringResource(R.string.group_4_title), stringResource(R.string.ultra_processed))
        else -> Triple(Color.Gray, stringResource(R.string.group_unknown), stringResource(R.string.processing_unrated))
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { showDialog = true },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.nova_classification),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = group?.toString() ?: "?",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = color
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.nova_classification),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.nova_info_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.nova_info_grades),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

@Composable
fun EcoScoreCard(grade: String?, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
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
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { showDialog = true },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.eco_score),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
            }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = null,
                    tint = color
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.eco_score),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.eco_score_info_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.eco_score_info_grades),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

@Composable
fun AdditiveChip(additive: AdditiveDetail, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }

    val (bg, textColor, borderC) = when (additive.risk) {
        AdditiveRisk.HIGH -> semanticDangerColors()
        AdditiveRisk.MODERATE -> semanticCautionColors()
        AdditiveRisk.SAFE -> semanticSafeColors()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderC, RoundedCornerShape(10.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = additive.code,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.width(5.dp))
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
                        text = stringResource(R.string.risk_level_label, localizedAdditiveRisk(additive.risk)),
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
