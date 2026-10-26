package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class ZFilmzOriginalsProvider : MainAPI() {
    override var mainUrl = "https://z-filmz-originals.com"
    override var name = "ZFilmzOriginals"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/videos" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl/videos" else "$mainUrl/videos?page=$page"
        val document = app.get(pageUrl).document

        val nextDataScript = document.selectFirst("script#__NEXT_DATA__")?.html() ?: ""
        if (nextDataScript.isEmpty()) {
            return newHomePageResponse(
                list = HomePageList(name = request.name, list = emptyList(), isHorizontalImages = true),
                hasNext = false
            )
        }

        val videos = mutableListOf<Triple<String, String, String>>() // title, trailerUrl, posterUrl
        try {
            // Parse JSON manually to avoid dependencies
            val json = nextDataScript.trim()
            
            // Extract contents.data array
            val contentsMatch = Regex("""\"contents\"\s*:\s*\{[^}]*\"data\"\s*:\s*\[(.+?)\]""").find(json)
            if (contentsMatch != null) {
                val dataStr = contentsMatch.groupValues[1]
                // Split by },{ to get individual items
                val items = dataStr.split("},\\s*\\{").map { s ->
                    if (!s.startsWith("{")) "{$s"
                    else if (!s.endsWith("}")) "$s}"
                    else s
                }
                
                items.forEach { item ->
                    val title = Regex("""\"title\"\s*:\s*\"([^\"]+)\"""").find(item)?.groupValues?.get(1) ?: ""
                    val trailerUrl = Regex("""\"trailer_url\"\s*:\s*\"([^\"]+)\"""").find(item)?.groupValues?.get(1) ?: ""
                    val thumb = Regex("""\"thumb\"\s*:\s*\"([^\"]+)\"""").find(item)?.groupValues?.get(1) ?: ""
                    
                    if (title.isNotBlank() && trailerUrl.isNotBlank()) {
                        videos.add(Triple(title, trailerUrl, thumb))
                    }
                }
            }
        } catch (e: Exception) {
            // JSON parsing failed
        }

        val home = videos.take(30).mapNotNull { (title, trailerUrl, posterUrl) ->
            newMovieSearchResponse(title, trailerUrl, TvType.Movie) {
                this.posterUrl = if (posterUrl.isNotBlank()) fixUrlNull(posterUrl) else ""
            }
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/videos?search=${query.replace(" ", "+")}").document
        val nextDataScript = document.selectFirst("script#__NEXT_DATA__")?.html() ?: ""
        if (nextDataScript.isEmpty()) return emptyList()

        val videos = mutableListOf<Triple<String, String, String>>()
        try {
            val json = nextDataScript.trim()
            val contentsMatch = Regex("""\"contents\"\s*:\s*\{[^}]*\"data\"\s*:\s*\[(.+?)\]""").find(json)
            if (contentsMatch != null) {
                val dataStr = contentsMatch.groupValues[1]
                val items = dataStr.split("},\\s*\\{").map { s ->
                    if (!s.startsWith("{")) "{$s"
                    else if (!s.endsWith("}")) "$s}"
                    else s
                }
                
                items.forEach { item ->
                    val title = Regex("""\"title\"\s*:\s*\"([^\"]+)\"""").find(item)?.groupValues?.get(1) ?: ""
                    val trailerUrl = Regex("""\"trailer_url\"\s*:\s*\"([^\"]+)\"""").find(item)?.groupValues?.get(1) ?: ""
                    val thumb = Regex("""\"thumb\"\s*:\s*\"([^\"]+)\"""").find(item)?.groupValues?.get(1) ?: ""
                    
                    if (title.isNotBlank() && trailerUrl.isNotBlank()) {
                        videos.add(Triple(title, trailerUrl, thumb))
                    }
                }
            }
        } catch (e: Exception) {
            // JSON parsing failed
        }

        return videos.take(30).mapNotNull { (title, trailerUrl, posterUrl) ->
            newMovieSearchResponse(title, trailerUrl, TvType.Movie) {
                this.posterUrl = if (posterUrl.isNotBlank()) fixUrlNull(posterUrl) else ""
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        // For trailer URLs, extract title from URL or use default
        val title = url.substringAfterLast("/").replace(".mp4", "").replace("_trailer", "")
            .split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = ""
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        // The URL is already the direct MP4/trailer URL
        newExtractorLink(
            name,
            name,
            fixUrl(data),
            INFER_TYPE
        ) { callback(this) }
        return true
    }
}
