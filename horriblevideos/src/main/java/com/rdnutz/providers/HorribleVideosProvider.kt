package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class HorribleVideosProvider : MainAPI() {
    override var mainUrl = "https://horriblevideos.com"
    override var name = "HorribleVideos"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl/videos/" else "$mainUrl/page$page/"
        val document = app.get(pageUrl).document

        // Extract video links: /video/slug-ID.html
        val videoLinks = document.select("a[href^='/video/']")
            .mapNotNull { el ->
                val href = el.attr("href")
                val title = el.attr("title")
                if (href.startsWith("/video/")) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        val home = videoLinks.mapNotNull { (videoPath, linkTitle) ->
            val fullUrl = fixUrl(videoPath) ?: return@mapNotNull null
            val videoDoc = app.get(fullUrl).document
            
            // Extract video URL from video tag
            val videoUrl = videoDoc.selectFirst("video source[src]")?.attr("src")
                ?: videoDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (videoUrl.isEmpty()) return@mapNotNull null

            // Extract title from page title or og:title
            val title = videoDoc.selectFirst("title")?.text()
                ?.replace(" - horriblevideos.com", "")
                ?.trim()
                ?: linkTitle
                ?: "HorribleVideos Video"
            
            // Extract poster from video poster attribute or og:image
            val posterUrl = videoDoc.selectFirst("video[poster]")?.attr("poster")
                ?: videoDoc.selectFirst("meta[property='og:image']")?.attr("content")
                ?: ""

            newMovieSearchResponse(title, fullUrl, TvType.NSFW) {
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
        val document = app.get("${mainUrl}/search?q=${query.replace(" ", "+")}").document
        val videoLinks = document.select("a[href^='/video/']")
            .mapNotNull { el ->
                val href = el.attr("href")
                val title = el.attr("title")
                if (href.startsWith("/video/")) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        return videoLinks.mapNotNull { (videoPath, linkTitle) ->
            val fullUrl = fixUrl(videoPath) ?: return@mapNotNull null
            val videoDoc = app.get(fullUrl).document
            val videoUrl = videoDoc.selectFirst("video source[src]")?.attr("src")
                ?: videoDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (videoUrl.isEmpty()) return@mapNotNull null

            val title = videoDoc.selectFirst("title")?.text()
                ?.replace(" - horriblevideos.com", "")
                ?.trim()
                ?: linkTitle
                ?: "HorribleVideos Video"
            
            val posterUrl = videoDoc.selectFirst("video[poster]")?.attr("poster")
                ?: videoDoc.selectFirst("meta[property='og:image']")?.attr("content")
                ?: ""

            newMovieSearchResponse(title, fullUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?.replace(" - horriblevideos.com", "")
            ?.trim()
            ?: "HorribleVideos Video"
        val posterUrl = document.selectFirst("video[poster]")?.attr("poster")
            ?: document.selectFirst("meta[property='og:image']")?.attr("content")
            ?: ""
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val videoUrl = document.selectFirst("video source[src]")?.attr("src")
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
