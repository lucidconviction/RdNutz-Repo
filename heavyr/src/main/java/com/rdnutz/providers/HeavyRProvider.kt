package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class HeavyRProvider : MainAPI() {
    override var mainUrl = "https://www.heavy-r.com"
    override var name = "Heavy-R"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python scraper uses RSS feed: https://www.heavy-r.com/modules/rss.php?type=recent
        // Then resolves each video page for direct MP4 from <source> tags
        val rssUrl = "${mainUrl}/modules/rss.php?type=recent"
        val rssDoc = app.get(rssUrl).document
        
        // Parse RSS for video links: <link>https://www.heavy-r.com/video/{id}/{slug}/</link>
        val videoLinks = rssDoc.select("item > link")
            .map { it.text() }
            .filter { it.contains("/video/") }
            .distinct()
            .take(50)

        val home = videoLinks.mapNotNull { videoUrl ->
            val videoDoc = app.get(videoUrl).document
            // Python: <source[^>]+src=["']([^"']+\.mp4[^"']*)["']
            val videoUrl2 = videoDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: videoDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (videoUrl2.isEmpty()) return@mapNotNull null

            val title = videoDoc.selectFirst("title")?.text()
                ?: "Heavy-R Video"
            val posterUrl = fixUrlNull(videoDoc.selectFirst("meta[property='og:image']")?.attr("content"))

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
            hasNext = home.isNotEmpty()
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/search/${query.replace(" ", "+")}").document
        val videoLinks = document.select("a[href*=/video/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.contains("/video/")) href else null
            }
            .distinct()
            .take(30)

        return videoLinks.mapNotNull { videoUrl ->
            val videoDoc = app.get(videoUrl).document
            val videoUrl2 = videoDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: videoDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (videoUrl2.isEmpty()) return@mapNotNull null

            val title = videoDoc.selectFirst("title")?.text()
                ?: "Heavy-R Video"
            val posterUrl = fixUrlNull(videoDoc.selectFirst("meta[property='og:image']")?.attr("content"))

            newMovieSearchResponse(title, videoUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?: "Heavy-R Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        
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