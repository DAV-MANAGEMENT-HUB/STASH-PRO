package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LibraryViewMode
import com.example.data.model.StashItem
import com.example.data.model.ThemeMode
import com.example.data.model.TrackerItem
import com.example.engine.StorageOverview
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StashItemCard
import com.example.ui.components.StashLogo
import com.example.ui.components.TrackerBadge
import com.example.ui.theme.Amber500
import com.example.ui.theme.RoseRed

@Composable
fun HomeScreen(
    items: List<StashItem>,
    screenshots: List<StashItem>,
    activeTrackers: List<TrackerItem>,
    suggestedTrackers: List<TrackerItem>,
    storageOverview: StorageOverview,
    themeMode: ThemeMode,
    searchQuery: String,
    searchResults: List<StashItem>,
    isScanning: Boolean,
    scanNotice: String?,
    onSearchChange: (String) -> Unit,
    onItemClick: (StashItem) -> Unit,
    onAddClick: () -> Unit,
    onScanClick: () -> Unit,
    onCloudClick: () -> Unit,
    onStorageClick: () -> Unit,
    onCycleTheme: () -> Unit,
    onDismissNotice: () -> Unit,
    onViewAllItemsClick: () -> Unit,
    onViewAllTrackersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val needsAttentionList = activeTrackers.filter { it.isNeedsAttention() }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Top Bar with Logo & Controls
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StashLogo()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Theme Toggle
                        IconButton(onClick = onCycleTheme, modifier = Modifier.testTag("theme_toggle_button")) {
                            val icon = when (themeMode) {
                                ThemeMode.SYSTEM -> Icons.Default.Brightness4
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                            }
                            Icon(icon, contentDescription = "Toggle Theme", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Storage Button
                        IconButton(onClick = onStorageClick, modifier = Modifier.testTag("home_storage_button")) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Storage",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Media Scan Button
                        IconButton(
                            onClick = onScanClick,
                            enabled = !isScanning,
                            modifier = Modifier.testTag("home_scan_button")
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = "Scan Media",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Cloud Sync Button
                        IconButton(onClick = onCloudClick, modifier = Modifier.testTag("home_cloud_button")) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Cloud & Vercel Sync",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Notice Banner
            if (scanNotice != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = scanNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissNotice) {
                                Text("OK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Global Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .testTag("universal_search_input"),
                    placeholder = { Text("Search vault & trash...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Live Search Results
            if (searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = "Search Results (${searchResults.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                if (searchResults.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "No matches found",
                            message = "No items in your stash or trash match \"$searchQuery\"."
                        )
                    }
                } else {
                    items(searchResults) { matchItem ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            Column {
                                if (matchItem.isTrashed) {
                                    Surface(
                                        color = RoseRed.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = RoseRed, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "IN TRASH (Deletes in ${matchItem.trashDaysRemaining()}d)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = RoseRed
                                            )
                                        }
                                    }
                                }
                                StashItemCard(
                                    item = matchItem,
                                    viewMode = LibraryViewMode.LIST,
                                    onClick = { onItemClick(matchItem) }
                                )
                            }
                        }
                    }
                }
            } else {
                // If vault is completely empty
                if (items.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "Your stash is empty.",
                            message = "Save something important and STASH will organize it for you.",
                            actionLabel = "Add to Stash",
                            onActionClick = onAddClick,
                            modifier = Modifier.padding(top = 40.dp)
                        )
                    }
                } else {
                    // Urgent Attention Card (if items due soon or overdue)
                    if (needsAttentionList.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                                    .clickable(onClick = onViewAllTrackersClick),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = RoseRed.copy(alpha = 0.1f)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = RoseRed, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${needsAttentionList.size} item${if (needsAttentionList.size != 1) "s" else ""} need attention",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = RoseRed
                                        )
                                        Text(
                                            text = "Due soon: ${needsAttentionList.first().title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = onViewAllTrackersClick) {
                                        Text("Review", color = RoseRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Vault Storage Glance Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .clickable(onClick = onStorageClick),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Storage: ${storageOverview.formattedActiveSize()}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${items.size} files in vault",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                TextButton(onClick = onStorageClick) {
                                    Text("Manage")
                                }
                            }
                        }
                    }

                    // Active Trackers Section
                    if (activeTrackers.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Trackers & Renewals",
                                count = activeTrackers.size,
                                onActionClick = onViewAllTrackersClick
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(activeTrackers) { tracker ->
                                    HomeTrackerCard(tracker = tracker)
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // Recently Added Section
                    item {
                        SectionHeader(
                            title = "Recently Added",
                            count = items.size,
                            onActionClick = onViewAllItemsClick
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(items.take(8)) { item ->
                                Box(modifier = Modifier.width(180.dp)) {
                                    StashItemCard(
                                        item = item,
                                        viewMode = LibraryViewMode.GRID,
                                        onClick = { onItemClick(item) }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Screenshots Section
                    if (screenshots.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Screenshots",
                                count = screenshots.size,
                                onActionClick = onViewAllItemsClick
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(screenshots.take(6)) { screenshotItem ->
                                    Box(modifier = Modifier.size(130.dp)) {
                                        StashItemCard(
                                            item = screenshotItem,
                                            viewMode = LibraryViewMode.GALLERY,
                                            onClick = { onItemClick(screenshotItem) }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        // Floating Action Button for Quick Add
        FloatingActionButton(
            onClick = onAddClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("home_quick_add_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add to STASH", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        TextButton(onClick = onActionClick) {
            Text("See all", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun HomeTrackerCard(tracker: TrackerItem) {
    Card(
        modifier = Modifier.width(220.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackerBadge(targetDate = tracker.targetDate)
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = tracker.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tracker.trackerType.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
