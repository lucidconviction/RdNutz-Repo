package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class WorldstarProvider : MainAPI() {
    override var mainUrl = "https://worldstarhiphop.com"
    override var name = "Worldstar"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl/videos/" else "$mainUrl/videos/page/$page/"
        val document = app.get(pageUrl).document
        
        // Extract all MP4 URLs from the page HTML
        val mp4Pattern = "https?://hw-[a-z.-]+\\.worldstar[^\\s\"'<>]+\\.mp4[^\\s\"'<>]*".toRegex()
        val matches = mp4Pattern.findAll(document.html())
        val mp4Urls = mutableListOf<String>()
        for (match in matches) {
            val url = match.value
            if (!url.contains("_mobile") && !mp4Urls.contains(url)) {
                mp4Urls.add(url)
            }
        }
        
        val home = mp4Urls.take(30).map { url ->
            val filename = url.substringAfterLast("/")
            val title = filename.replace(".mp4", "").replace("-", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            newMovieSearchResponse(title, url, TvType.NSFW) {
                this.posterUrl = ""
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
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val filename = url.substringAfterLast("/")
        val title = filename.replace(".mp4", "").replace("-", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = ""
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        if (data.endsWith(".mp4")) {
            newExtractorLink(
                name,
                name,
                fixUrl(data),
                INFER_TYPE
            ) { callback(this) }
            return true
        }
        return false
    }
}
