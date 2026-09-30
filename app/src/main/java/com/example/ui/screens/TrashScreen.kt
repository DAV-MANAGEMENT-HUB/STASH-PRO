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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BinViewMode
import com.example.data.model.FolderItem
import com.example.data.model.LibraryViewMode
import com.example.data.model.StashItem
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ItemTypeIcon
import com.example.ui.components.ViewModeSelector
import com.example.ui.theme.Amber500
import com.example.ui.theme.RoseRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrashScreen(
    trashedItems: List<StashItem>,
    trashedFolders: List<FolderItem>,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: (List<StashItem>) -> Unit,
    onClearSelection: () -> Unit,
    onRestoreItem: (StashItem) -> Unit,
    onRestoreFolder: (FolderItem) -> Unit,
    onRestoreSelected: () -> Unit,
    onRestoreAll: () -> Unit,
    onDeletePermanently: (StashItem) -> Unit,
    onDeleteFolderPermanently: (FolderItem) -> Unit,
    onDeleteSelectedPermanently: () -> Unit,
    onEmptyTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    var binMode by remember { mutableStateOf(BinViewMode.ORIGINAL_LOCATION) }
    var layoutMode by remember { mutableStateOf(LibraryViewMode.LIST) }
    var trashSearchQuery by remember { mutableStateOf("") }
    var currentBrowsingFolderId by remember { mutableStateOf<Long?>(null) }

    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var itemToDeletePermanently by remember { mutableStateOf<StashItem?>(null) }
    var folderToDeletePermanently by remember { mutableStateOf<FolderItem?>(null) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    // Filter trashed items based on search query
    val searchedItems = remember(trashedItems, trashSearchQuery) {
        if (trashSearchQuery.isBlank()) {
            trashedItems
        } else {
            val q = trashSearchQuery.lowercase(Locale.ROOT)
            trashedItems.filter {
                it.title.lowercase().contains(q) ||
                        it.category.lowercase().contains(q) ||
                        (it.vendor?.lowercase()?.contains(q) == true) ||
                        (it.originalLocation?.lowercase()?.contains(q) == true) ||
                        (it.originalFolderPath?.lowercase()?.contains(q) == true) ||
                        (it.tags.lowercase().contains(q))
            }
        }
    }

    // View Mode sorting and grouping
    val displayedItems = remember(searchedItems, binMode, currentBrowsingFolderId) {
        when (binMode) {
            BinViewMode.ORIGINAL_LOCATION -> {
                if (currentBrowsingFolderId != null) {
                    searchedItems.filter { it.originalFolderId == currentBrowsingFolderId }
                } else {
                    searchedItems.filter { it.originalFolderId == null }
                }
            }
            BinViewMode.ALL_DELETED -> searchedItems
            BinViewMode.RECENTLY_DELETED -> searchedItems.sortedByDescending { it.trashedTimestamp ?: 0L }
            BinViewMode.EXPIRING_SOON -> searchedItems.sortedBy { it.trashDaysRemaining() }
        }
    }

    // Trashed folders in current browsing level
    val displayedFolders = remember(trashedFolders, binMode, currentBrowsingFolderId) {
        if (binMode == BinViewMode.ORIGINAL_LOCATION) {
            trashedFolders.filter { it.originalParentFolderId == currentBrowsingFolderId }
        } else {
            emptyList()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Trash / Bin",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = (trashedItems.size + trashedFolders.size).toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ViewModeSelector(
                        currentMode = layoutMode,
                        onModeSelected = { layoutMode = it }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (trashedItems.isNotEmpty() || trashedFolders.isNotEmpty()) {
                        if (selectedIds.isNotEmpty()) {
                            TextButton(onClick = onRestoreSelected) {
                                Text("Restore (${selectedIds.size})")
                            }
                            IconButton(onClick = { showDeleteSelectedDialog = true }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            TextButton(
                                onClick = onRestoreAll,
                                modifier = Modifier.testTag("trash_restore_all_button")
                            ) {
                                Text("Restore All")
                            }
                            TextButton(
                                onClick = { showEmptyTrashDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Empty")
                            }
                        }
                    }
                }
            }

            // Bin View Modes TabRow
            TabRow(
                selectedTabIndex = binMode.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                BinViewMode.values().forEach { mode ->
                    Tab(
                        selected = binMode == mode,
                        onClick = {
                            binMode = mode
                            currentBrowsingFolderId = null
                        },
                        text = {
                            Text(
                                text = mode.title,
                                fontWeight = if (binMode == mode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            // Search Bar in Trash
            OutlinedTextField(
                value = trashSearchQuery,
                onValueChange = { trashSearchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                placeholder = { Text("Search items in Trash...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Breadcrumb row when inside a folder in Original Location mode
            if (binMode == BinViewMode.ORIGINAL_LOCATION && currentBrowsingFolderId != null) {
                val currentFld = trashedFolders.firstOrNull { it.id == currentBrowsingFolderId }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentBrowsingFolderId = null }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Original Folder: ${currentFld?.name ?: "Folder"}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Selection controls bar if items exist
            if (trashedItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedIds.isEmpty()) "Tap item to select" else "${selectedIds.size} selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row {
                        if (selectedIds.size < trashedItems.size) {
                            TextButton(onClick = { onSelectAll(trashedItems) }) {
                                Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Select All", fontSize = 12.sp)
                            }
                        } else {
                            TextButton(onClick = onClearSelection) {
                                Text("Deselect All", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Main Content: Folders + Items
            if (trashedItems.isEmpty() && trashedFolders.isEmpty()) {
                EmptyStateView(
                    title = "Trash is empty",
                    message = "Deleted items are preserved in their original hierarchy for 30 days before permanent removal."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Trashed Folders (in Original Location mode)
                    if (displayedFolders.isNotEmpty()) {
                        item {
                            Text(
                                text = "Deleted Folders",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }

                        items(displayedFolders) { folder ->
                            val itemsInside = remember(folder, trashedItems) {
                                trashedItems.filter { it.originalFolderId == folder.id }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { currentBrowsingFolderId = folder.id },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folder.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${itemsInside.size} deleted items inside",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onRestoreFolder(folder) }) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore Folder", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { folderToDeletePermanently = folder }) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete Folder Permanently", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    if (displayedItems.isNotEmpty()) {
                        item {
                            Text(
                                text = "Deleted Files (${displayedItems.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                            )
                        }

                        items(displayedItems) { item ->
                            val isSelected = selectedIds.contains(item.id)
                            HierarchicalTrashItemCard(
                                item = item,
                                isSelected = isSelected,
                                onToggleSelect = { onToggleSelect(item.id) },
                                onRestore = { onRestoreItem(item) },
                                onDeletePermanently = { itemToDeletePermanently = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs: Item Delete Permanently
    if (itemToDeletePermanently != null) {
        val target = itemToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { itemToDeletePermanently = null },
            title = { Text("Permanently delete this item?", fontWeight = FontWeight.Bold) },
            text = { Text("\"${target.title}\" will be permanently deleted and cannot be recovered.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePermanently(target)
                        itemToDeletePermanently = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeletePermanently = null }) { Text("Cancel") }
            }
        )
    }

    // Dialogs: Folder Delete Permanently
    if (folderToDeletePermanently != null) {
        val targetFolder = folderToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { folderToDeletePermanently = null },
            title = { Text("Permanently delete folder?", fontWeight = FontWeight.Bold) },
            text = { Text("The folder \"${targetFolder.name}\" and all deleted items inside it will be permanently erased.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFolderPermanently(targetFolder)
                        folderToDeletePermanently = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Folder Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDeletePermanently = null }) { Text("Cancel") }
            }
        )
    }

    // Bulk Delete Selected Dialog
    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text("Permanently delete ${selectedIds.size} items?", fontWeight = FontWeight.Bold) },
            text = { Text("These items will be permanently erased. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteSelectedDialog = false
                        onDeleteSelectedPermanently()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Empty Trash Dialog
    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash Completely?", fontWeight = FontWeight.Bold) },
            text = { Text("All items in Trash will be permanently erased immediately. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyTrashDialog = false
                        onEmptyTrash()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty Trash Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HierarchicalTrashItemCard(
    item: StashItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val remainingDays = item.trashDaysRemaining()
    val (pillBg, pillTextColor, pillText) = when {
        remainingDays <= 3 -> Triple(RoseRed.copy(alpha = 0.15f), RoseRed, "Deletes in $remainingDays day${if (remainingDays != 1L) "s" else ""}")
        remainingDays <= 10 -> Triple(Amber500.copy(alpha = 0.15f), Amber500, "Deletes in $remainingDays days")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Deletes permanently in $remainingDays days")
    }

    val locationPath = item.originalFolderPath ?: item.originalLocation ?: "Library Root"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelect)
            .testTag("trash_item_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.mimeType?.startsWith("image/") == true || item.isScreenshot) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        ItemTypeIcon(item.itemType, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Original: $locationPath",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onRestore, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }

                IconButton(onClick = onDeletePermanently, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Retention Countdown Badge
            Surface(
                color = pillBg,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(start = 48.dp)
            ) {
                Text(
                    text = pillText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = pillTextColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
