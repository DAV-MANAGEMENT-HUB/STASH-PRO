package com.example.engine

import com.example.data.model.StashItemType
import com.example.data.model.TrackerType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class TrackerCandidate(
    val title: String,
    val type: TrackerType,
    val targetDate: Long,
    val isHighConfidence: Boolean,
    val promptReason: String
)

data class CharacterizationResult(
    val title: String,
    val itemType: StashItemType,
    val category: String,
    val tags: List<String>,
    val vendor: String?,
    val product: String?,
    val person: String?,
    val detectedAmount: Double?,
    val currency: String?,
    val eventDate: Long?,
    val expiryDate: Long?,
    val isUncertain: Boolean,
    val suggestedTracker: TrackerCandidate?
)

object SmartCharacterizer {

    private val KNOWN_VENDORS = listOf(
        // Tech
        "Apple", "HP", "Dell", "Lenovo", "Samsung", "Sony", "Google", "Microsoft",
        "Asus", "Acer", "LG", "Bose", "Logitech", "Anker", "Intel", "AMD", "Canon", "Nikon",
        // Retail
        "Amazon", "Best Buy", "Walmart", "Target", "Costco", "Home Depot", "Ikea", "eBay",
        "Nike", "Adidas", "Sephora", "Zara",
        // Auto / Insurance
        "Geico", "State Farm", "Progressive", "Allstate", "Liberty Mutual", "AAA",
        "Toyota", "Honda", "Ford", "Tesla", "BMW", "Audi",
        // Services & Telecom
        "Netflix", "Spotify", "Adobe", "Disney+", "Verizon", "AT&T", "T-Mobile",
        "Uber", "Airbnb", "Delta", "United", "American Airlines",
        // Banking / Fintech
        "Chase", "Bank of America", "Wells Fargo", "Citi", "Amex", "PayPal", "Stripe", "Revolut"
    )

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US),
        SimpleDateFormat("MMM dd, yyyy", Locale.US),
        SimpleDateFormat("MMMM dd, yyyy", Locale.US),
        SimpleDateFormat("dd MMM yyyy", Locale.US)
    )

    fun analyze(
        originalName: String,
        mimeType: String?,
        isScreenshot: Boolean,
        textSnippet: String?,
        rawUrl: String? = null
    ): CharacterizationResult {
        val combinedText = buildString {
            append(originalName).append(" ")
            if (!textSnippet.isNullOrBlank()) append(textSnippet).append(" ")
            if (!rawUrl.isNullOrBlank()) append(rawUrl).append(" ")
        }
        val lower = combinedText.lowercase(Locale.ROOT)
        val nameLower = originalName.lowercase(Locale.ROOT)

        // 1. Detect Vendor / Brand
        val vendor = detectVendor(combinedText)

        // 2. Detect Product / Subject
        val product = detectProduct(nameLower, combinedText)

        // 3. Detect Currency & Amount
        val (detectedAmount, currency) = detectAmount(combinedText)

        // 4. Detect Dates (Event date and Expiry/Due date)
        val (eventDate, expiryDate, expiryReason) = detectDates(combinedText)

        // 5. Determine Type and Category
        var type = StashItemType.OTHER
        var category = "Other"
        val tags = mutableListOf<String>()
        var isUncertain = false

        when {
            rawUrl != null && rawUrl.startsWith("http", ignoreCase = true) -> {
                type = StashItemType.LINK
                category = "Links"
                tags.add("web")
                tags.add("bookmark")
            }
            isScreenshot -> {
                type = StashItemType.SCREENSHOT
                category = "Memories"
                tags.add("screenshot")
                if (lower.contains("receipt") || lower.contains("order") || lower.contains("subtotal")) {
                    category = "Receipts"
                    tags.add("receipt")
                } else if (lower.contains("warranty") || lower.contains("coverage")) {
                    category = "Warranties"
                    tags.add("warranty")
                }
            }
            nameLower.contains("receipt") || lower.contains("subtotal") || lower.contains("invoice") || lower.contains("order confirmation") || lower.contains("total: $") -> {
                type = StashItemType.RECEIPT
                category = "Receipts"
                tags.add("receipt")
                tags.add("expense")
            }
            nameLower.contains("warranty") || lower.contains("warranty period") || lower.contains("guarantee") || lower.contains("applecare") -> {
                type = StashItemType.WARRANTY
                category = "Warranties"
                tags.add("warranty")
                tags.add("guarantee")
            }
            nameLower.contains("resume") || nameLower.contains("cv") || lower.contains("curriculum vitae") || (lower.contains("education") && lower.contains("work experience")) -> {
                type = StashItemType.RESUME
                category = "Work"
                tags.add("career")
                tags.add("resume")
            }
            nameLower.contains("certificate") || nameLower.contains("diploma") || lower.contains("completion certificate") || lower.contains("awarded to") -> {
                type = StashItemType.CERTIFICATE
                category = "Certificates"
                tags.add("certificate")
                tags.add("credential")
            }
            nameLower.contains("passport") || nameLower.contains("license") || lower.contains("driver license") || lower.contains("national id") || lower.contains("identity card") -> {
                type = StashItemType.DOCUMENT
                category = "Identity"
                tags.add("id")
                tags.add("identity")
            }
            nameLower.contains("insurance") || lower.contains("policy number") || lower.contains("insurance policy") || lower.contains("coverage period") -> {
                type = StashItemType.DOCUMENT
                category = "Insurance"
                tags.add("insurance")
                tags.add("policy")
            }
            mimeType?.startsWith("image/") == true -> {
                type = StashItemType.PHOTO
                category = "Memories"
                tags.add("photo")
            }
            mimeType == "application/pdf" || nameLower.endsWith(".pdf") || nameLower.endsWith(".doc") || nameLower.endsWith(".docx") -> {
                type = StashItemType.DOCUMENT
                category = "Education"
                tags.add("document")
            }
            else -> {
                isUncertain = true
                type = StashItemType.OTHER
                category = "Other"
            }
        }

        if (vendor != null) {
            tags.add(vendor.lowercase())
        }

        // Clean user-friendly title
        val title = generateCleanTitle(originalName, type, vendor, product)

        // 6. Tracker Candidate Generation
        val suggestedTracker = generateTrackerCandidate(
            title = title,
            type = type,
            category = category,
            vendor = vendor,
            eventDate = eventDate,
            expiryDate = expiryDate,
            expiryReason = expiryReason
        )

        return CharacterizationResult(
            title = title,
            itemType = type,
            category = category,
            tags = tags.distinct(),
            vendor = vendor,
            product = product,
            person = null,
            detectedAmount = detectedAmount,
            currency = currency,
            eventDate = eventDate,
            expiryDate = expiryDate,
            isUncertain = isUncertain,
            suggestedTracker = suggestedTracker
        )
    }

    private fun detectVendor(text: String): String? {
        for (v in KNOWN_VENDORS) {
            val pattern = Pattern.compile("\\b${Pattern.quote(v)}\\b", Pattern.CASE_INSENSITIVE)
            if (pattern.matcher(text).find()) {
                return v
            }
        }
        return null
    }

    private fun detectProduct(nameLower: String, combined: String): String? {
        val keywords = listOf(
            "laptop", "macbook", "iphone", "ipad", "phone", "tv", "monitor",
            "headphones", "camera", "car", "watch", "tablet", "printer",
            "refrigerator", "microwave", "airpods", "console", "playstation", "xbox"
        )
        for (kw in keywords) {
            if (nameLower.contains(kw) || combined.contains(kw, ignoreCase = true)) {
                return kw.replaceFirstChar { it.uppercase() }
            }
        }
        return null
    }

    private fun detectAmount(text: String): Pair<Double?, String?> {
        // Match $123.45 or 123.45 USD or €123.45
        val regex = Regex("""([$€£₹])\s*(\d{1,5}(?:\.\d{2})?)""")
        val match = regex.find(text)
        if (match != null) {
            val symbol = match.groupValues[1]
            val amt = match.groupValues[2].toDoubleOrNull()
            val cur = when (symbol) {
                "$" -> "USD"
                "€" -> "EUR"
                "£" -> "GBP"
                "₹" -> "INR"
                else -> symbol
            }
            return Pair(amt, cur)
        }
        return Pair(null, null)
    }

    private fun detectDates(text: String): Triple<Long?, Long?, String?> {
        val dateRegex = Regex("""\b(\d{4}[-/]\d{1,2}[-/]\d{1,2}|\d{1,2}[-/]\d{1,2}[-/]\d{2,4})\b""")
        val matches = dateRegex.findAll(text).toList()

        var eventDate: Long? = null
        var expiryDate: Long? = null
        var expiryReason: String? = null

        val now = System.currentTimeMillis()

        for (m in matches) {
            val dateStr = m.value
            val parsed = parseDateString(dateStr) ?: continue

            // Check surrounding context for expiry
            val startIdx = (m.range.first - 30).coerceAtLeast(0)
            val endIdx = (m.range.last + 30).coerceAtMost(text.length)
            val window = text.substring(startIdx, endIdx).lowercase(Locale.ROOT)

            if (window.contains("expir") || window.contains("valid until") || window.contains("valid thru") || window.contains("due") || window.contains("renew")) {
                expiryDate = parsed
                expiryReason = when {
                    window.contains("expir") -> "Expiry Date"
                    window.contains("due") -> "Due Date"
                    window.contains("renew") -> "Renewal Date"
                    else -> "Valid Until"
                }
            } else if (window.contains("purchas") || window.contains("date:") || window.contains("order date") || window.contains("billed") || parsed <= now) {
                if (eventDate == null) {
                    eventDate = parsed
                }
            }
        }

        return Triple(eventDate, expiryDate, expiryReason)
    }

    private fun parseDateString(str: String): Long? {
        for (fmt in DATE_FORMATS) {
            try {
                val d = fmt.parse(str)
                if (d != null) {
                    // Reasonable bounds: 1980 to 2050
                    val cal = Calendar.getInstance().apply { time = d }
                    val year = cal.get(Calendar.YEAR)
                    if (year in 1980..2050) {
                        return d.time
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun generateCleanTitle(originalName: String, type: StashItemType, vendor: String?, product: String?): String {
        // Strip common extension
        val cleanName = originalName.substringBeforeLast(".")
            .replace("_", " ")
            .replace("-", " ")
            .trim()

        return when {
            vendor != null && product != null -> "$vendor $product ${type.label}"
            vendor != null -> "$vendor ${type.label}"
            cleanName.isNotBlank() && cleanName.length < 50 -> cleanName
            else -> "${type.label} (${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())})"
        }
    }

    private fun generateTrackerCandidate(
        title: String,
        type: StashItemType,
        category: String,
        vendor: String?,
        eventDate: Long?,
        expiryDate: Long?,
        expiryReason: String?
    ): TrackerCandidate? {
        val now = System.currentTimeMillis()

        // 1. Explicit detected expiry date
        if (expiryDate != null && expiryDate > now) {
            val trackerType = when (type) {
                StashItemType.WARRANTY -> TrackerType.WARRANTY
                StashItemType.RECEIPT -> TrackerType.RETURN_DEADLINE
                else -> when (category) {
                    "Insurance" -> TrackerType.INSURANCE
                    "Identity" -> TrackerType.ID_EXPIRY
                    "Certificates" -> TrackerType.CERTIFICATE
                    else -> TrackerType.CUSTOM
                }
            }
            return TrackerCandidate(
                title = "$title Expiry",
                type = trackerType,
                targetDate = expiryDate,
                isHighConfidence = true,
                promptReason = expiryReason ?: "Detected Expiry Date"
            )
        }

        // 2. Inferred Warranty from Purchase receipt: +1 year warranty standard
        if (type == StashItemType.WARRANTY || (type == StashItemType.RECEIPT && vendor != null)) {
            val baseTime = eventDate ?: now
            val oneYearLater = Calendar.getInstance().apply {
                timeInMillis = baseTime
                add(Calendar.YEAR, 1)
            }.timeInMillis

            if (oneYearLater > now) {
                return TrackerCandidate(
                    title = "$title 1-Year Warranty",
                    type = TrackerType.WARRANTY,
                    targetDate = oneYearLater,
                    isHighConfidence = type == StashItemType.WARRANTY,
                    promptReason = "Standard 1-Year Warranty from purchase date"
                )
            }
        }

        // 3. Insurance policy renewal: 1 year from purchase/policy date
        if (category == "Insurance" && eventDate != null) {
            val oneYearLater = Calendar.getInstance().apply {
                timeInMillis = eventDate
                add(Calendar.YEAR, 1)
            }.timeInMillis
            if (oneYearLater > now) {
                return TrackerCandidate(
                    title = "$title Policy Renewal",
                    type = TrackerType.INSURANCE,
                    targetDate = oneYearLater,
                    isHighConfidence = false,
                    promptReason = "Annual policy renewal date"
                )
            }
        }

        return null
    }
}
