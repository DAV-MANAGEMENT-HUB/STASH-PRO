package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.LibraryViewMode
import com.example.data.model.StashItem
import com.example.data.model.StashItemType
import com.example.data.model.TrackerItem
import com.example.ui.components.ItemTypeIcon
import com.example.ui.components.StashItemCard
import com.example.ui.components.TrackerBadge
import com.example.ui.theme.Amber400
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(
    item: StashItem,
    relatedItems: List<StashItem>,
    trackers: List<TrackerItem>,
    folderName: String?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRename: (String) -> Unit,
    onTogglePin: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onPromptMove: () -> Unit,
    onReplaceMedia: (Uri, String?, String?, Long) -> Unit,
    onDelete: () -> Unit,
    onAddTracker: () -> Unit,
    onRelatedItemClick: (StashItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTrashConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    val replaceFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: item.mimeType
            onReplaceMedia(uri, uri.lastPathSegment, mime, 0L)
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Important (Star) toggle
                    IconButton(onClick = onTogglePin, modifier = Modifier.testTag("detail_pin_button")) {
                        Icon(
                            imageVector = if (item.isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Important",
                            tint = if (item.isPinned) Amber400 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = item.mimeType ?: "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, item.title)
                                if (item.itemType == StashItemType.LINK) {
                                    putExtra(Intent.EXTRA_TEXT, "${item.title}: ${item.uri}")
                                } else {
                                    putExtra(Intent.EXTRA_TEXT, item.extractedText ?: item.title)
                                    try {
                                        putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri))
                                    } catch (_: Exception) {}
                                }
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share via STASH"))
                        },
                        modifier = Modifier.testTag("detail_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }

                    // Overflow Menu (Rename, Copy, Move, Replace, Delete)
                    Box {
                        IconButton(
                            onClick = { overflowMenuExpanded = true },
                            modifier = Modifier.testTag("detail_more_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                        }

                        DropdownMenu(
                            expanded = overflowMenuExpanded,
                            onDismissRequest = { overflowMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Details") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onEdit()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    showRenameDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Copy") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onCopyToClipboard()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Move to Folder") },
                                leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onPromptMove()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Replace File") },
                                leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    replaceFileLauncher.launch(arrayOf("*/*"))
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Copy Link / URI") },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("STASH Item Link", item.uri))
                                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    overflowMenuExpanded = false
                                    showTrashConfirm = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Actual Preview Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column {
                        if (item.mimeType?.startsWith("image/") == true || item.isScreenshot) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else if (item.itemType == StashItemType.LINK) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ItemTypeIcon(item.itemType, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = item.originalPath ?: "Web Link",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = item.uri,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.uri))
                                            context.startActivity(browserIntent)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("open_link_button")
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Link in Browser")
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ItemTypeIcon(item.itemType, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = item.mimeType ?: "Document",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Open original action row
                        if (item.uri.startsWith("content://") || item.uri.startsWith("file://")) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val sizeStr = item.formattedSize()
                                if (sizeStr.isNotBlank()) {
                                    Text(
                                        text = "Size: $sizeStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                TextButton(onClick = {
                                    try {
                                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(item.uri), item.mimeType ?: "*/*")
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(viewIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Original File")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Title & Location Tag Badges
            item {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = item.itemType.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (folderName != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = folderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // File Details & Metadata Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "File Details & Storage",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailInfoRow(label = "Location", value = folderName ?: "Library Root")
                        if (item.formattedSize().isNotBlank()) {
                            DetailInfoRow(label = "File Size", value = item.formattedSize())
                        }
                        if (!item.mimeType.isNullOrBlank()) {
                            DetailInfoRow(label = "MIME Type", value = item.mimeType)
                        }
                        DetailInfoRow(
                            label = "Date Added",
                            value = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(item.addedTimestamp))
                        )
                        DetailInfoRow(
                            label = "Date Modified",
                            value = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(item.modifiedTimestamp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Smart Characterization Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Smart Characterization",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (item.vendor != null) {
                            DetailInfoRow(label = "Company / Brand", value = item.vendor)
                        }
                        if (item.product != null) {
                            DetailInfoRow(label = "Product / Item", value = item.product)
                        }
                        if (item.detectedAmount != null) {
                            DetailInfoRow(
                                label = "Detected Amount",
                                value = "${item.detectedAmount} ${item.currency ?: ""}".trim()
                            )
                        }
                        if (item.eventDate != null) {
                            DetailInfoRow(
                                label = "Event / Purchase Date",
                                value = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(item.eventDate))
                            )
                        }
                        if (item.expiryDate != null) {
                            DetailInfoRow(
                                label = "Expiry / Renewal Date",
                                value = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(item.expiryDate))
                            )
                        }

                        if (!item.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Notes / Content:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (item.tags.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tags:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                item.getTagList().forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Connected Trackers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connected Trackers",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = onAddTracker) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Tracker")
                    }
                }

                if (trackers.isEmpty()) {
                    Text(
                        text = "No trackers set for this item.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    trackers.forEach { tracker ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tracker.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                    Text(
                                        "Target: ${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(tracker.targetDate))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TrackerBadge(targetDate = tracker.targetDate)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Related Items
            if (relatedItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Related Items in Stash",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(relatedItems) { relItem ->
                            Box(modifier = Modifier.width(170.dp)) {
                                StashItemCard(
                                    item = relItem,
                                    viewMode = LibraryViewMode.GRID,
                                    onClick = { onRelatedItemClick(relItem) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        RenameItemDialog(
            initialTitle = item.title,
            onDismiss = { showRenameDialog = false },
            onConfirm = {
                onRename(it)
                showRenameDialog = false
            }
        )
    }

    // Move to Trash Confirmation Dialog
    if (showTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showTrashConfirm = false },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Move to Trash?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${item.title}\" will be moved to Trash. It will be preserved for 30 days before permanent deletion, during which you can restore it anytime."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTrashConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_trash_button")
                ) {
                    Text("Move to Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrashConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
