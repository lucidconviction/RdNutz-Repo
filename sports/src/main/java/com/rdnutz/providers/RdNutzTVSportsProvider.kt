package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

data class ZliveChannel(val slug: String, val name: String)
data class StreamSource(val source: String, val id: String)

class RdNutzTVSportsProvider : MainAPI() {
    override var mainUrl = "https://streamed.pk"
    override var name = "RdNutz TV Sports n More"
    override val supportedTypes = setOf(TvType.Live)
    override var lang = "en"
    override val hasMainPage = true

    private val zliveChannels = listOf(
        ZliveChannel("auto-sky-sports-premier-league", "Sky Sports Premier League"),
        ZliveChannel("auto-sky-sports-main-event", "Sky Sports Main Event"),
        ZliveChannel("auto-sky-sports-f1", "Sky Sports F1"),
        ZliveChannel("auto-sky-sports-ultra-hd", "Sky Sports Ultra HD"),
        ZliveChannel("auto-tnt-sports", "TNT Sports"),
        ZliveChannel("auto-ebc-sports", "EBC Sports"),
        ZliveChannel("auto-bein-sports-1", "beIN Sports 1"),
        ZliveChannel("auto-bein-sports-2", "beIN Sports 2"),
        ZliveChannel("auto-espn", "ESPN"),
        ZliveChannel("auto-espn2", "ESPN 2"),
        ZliveChannel("auto-espnu", "ESPN U"),
        ZliveChannel("auto-fox-sports-1", "Fox Sports 1"),
        ZliveChannel("auto-fox-sports-2", "Fox Sports 2"),
        ZliveChannel("auto-nfl-network", "NFL Network"),
        ZliveChannel("auto-nba-tv", "NBA TV"),
        ZliveChannel("auto-nhl-network", "NHL Network"),
        ZliveChannel("auto-eurosport-1", "Eurosport 1"),
        ZliveChannel("auto-eurosport-2", "Eurosport 2"),
        ZliveChannel("auto-golf-channel", "Golf Channel"),
        ZliveChannel("auto-tennis-channel", "Tennis Channel"),
        ZliveChannel("auto-cricket-world", "Cricket World"),
        ZliveChannel("auto-willow-cricket", "Willow Cricket"),
    )

    override val mainPage = mainPageOf(
        "live" to "Live Now",
        "all" to "All Matches",
        "zlive" to "ZLive Channels",
        "football" to "Football",
        "basketball" to "Basketball",
        "american-football" to "NFL",
        "hockey" to "Hockey",
        "baseball" to "Baseball",
        "motor-sports" to "Motor Sports",
        "fight" to "UFC / MMA",
        "tennis" to "Tennis",
        "rugby" to "Rugby",
        "golf" to "Golf",
        "cricket" to "Cricket",
    )

    private fun extractJsonString(json: String, key: String): String {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\""
        val match = Regex(pattern).find(json)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun extractJsonLong(json: String, key: String): Long {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)"
        val match = Regex(pattern).find(json)
        return match?.groupValues?.get(1)?.toLong() ?: 0L
    }

    private fun extractJsonBoolean(json: String, key: String): Boolean {
        val pattern = "\"$key\"\\s*:\\s*(true|false)"
        val match = Regex(pattern).find(json)
        return match?.groupValues?.get(1) == "true"
    }

    private fun extractSources(json: String): List<StreamSource> {
        val results = mutableListOf<StreamSource>()
        val sourcePattern = "\"sources\"\\s*:\\s*\\[([^\\]]+)\\]".toRegex()
        val sourcesMatch = sourcePattern.find(json)
        if (sourcesMatch != null) {
            val sourcesStr = sourcesMatch.groupValues[1]
            val itemPattern = "\\{([^}]+)\\}".toRegex()
            itemPattern.findAll(sourcesStr).forEach { itemMatch ->
                val item = itemMatch.groupValues[1]
                val src = extractJsonString(item, "source")
                val id = extractJsonString(item, "id")
                if (src.isNotEmpty() && id.isNotEmpty()) {
                    results.add(StreamSource(src, id))
                }
            }
        }
        return results
    }

