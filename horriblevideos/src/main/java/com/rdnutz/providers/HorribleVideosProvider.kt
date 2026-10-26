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
        "${mainUrl}/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python: paginated pages (page1.html, page2.html) -> video links -> embed page -> MP4
        val pageUrl = if (page == 1) "$mainUrl/" else "$mainUrl/page${page}.html"
        val document = app.get(pageUrl).document

        // Python: <a[^>]*href="(https?://horriblevideos\.com/video/[^"]+)"[^>]*title="([^"]*)"
        val videoLinks = document.select("a[href*=/video/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                val title = el.attr("title")
                if (href != null && href.contains("/video/")) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        val home = videoLinks.mapNotNull { (videoUrl, title) ->
            // Python: embed URL is https://horriblevideos.com/embed/{id} from video URL ...-{id}.html
            val vidId = Regex("-(\\d+)\\.html").find(videoUrl)?.groupValues?.get(1)
            if (vidId == null) return@mapNotNull null
            
            val embedUrl = "$mainUrl/embed/$vidId"
            val embedDoc = app.get(embedUrl).document
            
            // Python: <source[^>]+src="(https?://[^"]+\.mp4[^"]*)"
            val mp4Url = embedDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: embedDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (mp4Url.isEmpty()) return@mapNotNull null

            val cleanTitle = if (title.isNotBlank()) title else "HorribleVideos Video $vidId"
            val posterUrl = fixUrlNull(embedDoc.selectFirst("meta[property='og:image']")?.attr("content"))

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
        // Search likely uses a different URL pattern
        val document = app.get("${mainUrl}/search?q=${query.replace(" ", "+")}").document
        val videoLinks = document.select("a[href*=/video/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                val title = el.attr("title")
                if (href != null && href.contains("/video/")) Pair(href, title) else null
            }
            .distinctBy { it.first }
            .take(30)

        return videoLinks.mapNotNull { (videoUrl, title) ->
            val vidId = Regex("-(\\d+)\\.html").find(videoUrl)?.groupValues?.get(1)
            if (vidId == null) return@mapNotNull null
            
            val embedUrl = "$mainUrl/embed/$vidId"
            val embedDoc = app.get(embedUrl).document
            val mp4Url = embedDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: embedDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (mp4Url.isEmpty()) return@mapNotNull null

            val cleanTitle = if (title.isNotBlank()) title else "HorribleVideos Video $vidId"
            val posterUrl = fixUrlNull(embedDoc.selectFirst("meta[property='og:image']")?.attr("content"))

            newMovieSearchResponse(cleanTitle, videoUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?: "HorribleVideos Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        // Python: embed URL from video URL
        val vidId = Regex("-(\\d+)\\.html").find(data)?.groupValues?.get(1)
        if (vidId == null) return false
        
        val embedUrl = "$mainUrl/embed/$vidId"
        val document = app.get(embedUrl).document
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