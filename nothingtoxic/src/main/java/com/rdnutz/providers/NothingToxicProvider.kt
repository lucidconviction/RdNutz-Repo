package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class NothingToxicProvider : MainAPI() {
    override var mainUrl = "https://nothingtoxic.com"
    override var name = "NothingToxic"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python: pagination is /page/{n}/ for page > 1
        // Links are: href="(https?://nothingtoxic\.com/out/\d+/)" with title="([^"]+)"
        val pageUrl = if (page == 1) mainUrl else "$mainUrl/page/$page/"
        val document = app.get(pageUrl).document

        val videoLinks = document.select("a[href*=/out/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                val title = el.attr("title")
                if (href != null && href.contains("/out/")) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        val home = videoLinks.mapNotNull { (videoUrl, title) ->
            val cleanTitle = if (title.isNotBlank()) title else "NothingToxic Video"
            newMovieSearchResponse(cleanTitle, videoUrl, TvType.NSFW) {
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
        val document = app.get("$mainUrl/search?q=${query.replace(" ", "+")}").document
        val videoLinks = document.select("a[href*=/out/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                val title = el.attr("title")
                if (href != null && href.contains("/out/")) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        return videoLinks.mapNotNull { (videoUrl, title) ->
            val cleanTitle = if (title.isNotBlank()) title else "NothingToxic Video"
            newMovieSearchResponse(cleanTitle, videoUrl, TvType.NSFW) {
                this.posterUrl = ""
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?: "NothingToxic Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        // The /out/ page likely redirects or contains the actual video embed
        val document = app.get(data).document
        
        // Try to find iframe/video embed
        val iframeUrl = document.selectFirst("iframe[src]")?.attr("src")
            ?: document.selectFirst("video source, source[src]")?.attr("src")
            ?: document.selectFirst("video[src]")?.attr("src")
            ?: ""
        
        if (iframeUrl.isNotEmpty()) {
            newExtractorLink(
                name,
                name,
                fixUrl(iframeUrl),
                INFER_TYPE
            ) { callback(this) }
            return true
        }
        return false
    }
}