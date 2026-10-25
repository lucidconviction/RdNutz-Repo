package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class HeavyfetishProvider : MainAPI() {
    override var mainUrl = "https://www.heavy-r.com"
    override var name = "Heavy-R"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/modules/rss.php?type=recent" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python: Uses RSS feed: https://www.heavy-r.com/modules/rss.php?type=recent
        // Extracts <link> tags with video URLs, then resolves each for <source src="...mp4...">
        val rssUrl = "https://www.heavy-r.com/modules/rss.php?type=recent"
        val rssDoc = app.get(rssUrl).document
        
        val videoLinks = rssDoc.select("link")
            .map { it.text().trim() }
            .filter { it.contains("/video/") }
            .distinct()
            .take(30)

        val home = videoLinks.mapNotNull { videoUrl ->
            val videoDoc = app.get(videoUrl).document
            val sourceUrl = videoDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: videoDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (sourceUrl.isEmpty()) return@mapNotNull null
            
            val title = videoDoc.selectFirst("title")?.text()
                ?.trim()
                ?: "Heavy-R Video"
            
            val posterUrl = fixUrlNull(videoDoc.selectFirst("meta[property='og:image']")?.attr("content"))
                ?: ""
            
            newMovieSearchResponse(title, videoUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        // No search implemented in Python scraper
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?.trim()
            ?: "Heavy-R Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
            ?: ""
        
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val videoUrl = document.selectFirst("video source, source[src]")?.attr("src")
            ?: document.selectFirst("video[src]")?.attr("src")
            ?: ""
        
        if (videoUrl.isNotEmpty()) {
            newExtractorLink(
                name,
                name,
                fixUrl(videoUrl),
                INFER_TYPE
            ) { callback(this) }
            return true
        }
        return false
    }
}