package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class EfuktProvider : MainAPI() {
    override var mainUrl = "https://efukt.com"
    override var name = "eFukt"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python scraper: page 1 = https://efukt.com/, page N = https://efukt.com/videos/page/N/
        val pageUrl = if (page == 1) "$mainUrl/" else "$mainUrl/videos/page/$page/"
        val document = app.get(pageUrl).document
        
        // Python: <a href="https?://efukt\.com/\d+_[^\"]*" title="([^\"]*)"
        // Filter URLs that contain underscore (indicating the \d+_ pattern)
        val videoLinks = document.select("a[href^=https://efukt.com/]")
            .filter { it.attr("href").contains("_") }
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                val title = el.attr("title")
                if (href != null && title != null && title.isNotBlank()) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        // Resolve each video link
        val home = videoLinks.mapNotNull { (videoUrl, title) ->
            val videoDoc = app.get(videoUrl).document
            // Python: <source src="...mp4...">
            val sourceUrl = videoDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: videoDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (sourceUrl.isEmpty()) return@mapNotNull null
            
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
        val document = app.get("${mainUrl}/search/${query.replace(" ", "-")}").document
        val videoLinks = document.select("a[href^=https://efukt.com/]")
            .filter { it.attr("href").contains("_") }
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                val title = el.attr("title")
                if (href != null && title != null && title.isNotBlank()) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        return videoLinks.mapNotNull { (videoUrl, title) ->
            val videoDoc = app.get(videoUrl).document
            val sourceUrl = videoDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: videoDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (sourceUrl.isEmpty()) return@mapNotNull null
            
            val posterUrl = fixUrlNull(videoDoc.selectFirst("meta[property='og:image']")?.attr("content"))
            
            newMovieSearchResponse(title, videoUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property='og:title']")?.attr("content")
            ?: document.selectFirst("title")?.text()
            ?: "eFukt Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
            ?: ""
        
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        // Python: <source src="...mp4...">
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