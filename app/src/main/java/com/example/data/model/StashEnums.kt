package com.example.data.model

enum class StashItemType(val label: String) {
    DOCUMENT("Document"),
    PHOTO("Photo"),
    SCREENSHOT("Screenshot"),
    RECEIPT("Receipt"),
    WARRANTY("Warranty"),
    CERTIFICATE("Certificate"),
    RESUME("Resume"),
    LINK("Link"),
    NOTE("Note"),
    OTHER("Other")
}

enum class TrackerType(val label: String) {
    SUBSCRIPTIONS("Subscription"),
    SUBSCRIPTION("Subscription"),
    WARRANTIES("Warranty"),
    WARRANTY("Warranty"),
    DOCUMENTS("Document Expiry"),
    ID_EXPIRY("Document / ID Expiry"),
    CERTIFICATE("Certificate"),
    INSURANCE("Insurance"),
    RETURNS("Return Deadline"),
    RETURN_DEADLINE("Return Deadline"),
    PAYMENTS("Payment Due"),
    MEMBERSHIPS("Membership"),
    RENEWALS("Renewal"),
    APPOINTMENTS("Appointment"),
    LICENSES("License"),
    DELIVERIES("Delivery"),
    MAINTENANCE("Maintenance"),
    CUSTOM("Custom")
}

enum class TrackerConfidence(val label: String) {
    CONFIRMED("Confirmed"),
    INFERRED_FROM_RECEIPT("Detected from Receipt"),
    INSTALLED_APP_POTENTIAL("Potential App Subscription"),
    USER_CREATED("Created by User")
}

enum class BinViewMode(val title: String) {
    ORIGINAL_LOCATION("Original Location"),
    ALL_DELETED("All Deleted"),
    RECENTLY_DELETED("Recently Deleted"),
    EXPIRING_SOON("Expiring Soon")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class LibraryViewMode(val title: String) {
    GRID("Grid"),
    LARGE_ICONS("Large Icons"),
    LIST("List"),
    GALLERY("Gallery"),
    TIMELINE("Timeline")
}

enum class LibrarySortOrder(val title: String) {
    NAME("Name"),
    DATE_ADDED("Date Added"),
    DATE_MODIFIED("Date Modified"),
    FILE_SIZE("File Size"),
    FILE_TYPE("File Type"),
    CATEGORY("Category"),
    RELEVANCE("Relevance")
}

enum class DateFilterOption(val label: String) {
    ALL("All Dates"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_YEAR("This Year")
}

enum class SizeFilterOption(val label: String) {
    ALL("All Sizes"),
    UNDER_1_MB("< 1 MB"),
    BETWEEN_1_AND_10_MB("1 – 10 MB"),
    OVER_10_MB("> 10 MB")
}

enum class SourceFilterOption(val label: String) {
    ALL("All Sources"),
    DEVICE_MEDIA("Device Media"),
    LINKS("Web Links"),
    NOTES("Notes")
}

object SystemCategories {
    val ALL = listOf(
        "Identity",
        "Purchases",
        "Warranties",
        "Receipts",
        "Insurance",
        "Subscriptions",
        "Finance",
        "Work",
        "Education",
        "Certificates",
        "Medical",
        "Legal",
        "Property",
        "Travel",
        "Memories",
        "Links",
        "Other"
    )
}
