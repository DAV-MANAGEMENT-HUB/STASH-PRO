package com.example.engine

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class LinkMetadata(
    val url: String,
    val title: String,
    val domain: String,
    val description: String?,
    val imageUrl: String?
)

object LinkMetadataFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchMetadata(rawUrl: String): LinkMetadata = withContext(Dispatchers.IO) {
        val cleanUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "https://$rawUrl"
        } else {
            rawUrl
        }

        val domain = try {
            Uri.parse(cleanUrl).host ?: cleanUrl
        } catch (_: Exception) {
            cleanUrl
        }

        var title: String? = null
        var description: String? = null
        var imageUrl: String? = null

        try {
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; STASH Digital Organizer)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    title = extractTag(body, "og:title")
                        ?: extractTag(body, "twitter:title")
                        ?: extractHtmlTitle(body)

                    description = extractTag(body, "og:description")
                        ?: extractTag(body, "twitter:description")
                        ?: extractMetaName(body, "description")

                    imageUrl = extractTag(body, "og:image")
                        ?: extractTag(body, "twitter:image")
                }
            }
        } catch (_: Exception) {
            // Graceful fallback: Network unavailable or invalid target
        }

        val finalTitle = title?.trim()?.takeIf { it.isNotBlank() } ?: domain

        LinkMetadata(
            url = cleanUrl,
            title = finalTitle,
            domain = domain,
            description = description?.trim(),
            imageUrl = imageUrl?.trim()
        )
    }

    private fun extractTag(html: String, property: String): String? {
        val pattern = Pattern.compile(
            """<meta\s+[^>]*property=["']${Pattern.quote(property)}["'][^>]*content=["']([^"']*)["']""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)

        val altPattern = Pattern.compile(
            """<meta\s+[^>]*content=["']([^"']*)["'][^>]*property=["']${Pattern.quote(property)}["']""",
            Pattern.CASE_INSENSITIVE
        )
        val altMatcher = altPattern.matcher(html)
        if (altMatcher.find()) return altMatcher.group(1)
        return null
    }

    private fun extractMetaName(html: String, name: String): String? {
        val pattern = Pattern.compile(
            """<meta\s+[^>]*name=["']${Pattern.quote(name)}["'][^>]*content=["']([^"']*)["']""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        return null
    }

    private fun extractHtmlTitle(html: String): String? {
        val pattern = Pattern.compile("""<title[^>]*>(.*?)</title>""", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)?.replace(Regex("""\s+"""), " ")
        }
        return null
    }
}
