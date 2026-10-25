package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class DaftpornProvider : MainAPI() {
    override var mainUrl = "https://daftporn.com"
    override var name = "DaftPorn"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home = document.select("a[href*='/video/']").mapNotNull { el ->
            val title = el.selectFirst(".video-title, .title")?.text()?.trim() ?: return@mapNotNull null
            val href = fixUrlNull(el.attr("href")) ?: return@mapNotNull null
            val posterUrl = fixUrlNull(el.selectFirst("img")?.attr("data-src") ?: el.selectFirst("img")?.attr("src"))
            newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
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
        return document.select("a[href*='/video/']").mapNotNull { el ->
            val title = el.selectFirst(".video-title, .title")?.text()?.trim() ?: return@mapNotNull null
            val href = fixUrlNull(el.attr("href")) ?: return@mapNotNull null
            val posterUrl = fixUrlNull(el.selectFirst("img")?.attr("data-src") ?: el.selectFirst("img")?.attr("src"))
            newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1, .video-title")?.text()?.trim()?.toString() ?: ""
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        val description = document.selectFirst("meta[property='og:description']")?.attr("content") ?: ""
        val duration = document.selectFirst(".duration")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("a.tag, a.category").map { it.text().trim() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
            this.plot = description
            this.duration = duration
            this.tags = tags
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val videoUrl = document.selectFirst("video source[src$='.m3u8']")?.attr("src")
            ?: document.selectFirst("video source")?.attr("src")
            ?: document.selectFirst("video[src]")?.attr("src")
            ?: document.selectFirst("iframe[src*='player']")?.attr("src")
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
