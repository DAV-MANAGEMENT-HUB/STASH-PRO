package com.example.engine

import android.content.Context
import android.content.pm.PackageManager
import com.example.data.model.StashItem
import com.example.data.model.TrackerConfidence
import com.example.data.model.TrackerItem
import com.example.data.model.TrackerType
import java.util.Calendar
import java.util.Locale

data class DetectedSubscription(
    val serviceName: String,
    val packageName: String?,
    val estimatedAmount: Double?,
    val currency: String?,
    val renewalIntervalMonths: Int,
    val confidence: TrackerConfidence,
    val sourceDescription: String,
    val isInstalledApp: Boolean
)

object SubscriptionIntelligence {

    private val KNOWN_SUBSCRIPTION_PACKAGES = mapOf(
        "com.spotify.music" to Pair("Spotify", 10.99),
        "com.netflix.mediaclient" to Pair("Netflix", 15.49),
        "com.google.android.youtube" to Pair("YouTube Premium", 13.99),
        "com.disney.disneyplus" to Pair("Disney+", 13.99),
        "com.amazon.mp3" to Pair("Amazon Music", 9.99),
        "com.amazon.kindle" to Pair("Amazon Kindle Unlimited", 11.99),
        "com.audible.application" to Pair("Audible", 14.95),
        "com.dropbox.android" to Pair("Dropbox Plus", 11.99),
        "com.duolingo" to Pair("Duolingo Super", 6.99),
        "com.adobe.reader" to Pair("Adobe Acrobat Pro", 19.99),
        "com.adobe.creativecloud" to Pair("Adobe Creative Cloud", 54.99),
        "com.microsoft.office.officehubrow" to Pair("Microsoft 365", 6.99),
        "com.google.android.apps.subscriptions.red" to Pair("Google One", 2.99),
        "com.calm.android" to Pair("Calm", 14.99),
        "com.getsomeheadspace.android" to Pair("Headspace", 12.99),
        "com.strava" to Pair("Strava Summit", 11.99),
        "com.nordvpn.android" to Pair("NordVPN", 12.99),
        "com.onepassword.android" to Pair("1Password", 2.99)
    )

    /**
     * Inspects installed applications on the device for known subscription services.
     * Clearly labels these as POTENTIAL subscriptions based on device installation.
     */
    fun detectInstalledSubscriptionApps(context: Context): List<DetectedSubscription> {
        val pm = context.packageManager
        val installed = try {
            pm.getInstalledPackages(0)
        } catch (_: Exception) {
            emptyList()
        }

        val results = mutableListOf<DetectedSubscription>()
        for (pkg in installed) {
            val known = KNOWN_SUBSCRIPTION_PACKAGES[pkg.packageName]
            if (known != null) {
                results.add(
                    DetectedSubscription(
                        serviceName = known.first,
                        packageName = pkg.packageName,
                        estimatedAmount = known.second,
                        currency = "USD",
                        renewalIntervalMonths = 1,
                        confidence = TrackerConfidence.INSTALLED_APP_POTENTIAL,
                        sourceDescription = "Installed app detected on device (potential subscription)",
                        isInstalledApp = true
                    )
                )
            }
        }
        return results
    }

    /**
     * Scans actual user receipts/documents stored in STASH to find recurring subscription patterns.
     */
    fun analyzeReceiptForSubscription(item: StashItem): DetectedSubscription? {
        val text = buildString {
            append(item.title).append(" ")
            append(item.extractedText ?: "").append(" ")
            append(item.notes ?: "")
        }.lowercase(Locale.ROOT)

        val isSubscriptionKeyword = text.contains("subscription") ||
                text.contains("/month") ||
                text.contains("monthly") ||
                text.contains("per month") ||
                text.contains("annual membership") ||
                text.contains("recurring charge") ||
                text.contains("auto-renew") ||
                text.contains("renewal date")

        if (!isSubscriptionKeyword) return null

        val isAnnual = text.contains("annual") || text.contains("yearly") || text.contains("/year")
        val intervalMonths = if (isAnnual) 12 else 1

        val serviceName = item.vendor ?: item.product ?: item.title
        val sourceDesc = "Inferred from receipt: \"${item.title}\""

        return DetectedSubscription(
            serviceName = serviceName,
            packageName = null,
            estimatedAmount = item.detectedAmount,
            currency = item.currency ?: "USD",
            renewalIntervalMonths = intervalMonths,
            confidence = TrackerConfidence.INFERRED_FROM_RECEIPT,
            sourceDescription = sourceDesc,
            isInstalledApp = false
        )
    }

    /**
     * Converts a DetectedSubscription into a suggested TrackerItem.
     */
    fun createTrackerFromDetection(
        detection: DetectedSubscription,
        stashItemId: Long? = null
    ): TrackerItem {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, detection.renewalIntervalMonths)
        }

        return TrackerItem(
            stashItemId = stashItemId,
            title = "${detection.serviceName} Renewal",
            trackerType = TrackerType.SUBSCRIPTIONS,
            targetDate = cal.timeInMillis,
            reminderDaysBefore = 3,
            isRecurring = true,
            recurrenceIntervalMonths = detection.renewalIntervalMonths,
            amount = detection.estimatedAmount,
            currency = detection.currency,
            confidence = detection.confidence,
            sourceDescription = detection.sourceDescription,
            isConfirmedByUser = false,
            packageName = detection.packageName
        )
    }
}
