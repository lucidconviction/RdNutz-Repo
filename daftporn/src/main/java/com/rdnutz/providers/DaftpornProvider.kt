package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class DaftpornProvider : MainAPI() {
    override var mainUrl = "https://www.daftporn.com"
    override var name = "DaftPorn"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl/" else "$mainUrl/page/$page/"
        val document = app.get(pageUrl).document
        
        val videoLinks = document.select("a[href*=extreme-videos]")
            .filter { it.attr("href").contains(".php") }
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                val title = el.attr("title")
                if (href != null && title != null && title.isNotBlank()) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        val home = videoLinks.mapNotNull { (videoUrl, title) ->
            val videoDoc = app.get(videoUrl).document
            val sourceUrl = videoDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: videoDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (sourceUrl.isEmpty()) return@mapNotNull null
            
            val posterUrl = fixUrlNull(videoDoc.selectFirst("meta[property='og:image']")?.attr("content"))
                ?: fixUrlNull(videoDoc.selectFirst("video")?.attr("poster"))
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
            hasNext = home.isNotEmpty()
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/search/${query.replace(" ", "-")}").document
        val videoLinks = document.select("a[href*=extreme-videos]")
            .filter { it.attr("href").contains(".php") }
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
                ?: fixUrlNull(videoDoc.selectFirst("video")?.attr("poster"))
                ?: ""
            
            newMovieSearchResponse(title, videoUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property='og:title']")?.attr("content")
            ?: document.selectFirst("title")?.text()
            ?: "DaftPorn Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
            ?: fixUrlNull(document.selectFirst("video")?.attr("poster"))
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