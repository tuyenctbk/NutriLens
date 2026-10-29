package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HealthAnalyzer
import com.example.model.UserProfile
import com.example.ui.theme.Nova1
import com.example.ui.theme.Nova2
import com.example.ui.theme.Nova3
import com.example.ui.theme.Nova4
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Favorite

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    onToggleAllergen: (String, Boolean) -> Unit,
    onSetVegan: (Boolean) -> Unit,
    onSetVegetarian: (Boolean) -> Unit,
    onSetGlutenFree: (Boolean) -> Unit,
    onAddCustomKeyword: (String) -> Unit,
    onRemoveCustomKeyword: (String) -> Unit,
    onSetAvoidPalmOil: (Boolean) -> Unit,
    onSetAvoidNova4: (Boolean) -> Unit,
    onSetAvoidHighRiskAdditives: (Boolean) -> Unit,
    onResetToDefaults: () -> Unit,
    onOpenDonationDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var customKeywordInput by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    fun notifySaved(msg: String = "Dietary preference saved to DataStore") {
        scope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dietary & Allergen Settings",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Saved automatically to Jetpack DataStore",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // DataStore Status Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Personalized Food Safety Profile",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Define your allergies and dietary restrictions. NutriLens automatically evaluates every scanned product against these saved preferences.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Monitored Allergies
            Text(
                text = "1. Food Allergies & Sensitivities",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap any allergen to enable/disable monitoring:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HealthAnalyzer.KNOWN_ALLERGENS.forEach { (allergenName, _) ->
                    val isSelected = userProfile.monitoredAllergens.contains(allergenName)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newSelected = !isSelected
                            onToggleAllergen(allergenName, newSelected)
                            notifySaved("${if (newSelected) "Added" else "Removed"} $allergenName allergy")
                        },
                        label = {
                            Text(
                                text = allergenName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEBEE),
                            selectedLabelColor = Color(0xFFC62828)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("allergen_chip_$allergenName")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Dietary Lifestyle Restrictions
            Text(
                text = "2. Dietary Restrictions & Lifestyles",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PreferenceSwitchRow(
                        title = "Gluten-Free Diet",
                        description = "Strictly flag wheat, barley, rye, spelt, oats, or gluten traces",
                        checked = userProfile.isGlutenFree,
                        onCheckedChange = { checked ->
                            onSetGlutenFree(checked)
                            notifySaved("Gluten-Free setting updated in DataStore")
                        },
                        testTag = "switch_gluten_free"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    PreferenceSwitchRow(
                        title = "Vegan Diet",
                        description = "Flag dairy, eggs, honey, gelatin, or animal derivatives",
                        checked = userProfile.isVegan,
                        onCheckedChange = { checked ->
                            onSetVegan(checked)
                            notifySaved("Vegan setting updated in DataStore")
                        },
                        testTag = "switch_vegan"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    PreferenceSwitchRow(
                        title = "Vegetarian Diet",
                        description = "Flag meat, poultry, fish, gelatin, or animal rennet",
                        checked = userProfile.isVegetarian,
                        onCheckedChange = { checked ->
                            onSetVegetarian(checked)
                            notifySaved("Vegetarian setting updated in DataStore")
                        },
                        testTag = "switch_vegetarian"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Custom Avoid Keywords
            Text(
                text = "3. Custom Ingredients Avoid List",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add custom ingredient terms to flag (e.g. MSG, Caffeine, Red 40, Corn Syrup):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customKeywordInput,
                    onValueChange = { customKeywordInput = it },
                    placeholder = { Text(stringResource(R.string.custom_avoid_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (customKeywordInput.isNotBlank()) {
                                onAddCustomKeyword(customKeywordInput.trim())
                                notifySaved("Added custom avoid keyword: ${customKeywordInput.trim()}")
                                customKeywordInput = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_keyword_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (customKeywordInput.isNotBlank()) {
                            onAddCustomKeyword(customKeywordInput.trim())
                            notifySaved("Added custom avoid keyword: ${customKeywordInput.trim()}")
                            customKeywordInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_custom_keyword_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add term")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (userProfile.customAvoidKeywords.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    userProfile.customAvoidKeywords.forEach { kw ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = kw,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove $kw",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            onRemoveCustomKeyword(kw)
                                            notifySaved("Removed $kw")
                                        }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 4: Health & Processing Guidelines
            Text(
                text = "4. Ingredient Processing Guidelines",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PreferenceSwitchRow(
                        title = "Avoid Palm Oil",
                        description = "Flag palm oil, palm kernel, and palmolein fractions",
                        checked = userProfile.avoidPalmOil,
                        onCheckedChange = { checked ->
                            onSetAvoidPalmOil(checked)
                            notifySaved("Palm Oil setting saved")
                        },
                        testTag = "switch_palm_oil"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    PreferenceSwitchRow(
                        title = "NOVA 4 Ultra-Processed Alert",
                        description = "Warn when a product is classified as ultra-processed",
                        checked = userProfile.avoidNova4UltraProcessed,
                        onCheckedChange = { checked ->
                            onSetAvoidNova4(checked)
                            notifySaved("NOVA 4 setting saved")
                        },
                        testTag = "switch_nova_4"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    PreferenceSwitchRow(
                        title = "Avoid High-Risk Additives",
                        description = "Flag artificial dyes, nitrites (E249-E252), aspartame (E951), BHA/BHT",
                        checked = userProfile.avoidHighRiskAdditives,
                        onCheckedChange = { checked ->
                            onSetAvoidHighRiskAdditives(checked)
                            notifySaved("High-risk additives setting saved")
                        },
                        testTag = "switch_additives"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 5: Support Independent Development
            Text(
                text = stringResource(R.string.support_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("profile_donation_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (userProfile.isSupporter) stringResource(R.string.donation_active_supporter) else stringResource(R.string.donation_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (userProfile.isSupporter) {
                            stringResource(R.string.donation_thank_you)
                        } else {
                            stringResource(R.string.support_section_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenDonationDialog,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.support_dev_button))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reset Settings
            OutlinedButton(
                onClick = { showResetConfirmDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("reset_settings_button")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reset_settings))
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(stringResource(R.string.reset_dialog_title)) },
            text = { Text(stringResource(R.string.reset_dialog_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetToDefaults()
                        notifySaved("Preferences reset to defaults")
                    }
                ) {
                    Text(stringResource(R.string.reset_defaults), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier
        )
    }
}