    private suspend fun fetchJsonArray(url: String): List<String> {
        return try {
            val response = app.get(url, headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "Accept" to "application/json"
            ))
            val text = response.text
            val start = text.indexOf('[')
            val end = text.lastIndexOf(']')
            if (start >= 0 && end > start) {
                text.substring(start + 1, end).split("},{").map { s ->
                    var item = s.trim()
                    if (item.startsWith("[")) item = item.substring(1)
                    if (item.endsWith("]")) item = item.dropLast(1)
                    if (!item.startsWith("{")) item = "{$item"
                    if (!item.endsWith("}")) item = "$item}"
                    item
                }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun formatMatchDate(ms: Long): String {
        if (ms <= 0) return ""
        val now = System.currentTimeMillis()
        val diff = ms - now
        if (diff < 0) {
            val absDiff = kotlin.math.abs(diff)
            val mins = absDiff / 60000
            if (mins < 60) return "Ended ${mins}m ago"
            val hours = mins / 60
            return "Ended ${hours}h ago"
        }
        val hours = diff / 3600000
        val mins = (diff % 3600000) / 60000
        return "In ${hours}h ${mins}m"
    }

    private fun posterUrl(path: String): String {
        return if (path.startsWith("/api/images/")) "https://streamed.pk$path"
        else if (path.startsWith("http")) path else ""
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val home = mutableListOf<SearchResponse>()

        when (request.name) {
            "live" -> {
                val liveMatches = fetchJsonArray("https://streamed.pk/api/matches/live")
                liveMatches.forEach { jsonStr ->
                    val title = extractJsonString(jsonStr, "title")
                    if (title.isEmpty()) return@forEach
                    val posterPath = extractJsonString(jsonStr, "poster")
                    val sources = extractSources(jsonStr)
                    val src = sources.firstOrNull()
                    val streamSource = src?.source ?: "admin"
                    val streamId = src?.id ?: extractJsonString(jsonStr, "id")
                    home.add(newMovieSearchResponse(title, "streamed://${streamSource}/${streamId}", TvType.Live) {
                        this.posterUrl = posterUrl(posterPath)
                    })
                }
                zliveChannels.forEach { ch ->
                    home.add(newMovieSearchResponse(ch.name, "zlive://${ch.slug}", TvType.Live) {
                        this.posterUrl = ""
                    })
                }
            }
            "zlive" -> {
                zliveChannels.forEach { ch ->
                    home.add(newMovieSearchResponse(ch.name, "zlive://${ch.slug}", TvType.Live) {
                        this.posterUrl = ""
                    })
                }
            }
            "all" -> {
                val allMatches = fetchJsonArray("https://streamed.pk/api/matches/all")
                allMatches.forEach { jsonStr ->
                    val title = extractJsonString(jsonStr, "title")
                    if (title.isEmpty()) return@forEach
                    val posterPath = extractJsonString(jsonStr, "poster")
                    val date = extractJsonLong(jsonStr, "date")
                    val sources = extractSources(jsonStr)
                    val src = sources.firstOrNull()
                    val streamSource = src?.source ?: "admin"
                    val streamId = src?.id ?: extractJsonString(jsonStr, "id")
                    val timeStr = if (date > 0) " | ${formatMatchDate(date)}" else ""
                    home.add(newMovieSearchResponse("$title$timeStr", "streamed://${streamSource}/${streamId}", TvType.Live) {
                        this.posterUrl = posterUrl(posterPath)
                    })
                }
            }
            else -> {
                val sportMatches = fetchJsonArray("https://streamed.pk/api/matches/${request.name}")
                sportMatches.forEach { jsonStr ->
                    val title = extractJsonString(jsonStr, "title")
                    if (title.isEmpty()) return@forEach
                    val posterPath = extractJsonString(jsonStr, "poster")
                    val date = extractJsonLong(jsonStr, "date")
                    val sources = extractSources(jsonStr)
                    val src = sources.firstOrNull()
                    val streamSource = src?.source ?: "admin"
                    val streamId = src?.id ?: extractJsonString(jsonStr, "id")
                    val timeStr = if (date > 0) " | ${formatMatchDate(date)}" else ""
                    home.add(newMovieSearchResponse("$title$timeStr", "streamed://${streamSource}/${streamId}", TvType.Live) {
                        this.posterUrl = posterUrl(posterPath)
                    })
                }
            }
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = request.name in listOf("live", "zlive")
            ),
            hasNext = home.isNotEmpty()
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val home = mutableListOf<SearchResponse>()
        val q = query.lowercase()

        val allMatches = fetchJsonArray("https://streamed.pk/api/matches/all")
        allMatches.forEach { jsonStr ->
            val title = extractJsonString(jsonStr, "title")
            if (title.lowercase().contains(q)) {
                val posterPath = extractJsonString(jsonStr, "poster")
                val sources = extractSources(jsonStr)
                val src = sources.firstOrNull()
                val streamSource = src?.source ?: "admin"
                val streamId = src?.id ?: extractJsonString(jsonStr, "id")
                home.add(newMovieSearchResponse(title, "streamed://${streamSource}/${streamId}", TvType.Live) {
                    this.posterUrl = posterUrl(posterPath)
                })
            }
        }

        zliveChannels.forEach { ch ->
            if (ch.name.lowercase().contains(q) || ch.slug.contains(q)) {
                home.add(newMovieSearchResponse(ch.name, "zlive://${ch.slug}", TvType.Live) {
                    this.posterUrl = ""
                })
            }
        }

        return home.take(50)
    }

    override suspend fun load(url: String): LoadResponse {
        if (url.startsWith("zlive://")) {
            val slug = url.removePrefix("zlive://")
            val channel = zliveChannels.firstOrNull { it.slug == slug }
            val title = channel?.name ?: slug.replace("-", " ").replace("amp", "&")
            return newMovieLoadResponse(title, url, TvType.Live, url) {
                this.posterUrl = ""
            }
        } else if (url.startsWith("streamed://")) {
            val parts = url.removePrefix("streamed://").split("/")
            val streamSource = parts.getOrElse(0) { "admin" }
            return newMovieLoadResponse("Live Stream ($streamSource)", url, TvType.Live, url) {
                this.posterUrl = ""
            }
        }
        return newMovieLoadResponse("Stream", url, TvType.Live, url)
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        if (data.startsWith("zlive://")) {
            val slug = data.removePrefix("zlive://")
            val embedUrl = "https://zlive.st/embed/$slug"
            newExtractorLink(name, "ZLive", embedUrl, INFER_TYPE) { callback(this) }
            return true
        } else if (data.startsWith("streamed://")) {
            val parts = data.removePrefix("streamed://").split("/")
            val streamSource = parts.getOrElse(0) { "admin" }
            val streamId = parts.getOrElse(1) { "" }

            try {
                val streamItems = fetchJsonArray("https://streamed.pk/api/stream/${streamSource}/${streamId}")
                var hasStream = false
                streamItems.forEach { jsonStr ->
                    val embedUrl = extractJsonString(jsonStr, "embedUrl")
                    if (embedUrl.isEmpty()) return@forEach
                    hasStream = true
                    val language = extractJsonString(jsonStr, "language")
                    val hd = extractJsonBoolean(jsonStr, "hd")
                    newExtractorLink(
                        name,
                        "Streamed ${language} ${if (hd) "HD" else "SD"}",
                        embedUrl,
                        INFER_TYPE
                    ) { callback(this) }
                }

                if (!hasStream) {
                    newExtractorLink(
                        name,
                        "Streamed (Default)",
                        "https://embed.st/embed/$streamSource/$streamId/1",
                        INFER_TYPE
                    ) { callback(this) }
                }
            } catch (e: Exception) {
                newExtractorLink(
                    name,
                    "Streamed (Fallback)",
                    "https://embed.st/embed/$streamSource/$streamId/1",
                    INFER_TYPE
                ) { callback(this) }
            }
            return true
        }
        return false
    }
}
