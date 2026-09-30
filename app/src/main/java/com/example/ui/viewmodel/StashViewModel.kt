package com.example.ui.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DateFilterOption
import com.example.data.model.FolderItem
import com.example.data.model.LibrarySortOrder
import com.example.data.model.LibraryViewMode
import com.example.data.model.SizeFilterOption
import com.example.data.model.SourceFilterOption
import com.example.data.model.StashItem
import com.example.data.model.StashItemType
import com.example.data.model.ThemeMode
import com.example.data.model.TrackerItem
import com.example.data.model.TrackerType
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.StashRepository
import com.example.engine.StorageAnalyzer
import com.example.engine.StorageOverview
import com.example.sync.CloudSyncManager
import com.example.sync.SyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class ClipboardPayload(
    val items: List<StashItem>,
    val isCut: Boolean = false
)

class StashViewModel(
    private val repository: StashRepository,
    private val preferencesRepository: PreferencesRepository,
    private val cloudSyncManager: CloudSyncManager
) : ViewModel() {

    // Active Library Streams
    val allItems: StateFlow<List<StashItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val screenshots: StateFlow<List<StashItem>> = repository.screenshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTrackers: StateFlow<List<TrackerItem>> = repository.activeTrackers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTrackers: StateFlow<List<TrackerItem>> = repository.completedTrackers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTrackers: StateFlow<List<TrackerItem>> = repository.allTrackers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeTrackerCount: StateFlow<Int> = repository.activeTrackerCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val screenshotCount: StateFlow<Int> = repository.screenshotCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Trash Streams
    val trashedItems: StateFlow<List<StashItem>> = repository.trashedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedCount: StateFlow<Int> = repository.trashedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trashedFolders: StateFlow<List<FolderItem>> = repository.trashedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestedTrackers: StateFlow<List<TrackerItem>> = repository.suggestedTrackers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val storageOverview: StateFlow<StorageOverview> = combine(allItems, trashedItems) { active, trashed ->
        StorageAnalyzer.analyze(active, trashed)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StorageOverview(0, 0, 0, emptyList(), emptyList(), emptyList())
    )

    // Folders
    val allFolders: StateFlow<List<FolderItem>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentFolderId = MutableStateFlow<Long?>(null)

    val currentFolder: StateFlow<FolderItem?> = combine(allFolders, currentFolderId) { folders, id ->
        if (id == null) null else folders.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Preferences & Layout
    val viewMode: StateFlow<LibraryViewMode> = preferencesRepository.viewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryViewMode.GRID)

    val sortOrder = MutableStateFlow(LibrarySortOrder.DATE_ADDED)
    val sortAscending = MutableStateFlow(false)

    val onboardingCompleted: StateFlow<Boolean> = preferencesRepository.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val syncStatus: StateFlow<SyncStatus> = cloudSyncManager.syncStatus

    val userEmail: StateFlow<String?> = preferencesRepository.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLoggedIn: StateFlow<Boolean> = preferencesRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Filtering
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val selectedTypeFilter = MutableStateFlow<StashItemType?>(null)
    val selectedDateFilter = MutableStateFlow(DateFilterOption.ALL)
    val selectedSizeFilter = MutableStateFlow(SizeFilterOption.ALL)
    val selectedSourceFilter = MutableStateFlow(SourceFilterOption.ALL)
    val onlyPinned = MutableStateFlow(false)
    val onlyTracked = MutableStateFlow(false)
    val selectedTag = MutableStateFlow<String?>(null)

    // Multi-Select State
    val isSelectionMode = MutableStateFlow(false)
    val selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())

    // Trash Multi-Select State
    val selectedTrashIds = MutableStateFlow<Set<Long>>(emptySet())

    // Internal Clipboard
    val clipboard = MutableStateFlow<ClipboardPayload?>(null)

    // Scanning & Notices
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanNotice = MutableStateFlow<String?>(null)
    val scanNotice: StateFlow<String?> = _scanNotice.asStateFlow()

    private val _selectedItem = MutableStateFlow<StashItem?>(null)
    val selectedItem: StateFlow<StashItem?> = _selectedItem.asStateFlow()

    // Dialog flags
    val showAddMenu = MutableStateFlow(false)
    val showAddLinkDialog = MutableStateFlow(false)
    val showAddNoteDialog = MutableStateFlow(false)
    val showAddTrackerDialog = MutableStateFlow(false)
    val showCloudSyncDialog = MutableStateFlow(false)
    val showCreateFolderDialog = MutableStateFlow(false)
    val showMoveDialog = MutableStateFlow(false)
    val itemsToMove = MutableStateFlow<List<StashItem>>(emptyList())

    init {
        // Enforce 30-day retention cleanup whenever viewmodel starts
        viewModelScope.launch {
            repository.cleanupExpiredTrash()
        }
    }

    // Comprehensive Filter & Sort reactive stream
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredItems: StateFlow<List<StashItem>> = combine(
        searchQuery,
        currentFolderId
    ) { query, folderId ->
        Pair(query, folderId)
    }.flatMapLatest { (query, _) ->
        if (query.isNotBlank()) {
            repository.search(query)
        } else {
            repository.allItems
        }
    }.combine(
        combine(
            currentFolderId,
            selectedCategory,
            selectedTypeFilter,
            selectedDateFilter,
            selectedSizeFilter
        ) { folderId, cat, type, date, size ->
            FilterTuple1(folderId, cat, type, date, size)
        }
    ) { items, f1 ->
        Pair(items, f1)
    }.combine(
        combine(
            combine(selectedSourceFilter, onlyPinned, onlyTracked) { src, pinned, tracked -> Triple(src, pinned, tracked) },
            combine(selectedTag, sortOrder, sortAscending) { tag, sort, asc -> Triple(tag, sort, asc) }
        ) { (src, pinned, tracked), (tag, sort, asc) ->
            FilterTuple2(src, pinned, tracked, tag, sort, asc)
        }
    ) { (items, f1), f2 ->
        val now = System.currentTimeMillis()
        var result = items

        // 1. Folder filter (if inside a specific folder, show its items; if root and no query, show root items)
        if (searchQuery.value.isBlank()) {
            result = if (f1.folderId != null) {
                result.filter { it.folderId == f1.folderId }
            } else {
                result.filter { it.folderId == null }
            }
        }

        // 2. Category
        if (f1.category != null) {
            result = result.filter { it.category.equals(f1.category, ignoreCase = true) }
        }

        // 3. Type
        if (f1.type != null) {
            result = result.filter { it.itemType == f1.type }
        }

        // 4. Date Filter
        if (f1.dateFilter != DateFilterOption.ALL) {
            val cal = Calendar.getInstance()
            val startOfDay = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            result = when (f1.dateFilter) {
                DateFilterOption.TODAY -> result.filter { it.addedTimestamp >= startOfDay }
                DateFilterOption.LAST_7_DAYS -> {
                    val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000L)
                    result.filter { it.addedTimestamp >= sevenDaysAgo }
                }
                DateFilterOption.LAST_30_DAYS -> {
                    val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000L)
                    result.filter { it.addedTimestamp >= thirtyDaysAgo }
                }
                DateFilterOption.THIS_YEAR -> {
                    cal.set(Calendar.DAY_OF_YEAR, 1)
                    val startOfYear = cal.timeInMillis
                    result.filter { it.addedTimestamp >= startOfYear }
                }
                DateFilterOption.ALL -> result
            }
        }

        // 5. Size Filter
        if (f1.sizeFilter != SizeFilterOption.ALL) {
            result = when (f1.sizeFilter) {
                SizeFilterOption.UNDER_1_MB -> result.filter { it.fileSize < 1024 * 1024L }
                SizeFilterOption.BETWEEN_1_AND_10_MB -> result.filter { it.fileSize in (1024 * 1024L)..(10 * 1024 * 1024L) }
                SizeFilterOption.OVER_10_MB -> result.filter { it.fileSize > 10 * 1024 * 1024L }
                SizeFilterOption.ALL -> result
            }
        }

        // 6. Source Filter
        if (f2.sourceFilter != SourceFilterOption.ALL) {
            result = when (f2.sourceFilter) {
                SourceFilterOption.DEVICE_MEDIA -> result.filter { it.uri.startsWith("content://") || it.uri.startsWith("file://") }
                SourceFilterOption.LINKS -> result.filter { it.itemType == StashItemType.LINK }
                SourceFilterOption.NOTES -> result.filter { it.itemType == StashItemType.NOTE }
                SourceFilterOption.ALL -> result
            }
        }

        // 7. Important / Pinned
        if (f2.onlyPinned) {
            result = result.filter { it.isPinned }
        }

        // 8. Has Tracker
        if (f2.onlyTracked) {
            result = result.filter { it.expiryDate != null }
        }

        // 9. Tag
        if (!f2.selectedTag.isNullOrBlank()) {
            result = result.filter { it.tags.contains(f2.selectedTag, ignoreCase = true) }
        }

        // 10. Sorting
        val sorted = when (f2.sort) {
            LibrarySortOrder.NAME -> result.sortedBy { it.title.lowercase() }
            LibrarySortOrder.DATE_ADDED -> result.sortedBy { it.addedTimestamp }
            LibrarySortOrder.DATE_MODIFIED -> result.sortedBy { it.modifiedTimestamp }
            LibrarySortOrder.FILE_SIZE -> result.sortedBy { it.fileSize }
            LibrarySortOrder.FILE_TYPE -> result.sortedBy { it.itemType.label }
            LibrarySortOrder.CATEGORY -> result.sortedBy { it.category }
            LibrarySortOrder.RELEVANCE -> result.sortedByDescending { if (it.isPinned) 1 else 0 }
        }

        if (f2.asc) sorted else sorted.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SELECTION & BULK ACTIONS ---

    fun toggleSelectionMode() {
        val current = isSelectionMode.value
        isSelectionMode.value = !current
        if (!isSelectionMode.value) {
            selectedItemIds.value = emptySet()
        }
    }

    fun toggleItemSelection(id: Long) {
        val current = selectedItemIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        selectedItemIds.value = current
        if (current.isNotEmpty()) {
            isSelectionMode.value = true
        }
    }

    fun selectAll(items: List<StashItem>) {
        isSelectionMode.value = true
        selectedItemIds.value = items.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedItemIds.value = emptySet()
        isSelectionMode.value = false
    }

    fun bulkDeleteSelected() {
        val ids = selectedItemIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val itemsToDelete = allItems.value.filter { it.id in ids }
            repository.moveToTrashBulk(itemsToDelete)
            clearSelection()
            _scanNotice.value = "Moved ${itemsToDelete.size} items to Trash (held for 30 days)."
        }
    }

    fun bulkPinSelected(pin: Boolean) {
        val ids = selectedItemIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.togglePinBulk(ids, pin)
            clearSelection()
            _scanNotice.value = if (pin) "Marked as Important." else "Removed from Important."
        }
    }

    fun promptBulkMove() {
        val ids = selectedItemIds.value
        if (ids.isEmpty()) return
        val items = allItems.value.filter { it.id in ids }
        itemsToMove.value = items
        showMoveDialog.value = true
    }

    fun promptMoveItem(item: StashItem) {
        itemsToMove.value = listOf(item)
        showMoveDialog.value = true
    }

    fun executeMove(targetFolderId: Long?) {
        val items = itemsToMove.value
        if (items.isEmpty()) return
        viewModelScope.launch {
            repository.moveItemsToFolder(items.map { it.id }, targetFolderId)
            showMoveDialog.value = false
            itemsToMove.value = emptyList()
            clearSelection()
            _scanNotice.value = "Moved ${items.size} item(s)."
        }
    }

    // --- COPY & PASTE ---

    fun copyItemToClipboard(item: StashItem, isCut: Boolean = false) {
        clipboard.value = ClipboardPayload(listOf(item), isCut = isCut)
        _scanNotice.value = if (isCut) "\"${item.title}\" cut to clipboard." else "\"${item.title}\" copied to clipboard."
    }

    fun copySelectedToClipboard(isCut: Boolean = false) {
        val ids = selectedItemIds.value
        if (ids.isEmpty()) return
        val items = allItems.value.filter { it.id in ids }
        clipboard.value = ClipboardPayload(items, isCut = isCut)
        clearSelection()
        _scanNotice.value = if (isCut) "Cut ${items.size} item(s) to clipboard." else "Copied ${items.size} item(s) to clipboard."
    }

    fun pasteClipboard(targetFolderId: Long? = currentFolderId.value) {
        val payload = clipboard.value ?: return
        viewModelScope.launch {
            if (payload.isCut) {
                repository.moveItemsToFolder(payload.items.map { it.id }, targetFolderId)
                clipboard.value = null
                _scanNotice.value = "Moved ${payload.items.size} item(s)."
            } else {
                repository.copyItemsBulk(payload.items, targetFolderId)
                _scanNotice.value = "Pasted ${payload.items.size} item(s)."
            }
        }
    }

    fun clearClipboard() {
        clipboard.value = null
    }

    // --- FOLDERS ---

    fun createFolder(name: String, parentId: Long? = currentFolderId.value, colorHex: String? = null) {
        viewModelScope.launch {
            repository.createFolder(name, parentId, colorHex)
            showCreateFolderDialog.value = false
            _scanNotice.value = "Folder \"$name\" created."
        }
    }

    fun renameFolder(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renameFolder(id, newName)
            _scanNotice.value = "Folder renamed."
        }
    }

    fun deleteFolder(folder: FolderItem) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            if (currentFolderId.value == folder.id) {
                currentFolderId.value = folder.parentFolderId
            }
            _scanNotice.value = "Folder deleted. Items moved to parent."
        }
    }

    fun navigateToFolder(folderId: Long?) {
        currentFolderId.value = folderId
        clearSelection()
    }

    fun navigateUp() {
        val current = currentFolder.value
        currentFolderId.value = current?.parentFolderId
        clearSelection()
    }

    // --- ITEM ACTIONS ---

    fun renameItem(item: StashItem, newTitle: String) {
        viewModelScope.launch {
            repository.renameItem(item, newTitle)
            if (_selectedItem.value?.id == item.id) {
                _selectedItem.value = _selectedItem.value?.copy(title = newTitle)
            }
            _scanNotice.value = "Item renamed."
        }
    }

    fun replaceMedia(item: StashItem, newUri: Uri, newName: String?, newMimeType: String?, newSize: Long) {
        viewModelScope.launch {
            repository.replaceMedia(item, newUri, newName, newMimeType, newSize)
            _selectedItem.value = repository.getItemById(item.id).map { it }.stateIn(viewModelScope).value
            _scanNotice.value = "File replaced and re-analyzed."
        }
    }

    fun togglePin(item: StashItem) {
        viewModelScope.launch {
            repository.togglePin(item)
            if (_selectedItem.value?.id == item.id) {
                _selectedItem.value = _selectedItem.value?.copy(isPinned = !item.isPinned)
            }
        }
    }

    // --- TRASH & RETENTION OPERATIONS ---

    fun moveToTrash(item: StashItem) {
        viewModelScope.launch {
            repository.moveToTrash(item)
            if (_selectedItem.value?.id == item.id) {
                _selectedItem.value = null
            }
            _scanNotice.value = "Moved to Trash (30 days retention)."
        }
    }

    fun toggleTrashSelection(id: Long) {
        val current = selectedTrashIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        selectedTrashIds.value = current
    }

    fun selectAllTrash(items: List<StashItem>) {
        selectedTrashIds.value = items.map { it.id }.toSet()
    }

    fun clearTrashSelection() {
        selectedTrashIds.value = emptySet()
    }

    fun restoreItem(item: StashItem) {
        viewModelScope.launch {
            val folderExists = repository.restoreItem(item)
            _scanNotice.value = if (folderExists) {
                "\"${item.title}\" restored to original location."
            } else {
                "Original folder not found. Restored to Library root."
            }
        }
    }

    fun restoreSelectedTrash() {
        val ids = selectedTrashIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val items = trashedItems.value.filter { it.id in ids }
            val outcome = repository.restoreItemsBulk(items)
            clearTrashSelection()
            _scanNotice.value = if (outcome.fallbackCount > 0) {
                "Restored ${outcome.restoredCount} items (${outcome.fallbackCount} to Library root as original folder was missing)."
            } else {
                "Restored ${outcome.restoredCount} item(s)."
            }
        }
    }

    fun restoreAllTrash() {
        viewModelScope.launch {
            val outcome = repository.restoreAllTrash()
            clearTrashSelection()
            _scanNotice.value = "Restored all ${outcome.restoredCount} items from Trash."
        }
    }

    fun deletePermanently(item: StashItem) {
        viewModelScope.launch {
            repository.deletePermanently(item)
            _scanNotice.value = "Permanently deleted \"${item.title}\"."
        }
    }

    fun deleteSelectedTrashPermanently() {
        val ids = selectedTrashIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val items = trashedItems.value.filter { it.id in ids }
            repository.deletePermanentlyBulk(items)
            clearTrashSelection()
            _scanNotice.value = "Permanently deleted ${items.size} item(s)."
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            clearTrashSelection()
            _scanNotice.value = "Trash emptied permanently."
        }
    }

    fun restoreFolder(folder: FolderItem) {
        viewModelScope.launch {
            repository.restoreFolder(folder)
            _scanNotice.value = "Folder \"${folder.name}\" and its contents restored."
        }
    }

    fun deleteFolderPermanently(folder: FolderItem) {
        viewModelScope.launch {
            repository.deleteFolderPermanently(folder)
            _scanNotice.value = "Permanently deleted folder \"${folder.name}\"."
        }
    }

    fun cycleThemeMode() {
        viewModelScope.launch {
            val next = when (themeMode.value) {
                ThemeMode.SYSTEM -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.SYSTEM
            }
            preferencesRepository.setThemeMode(next)
        }
    }

    fun confirmSuggestedTracker(tracker: TrackerItem) {
        viewModelScope.launch {
            repository.confirmSuggestedTracker(tracker)
            _scanNotice.value = "Added \"${tracker.title}\" to active trackers!"
        }
    }

    fun dismissSuggestedTracker(tracker: TrackerItem) {
        viewModelScope.launch {
            repository.dismissSuggestedTracker(tracker)
        }
    }

    fun cleanupExpiredTrash() {
        viewModelScope.launch {
            val cleaned = repository.cleanupExpiredTrash()
            if (cleaned > 0) {
                _scanNotice.value = "Cleaned up $cleaned expired item(s) older than 30 days."
            }
        }
    }

    // --- EXISTING DELEGATES ---

    fun selectItem(item: StashItem?) {
        _selectedItem.value = item
    }

    fun setViewMode(mode: LibraryViewMode) {
        viewModelScope.launch {
            preferencesRepository.setViewMode(mode)
        }
    }

    fun setSortOrder(order: LibrarySortOrder) {
        sortOrder.value = order
    }

    fun toggleSortDirection() {
        sortAscending.value = !sortAscending.value
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
        }
    }

    fun triggerMediaScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanNotice.value = "Indexing accessible media..."
            try {
                val count = repository.scanDeviceMedia()
                preferencesRepository.setAutoScanCompleted(true)
                _scanNotice.value = if (count > 0) {
                    "Discovered and organized $count real items!"
                } else {
                    "Scan complete. No new items found on device."
                }
            } catch (e: Exception) {
                _scanNotice.value = "Scan error: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun dismissScanNotice() {
        _scanNotice.value = null
    }

    fun addManualMedia(
        uri: Uri,
        name: String? = null,
        mimeType: String? = null,
        isScreenshot: Boolean = false,
        folderId: Long? = currentFolderId.value
    ) {
        viewModelScope.launch {
            repository.addManualMedia(uri, name, mimeType, isScreenshot, folderId)
            _scanNotice.value = "Item saved and analyzed by STASH!"
        }
    }

    fun addLink(url: String, category: String? = null, folderId: Long? = currentFolderId.value) {
        viewModelScope.launch {
            repository.addLink(url, category, folderId)
            showAddLinkDialog.value = false
            _scanNotice.value = "Saved link and extracted metadata!"
        }
    }

    fun addNote(title: String, content: String, category: String = "Other", folderId: Long? = currentFolderId.value) {
        viewModelScope.launch {
            repository.addNote(title, content, category, folderId)
            showAddNoteDialog.value = false
            _scanNotice.value = "Saved note to your stash!"
        }
    }

    fun updateItem(item: StashItem) {
        viewModelScope.launch {
            repository.updateItem(item)
            if (_selectedItem.value?.id == item.id) {
                _selectedItem.value = item
            }
        }
    }

    fun deleteItem(item: StashItem) {
        // Normal Delete = Move to Trash (30 days retention policy)
        moveToTrash(item)
    }

    fun addTracker(
        title: String,
        type: TrackerType,
        targetDate: Long,
        stashItemId: Long? = null,
        reminderDays: Int = 7,
        notes: String? = null
    ) {
        viewModelScope.launch {
            repository.addTracker(title, type, targetDate, stashItemId, reminderDays, notes)
            showAddTrackerDialog.value = false
        }
    }

    fun toggleTrackerCompleted(tracker: TrackerItem) {
        viewModelScope.launch {
            repository.toggleTrackerCompleted(tracker)
        }
    }

    fun deleteTracker(tracker: TrackerItem) {
        viewModelScope.launch {
            repository.deleteTracker(tracker)
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            val items = allItems.value
            val trackers = allTrackers.value
            cloudSyncManager.syncWithVercelBackend(items, trackers, "https://stash-api.vercel.app")
            repository.cleanupExpiredTrash()
        }
    }

    fun setUserSession(email: String?) {
        viewModelScope.launch {
            preferencesRepository.setUserSession(email)
        }
    }

    fun handleSharedIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type ?: return

        when (action) {
            Intent.ACTION_SEND -> {
                if (type.startsWith("text/")) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                    if (sharedText.startsWith("http://") || sharedText.startsWith("https://")) {
                        addLink(sharedText)
                    } else {
                        addNote(title = "Shared Note", content = sharedText)
                    }
                } else if (type.startsWith("image/") || type == "application/pdf") {
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return
                    addManualMedia(uri, mimeType = type)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: return
                for (uri in uris) {
                    addManualMedia(uri, mimeType = type)
                }
            }
        }
    }

    private data class FilterTuple1(
        val folderId: Long?,
        val category: String?,
        val type: StashItemType?,
        val dateFilter: DateFilterOption,
        val sizeFilter: SizeFilterOption
    )

    private data class FilterTuple2(
        val sourceFilter: SourceFilterOption,
        val onlyPinned: Boolean,
        val onlyTracked: Boolean,
        val selectedTag: String?,
        val sort: LibrarySortOrder,
        val asc: Boolean
    )

    class Factory(
        private val repository: StashRepository,
        private val preferencesRepository: PreferencesRepository,
        private val cloudSyncManager: CloudSyncManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StashViewModel(repository, preferencesRepository, cloudSyncManager) as T
        }
    }
}
