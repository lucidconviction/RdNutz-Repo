package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class SexyAndFunnyProvider : MainAPI() {
    override var mainUrl = "https://sexyandfunny.com"
    override var name = "SexyAndFunny"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python: WordPress site with video posts containing direct MP4s
        // Video pages: https://sexyandfunny.com/video/{slug}/
        // MP4s: https://sexyandfunny.com/wp-content/uploads/{year}/{month}/{slug}.mp4
        // Pagination: /video/page/{n}/
        val pageUrl = if (page == 1) "$mainUrl/videos/" else "$mainUrl/video/page/$page/"
        val document = app.get(pageUrl).document

        // Find all video links
        val videoLinks = document.select("a[href*=/video/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.contains("/video/") && !href.contains("/page/") && !href.contains("/feed/")) href else null
            }
            .distinct()
            .take(50)

        // Resolve each video page for MP4
        val home = videoLinks.mapNotNull { videoUrl ->
            val videoDoc = app.get(videoUrl).document
            
            // Python: <video> -> <source src="..."> or direct MP4 in wp-content/uploads
            val mp4Url = videoDoc.selectFirst("video source[src]")?.attr("src")
                ?: videoDoc.select("video source[src*=wp-content/uploads], source[src*=wp-content/uploads]")
                    .map { fixUrlNull(it.attr("src")) }
                    .filterNotNull()
                    .firstOrNull { it -> !it.contains("banner") && !it.contains("sfam/banners") }
                ?: ""
            if (mp4Url.isEmpty()) return@mapNotNull null

            val title = videoDoc.selectFirst("title")?.text()
                ?.replace(" - Sexy and Funny", "")
                ?.replace(" - Hot Models", "")
                ?.replace(" - Free Videos", "")
                ?.trim()
                ?: "SexyAndFunny Video"
            
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
        val videoLinks = document.select("a[href*=/video/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.contains("/video/") && !href.contains("/page/") && !href.contains("/feed/")) href else null
            }
            .distinct()
            .take(30)

        return videoLinks.mapNotNull { videoUrl ->
            val videoDoc = app.get(videoUrl).document
            val mp4Url = videoDoc.selectFirst("video source[src]")?.attr("src")
                ?: videoDoc.select("video source[src*=wp-content/uploads], source[src*=wp-content/uploads]")
                    .map { fixUrlNull(it.attr("src")) }
                    .filterNotNull()
                    .firstOrNull { it -> !it.contains("banner") && !it.contains("sfam/banners") }
                ?: ""
            if (mp4Url.isEmpty()) return@mapNotNull null

            val title = videoDoc.selectFirst("title")?.text()
                ?.replace(" - Sexy and Funny", "")
                ?.replace(" - Hot Models", "")
                ?.replace(" - Free Videos", "")
                ?.trim()
                ?: "SexyAndFunny Video"
            
            val posterUrl = fixUrlNull(videoDoc.selectFirst("meta[property='og:image']")?.attr("content"))

            newMovieSearchResponse(title, videoUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?.replace(" - Sexy and Funny", "")
            ?.replace(" - Hot Models", "")
            ?.replace(" - Free Videos", "")
            ?.trim()
            ?: "SexyAndFunny Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val videoUrl = document.selectFirst("video source[src]")?.attr("src")
            ?: document.select("video source[src*=wp-content/uploads], source[src*=wp-content/uploads]")
                .map { fixUrlNull(it.attr("src")) }
                .filterNotNull()
                .firstOrNull { it -> !it.contains("banner") && !it.contains("sfam/banners") }
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