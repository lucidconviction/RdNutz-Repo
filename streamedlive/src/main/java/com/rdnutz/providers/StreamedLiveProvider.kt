package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONArray
import org.json.JSONObject

class StreamedLiveProvider : MainAPI() {
    override var mainUrl = "https://strmfree.st"
    override var name = "StreamedLive"
    override val supportedTypes = setOf(TvType.Live)
    override var lang = "en"
    override val hasMainPage = true

    private data class Category(val key: String, val label: String)

    private val categories = listOf(
        Category("soccer", "Soccer"),
        Category("basketball", "Basketball"),
        Category("hockey", "Hockey"),
        Category("combat", "Combat"),
        Category("baseball", "Baseball"),
        Category("football", "American Football"),
        Category("racing", "Racing"),
        Category("tennis", "Tennis"),
        Category("cricket", "Cricket"),
    )

    private val apiBase = "https://streamfree.top"

    override val mainPage = mainPageOf(
        *categories.map { "${apiBase}/api/v1/streams?category=${it.key}" to it.label }.toTypedArray()
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val cat = categories.find { it.label == request.name }
            ?: return newHomePageResponse(emptyList())

        val url = "${apiBase}/api/v1/streams?category=${cat.key}"
        val response = app.get(url)
        val body = response.body.string()

        val streams = mutableListOf<SearchResponse>()
        try {
            val json = JSONObject(body)
            val streamArray = json.optJSONArray("streams") ?: JSONArray()
            for (i in 0 until streamArray.length()) {
                val s = streamArray.getJSONObject(i)
                val name = s.optString("name").trim()
                val sources = s.optJSONArray("sources")
                if (name.isEmpty() || sources == null || sources.length() == 0) continue

                // Use last source (usually highest quality)
                val embedUrl = sources.optString(sources.length() - 1).trim()
                if (embedUrl.isEmpty()) continue

                val poster = s.optString("thumbnail_url", "")
                streams.add(
                    newLiveSearchResponse(name, embedUrl, TvType.Live) {
                        if (poster.isNotEmpty()) this.posterUrl = poster
                    }
                )
            }
        } catch (e: Exception) {
            println("StreamedLive API error: ${e.message}")
        }

        return newHomePageResponse(
            list = HomePageList(name = cat.label, list = streams, isHorizontalImages = false),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        for (cat in categories) {
            val url = "${apiBase}/api/v1/streams?category=${cat.key}"
            try {
                val resp = app.get(url)
                val json = JSONObject(resp.body.string())
                val streamArray = json.optJSONArray("streams") ?: continue
                for (i in 0 until streamArray.length()) {
                    val s = streamArray.getJSONObject(i)
                    val name = s.optString("name").trim()
                    if (!name.lowercase().contains(query.lowercase())) continue
                    val sources = s.optJSONArray("sources")
                    if (sources == null || sources.length() == 0) continue
                    val embedUrl = sources.optString(sources.length() - 1).trim()
                    if (embedUrl.isEmpty()) continue
                    val poster = s.optString("thumbnail_url", "")
                    results.add(
                        newLiveSearchResponse(name, embedUrl, TvType.Live) {
                            if (poster.isNotEmpty()) this.posterUrl = poster
                        }
                    )
                }
            } catch (_: Exception) {}
        }
        return results.distinctBy { it.url }.take(50)
    }

    override suspend fun load(url: String): LoadResponse {
        val title = url.split("/").lastOrNull()
            ?.replace(Regex("[^a-zA-Z0-9]"), " ")
            ?.trim()
            ?: "Live Stream"
        // Use newMovieLoadResponse with TvType.Live (same pattern LiveGore uses for NSFW)
        return newMovieLoadResponse(title, url, TvType.Live, url) {
            this.posterUrl = null
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // data is the embed URL from the API
        if (!data.contains("strmfree.st/embed") && !data.contains("streamfree.top/embed")) {
            return false
        }

        val embedDoc = app.get(data).document
        val html = embedDoc.html()

        // Extract _0x token map using Kotlin regex
        val tokenMatch = Regex("const _0x\\s*=\\s*(\\{[^}]+\\});").find(html)
        var tokensJson = "{}"
        tokenMatch?.let { tokensJson = it.groupValues[1] }

        // Extract VARIANT, SOURCE_SUFFIX, QUALITY
        val variantMatch = Regex("const VARIANT\\s*=\\s*'([^']+)'").find(html)
        val suffixMatch = Regex("const SOURCE_SUFFIX\\s*=\\s*'([^']*)'").find(html)
        val qualityMatch = Regex("const QUALITY\\s*=\\s*'([^']+)'").find(html)

        var variant = ""
        var sourceSuffix = ""
        var quality = ""
        variantMatch?.let { variant = it.groupValues[1] }
        suffixMatch?.let { sourceSuffix = it.groupValues[1] }
        qualityMatch?.let { quality = it.groupValues[1] }

        if (variant.isEmpty()) return false

        // Build m3u8 path: /live/{variant}/index.m3u8
        // VARIANT already contains the full key+quality+suffix, e.g. "memphis-grizzlies-vs-utah-jazz1080p3"
        val m3u8Path = "/live/${variant}/index.m3u8"

        val m3u8Url = buildM3u8Url(m3u8Path, tokensJson, quality)

        if (m3u8Url == null) return false

        newExtractorLink(
            name = "StreamedLive",
            source = "strmfree.st",
            url = m3u8Url,
            type = INFER_TYPE
        ) {
            this.referer = "https://strmfree.st/"
            callback(this)
        }
        return true
    }

    /**
     * Builds the authenticated m3u8 URL from the token map found in the embed page.
     * Returns null if no valid tokens are found.
     */
    private fun buildM3u8Url(path: String, tokensJson: String, quality: String): String? {
        try {
            val tokens = JSONObject(tokensJson)
            // Try the requested quality first, then fallback to common qualities
            val qualities = if (quality.isNotEmpty()) {
                listOf(quality) + listOf("720p", "540p")
            } else {
                listOf("1080p", "720p", "540p")
            }
            for (q in qualities) {
                val qObj = tokens.optJSONObject(q)
                if (qObj == null) continue
                val token = qObj.optString("_t", "").trim()
                val expiry = qObj.optInt("_e", 0)
                val nonce = qObj.optString("_n", "").trim()
                if (token.isEmpty()) continue
                return "$mainUrl$path?_t=$token&_e=$expiry&_n=$nonce"
            }
        } catch (e: Exception) {
            println("StreamedLive token error: ${e.message}")
        }
        return null
    }
}
