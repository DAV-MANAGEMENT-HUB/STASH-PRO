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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrackerConfidence
import com.example.data.model.TrackerItem
import com.example.data.model.TrackerType
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TrackerBadge
import com.example.ui.theme.Amber500
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrackersScreen(
    activeTrackers: List<TrackerItem>,
    completedTrackers: List<TrackerItem>,
    suggestedTrackers: List<TrackerItem>,
    onToggleCompleted: (TrackerItem) -> Unit,
    onDeleteTracker: (TrackerItem) -> Unit,
    onConfirmSuggestedTracker: (TrackerItem) -> Unit,
    onDismissSuggestedTracker: (TrackerItem) -> Unit,
    onAddTrackerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategoryFilter by remember { mutableStateOf<TrackerType?>(null) }
    val tabs = listOf("Active", "Needs Attention", "Subscriptions", "Completed")

    val needsAttentionList = remember(activeTrackers) {
        activeTrackers.filter { it.isNeedsAttention() }
    }

    val subscriptionList = remember(activeTrackers) {
        activeTrackers.filter { it.trackerType == TrackerType.SUBSCRIPTIONS }
    }

    val baseList = when (selectedTab) {
        0 -> activeTrackers
        1 -> needsAttentionList
        2 -> subscriptionList
        3 -> completedTrackers
        else -> activeTrackers
    }

    val displayList = remember(baseList, selectedCategoryFilter) {
        if (selectedCategoryFilter != null) {
            baseList.filter { it.trackerType == selectedCategoryFilter }
        } else {
            baseList
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trackers & Renewals",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Tracker Overview Metric Grid
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricOverviewCard(
                        title = "Attention",
                        count = needsAttentionList.size,
                        color = if (needsAttentionList.isNotEmpty()) RoseRed else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                    MetricOverviewCard(
                        title = "Expiring",
                        count = activeTrackers.count { it.isExpiringSoon(30) },
                        color = Amber500,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    MetricOverviewCard(
                        title = "Active",
                        count = activeTrackers.size,
                        color = EmeraldGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    MetricOverviewCard(
                        title = "Done",
                        count = completedTrackers.size,
                        color = ElectricBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 3 }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Subscription Intelligence Suggestions (if any detected)
            if (suggestedTrackers.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Subscription Intelligence",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "STASH discovered potential subscriptions from your receipts or installed apps. Confirm to begin tracking.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                suggestedTrackers.take(3).forEach { suggested ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = suggested.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = suggested.sourceDescription ?: suggested.confidence.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                if (suggested.amount != null) {
                                                    Text(
                                                        text = "Estimated: $${suggested.amount}/mo",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Row {
                                                Button(
                                                    onClick = { onConfirmSuggestedTracker(suggested) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Track It", fontSize = 12.sp)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                TextButton(
                                                    onClick = { onDismissSuggestedTracker(suggested) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Text("Not Now", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Status Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                val count = when (index) {
                                    0 -> activeTrackers.size
                                    1 -> needsAttentionList.size
                                    2 -> subscriptionList.size
                                    3 -> completedTrackers.size
                                    else -> 0
                                }
                                Text("$title ($count)", fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Category Filter Chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("All Categories") }
                        )
                    }
                    items(TrackerType.values()) { type ->
                        FilterChip(
                            selected = selectedCategoryFilter == type,
                            onClick = { selectedCategoryFilter = if (selectedCategoryFilter == type) null else type },
                            label = { Text(type.label) }
                        )
                    }
                }
            }

            // Trackers Content List
            if (displayList.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "Nothing needs tracking here.",
                        message = when (selectedTab) {
                            1 -> "No trackers require urgent attention right now."
                            2 -> "No subscriptions tracked. Add a subscription or scan receipts to discover them."
                            3 -> "No completed trackers."
                            else -> "When you stash receipts, warranties, policies, or IDs, STASH automatically detects expiry dates and creates trackers."
                        },
                        actionLabel = "Add Custom Tracker",
                        onActionClick = onAddTrackerClick,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                items(displayList) { tracker ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                        RichTrackerCardItem(
                            tracker = tracker,
                            onToggle = { onToggleCompleted(tracker) },
                            onDelete = { onDeleteTracker(tracker) }
                        )
                    }
                }
            }
        }

        // Add Tracker FAB
        FloatingActionButton(
            onClick = onAddTrackerClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_custom_tracker_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Tracker", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun MetricOverviewCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RichTrackerCardItem(
    tracker: TrackerItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("tracker_item_${tracker.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                    if (tracker.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Circle,
                            contentDescription = "Mark Complete",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tracker.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (tracker.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = tracker.trackerType.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (tracker.amount != null) {
                            Text(
                                text = " • $${tracker.amount}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (tracker.isRecurring) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Repeat, contentDescription = "Recurring", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                TrackerBadge(targetDate = tracker.targetDate)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details footer
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Due: ${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(tracker.targetDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!tracker.sourceDescription.isNullOrBlank()) {
                        Text(
                            text = tracker.sourceDescription,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
