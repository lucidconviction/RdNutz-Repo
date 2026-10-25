package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class CrazyshitProvider : MainAPI() {
    override var mainUrl = "https://crazyshit.com"
    override var name = "Crazyshit"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python scraper uses: https://crazyshit.com/videos/ and https://crazyshit.com/videos/{i}/
        val pageUrl = if (page == 1) "$mainUrl/videos/" else "$mainUrl/videos/$page/"
        val document = app.get(pageUrl).document
        
        // Python: regex href="(https?://crazyshit\.com/cnt/medias/\d+[^\"]*)"
        val mediaLinks = document.select("a[href*=/cnt/medias/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.contains("/cnt/medias/")) href else null
            }
            .distinct()
            .take(30)

        // Resolve each media link to get the actual video - Python does this in parallel
        val home = mediaLinks.mapNotNull { mediaUrl ->
            val mediaDoc = app.get(mediaUrl).document
            // Python: <source src="...mp4...">
            val videoUrl = mediaDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: mediaDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (videoUrl.isEmpty()) return@mapNotNull null
            
            // Python: title from og:title or <title>
            val title = mediaDoc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: mediaDoc.selectFirst("title")?.text()
                ?: "Crazyshit Video"
            
            val posterUrl = fixUrlNull(mediaDoc.selectFirst("meta[property='og:image']")?.attr("content"))
            
            newMovieSearchResponse(title, mediaUrl, TvType.NSFW) {
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
        val mediaLinks = document.select("a[href*=/cnt/medias/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.contains("/cnt/medias/")) href else null
            }
            .distinct()
            .take(30)

        return mediaLinks.mapNotNull { mediaUrl ->
            val mediaDoc = app.get(mediaUrl).document
            val videoUrl = mediaDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: mediaDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (videoUrl.isEmpty()) return@mapNotNull null
            
            val title = mediaDoc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: mediaDoc.selectFirst("title")?.text()
                ?: "Crazyshit Video"
            
            val posterUrl = fixUrlNull(mediaDoc.selectFirst("meta[property='og:image']")?.attr("content"))
            
            newMovieSearchResponse(title, mediaUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        // Python: title from og:title or <title>
        val title = document.selectFirst("meta[property='og:title']")?.attr("content")
            ?: document.selectFirst("title")?.text()
            ?: "Crazyshit Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        
        // Python: extract description from meta og:description
        val description = document.selectFirst("meta[property='og:description']")?.attr("content") ?: ""
        
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
            this.plot = description
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