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

    // ── Main page entries ──────────────────────────────────────────────────────
    // Format: "sourceId|||payload"  →  "SourceLabel - EntryName"
    override val mainPage = mainPageOf(
        *categories.map { cat -> "strmfree|||${"https://streamfree.top/api/v1/streams?category=${cat.key}"}" to "${cat.label} (StrmFree)" }.toTypedArray(),
        "daddylive|||https://daddylive.app/api/events" to "DaddyLive",
        "ppv|||https://api.ppv.st/api/streams" to "PPV",
    )

    // ── getMainPage ────────────────────────────────────────────────────────────
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val sourceId = request.name.substringBefore(" - ")
        val payload = request.name.substringAfter(" - ", "").trim()

        return when (sourceId) {
            "strmfree" -> getStrmFreeMainPage(payload)
            "daddylive" -> getDaddyLiveMainPage()
            "ppv" -> getPpvMainPage()
            else -> newHomePageResponse(emptyList())
        }
    }

    // ── StrmFree source ────────────────────────────────────────────────────────
    private suspend fun getStrmFreeMainPage(categoryUrl: String): HomePageResponse {
        val catKey = Regex("category=([a-z]+)").find(categoryUrl)?.groupValues?.get(1) ?: ""
        val catLabel = categories.find { it.key == catKey }?.label ?: catKey

        val response = app.get(categoryUrl)
        val body = response.body.string()
        val streams = mutableListOf<SearchResponse>()

        try {
            val json = JSONObject(body)
            val streamArray = json.optJSONArray("streams") ?: JSONArray()
            for (i in 0 until streamArray.length()) {
                val s = streamArray.getJSONObject(i)
                val name = s.optString("name").trim()
                val sourcesArr = s.optJSONArray("sources")
                if (name.isEmpty() || sourcesArr == null || sourcesArr.length() == 0) continue
                val embedUrl = sourcesArr.optString(sourcesArr.length() - 1).trim()
                if (embedUrl.isEmpty()) continue
                val poster = s.optString("thumbnail_url", "")
                streams.add(newLiveSearchResponse(name, embedUrl, TvType.Live) {
                    if (poster.isNotEmpty()) this.posterUrl = poster
                })
            }
        } catch (e: Exception) {
            println("StreamedLive[strmfree] error: ${e.message}")
        }

        return newHomePageResponse(
            list = HomePageList(name = catLabel, list = streams, isHorizontalImages = false),
            hasNext = false
        )
    }

    // ── DaddyLive source ───────────────────────────────────────────────────────
    private suspend fun getDaddyLiveMainPage(): HomePageResponse {
        val response = app.get("https://daddylive.app/api/events")
        val body = response.body.string()
        val streams = mutableListOf<SearchResponse>()

        try {
            val json = JSONObject(body)
            val categoriesObj = json.optJSONObject("categories") ?: JSONObject()
            val eventKeys = categoriesObj.names()
            if (eventKeys != null) {
                for (i in 0 until eventKeys.length()) {
                    val catName = eventKeys.getString(i)
                    val events = categoriesObj.optJSONArray(catName) ?: continue
                    for (j in 0 until events.length()) {
                        val ev = events.getJSONObject(j)
                        val eventName = ev.optString("event", "").trim()
                        val channels = ev.optJSONArray("channels") ?: continue
                        for (k in 0 until channels.length()) {
                            val ch = channels.getJSONObject(k)
                            val chUrl = ch.optString("url", "").trim()
                            if (chUrl.isEmpty()) continue
                            streams.add(newLiveSearchResponse(eventName, chUrl, TvType.Live))
                        }
                        if (streams.size >= 100) break
                    }
                    if (streams.size >= 100) break
                }
            }
        } catch (e: Exception) {
            println("StreamedLive[daddylive] error: ${e.message}")
        }

        return newHomePageResponse(
            list = HomePageList(name = "DaddyLive", list = streams, isHorizontalImages = false),
            hasNext = false
        )
    }

    // ── PPV source ─────────────────────────────────────────────────────────────
    private suspend fun getPpvMainPage(): HomePageResponse {
        val response = app.get("https://api.ppv.st/api/streams")
        val body = response.body.string()
        val streams = mutableListOf<SearchResponse>()

        try {
            val json = JSONObject(body)
            val streamsArr = json.optJSONArray("streams") ?: JSONArray()
            for (i in 0 until streamsArr.length()) {
                val cat = streamsArr.getJSONObject(i)
                val catName = cat.optString("category", "").trim()
                val catStreams = cat.optJSONArray("streams") ?: continue
                for (j in 0 until catStreams.length()) {
                    val s = catStreams.getJSONObject(j)
                    val name = s.optString("name", "").trim()
                    val tag = s.optString("tag", "").trim()
                    val display = if (tag.isNotEmpty()) "$name [$tag]" else name
                    val iframe = s.optString("iframe", "").trim()
                    if (iframe.isEmpty()) continue
                    val poster = s.optString("poster", "")
                    streams.add(newLiveSearchResponse(display, iframe, TvType.Live) {
                        if (poster.isNotEmpty()) this.posterUrl = poster
                    })
                }
                if (streams.size >= 100) break
            }
        } catch (e: Exception) {
            println("StreamedLive[ppv] error: ${e.message}")
        }

        return newHomePageResponse(
            list = HomePageList(name = "PPV", list = streams, isHorizontalImages = false),
            hasNext = false
        )
    }

    // ── search ─────────────────────────────────────────────────────────────────
    override suspend fun search(query: String): List<SearchResponse> {
        val all = mutableListOf<SearchResponse>()

        // Search strmfree
        for (cat in categories) {
            try {
                val resp = app.get("https://streamfree.top/api/v1/streams?category=${cat.key}")
                val json = JSONObject(resp.body.string())
                val arr = json.optJSONArray("streams") ?: continue
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    val name = s.optString("name").trim()
                    if (!name.lowercase().contains(query.lowercase())) continue
                    val srcs = s.optJSONArray("sources") ?: continue
                    val embed = srcs.optString(srcs.length() - 1).trim()
                    if (embed.isEmpty()) continue
                    all.add(newLiveSearchResponse(name, embed, TvType.Live) {
                        val poster = s.optString("thumbnail_url", "")
                        if (poster.isNotEmpty()) this.posterUrl = poster
                    })
                }
            } catch (_: Exception) {}
        }

        // Search daddylive
        try {
            val resp = app.get("https://daddylive.app/api/events")
            val json = JSONObject(resp.body.string())
            val cats = json.optJSONObject("categories") ?: JSONObject()
            val keys = cats.names()
            if (keys != null) {
                for (i in 0 until keys.length()) {
                    val evts = cats.optJSONArray(keys.getString(i)) ?: continue
                    for (j in 0 until evts.length()) {
                        val ev = evts.getJSONObject(j)
                        val eventName = ev.optString("event", "").trim()
                        if (!eventName.lowercase().contains(query.lowercase())) continue
                        val chans = ev.optJSONArray("channels") ?: continue
                        for (k in 0 until chans.length()) {
                            val url = chans.getJSONObject(k).optString("url", "").trim()
                            if (url.isEmpty()) continue
                            all.add(newLiveSearchResponse(eventName, url, TvType.Live))
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Search ppv
        try {
            val resp = app.get("https://api.ppv.st/api/streams")
            val json = JSONObject(resp.body.string())
            val arr = json.optJSONArray("streams") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val cat = arr.getJSONObject(i)
                val catStreams = cat.optJSONArray("streams") ?: continue
                for (j in 0 until catStreams.length()) {
                    val s = catStreams.getJSONObject(j)
                    val name = s.optString("name", "").trim()
                    val tag = s.optString("tag", "").trim()
                    val display = if (tag.isNotEmpty()) "$name [$tag]" else name
                    if (!display.lowercase().contains(query.lowercase())) continue
                    val iframe = s.optString("iframe", "").trim()
                    if (iframe.isEmpty()) continue
                    all.add(newLiveSearchResponse(display, iframe, TvType.Live))
                }
                if (all.size >= 50) break
            }
        } catch (_: Exception) {}

        return all.distinctBy { it.url }.take(50)
    }

    // ── load ───────────────────────────────────────────────────────────────────
    override suspend fun load(url: String): LoadResponse {
        val title = url.split("/").lastOrNull()
            ?.replace(Regex("[^a-zA-Z0-9]"), " ")
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?: "Live Stream"
        return newMovieLoadResponse(title, url, TvType.Live, url) {
            this.posterUrl = null
        }
    }

    // ── loadLinks (multi-source pipeline) ──────────────────────────────────────
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var found = false

        // Source 1: strmfree.st direct embed
        if (data.contains("strmfree.st/embed") || data.contains("streamfree.top/embed")) {
            found = loadStrmFreeLinks(data, callback) || found
        }

        // Source 2: daddylive.app embed → resolve iframe → route
        if (data.contains("daddylive.app/player/embed.php")) {
            found = loadDaddyLiveLinks(data, callback) || found
        }

        // Source 3: ppv.st / taifood-blog.asia / embed.st
        if (data.contains("taifood-blog.asia/embed")
            || data.contains("embed.st/embed")
            || data.contains("streamec.st")
            || data.contains("ordernap.net")
            || data.contains("bintv-sources.pages.dev")) {
            found = loadGenericEmbedLinks(data, callback) || found
        }

        return found
    }

    // ── strmfree.st pipeline ───────────────────────────────────────────────────
    private suspend fun loadStrmFreeLinks(
        embedUrl: String,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val embedDoc = app.get(embedUrl).document
        val html = embedDoc.html()

        // Token map — match everything up to the closing semicolon (handles multi-line JSON)
        val tokenMatch = Regex("const _0x\\s*=\\s*([^;]+);").find(html)
        var tokensJson = "{}"
        tokenMatch?.let { tokensJson = it.groupValues[1] }

        // VARIANT JS computes: 'key' + QUALITY + SOURCE_SUFFIX
        // The regex only captures the quoted key portion
        val variantMatch = Regex("""const VARIANT\s*=\s*'([^']+)'""").find(html)
        val suffixMatch  = Regex("""const SOURCE_SUFFIX\s*=\s*'([^']*)'""").find(html)
        val qualityMatch = Regex("""const QUALITY\s*=\s*'([^']+)'""").find(html)

        var variantKey = ""
        var sourceSuffix = ""
        var quality = ""
        variantMatch?.let { variantKey = it.groupValues[1] }
        suffixMatch?.let  { sourceSuffix = it.groupValues[1] }
        qualityMatch?.let { quality = it.groupValues[1] }

        if (variantKey.isEmpty()) return false

        // Reconstruct full VARIANT: key + quality + suffix
        // e.g. "golden-state-warriors-vs-denver-nuggets" + "1080p" + "3"
        val fullVariant = variantKey + quality + sourceSuffix
        val m3u8Path = "/live/${fullVariant}/index.m3u8"
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

    // ── DaddyLive embed resolver ───────────────────────────────────────────────
    private suspend fun loadDaddyLiveLinks(
        daddyliveUrl: String,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val embedDoc = app.get(daddyliveUrl).document
        val html = embedDoc.html()

        // Extract inner iframe src
        val iframeMatch = Regex("""<iframe[^>]+src=["']([^"']+)["']""").find(html)
        val innerUrl = iframeMatch?.groupValues?.get(1)
            ?: Regex("""src=["']([^"']*embed["'][^"']*)["']""").find(html)?.groupValues?.get(1)
            ?: return false

        // Normalize protocol-relative URLs
        val resolvedUrl = if (innerUrl.startsWith("//")) "https:$innerUrl" else innerUrl

        // Route to appropriate handler
        return when {
            resolvedUrl.contains("strmfree.st/embed") -> loadStrmFreeLinks(resolvedUrl, callback)
            resolvedUrl.contains("taifood-blog.asia/embed") -> loadGenericEmbedLinks(resolvedUrl, callback)
            resolvedUrl.contains("embed.st/embed") -> loadGenericEmbedLinks(resolvedUrl, callback)
            else -> loadGenericEmbedLinks(resolvedUrl, callback)
        }
    }

    // ── Generic embed pipeline (for ppv/taifood/other embeds) ──────────────────
    private suspend fun loadGenericEmbedLinks(
        embedUrl: String,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        try {
            val doc = app.get(embedUrl).document
            val html = doc.html()

            // Try direct SRC extraction (some embeds expose it inline)
            val srcMatch = Regex("""const\s+SRC\s*=\s*"([^"]+)" """).find(html)
            if (srcMatch != null) {
                newExtractorLink(
                    name = "StreamedLive",
                    source = embedUrl.split("/")[2],
                    url = srcMatch.groupValues[1],
                    type = INFER_TYPE
                ) {
                    this.referer = embedUrl
                    callback(this)
                }
                return true
            }

            // Try strmfree pattern tokens (taifood may contain same player)
            if (html.contains("const _0x")) {
                return loadStrmFreeLinks(embedUrl, callback)
            }

            // Try to find any m3u8 URL in the page
            val m3u8Match = Regex("""https?://[^"'\s<>]+\.(?:m3u8|m3u)[^"'\s<>]*""").find(html)
            if (m3u8Match != null) {
                newExtractorLink(
                    name = "StreamedLive",
                    source = embedUrl.split("/")[2],
                    url = m3u8Match.value,
                    type = INFER_TYPE
                ) {
                    this.referer = embedUrl
                    callback(this)
                }
                return true
            }
        } catch (e: Exception) {
            println("StreamedLive[generic] error: ${e.message}")
        }
        return false
    }

    // ── m3u8 URL builder ───────────────────────────────────────────────────────
    private fun buildM3u8Url(path: String, tokensJson: String, quality: String): String? {
        try {
            val tokens = JSONObject(tokensJson)
            val qualities = if (quality.isNotEmpty())
                listOf(quality) + listOf("720p", "540p")
            else
                listOf("1080p", "720p", "540p")

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
