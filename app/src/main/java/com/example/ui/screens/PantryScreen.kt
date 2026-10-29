package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.model.PantryItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PantryFilter {
    ALL,
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED,
    CONSUMED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    pantryItems: List<PantryItem>,
    onToggleConsumed: (String, Boolean) -> Unit,
    onUpdateExpiration: (String, Long) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearPantry: () -> Unit,
    onSeedPantry: () -> Unit,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentFilter by remember { mutableStateOf(PantryFilter.ACTIVE) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var editingItemForDate by remember { mutableStateOf<PantryItem?>(null) }

    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val threeDaysMs = 1000L * 60L * 60L * 24L * 3L

    val filteredItems = remember(pantryItems, currentFilter, searchQuery) {
        pantryItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.brand.contains(searchQuery, ignoreCase = true) ||
                item.barcode.contains(searchQuery)

            val isExpired = item.expirationDate < now && !item.isConsumed
            val isExpiringSoon = item.expirationDate in now..(now + threeDaysMs) && !item.isConsumed

            val matchesFilter = when (currentFilter) {
                PantryFilter.ALL -> true
                PantryFilter.ACTIVE -> !item.isConsumed && !isExpired
                PantryFilter.EXPIRING_SOON -> isExpiringSoon
                PantryFilter.EXPIRED -> isExpired
                PantryFilter.CONSUMED -> item.isConsumed
            }

            matchesSearch && matchesFilter
        }
    }

    val activeCount = pantryItems.count { !it.isConsumed && it.expirationDate >= now }
    val expiringCount = pantryItems.count { !it.isConsumed && it.expirationDate in now..(now + threeDaysMs) }
    val expiredCount = pantryItems.count { !it.isConsumed && it.expirationDate < now }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.pantry_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (pantryItems.isEmpty()) stringResource(R.string.pantry_subtitle_empty) else stringResource(R.string.pantry_subtitle_count, pantryItems.size),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    if (pantryItems.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("clear_pantry_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.clear_pantry_desc))
                        }
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
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("pantry_search_input"),
                placeholder = { Text(stringResource(R.string.pantry_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = currentFilter == PantryFilter.ACTIVE,
                    onClick = { currentFilter = PantryFilter.ACTIVE },
                    label = { Text(stringResource(R.string.filter_active, activeCount)) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("filter_chip_active")
                )
                FilterChip(
                    selected = currentFilter == PantryFilter.EXPIRING_SOON,
                    onClick = { currentFilter = PantryFilter.EXPIRING_SOON },
                    label = { Text(stringResource(R.string.filter_expiring_soon, expiringCount)) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("filter_chip_expiring")
                )
                FilterChip(
                    selected = currentFilter == PantryFilter.EXPIRED,
                    onClick = { currentFilter = PantryFilter.EXPIRED },
                    label = { Text(stringResource(R.string.filter_expired, expiredCount)) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("filter_chip_expired")
                )
                FilterChip(
                    selected = currentFilter == PantryFilter.CONSUMED,
                    onClick = { currentFilter = PantryFilter.CONSUMED },
                    label = { Text(stringResource(R.string.filter_consumed)) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("filter_chip_consumed")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Kitchen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) stringResource(R.string.no_matching_products) else stringResource(R.string.pantry_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    stringResource(R.string.pantry_empty_search_desc)
                                } else {
                                    stringResource(R.string.pantry_empty_desc)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            if (pantryItems.isEmpty()) {
                                Button(
                                    onClick = onSeedPantry,
                                    modifier = Modifier.fillMaxWidth().testTag("seed_pantry_button")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.populate_sample_pantry))
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.barcode }) { item ->
                        PantryItemCard(
                            item = item,
                            onToggleConsumed = { consumed -> onToggleConsumed(item.barcode, consumed) },
                            onEditExpiration = { editingItemForDate = item },
                            onDelete = { onDeleteItem(item.barcode) },
                            onClick = { onProductClick(item.barcode) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Expiration Date Picker Dialog
    if (editingItemForDate != null) {
        val item = editingItemForDate!!
        val calendar = Calendar.getInstance().apply { timeInMillis = item.expirationDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply { set(year, month, dayOfMonth, 23, 59, 59) }
                onUpdateExpiration(item.barcode, newCal.timeInMillis)
                editingItemForDate = null
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { editingItemForDate = null }
        }.show()
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_pantry_confirm_title)) },
            text = { Text(stringResource(R.string.clear_pantry_confirm_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearPantry()
                    }
                ) {
                    Text(stringResource(R.string.clear_all), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@Composable
private fun PantryItemCard(
    item: PantryItem,
    onToggleConsumed: (Boolean) -> Unit,
    onEditExpiration: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val threeDaysMs = 1000L * 60L * 60L * 24L * 3L
    val isExpired = item.expirationDate < now && !item.isConsumed
    val isExpiringSoon = item.expirationDate in now..(now + threeDaysMs) && !item.isConsumed

    val (statusBg, statusText, statusColor) = when {
        item.isConsumed -> Triple(Color(0xFFEEEEEE), stringResource(R.string.status_consumed), Color(0xFF757575))
        isExpired -> Triple(Color(0xFFFFEBEE), stringResource(R.string.status_expired), Color(0xFFC62828))
        isExpiringSoon -> Triple(Color(0xFFFFF8E1), stringResource(R.string.status_expiring_soon), Color(0xFFEF6C00))
        else -> Triple(Color(0xFFE8F5E9), stringResource(R.string.status_fresh), Color(0xFF2E7D32))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("pantry_item_${item.barcode}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Consumed checkbox & Thumbnail
                Checkbox(
                    checked = item.isConsumed,
                    onCheckedChange = onToggleConsumed,
                    modifier = Modifier.testTag("checkbox_consumed_${item.barcode}")
                )

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.NoFood,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.brand,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!item.quantity.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.net_qty, item.quantity),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_item),
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expiration status pill & date picker trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onEditExpiration)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.expires_on, formatDate(item.expirationDate)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
}
