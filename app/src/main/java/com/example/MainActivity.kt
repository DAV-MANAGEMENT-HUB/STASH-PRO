package com.example

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.example.data.model.StashItem
import com.example.ui.screens.AddLinkDialog
import com.example.ui.screens.AddNoteDialog
import com.example.ui.screens.AddStashBottomSheet
import com.example.ui.screens.AddTrackerDialog
import com.example.ui.screens.CloudSyncDialog
import com.example.ui.screens.CreateFolderDialog
import com.example.ui.screens.EditItemDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ItemDetailScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MoveItemsDialog
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.StorageScreen
import com.example.ui.screens.TrackersScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StashViewModel
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: StashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as StashApplication
        val factory = StashViewModel.Factory(
            repository = app.repository,
            preferencesRepository = app.preferencesRepository,
            cloudSyncManager = app.cloudSyncManager
        )
        viewModel = ViewModelProvider(this, factory)[StashViewModel::class.java]

        if (intent != null) {
            viewModel.handleSharedIntent(intent)
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            MyApplicationTheme(themeMode = themeMode) {
                MainContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleSharedIntent(intent)
    }
}

@Composable
fun MainContent(viewModel: StashViewModel) {
    val context = LocalContext.current
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    val screenshots by viewModel.screenshots.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()
    val activeTrackers by viewModel.activeTrackers.collectAsState()
    val completedTrackers by viewModel.completedTrackers.collectAsState()
    val trashedItems by viewModel.trashedItems.collectAsState()
    val trashedCount by viewModel.trashedCount.collectAsState()
    val folders by viewModel.allFolders.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()

    val viewMode by viewModel.viewMode.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanNotice by viewModel.scanNotice.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val selectedSizeFilter by viewModel.selectedSizeFilter.collectAsState()
    val selectedSourceFilter by viewModel.selectedSourceFilter.collectAsState()
    val onlyPinned by viewModel.onlyPinned.collectAsState()
    val onlyTracked by viewModel.onlyTracked.collectAsState()

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val selectedTrashIds by viewModel.selectedTrashIds.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()

    val themeMode by viewModel.themeMode.collectAsState()
    val trashedFolders by viewModel.trashedFolders.collectAsState()
    val suggestedTrackers by viewModel.suggestedTrackers.collectAsState()
    val storageOverview by viewModel.storageOverview.collectAsState()
    var showStorageScreen by rememberSaveable { mutableStateOf(false) }

    val showAddMenu by viewModel.showAddMenu.collectAsState()
    val showAddLinkDialog by viewModel.showAddLinkDialog.collectAsState()
    val showAddNoteDialog by viewModel.showAddNoteDialog.collectAsState()
    val showAddTrackerDialog by viewModel.showAddTrackerDialog.collectAsState()
    val showCloudSyncDialog by viewModel.showCloudSyncDialog.collectAsState()
    val showCreateFolderDialog by viewModel.showCreateFolderDialog.collectAsState()
    val showMoveDialog by viewModel.showMoveDialog.collectAsState()
    val itemsToMove by viewModel.itemsToMove.collectAsState()

    var editingItem by remember { mutableStateOf<StashItem?>(null) }
    var currentTab by rememberSaveable { mutableIntStateOf(0) }

    // Media & Document Activity Launchers
    val takePhotoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            viewModel.addManualMedia(
                uri = Uri.fromFile(file),
                name = "Photo_${System.currentTimeMillis()}.jpg",
                mimeType = "image/jpeg"
            )
        }
    }

    val pickMediaLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            viewModel.addManualMedia(uri = uri, mimeType = mime)
        }
    }

    val openDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val mime = context.contentResolver.getType(uri) ?: "application/pdf"
            viewModel.addManualMedia(uri = uri, mimeType = mime)
        }
    }

    if (!onboardingCompleted) {
        OnboardingScreen(
            onPermissionGrantedAndScan = {
                viewModel.completeOnboarding()
                viewModel.triggerMediaScan()
            },
            onSkipToManual = {
                viewModel.completeOnboarding()
            }
        )
        return
    }

    // Storage Overview Screen
    if (showStorageScreen) {
        StorageScreen(
            storageOverview = storageOverview,
            onItemClick = {
                showStorageScreen = false
                viewModel.selectItem(it)
            },
            onMoveDuplicateToTrash = { viewModel.moveToTrash(it) },
            onEmptyTrashClick = { viewModel.emptyTrash() }
        )
        BackHandler {
            showStorageScreen = false
        }
        return
    }

    // Detail Screen (if an item is tapped)
    if (selectedItem != null) {
        val current = selectedItem!!
        val relatedItems = remember(current, allItems) {
            allItems.filter { it.id != current.id && ((current.vendor != null && it.vendor == current.vendor) || it.category == current.category) }
        }
        val itemTrackers = remember(current, activeTrackers, completedTrackers) {
            (activeTrackers + completedTrackers).filter { it.stashItemId == current.id }
        }
        val itemFolder = remember(current, folders) {
            current.folderId?.let { fid -> folders.firstOrNull { it.id == fid } }
        }

        ItemDetailScreen(
            item = current,
            relatedItems = relatedItems,
            trackers = itemTrackers,
            folderName = itemFolder?.name,
            onBack = { viewModel.selectItem(null) },
            onEdit = { editingItem = current },
            onRename = { viewModel.renameItem(current, it) },
            onTogglePin = { viewModel.togglePin(current) },
            onCopyToClipboard = { viewModel.copyItemToClipboard(current) },
            onPromptMove = { viewModel.promptMoveItem(current) },
            onReplaceMedia = { uri, name, mime, size -> viewModel.replaceMedia(current, uri, name, mime, size) },
            onDelete = { viewModel.deleteItem(current) },
            onAddTracker = { viewModel.showAddTrackerDialog.value = true },
            onRelatedItemClick = { viewModel.selectItem(it) }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Library") },
                        label = { Text("Library") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = {
                            if (activeTrackers.isNotEmpty()) {
                                BadgedBox(badge = { Badge { Text(activeTrackers.size.toString()) } }) {
                                    Icon(Icons.Default.Alarm, contentDescription = "Trackers")
                                }
                            } else {
                                Icon(Icons.Default.Alarm, contentDescription = "Trackers")
                            }
                        },
                        label = { Text("Trackers") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = {
                            if (trashedCount > 0) {
                                BadgedBox(badge = { Badge { Text(trashedCount.toString()) } }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Trash")
                                }
                            } else {
                                Icon(Icons.Default.Delete, contentDescription = "Trash")
                            }
                        },
                        label = { Text("Trash") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    0 -> HomeScreen(
                        items = allItems,
                        screenshots = screenshots,
                        activeTrackers = activeTrackers,
                        suggestedTrackers = suggestedTrackers,
                        storageOverview = storageOverview,
                        themeMode = themeMode,
                        searchQuery = searchQuery,
                        searchResults = filteredItems,
                        isScanning = isScanning,
                        scanNotice = scanNotice,
                        onSearchChange = { viewModel.searchQuery.value = it },
                        onItemClick = { viewModel.selectItem(it) },
                        onAddClick = { viewModel.showAddMenu.value = true },
                        onScanClick = { viewModel.triggerMediaScan() },
                        onCloudClick = { viewModel.showCloudSyncDialog.value = true },
                        onStorageClick = { showStorageScreen = true },
                        onCycleTheme = { viewModel.cycleThemeMode() },
                        onDismissNotice = { viewModel.dismissScanNotice() },
                        onViewAllItemsClick = { currentTab = 1 },
                        onViewAllTrackersClick = { currentTab = 2 }
                    )

                    1 -> LibraryScreen(
                        items = filteredItems,
                        allItems = allItems,
                        folders = folders,
                        currentFolder = currentFolder,
                        viewMode = viewMode,
                        sortOrder = sortOrder,
                        sortAscending = sortAscending,
                        selectedCategory = selectedCategory,
                        selectedType = selectedTypeFilter,
                        selectedDate = selectedDateFilter,
                        selectedSize = selectedSizeFilter,
                        selectedSource = selectedSourceFilter,
                        onlyPinned = onlyPinned,
                        onlyTracked = onlyTracked,
                        isSelectionMode = isSelectionMode,
                        selectedItemIds = selectedItemIds,
                        clipboard = clipboard,
                        onViewModeChange = { viewModel.setViewMode(it) },
                        onSortOrderChange = { viewModel.setSortOrder(it) },
                        onToggleSortDirection = { viewModel.toggleSortDirection() },
                        onCategoryChange = { viewModel.selectedCategory.value = it },
                        onTypeChange = { viewModel.selectedTypeFilter.value = it },
                        onDateChange = { viewModel.selectedDateFilter.value = it },
                        onSizeChange = { viewModel.selectedSizeFilter.value = it },
                        onSourceChange = { viewModel.selectedSourceFilter.value = it },
                        onPinnedChange = { viewModel.onlyPinned.value = it },
                        onTrackedChange = { viewModel.onlyTracked.value = it },
                        onResetFilters = {
                            viewModel.selectedCategory.value = null
                            viewModel.selectedTypeFilter.value = null
                            viewModel.selectedDateFilter.value = com.example.data.model.DateFilterOption.ALL
                            viewModel.selectedSizeFilter.value = com.example.data.model.SizeFilterOption.ALL
                            viewModel.selectedSourceFilter.value = com.example.data.model.SourceFilterOption.ALL
                            viewModel.onlyPinned.value = false
                            viewModel.onlyTracked.value = false
                        },
                        onItemClick = { viewModel.selectItem(it) },
                        onToggleSelect = { viewModel.toggleItemSelection(it) },
                        onSelectAll = { viewModel.selectAll(it) },
                        onClearSelection = { viewModel.clearSelection() },
                        onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                        onPromptBulkMove = { viewModel.promptBulkMove() },
                        onBulkCopy = { viewModel.copySelectedToClipboard() },
                        onBulkDelete = { viewModel.bulkDeleteSelected() },
                        onBulkPin = { viewModel.bulkPinSelected(it) },
                        onPasteClipboard = { viewModel.pasteClipboard() },
                        onClearClipboard = { viewModel.clearClipboard() },
                        onCreateFolderClick = { viewModel.showCreateFolderDialog.value = true },
                        onFolderClick = { viewModel.navigateToFolder(it.id) },
                        onNavigateUp = { viewModel.navigateUp() },
                        onAddClick = { viewModel.showAddMenu.value = true }
                    )

                    2 -> TrackersScreen(
                        activeTrackers = activeTrackers,
                        completedTrackers = completedTrackers,
                        suggestedTrackers = suggestedTrackers,
                        onToggleCompleted = { viewModel.toggleTrackerCompleted(it) },
                        onDeleteTracker = { viewModel.deleteTracker(it) },
                        onConfirmSuggestedTracker = { viewModel.confirmSuggestedTracker(it) },
                        onDismissSuggestedTracker = { viewModel.dismissSuggestedTracker(it) },
                        onAddTrackerClick = { viewModel.showAddTrackerDialog.value = true }
                    )

                    3 -> TrashScreen(
                        trashedItems = trashedItems,
                        trashedFolders = trashedFolders,
                        selectedIds = selectedTrashIds,
                        onToggleSelect = { viewModel.toggleTrashSelection(it) },
                        onSelectAll = { viewModel.selectAllTrash(it) },
                        onClearSelection = { viewModel.clearTrashSelection() },
                        onRestoreItem = { viewModel.restoreItem(it) },
                        onRestoreFolder = { viewModel.restoreFolder(it) },
                        onRestoreSelected = { viewModel.restoreSelectedTrash() },
                        onRestoreAll = { viewModel.restoreAllTrash() },
                        onDeletePermanently = { viewModel.deletePermanently(it) },
                        onDeleteFolderPermanently = { viewModel.deleteFolderPermanently(it) },
                        onDeleteSelectedPermanently = { viewModel.deleteSelectedTrashPermanently() },
                        onEmptyTrash = { viewModel.emptyTrash() }
                    )
                }
            }
        }
    }

    // Modal dialogs & bottom sheets
    if (showAddMenu) {
        AddStashBottomSheet(
            onDismiss = { viewModel.showAddMenu.value = false },
            onTakePhoto = { takePhotoLauncher.launch(null) },
            onPickPhotos = {
                pickMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onPickFiles = {
                openDocumentLauncher.launch(arrayOf("*/*"))
            },
            onSaveLink = { viewModel.showAddLinkDialog.value = true },
            onCreateNote = { viewModel.showAddNoteDialog.value = true }
        )
    }

    if (showAddLinkDialog) {
        AddLinkDialog(
            onDismiss = { viewModel.showAddLinkDialog.value = false },
            onConfirm = { url, category -> viewModel.addLink(url, category) }
        )
    }

    if (showAddNoteDialog) {
        AddNoteDialog(
            onDismiss = { viewModel.showAddNoteDialog.value = false },
            onConfirm = { title, content, category -> viewModel.addNote(title, content, category) }
        )
    }

    if (showAddTrackerDialog) {
        AddTrackerDialog(
            onDismiss = { viewModel.showAddTrackerDialog.value = false },
            onConfirm = { title, type, targetDate, reminderDays, notes ->
                viewModel.addTracker(
                    title = title,
                    type = type,
                    targetDate = targetDate,
                    stashItemId = selectedItem?.id,
                    reminderDays = reminderDays,
                    notes = notes
                )
            },
            initialTitle = selectedItem?.title ?: ""
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { viewModel.showCreateFolderDialog.value = false },
            onConfirm = { name -> viewModel.createFolder(name) }
        )
    }

    if (showMoveDialog) {
        MoveItemsDialog(
            itemsToMove = itemsToMove,
            folders = folders,
            currentFolderId = currentFolder?.id,
            onDismiss = { viewModel.showMoveDialog.value = false },
            onConfirmMove = { targetId -> viewModel.executeMove(targetId) }
        )
    }

    if (editingItem != null) {
        EditItemDialog(
            item = editingItem!!,
            onDismiss = { editingItem = null },
            onConfirm = {
                viewModel.updateItem(it)
                editingItem = null
            }
        )
    }

    if (showCloudSyncDialog) {
        CloudSyncDialog(
            onDismiss = { viewModel.showCloudSyncDialog.value = false },
            syncStatus = syncStatus,
            userEmail = userEmail,
            isLoggedIn = isLoggedIn,
            onSignIn = { viewModel.setUserSession(it) },
            onSignOut = { viewModel.setUserSession(null) },
            onSyncNow = { viewModel.triggerCloudSync() }
        )
    }
}
