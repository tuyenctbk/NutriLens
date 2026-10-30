package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.stringResource
import com.example.R
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.AdditiveRisk
import com.example.model.PalmOilStatus
import com.example.model.Product
import com.example.ui.HistoryFilter
import com.example.ui.theme.Nova1
import com.example.ui.theme.Nova2
import com.example.ui.theme.Nova3
import com.example.ui.theme.Nova4
import com.example.ui.theme.NutriScoreA
import com.example.ui.theme.NutriScoreB
import com.example.ui.theme.NutriScoreC
import com.example.ui.theme.NutriScoreD
import com.example.ui.theme.NutriScoreE
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    products: List<Product>,
    currentFilter: HistoryFilter,
    searchQuery: String,
    onFilterChange: (HistoryFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onProductClick: (String) -> Unit,
    onToggleFavorite: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onClearAll: () -> Unit,
    onSeedSamples: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, currentFilter, searchQuery) {
        products.filter { prod ->
            val matchesSearch = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.brand.contains(searchQuery, ignoreCase = true) ||
                prod.barcode.contains(searchQuery) ||
                prod.allergens.any { it.contains(searchQuery, ignoreCase = true) } ||
                prod.additives.any { it.code.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true) }

            val matchesFilter = when (currentFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.FAVORITES -> prod.isFavorite
                HistoryFilter.ALERTED -> prod.novaGroup == 4 ||
                    prod.palmOilStatus == PalmOilStatus.CONTAINS ||
                    prod.allergens.isNotEmpty() ||
                    prod.additives.any { it.risk == AdditiveRisk.HIGH }
                HistoryFilter.CLEAN_SAFE -> (prod.novaGroup == null || prod.novaGroup in 1..2) &&
                    prod.palmOilStatus == PalmOilStatus.FREE &&
                    prod.additives.none { it.risk == AdditiveRisk.HIGH }
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.scan_history_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (products.isEmpty()) stringResource(R.string.offline_room_cache_title) else stringResource(R.string.scan_history_subtitle_count, products.size),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (products.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.clear_scans_cd))
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
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("history_search_input"),
                placeholder = { Text(stringResource(R.string.search_history_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentFilter == HistoryFilter.ALL,
                    onClick = { onFilterChange(HistoryFilter.ALL) },
                    label = { Text("${stringResource(R.string.filter_all)} (${products.size})") },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = currentFilter == HistoryFilter.FAVORITES,
                    onClick = { onFilterChange(HistoryFilter.FAVORITES) },
                    label = { Text(stringResource(R.string.filter_favorites)) },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = currentFilter == HistoryFilter.ALERTED,
                    onClick = { onFilterChange(HistoryFilter.ALERTED) },
                    label = { Text(stringResource(R.string.filter_warnings)) },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = currentFilter == HistoryFilter.CLEAN_SAFE,
                    onClick = { onFilterChange(HistoryFilter.CLEAN_SAFE) },
                    label = { Text(stringResource(R.string.filter_clean)) },
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredProducts.isEmpty()) {
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
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) stringResource(R.string.no_matching_products) else stringResource(R.string.no_scan_history),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    stringResource(R.string.pantry_empty_search_desc)
                                } else {
                                    stringResource(R.string.empty_history_desc)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            if (products.isEmpty()) {
                                Button(
                                    onClick = onSeedSamples,
                                    modifier = Modifier.fillMaxWidth().testTag("seed_sample_scans_button")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.populate_sample_scans))
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
                    items(filteredProducts, key = { it.barcode }) { product ->
                        ProductHistoryItemCard(
                            product = product,
                            onClick = { onProductClick(product.barcode) },
                            onToggleFavorite = { onToggleFavorite(product) },
                            onDelete = { onDeleteProduct(product.barcode) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearAll()
                    }
                ) {
                    Text(stringResource(R.string.clear_all), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductHistoryItemCard(
    product: Product,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("history_item_${product.barcode}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Product Thumbnail
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
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
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.brand,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Relative scan time & barcode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatScanTimestamp(product.scannedAt),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = product.barcode,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Actions: Bookmark & Delete
                Row {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.bookmark_cd),
                            tint = if (product.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_scan_cd),
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ingredient Check Snapshot Badges
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // NutriScore Pill
                product.nutriscoreGrade?.let { grade ->
                    val (nsColor, _) = when (grade.lowercase()) {
                        "a" -> Pair(NutriScoreA, "A")
                        "b" -> Pair(NutriScoreB, "B")
                        "c" -> Pair(NutriScoreC, "C")
                        "d" -> Pair(NutriScoreD, "D")
                        "e" -> Pair(NutriScoreE, "E")
                        else -> Pair(Color.Gray, "-")
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = nsColor
                    ) {
                        Text(
                            text = stringResource(R.string.nutri_score_badge, grade.uppercase()),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // NOVA Classification Pill
                product.novaGroup?.let { nova ->
                    val novaColor = when (nova) {
                        1 -> Nova1
                        2 -> Nova2
                        3 -> Nova3
                        4 -> Nova4
                        else -> Color.Gray
                    }
                    val label = when (nova) {
                        1 -> stringResource(R.string.nova_1_natural)
                        2 -> stringResource(R.string.nova_2_culinary)
                        3 -> stringResource(R.string.nova_3_processed)
                        4 -> stringResource(R.string.nova_4_ultra_processed)
                        else -> stringResource(R.string.nova_generic, nova)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = novaColor
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Palm Oil Status
                if (product.palmOilStatus == PalmOilStatus.CONTAINS) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = stringResource(R.string.palm_oil_badge),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                } else if (product.palmOilStatus == PalmOilStatus.FREE) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = stringResource(R.string.palm_oil_free_badge),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Allergens summary
                if (product.allergens.isNotEmpty()) {
                    val allergenText = product.allergens.take(2).joinToString(", ") +
                        if (product.allergens.size > 2) " +${product.allergens.size - 2}" else ""
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = "⚠️ $allergenText",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Additives summary
                if (product.additives.isNotEmpty()) {
                    val hasHighRisk = product.additives.any { it.risk == AdditiveRisk.HIGH }
                    val chipColor = if (hasHighRisk) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
                    val textColor = if (hasHighRisk) Color(0xFFC62828) else Color(0xFFEF6C00)

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = chipColor
                    ) {
                        Text(
                            text = stringResource(R.string.additives_count_badge, product.additives.size),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Revisit Check Action Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.revisit_analysis),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun formatScanTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return stringResource(R.string.time_recent)
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000L -> stringResource(R.string.time_just_now)
        diff < 3600_000L -> stringResource(R.string.time_minutes_ago, (diff / 60_000L).toInt())
        diff < 86400_000L -> stringResource(R.string.time_hours_ago, (diff / 3600_000L).toInt())
        diff < 172800_000L -> stringResource(R.string.time_yesterday)
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

