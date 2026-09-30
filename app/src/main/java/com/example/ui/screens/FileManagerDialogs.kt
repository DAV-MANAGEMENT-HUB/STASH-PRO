package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DateFilterOption
import com.example.data.model.FolderItem
import com.example.data.model.LibrarySortOrder
import com.example.data.model.SizeFilterOption
import com.example.data.model.SourceFilterOption
import com.example.data.model.StashItem
import com.example.data.model.StashItemType
import com.example.data.model.SystemCategories

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CreateNewFolder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("New Folder", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Create a folder to group your receipts, warranties, projects, or documents.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    placeholder = { Text("e.g. Tax 2026, Home Renovation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("folder_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (folderName.isNotBlank()) onConfirm(folderName.trim()) },
                enabled = folderName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_folder_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MoveItemsDialog(
    itemsToMove: List<StashItem>,
    folders: List<FolderItem>,
    currentFolderId: Long?,
    onDismiss: () -> Unit,
    onConfirmMove: (targetFolderId: Long?) -> Unit
) {
    var selectedTargetId by remember { mutableStateOf<Long?>(currentFolderId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.DriveFileMove,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                if (itemsToMove.size == 1) "Move \"${itemsToMove.first().title}\"" else "Move ${itemsToMove.size} Items",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Select destination folder:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Root destination
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTargetId = null }
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTargetId == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Library Root",
                            fontWeight = if (selectedTargetId == null) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedTargetId == null) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Available folders
                folders.forEach { folder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTargetId = folder.id }
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedTargetId == folder.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                folder.name,
                                fontWeight = if (selectedTargetId == folder.id) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedTargetId == folder.id) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmMove(selectedTargetId) },
                modifier = Modifier.testTag("confirm_move_button")
            ) {
                Text("Move Here")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RenameItemDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("Rename", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("rename_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title.trim()) },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_rename_button")
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComprehensiveFilterBottomSheet(
    selectedCategory: String?,
    selectedType: StashItemType?,
    selectedDate: DateFilterOption,
    selectedSize: SizeFilterOption,
    selectedSource: SourceFilterOption,
    onlyPinned: Boolean,
    onlyTracked: Boolean,
    onCategoryChange: (String?) -> Unit,
    onTypeChange: (StashItemType?) -> Unit,
    onDateChange: (DateFilterOption) -> Unit,
    onSizeChange: (SizeFilterOption) -> Unit,
    onSourceChange: (SourceFilterOption) -> Unit,
    onPinnedChange: (Boolean) -> Unit,
    onTrackedChange: (Boolean) -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters & Organization",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onResetAll) {
                    Text("Reset All")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Toggles (Important / Has Tracker)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Important Only", fontWeight = FontWeight.SemiBold)
                Switch(checked = onlyPinned, onCheckedChange = onPinnedChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Has Active Tracker", fontWeight = FontWeight.SemiBold)
                Switch(checked = onlyTracked, onCheckedChange = onTrackedChange)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // File Type
            Text("File Type", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { onTypeChange(null) },
                    label = { Text("All Types") }
                )
                StashItemType.values().forEach { t ->
                    FilterChip(
                        selected = selectedType == t,
                        onClick = { onTypeChange(if (selectedType == t) null else t) },
                        label = { Text(t.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Added
            Text("Date Added", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DateFilterOption.values().forEach { opt ->
                    FilterChip(
                        selected = selectedDate == opt,
                        onClick = { onDateChange(opt) },
                        label = { Text(opt.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Size Range
            Text("File Size", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SizeFilterOption.values().forEach { opt ->
                    FilterChip(
                        selected = selectedSize == opt,
                        onClick = { onSizeChange(opt) },
                        label = { Text(opt.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Source
            Text("Source", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SourceFilterOption.values().forEach { opt ->
                    FilterChip(
                        selected = selectedSource == opt,
                        onClick = { onSourceChange(opt) },
                        label = { Text(opt.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category
            Text("Category", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("All Categories") }
                )
                SystemCategories.ALL.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategoryChange(if (selectedCategory == cat) null else cat) },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Apply Filters")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
