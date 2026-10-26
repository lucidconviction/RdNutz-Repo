package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class YouPornProvider : MainAPI() {
    override var mainUrl = "https://www.you-porn.com"
    override var name = "YouPorn"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python: extracts token-protected MP4 URLs from video watch pages
        // Base: https://www.you-porn.com
        // Video links: /watch/{id}
        // MP4 pattern: ev-ph.ypncdn.com/videos/..._{id}P_{id}K_{id}_fb.mp4?token
        val pageUrl = if (page == 1) mainUrl else "$mainUrl/page/$page/"
        val document = app.get(pageUrl).document

        // Find all watch links
        val watchLinks = document.select("a[href*=/watch/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.contains("/watch/")) href else null
            }
            .distinct()
            .take(30)

        // Resolve each watch page for MP4
        val home = watchLinks.mapNotNull { videoUrl ->
            val videoDoc = app.get(videoUrl).document
            
            // Python: video_title["':\s]+["']([^"']+)["']
            // Fallback: <title>...</title>
            val title = videoDoc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: videoDoc.selectFirst("title")?.text()
                    ?.replace(" - Free Porn Videos - YouPorn", "")
                    ?.trim()
                ?: "YouPorn Video"
            
            // Python: (ev-ph\.ypncdn\.com/videos/[^"\s]+_\d+P_\d+K_\d+_fb\.mp4\?[^\s"<>]+)
            val mp4Url = videoDoc.select("script, body")
                .flatMap { Regex("""(ev-ph\.ypncdn\.com/videos/[^"\s]+_\d+P_\d+K_\d+_fb\.mp4\?[^\s"<>]+)""").findAll(it.html()) }
                .map { "https://${it.groupValues[1]}" }
                .firstOrNull()
            if (mp4Url == null) return@mapNotNull null

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
        val document = app.get("$mainUrl/search?q=${query.replace(" ", "+")}").document
        val watchLinks = document.select("a[href*=/watch/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.contains("/watch/")) href else null
            }
            .distinct()
            .take(30)

        return watchLinks.mapNotNull { videoUrl ->
            val videoDoc = app.get(videoUrl).document
            
            val title = videoDoc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: videoDoc.selectFirst("title")?.text()
                    ?.replace(" - Free Porn Videos - YouPorn", "")
                    ?.trim()
                ?: "YouPorn Video"
            
            val mp4Url = videoDoc.select("script, body")
                .flatMap { Regex("""(ev-ph\.ypncdn\.com/videos/[^"\s]+_\d+P_\d+K_\d+_fb\.mp4\?[^\s"<>]+)""").findAll(it.html()) }
                .map { "https://${it.groupValues[1]}" }
                .firstOrNull()
            if (mp4Url == null) return@mapNotNull null

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
                ?.replace(" - Free Porn Videos - YouPorn", "")
                ?.trim()
            ?: "YouPorn Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        
        val mp4Url = document.select("script, body")
            .flatMap { Regex("""(ev-ph\.ypncdn\.com/videos/[^"\s]+_\d+P_\d+K_\d+_fb\.mp4\?[^\s"<>]+)""").findAll(it.html()) }
            .map { "https://${it.groupValues[1]}" }
            .firstOrNull()
            ?: ""
        
        if (mp4Url.isNotEmpty()) {
            newExtractorLink(
                name,
                name,
                fixUrl(mp4Url),
                INFER_TYPE
            ) { callback(this) }
            return true
        }
        return false
    }
}