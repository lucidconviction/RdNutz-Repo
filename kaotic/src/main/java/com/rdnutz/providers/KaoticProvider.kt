package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class KaoticProvider : MainAPI() {
    override var mainUrl = "https://kaotic.com"
    override var name = "Kaotic"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/recent/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl/recent/" else "$mainUrl/recent/page/$page/"
        val document = app.get(pageUrl).document
        
        val postLinks = document.select("a[href*=/video/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.contains("/video/")) href else null
            }
            .distinct()
            .take(30)

        val home = postLinks.mapNotNull { postUrl ->
            val postDoc = app.get(postUrl).document
            val videoUrl = postDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: postDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (videoUrl.isEmpty()) return@mapNotNull null
            
            val title = postDoc.selectFirst("title")?.text()
                ?: "Kaotic Video"
            
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
        val document = app.get("${mainUrl}/search/${query.replace(" ", "-")}").document
        val postLinks = document.select("a[href*=/video/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.contains("/video/")) href else null
            }
            .distinct()
            .take(30)

        return postLinks.mapNotNull { postUrl ->
            val postDoc = app.get(postUrl).document
            val videoUrl = postDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: postDoc.selectFirst("video[src]")?.attr("src")
                ?: ""
            if (videoUrl.isEmpty()) return@mapNotNull null
            
            val title = postDoc.selectFirst("title")?.text()
                ?: "Kaotic Video"
            
            val posterUrl = fixUrlNull(postDoc.selectFirst("meta[property='og:image']")?.attr("content"))
            
            newMovieSearchResponse(title, postUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?: "Kaotic Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        
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