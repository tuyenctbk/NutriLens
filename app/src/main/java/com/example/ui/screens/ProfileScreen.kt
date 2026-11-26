package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.IconButton
import com.example.R
import com.example.data.HealthAnalyzer
import com.example.model.UserProfile
import com.example.ui.components.localizedAllergenName
import com.example.ui.components.localizedProfileName
import com.example.ui.theme.semanticDangerColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    onSelectProfile: (String) -> Unit = {},
    onCreateProfile: (String) -> Unit = {},
    onDeleteProfile: (String) -> Unit = {},
    onSetPantryRemindersEnabled: (Boolean) -> Unit = {},
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
    onOpenSupportDevelopment: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var customKeywordInput by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showNewProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    val (dangerBg, dangerFg, _) = semanticDangerColors()

    fun notifySaved(msg: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
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
                            text = stringResource(R.string.settings_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.settings_subtitle),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Section 0: Switchable Household Dietary Profiles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.household_profiles_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.household_profiles_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    userProfile.savedProfiles.forEach { householdProfile ->
                        val isActive = householdProfile.id == userProfile.activeProfileId
                        val displayProfileName = localizedProfileName(householdProfile.id, householdProfile.name)
                        InputChip(
                            selected = isActive,
                            onClick = {
                                onSelectProfile(householdProfile.id)
                                notifySaved(context.getString(R.string.household_profile_switched, displayProfileName))
                            },
                            label = {
                                Text(
                                    text = displayProfileName,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = if (isActive) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(InputChipDefaults.IconSize)
                                    )
                                }
                            } else null,
                            trailingIcon = if (!householdProfile.isBuiltIn) {
                                {
                                    IconButton(
                                        onClick = {
                                            onDeleteProfile(householdProfile.id)
                                            notifySaved(context.getString(R.string.household_profile_deleted, displayProfileName))
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.delete_profile_cd, displayProfileName),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("household_profile_chip_${householdProfile.id}")
                        )
                    }

                    AssistChip(
                        onClick = { showNewProfileDialog = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.add_profile_button)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_household_profile_button")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DataStore Status Banner
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
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
                            val activeProfileDisplay = localizedProfileName(userProfile.activeProfileId, userProfile.activeProfileName)
                            Text(
                                text = stringResource(R.string.active_profile_label, activeProfileDisplay),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.settings_intro),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 1: Monitored Allergies
                Text(
                    text = stringResource(R.string.allergies_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.allergies_subtitle),
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
                            },
                            label = {
                                Text(
                                    text = localizedAllergenName(allergenName),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = dangerFg,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = dangerBg,
                                selectedLabelColor = dangerFg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("allergen_chip_$allergenName")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 2: Dietary Lifestyle Restrictions
                Text(
                    text = stringResource(R.string.lifestyle_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PreferenceSwitchRow(
                            title = stringResource(R.string.gluten_free_title),
                            description = stringResource(R.string.gluten_free_desc),
                            checked = userProfile.isGlutenFree,
                            onCheckedChange = { checked -> onSetGlutenFree(checked) },
                            testTag = "switch_gluten_free"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        PreferenceSwitchRow(
                            title = stringResource(R.string.vegan_title),
                            description = stringResource(R.string.vegan_desc),
                            checked = userProfile.isVegan,
                            onCheckedChange = { checked -> onSetVegan(checked) },
                            testTag = "switch_vegan"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        PreferenceSwitchRow(
                            title = stringResource(R.string.vegetarian_title),
                            description = stringResource(R.string.vegetarian_desc),
                            checked = userProfile.isVegetarian,
                            onCheckedChange = { checked -> onSetVegetarian(checked) },
                            testTag = "switch_vegetarian"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 3: Custom Avoid Keywords
                Text(
                    text = stringResource(R.string.custom_avoid_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.custom_avoid_subtitle),
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
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val clean = customKeywordInput.trim()
                                if (clean.isNotBlank()) {
                                    onAddCustomKeyword(clean)
                                    notifySaved(context.getString(R.string.custom_keyword_added, clean))
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
                            val clean = customKeywordInput.trim()
                            if (clean.isNotBlank()) {
                                onAddCustomKeyword(clean)
                                notifySaved(context.getString(R.string.custom_keyword_added, clean))
                                customKeywordInput = ""
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("add_custom_keyword_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_term_cd))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (userProfile.customAvoidKeywords.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        userProfile.customAvoidKeywords.forEach { kw ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    onRemoveCustomKeyword(kw)
                                    notifySaved(context.getString(R.string.custom_keyword_removed, kw))
                                },
                                label = {
                                    Text(
                                        text = kw,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_term_cd, kw),
                                        modifier = Modifier.size(InputChipDefaults.IconSize)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 4: Health & Processing Guidelines
                Text(
                    text = stringResource(R.string.processing_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PreferenceSwitchRow(
                            title = stringResource(R.string.avoid_palm_oil_title),
                            description = stringResource(R.string.avoid_palm_oil_desc),
                            checked = userProfile.avoidPalmOil,
                            onCheckedChange = { checked -> onSetAvoidPalmOil(checked) },
                            testTag = "switch_palm_oil"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        PreferenceSwitchRow(
                            title = stringResource(R.string.avoid_nova4_title),
                            description = stringResource(R.string.avoid_nova4_desc),
                            checked = userProfile.avoidNova4UltraProcessed,
                            onCheckedChange = { checked -> onSetAvoidNova4(checked) },
                            testTag = "switch_nova_4"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        PreferenceSwitchRow(
                            title = stringResource(R.string.avoid_additives_title),
                            description = stringResource(R.string.avoid_additives_desc),
                            checked = userProfile.avoidHighRiskAdditives,
                            onCheckedChange = { checked -> onSetAvoidHighRiskAdditives(checked) },
                            testTag = "switch_additives"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        PreferenceSwitchRow(
                            title = stringResource(R.string.pantry_reminders_setting_title),
                            description = stringResource(R.string.pantry_reminders_setting_desc),
                            checked = userProfile.pantryRemindersEnabled,
                            onCheckedChange = { checked -> onSetPantryRemindersEnabled(checked) },
                            testTag = "switch_pantry_reminders"
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
                    onClick = onOpenSupportDevelopment,
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)),
                    modifier = Modifier.fillMaxWidth().testTag("profile_donation_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                text = if (userProfile.isSupporter) {
                                    stringResource(R.string.donation_active_supporter)
                                } else {
                                    stringResource(R.string.support_screen_title)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                            onClick = onOpenSupportDevelopment,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_support_development_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.support_open_screen_button))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Reset Settings
                OutlinedButton(
                    onClick = { showResetConfirmDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("reset_settings_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.reset_settings))
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showNewProfileDialog) {
        AlertDialog(
            onDismissRequest = { showNewProfileDialog = false },
            title = { Text(stringResource(R.string.new_profile_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.new_profile_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        placeholder = { Text(stringResource(R.string.new_profile_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_profile_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newProfileName.trim()
                        if (clean.isNotBlank()) {
                            onCreateProfile(clean)
                            notifySaved(context.getString(R.string.household_profile_created, clean))
                            newProfileName = ""
                            showNewProfileDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_new_profile_button")
                ) {
                    Text(stringResource(R.string.create_profile_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProfileDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
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
                        notifySaved(context.getString(R.string.prefs_reset_toast))
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
