package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class InhumanityProvider : MainAPI() {
    override var mainUrl = "https://inhumanity.com"
    override var name = "Inhumanity"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) mainUrl else "$mainUrl/page/$page/"
        val document = app.get(pageUrl).document

        val videoLinks = document.select("a[href*=/plug/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                val title = el.attr("title")
                if (href != null && href.contains("/plug/")) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        val home = videoLinks.mapNotNull { (videoUrl, title) ->
            val cleanTitle = if (title.isNotBlank()) title else "Inhumanity Video"
            // Try to fetch the plug page to get a thumbnail
            val plugDoc = app.get(videoUrl).document
            val posterUrl = fixUrlNull(plugDoc.selectFirst("meta[property='og:image']")?.attr("content"))
                ?: fixUrlNull(plugDoc.selectFirst("video[poster]")?.attr("poster"))
                ?: ""
            newMovieSearchResponse(cleanTitle, videoUrl, TvType.NSFW) {
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
        val document = app.get("$mainUrl/search?q=${query.replace(" ", "+")}").document
        val videoLinks = document.select("a[href*=/plug/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                val title = el.attr("title")
                if (href != null && href.contains("/plug/")) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        return videoLinks.mapNotNull { (videoUrl, title) ->
            val cleanTitle = if (title.isNotBlank()) title else "Inhumanity Video"
            val plugDoc = app.get(videoUrl).document
            val posterUrl = fixUrlNull(plugDoc.selectFirst("meta[property='og:image']")?.attr("content"))
                ?: fixUrlNull(plugDoc.selectFirst("video[poster]")?.attr("poster"))
                ?: ""
            newMovieSearchResponse(cleanTitle, videoUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?: "Inhumanity Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
            ?: fixUrlNull(document.selectFirst("video[poster]")?.attr("poster"))
            ?: ""

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document

        // Try to find iframe/video embed - order matters!
        val videoUrl = document.selectFirst("iframe[src]")?.attr("src")
            ?: document.selectFirst("video source, source[src]")?.attr("src")
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