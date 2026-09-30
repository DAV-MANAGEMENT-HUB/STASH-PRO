package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DateFilterOption
import com.example.data.model.FolderItem
import com.example.data.model.LibrarySortOrder
import com.example.data.model.LibraryViewMode
import com.example.data.model.SizeFilterOption
import com.example.data.model.SourceFilterOption
import com.example.data.model.StashItem
import com.example.data.model.StashItemType
import com.example.data.model.SystemCategories
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FolderCard
import com.example.ui.components.PasteBanner
import com.example.ui.components.StashItemCard
import com.example.ui.components.ViewModeSelector
import com.example.ui.viewmodel.ClipboardPayload

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    items: List<StashItem>,
    allItems: List<StashItem>,
    folders: List<FolderItem>,
    currentFolder: FolderItem?,
    viewMode: LibraryViewMode,
    sortOrder: LibrarySortOrder,
    sortAscending: Boolean,
    selectedCategory: String?,
    selectedType: StashItemType?,
    selectedDate: DateFilterOption,
    selectedSize: SizeFilterOption,
    selectedSource: SourceFilterOption,
    onlyPinned: Boolean,
    onlyTracked: Boolean,
    isSelectionMode: Boolean,
    selectedItemIds: Set<Long>,
    clipboard: ClipboardPayload?,
    onViewModeChange: (LibraryViewMode) -> Unit,
    onSortOrderChange: (LibrarySortOrder) -> Unit,
    onToggleSortDirection: () -> Unit,
    onCategoryChange: (String?) -> Unit,
    onTypeChange: (StashItemType?) -> Unit,
    onDateChange: (DateFilterOption) -> Unit,
    onSizeChange: (SizeFilterOption) -> Unit,
    onSourceChange: (SourceFilterOption) -> Unit,
    onPinnedChange: (Boolean) -> Unit,
    onTrackedChange: (Boolean) -> Unit,
    onResetFilters: () -> Unit,
    onItemClick: (StashItem) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: (List<StashItem>) -> Unit,
    onClearSelection: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onPromptBulkMove: () -> Unit,
    onBulkCopy: () -> Unit,
    onBulkDelete: () -> Unit,
    onBulkPin: (Boolean) -> Unit,
    onPasteClipboard: () -> Unit,
    onClearClipboard: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onFolderClick: (FolderItem) -> Unit,
    onNavigateUp: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val activeFiltersCount = remember(
        selectedCategory, selectedType, selectedDate, selectedSize, selectedSource, onlyPinned, onlyTracked
    ) {
        var count = 0
        if (selectedCategory != null) count++
        if (selectedType != null) count++
        if (selectedDate != DateFilterOption.ALL) count++
        if (selectedSize != SizeFilterOption.ALL) count++
        if (selectedSource != SourceFilterOption.ALL) count++
        if (onlyPinned) count++
        if (onlyTracked) count++
        count
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Contextual Top App Bar for Selection Mode
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedItemIds.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onSelectAll(items) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = onPromptBulkMove, enabled = selectedItemIds.isNotEmpty()) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "Move")
                        }
                        IconButton(onClick = onBulkCopy, enabled = selectedItemIds.isNotEmpty()) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        IconButton(onClick = { onBulkPin(true) }, enabled = selectedItemIds.isNotEmpty()) {
                            Icon(Icons.Default.Star, contentDescription = "Mark Important")
                        }
                        IconButton(
                            onClick = {
                                val selectedItems = allItems.filter { it.id in selectedItemIds }
                                val uris = selectedItems.mapNotNull {
                                    try { Uri.parse(it.uri) } catch (_: Exception) { null }
                                }
                                if (uris.isNotEmpty()) {
                                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "*/*"
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Selected"))
                                }
                            },
                            enabled = selectedItemIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = onBulkDelete, enabled = selectedItemIds.isNotEmpty()) {
                            Icon(Icons.Default.Delete, contentDescription = "Move to Trash", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                // Normal Library Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentFolder != null) {
                            IconButton(onClick = onNavigateUp, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Library")
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentFolder.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Text(
                                text = "Library",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = items.size.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ViewModeSelector(
                            currentMode = viewMode,
                            onModeSelected = onViewModeChange,
                            modifier = Modifier.testTag("view_mode_selector")
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Filter Button with Badge
                        IconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier.testTag("library_filter_button")
                        ) {
                            if (activeFiltersCount > 0) {
                                BadgedBox(badge = { Badge { Text(activeFiltersCount.toString()) } }) {
                                    Icon(Icons.Default.Tune, contentDescription = "Filter")
                                }
                            } else {
                                Icon(Icons.Default.Tune, contentDescription = "Filter")
                            }
                        }

                        // Sort Menu Dropdown
                        Box {
                            IconButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier.testTag("sort_order_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                LibrarySortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = order.title,
                                                    fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (sortOrder == order) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        onClick = {
                                            onSortOrderChange(order)
                                            sortMenuExpanded = false
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (sortAscending) "Order: Ascending" else "Order: Descending")
                                        }
                                    },
                                    onClick = {
                                        onToggleSortDirection()
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }

                        // Select mode toggle
                        IconButton(
                            onClick = onToggleSelectionMode,
                            modifier = Modifier.testTag("toggle_selection_mode_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Select items")
                        }
                    }
                }
            }

            // Paste Clipboard Banner (if active items on clipboard)
            if (clipboard != null) {
                PasteBanner(
                    itemCount = clipboard.items.size,
                    isCut = clipboard.isCut,
                    destinationName = currentFolder?.name ?: "Library Root",
                    onPaste = onPasteClipboard,
                    onCancel = onClearClipboard
                )
            }

            // Quick Category / Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null && selectedType == null,
                        onClick = {
                            onCategoryChange(null)
                            onTypeChange(null)
                        },
                        label = { Text("All") }
                    )
                }

                // Important chip
                item {
                    FilterChip(
                        selected = onlyPinned,
                        onClick = { onPinnedChange(!onlyPinned) },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Important") }
                    )
                }

                // New Folder Button Chip
                if (currentFolder == null) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = onCreateFolderClick,
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("+ Folder") }
                        )
                    }
                }

                // Type filter chips
                items(StashItemType.values()) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            onTypeChange(if (selectedType == type) null else type)
                        },
                        label = { Text(type.label) }
                    )
                }

                // Category filter chips
                items(SystemCategories.ALL) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            onCategoryChange(if (selectedCategory == cat) null else cat)
                        },
                        label = { Text(cat) }
                    )
                }
            }

            // Folders Row (at root level)
            if (currentFolder == null && folders.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(folders) { folder ->
                        val countInFolder = remember(folder, allItems) {
                            allItems.count { it.folderId == folder.id }
                        }
                        Box(modifier = Modifier.width(180.dp)) {
                            FolderCard(
                                folder = folder,
                                itemCount = countInFolder,
                                onClick = { onFolderClick(folder) }
                            )
                        }
                    }
                }
            }

            // Items Content
            if (items.isEmpty()) {
                EmptyStateView(
                    title = if (currentFolder != null) "Folder is empty" else "No items in your stash",
                    message = if (activeFiltersCount > 0) {
                        "No items match the active filters. Try adjusting or clearing your filters."
                    } else if (currentFolder != null) {
                        "Move or save items into this folder to keep your stash organized."
                    } else {
                        "Save something important and STASH will organize it for you."
                    },
                    actionLabel = "Add to Stash",
                    onActionClick = onAddClick,
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else {
                when (viewMode) {
                    LibraryViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items) { item ->
                                StashItemCard(
                                    item = item,
                                    viewMode = LibraryViewMode.GRID,
                                    onClick = {
                                        if (isSelectionMode) {
                                            onToggleSelect(item.id)
                                        } else {
                                            onItemClick(item)
                                        }
                                    },
                                    isSelected = selectedItemIds.contains(item.id),
                                    isSelectionMode = isSelectionMode,
                                    onLongClick = { onToggleSelect(item.id) }
                                )
                            }
                        }
                    }
                    LibraryViewMode.LARGE_ICONS -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items) { item ->
                                StashItemCard(
                                    item = item,
                                    viewMode = LibraryViewMode.LARGE_ICONS,
                                    onClick = {
                                        if (isSelectionMode) {
                                            onToggleSelect(item.id)
                                        } else {
                                            onItemClick(item)
                                        }
                                    },
                                    isSelected = selectedItemIds.contains(item.id),
                                    isSelectionMode = isSelectionMode,
                                    onLongClick = { onToggleSelect(item.id) }
                                )
                            }
                        }
                    }
                    LibraryViewMode.LIST -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items) { item ->
                                StashItemCard(
                                    item = item,
                                    viewMode = LibraryViewMode.LIST,
                                    onClick = {
                                        if (isSelectionMode) {
                                            onToggleSelect(item.id)
                                        } else {
                                            onItemClick(item)
                                        }
                                    },
                                    isSelected = selectedItemIds.contains(item.id),
                                    isSelectionMode = isSelectionMode,
                                    onLongClick = { onToggleSelect(item.id) }
                                )
                            }
                        }
                    }
                    LibraryViewMode.GALLERY -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items) { item ->
                                StashItemCard(
                                    item = item,
                                    viewMode = LibraryViewMode.GALLERY,
                                    onClick = {
                                        if (isSelectionMode) {
                                            onToggleSelect(item.id)
                                        } else {
                                            onItemClick(item)
                                        }
                                    },
                                    isSelected = selectedItemIds.contains(item.id),
                                    isSelectionMode = isSelectionMode,
                                    onLongClick = { onToggleSelect(item.id) }
                                )
                            }
                        }
                    }
                    LibraryViewMode.TIMELINE -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items) { item ->
                                StashItemCard(
                                    item = item,
                                    viewMode = LibraryViewMode.TIMELINE,
                                    onClick = {
                                        if (isSelectionMode) {
                                            onToggleSelect(item.id)
                                        } else {
                                            onItemClick(item)
                                        }
                                    },
                                    isSelected = selectedItemIds.contains(item.id),
                                    isSelectionMode = isSelectionMode,
                                    onLongClick = { onToggleSelect(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("library_add_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add to STASH", modifier = Modifier.size(28.dp))
        }
    }

    // Comprehensive Filter Bottom Sheet
    if (showFilterSheet) {
        ComprehensiveFilterBottomSheet(
            selectedCategory = selectedCategory,
            selectedType = selectedType,
            selectedDate = selectedDate,
            selectedSize = selectedSize,
            selectedSource = selectedSource,
            onlyPinned = onlyPinned,
            onlyTracked = onlyTracked,
            onCategoryChange = onCategoryChange,
            onTypeChange = onTypeChange,
            onDateChange = onDateChange,
            onSizeChange = onSizeChange,
            onSourceChange = onSourceChange,
            onPinnedChange = onPinnedChange,
            onTrackedChange = onTrackedChange,
            onResetAll = onResetFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}
