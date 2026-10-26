package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class XraresProvider : MainAPI() {
    override var mainUrl = "https://www.xrares.com"
    override var name = "Xrares"
    override val supportedTypes = setOf(TvType.NSFW)
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python scraper uses: https://www.xrares.com/videos/?page={page}
        val pageUrl = "$mainUrl/videos/?page=$page"
        val document = app.get(pageUrl).document
        
        // Python: href="(/video/\d+/[^\"]+)"
        val videoLinks = document.select("a[href*=/video/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.contains("/video/")) href else null
            }
            .distinct()
            .take(30)

        // Resolve each video link
        val home = videoLinks.mapNotNull { videoUrl ->
            val videoDoc = app.get(videoUrl).document
            // Python: <source src="...">
            val sourceUrl = videoDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: videoDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (sourceUrl.isEmpty()) return@mapNotNull null
            
            // Python: title from <title>, remove " - Xrares" and " - Free Amateur Porn"
            val title = videoDoc.selectFirst("title")?.text()
                ?.replace(" - Xrares", "")
                ?.replace(" - Free Amateur Porn", "")
                ?.trim()
                ?: "Xrares Video"
            
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
        val videoLinks = document.select("a[href*=/video/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.contains("/video/")) href else null
            }
            .distinct()
            .take(30)

        return videoLinks.mapNotNull { videoUrl ->
            val videoDoc = app.get(videoUrl).document
            val sourceUrl = videoDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: videoDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (sourceUrl.isEmpty()) return@mapNotNull null
            
            val title = videoDoc.selectFirst("title")?.text()
                ?.replace(" - Xrares", "")
                ?.replace(" - Free Amateur Porn", "")
                ?.trim()
                ?: "Xrares Video"
            
            val posterUrl = fixUrlNull(videoDoc.selectFirst("meta[property='og:image']")?.attr("content"))
            
            newMovieSearchResponse(title, videoUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?.replace(" - Xrares", "")
            ?.replace(" - Free Amateur Porn", "")
            ?.trim()
            ?: "Xrares Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        // Python: <source src="...">
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