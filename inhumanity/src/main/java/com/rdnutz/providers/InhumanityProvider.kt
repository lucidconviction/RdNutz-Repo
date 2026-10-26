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

        // Extract plug links - pattern is /plug/ID/
        val videoLinks = document.select("a[href^='/plug/']")
            .mapNotNull { el ->
                val href = el.attr("href")
                val title = el.attr("title")
                if (href.startsWith("/plug/")) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        val home = videoLinks.mapNotNull { (videoPath, linkTitle) ->
            val fullUrl = fixUrl(videoPath) ?: return@mapNotNull null
            val plugDoc = app.get(fullUrl).document
            
            // Extract title from page
            val title = plugDoc.selectFirst("title")?.text()
                ?.replace(" - Inhumanity", "")
                ?.trim()
                ?: linkTitle
                ?: "Inhumanity Video"
            
            // Try to find video/embed
            val videoUrl = plugDoc.selectFirst("video source[src]")?.attr("src")
                ?: plugDoc.selectFirst("video[src]")?.attr("src")
                ?: plugDoc.selectFirst("iframe[src]")?.attr("src")
                ?: ""
            
            // Extract poster from og:image
            val posterUrl = plugDoc.selectFirst("meta[property='og:image']")?.attr("content")
                ?: plugDoc.selectFirst("video[poster]")?.attr("poster")
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
        val document = app.get("$mainUrl/search?q=${query.replace(" ", "+")}").document
        val videoLinks = document.select("a[href^='/plug/']")
            .mapNotNull { el ->
                val href = el.attr("href")
                val title = el.attr("title")
                if (href.startsWith("/plug/")) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        return videoLinks.mapNotNull { (videoPath, linkTitle) ->
            val fullUrl = fixUrl(videoPath) ?: return@mapNotNull null
            val plugDoc = app.get(fullUrl).document
            val title = plugDoc.selectFirst("title")?.text()
                ?.replace(" - Inhumanity", "")
                ?.trim()
                ?: linkTitle
                ?: "Inhumanity Video"
            val posterUrl = plugDoc.selectFirst("meta[property='og:image']")?.attr("content")
                ?: ""
            newMovieSearchResponse(title, fullUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?.replace(" - Inhumanity", "")
            ?.trim()
            ?: "Inhumanity Video"
        val posterUrl = document.selectFirst("meta[property='og:image']")?.attr("content")
            ?: ""
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        
        // Try multiple video sources
        val videoUrl = document.selectFirst("video source[src]")?.attr("src")
            ?: document.selectFirst("video[src]")?.attr("src")
            ?: document.selectFirst("iframe[src]")?.attr("src")
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