package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FolderItem
import com.example.data.model.StashItem
import com.example.data.model.StashItemType
import com.example.data.model.TrackerType
import com.example.engine.SmartCharacterizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("STASH", appName)
    }

    @Test
    fun `smart characterizer detects receipt and vendor`() {
        val result = SmartCharacterizer.analyze(
            originalName = "apple_store_receipt_macbook.pdf",
            mimeType = "application/pdf",
            isScreenshot = false,
            textSnippet = "Apple Store Order #12345 Subtotal: $1999.00 Tax: $150.00 Total: $2149.00"
        )

        assertEquals("Apple", result.vendor)
        assertEquals(StashItemType.RECEIPT, result.itemType)
        assertEquals("Receipts", result.category)
        assertTrue(result.tags.contains("receipt"))
        assertNotNull(result.suggestedTracker)
        assertEquals(TrackerType.WARRANTY, result.suggestedTracker?.type)
    }

    @Test
    fun `smart characterizer detects screenshot`() {
        val result = SmartCharacterizer.analyze(
            originalName = "Screenshot_20260929-123000.png",
            mimeType = "image/png",
            isScreenshot = true,
            textSnippet = "Screenshots folder"
        )

        assertEquals(StashItemType.SCREENSHOT, result.itemType)
        assertTrue(result.tags.contains("screenshot"))
    }

    @Test
    fun `trash 30-day retention calculation works as expected`() {
        val now = System.currentTimeMillis()
        val itemJustDeleted = StashItem(
            id = 1L,
            uri = "content://media/1",
            title = "Receipt",
            itemType = StashItemType.RECEIPT,
            category = "Receipts",
            isTrashed = true,
            trashedTimestamp = now
        )

        // Just deleted: 30 days remaining
        assertEquals(30L, itemJustDeleted.trashDaysRemaining(now))
        assertFalse(itemJustDeleted.isTrashExpired(now))

        // 10 days later: 20 days remaining
        val tenDaysLater = now + (10L * 24 * 60 * 60 * 1000L)
        assertEquals(20L, itemJustDeleted.trashDaysRemaining(tenDaysLater))
        assertFalse(itemJustDeleted.isTrashExpired(tenDaysLater))

        // 31 days later: expired!
        val thirtyOneDaysLater = now + (31L * 24 * 60 * 60 * 1000L)
        assertEquals(0L, itemJustDeleted.trashDaysRemaining(thirtyOneDaysLater))
        assertTrue(itemJustDeleted.isTrashExpired(thirtyOneDaysLater))
    }

    @Test
    fun `folder item model creates valid folder hierarchy`() {
        val rootFolder = FolderItem(id = 10L, name = "Finance")
        val subFolder = FolderItem(id = 11L, name = "Taxes", parentFolderId = 10L)

        assertEquals("Finance", rootFolder.name)
        assertEquals(10L, subFolder.parentFolderId)
    }
}
