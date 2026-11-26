package com.example.ui.screens

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.PantryItem
import com.example.ui.theme.semanticCautionColors
import com.example.ui.theme.semanticDangerColors
import com.example.ui.theme.semanticNeutralColors
import com.example.ui.theme.semanticSafeColors
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
    pantryRemindersEnabled: Boolean = true,
    onTogglePantryReminders: (Boolean) -> Unit = {},
    onCheckExpiringNow: ((Int) -> Unit) -> Unit = {},
    onToggleConsumed: (String, Boolean) -> Unit,
    onUpdateExpiration: (String, Long) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearPantry: () -> Unit,
    onScanProduct: () -> Unit,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentFilter by remember { mutableStateOf(PantryFilter.ACTIVE) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var editingItemForDate by remember { mutableStateOf<PantryItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val now = remember(pantryItems) { System.currentTimeMillis() }
    val threeDaysMs = 1000L * 60L * 60L * 24L * 3L

    val filteredItems = remember(pantryItems, currentFilter, searchQuery, now) {
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

    val activeCount = remember(pantryItems, now) {
        pantryItems.count { !it.isConsumed && it.expirationDate >= now }
    }
    val expiringCount = remember(pantryItems, now) {
        pantryItems.count { !it.isConsumed && it.expirationDate in now..(now + threeDaysMs) }
    }
    val expiredCount = remember(pantryItems, now) {
        pantryItems.count { !it.isConsumed && it.expirationDate < now }
    }

    // Properly manage native DatePickerDialog lifecycle inside DisposableEffect with Activity safety
    DisposableEffect(editingItemForDate) {
        val item = editingItemForDate
        val hostActivity = context.findPantryActivity()
        val dialog = if (item != null && hostActivity != null && !hostActivity.isFinishing && !hostActivity.isDestroyed) {
            val calendar = Calendar.getInstance().apply { timeInMillis = item.expirationDate }
            DatePickerDialog(
                hostActivity,
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
                setOnDismissListener { editingItemForDate = null }
                show()
            }
        } else {
            if (item != null) editingItemForDate = null
            null
        }
        onDispose {
            dialog?.dismiss()
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
                            text = stringResource(R.string.pantry_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (pantryItems.isEmpty()) {
                                stringResource(R.string.pantry_subtitle_empty)
                            } else {
                                stringResource(R.string.pantry_subtitle_count, pantryItems.size)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val next = !pantryRemindersEnabled
                            onTogglePantryReminders(next)
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    context.getString(
                                        if (next) R.string.pantry_reminders_enabled_toast
                                        else R.string.pantry_reminders_disabled_toast
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("pantry_reminders_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (pantryRemindersEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = stringResource(R.string.pantry_reminders_toggle_cd),
                            tint = if (pantryRemindersEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
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
                    shape = RoundedCornerShape(14.dp)
                )

                // Filter Chips (with live item counts across all locales)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentFilter == PantryFilter.ACTIVE,
                        onClick = { currentFilter = PantryFilter.ACTIVE },
                        label = { Text("${stringResource(R.string.filter_active)} ($activeCount)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("filter_chip_active")
                    )
                    FilterChip(
                        selected = currentFilter == PantryFilter.EXPIRING_SOON,
                        onClick = { currentFilter = PantryFilter.EXPIRING_SOON },
                        label = { Text("${stringResource(R.string.filter_expiring_soon)} ($expiringCount)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("filter_chip_expiring")
                    )
                    FilterChip(
                        selected = currentFilter == PantryFilter.EXPIRED,
                        onClick = { currentFilter = PantryFilter.EXPIRED },
                        label = { Text("${stringResource(R.string.filter_expired)} ($expiredCount)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("filter_chip_expired")
                    )
                    FilterChip(
                        selected = currentFilter == PantryFilter.CONSUMED,
                        onClick = { currentFilter = PantryFilter.CONSUMED },
                        label = { Text(stringResource(R.string.filter_consumed)) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("filter_chip_consumed")
                    )
                }

                // Smart Expiration Reminders Banner
                if (pantryItems.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (pantryRemindersEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (pantryRemindersEnabled) {
                                    stringResource(R.string.pantry_reminders_banner_on)
                                } else {
                                    stringResource(R.string.pantry_reminders_banner_off)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    onCheckExpiringNow { count ->
                                        scope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(
                                                if (count > 0) {
                                                    context.getString(R.string.pantry_check_found_toast, count)
                                                } else {
                                                    context.getString(R.string.pantry_check_none_toast)
                                                }
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("pantry_check_reminders_button")
                            ) {
                                Text(stringResource(R.string.pantry_check_now_button), fontSize = 12.sp)
                            }
                        }
                    }
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
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
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
                                    text = if (pantryItems.isNotEmpty()) {
                                        stringResource(R.string.no_matching_products)
                                    } else {
                                        stringResource(R.string.pantry_empty_title)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (pantryItems.isNotEmpty()) {
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
                                        onClick = onScanProduct,
                                        modifier = Modifier.fillMaxWidth().testTag("pantry_scan_product_button")
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.scan_a_product))
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = {
                                            searchQuery = ""
                                            currentFilter = PantryFilter.ALL
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reset_pantry_filter_button")
                                    ) {
                                        Text(stringResource(R.string.filter_all))
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
                                now = now,
                                onToggleConsumed = { consumed -> onToggleConsumed(item.barcode, consumed) },
                                onEditExpiration = { editingItemForDate = item },
                                onDelete = { onDeleteItem(item.barcode) },
                                onClick = { onProductClick(item.barcode) },
                                modifier = Modifier.animateItem()
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
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
    now: Long,
    onToggleConsumed: (Boolean) -> Unit,
    onEditExpiration: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val threeDaysMs = 1000L * 60L * 60L * 24L * 3L
    val isExpired = item.expirationDate < now && !item.isConsumed
    val isExpiringSoon = item.expirationDate in now..(now + threeDaysMs) && !item.isConsumed

    val (statusBg, statusColor, _) = when {
        item.isConsumed -> semanticNeutralColors()
        isExpired -> semanticDangerColors()
        isExpiringSoon -> semanticCautionColors()
        else -> semanticSafeColors()
    }
    val statusText = when {
        item.isConsumed -> stringResource(R.string.status_consumed)
        isExpired -> stringResource(R.string.status_expired)
        isExpiringSoon -> stringResource(R.string.status_expiring_soon)
        else -> stringResource(R.string.status_fresh)
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (item.isConsumed) 0.62f else 1f,
        animationSpec = tween(240),
        label = "pantry_consumed_alpha"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("pantry_item_${item.barcode}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = item.isConsumed,
                    onCheckedChange = onToggleConsumed,
                    modifier = Modifier.testTag("checkbox_consumed_${item.barcode}")
                )

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .alpha(contentAlpha)
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(contentAlpha)
                ) {
                    Text(
                        text = item.brand,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.isConsumed) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!item.quantity.isNullOrBlank()) {
                        Text(
                            text = "${stringResource(R.string.net_qty)}: ${item.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_item),
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
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
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onEditExpiration)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                        text = "${stringResource(R.string.expires_on)}: ${formatDate(item.expirationDate)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private val pantryDateFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
}

private fun formatDate(timestamp: Long): String {
    return pantryDateFormatter.get()?.format(Date(timestamp)).orEmpty()
}

private tailrec fun Context.findPantryActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findPantryActivity()
    else -> null
}
