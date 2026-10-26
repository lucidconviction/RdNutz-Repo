package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class SickJunkProvider : MainAPI() {
    override var mainUrl = "https://sickjunk.com"
    override var name = "SickJunk"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python: WordPress blog with direct MP4 uploads
        // Posts: https://sickjunk.com/{slug}/
        // MP4s: https://sickjunk.com/wp-content/uploads/{year}/{month}/{day}/{Title}.mp4
        // Pagination: /page/{n}/
        val pageUrl = if (page == 1) mainUrl else "$mainUrl/page/$page/"
        val document = app.get(pageUrl).document

        // Find all post links
        val postLinks = document.select("a[href*=/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.startsWith(mainUrl) && !href.contains("/page/") && !href.contains("/feed/") && href != mainUrl) href else null
            }
            .distinct()
            .take(50)

        // Resolve each post to find MP4
        val home = postLinks.mapNotNull { postUrl ->
            val postDoc = app.get(postUrl).document
            // Python: (https?://sickjunk\.com/wp-content/uploads/[^\s"']+\.[Mm][Pp]4)
            val mp4Url = postDoc.select("video source[src*=wp-content/uploads], source[src*=wp-content/uploads]")
                .map { fixUrlNull(it.attr("src")) }
                .filterNotNull()
                .firstOrNull()
            if (mp4Url == null) return@mapNotNull null

            val title = postDoc.selectFirst("title")?.text()
                ?.replace(" - SickJunk.com", "")
                ?.replace(" - Humanity At Its Worst", "")
                ?.trim()
                ?: "SickJunk Video"
            
            val posterUrl = fixUrlNull(postDoc.selectFirst("meta[property='og:image']")?.attr("content"))

            newMovieSearchResponse(title, postUrl, TvType.NSFW) {
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
        val postLinks = document.select("a[href*=/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.startsWith(mainUrl) && !href.contains("/page/") && !href.contains("/feed/") && href != mainUrl) href else null
            }
            .distinct()
            .take(30)

        return postLinks.mapNotNull { postUrl ->
            val postDoc = app.get(postUrl).document
            val mp4Url = postDoc.select("video source[src*=wp-content/uploads], source[src*=wp-content/uploads]")
                .map { fixUrlNull(it.attr("src")) }
                .filterNotNull()
                .firstOrNull()
            if (mp4Url == null) return@mapNotNull null

            val title = postDoc.selectFirst("title")?.text()
                ?.replace(" - SickJunk.com", "")
                ?.replace(" - Humanity At Its Worst", "")
                ?.trim()
                ?: "SickJunk Video"
            
            val posterUrl = fixUrlNull(postDoc.selectFirst("meta[property='og:image']")?.attr("content"))

            newMovieSearchResponse(title, postUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?.replace(" - SickJunk.com", "")
            ?.replace(" - Humanity At Its Worst", "")
            ?.trim()
            ?: "SickJunk Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val videoUrl = document.select("video source[src*=wp-content/uploads], source[src*=wp-content/uploads]")
            .map { fixUrlNull(it.attr("src")) }
            .filterNotNull()
            .firstOrNull()
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