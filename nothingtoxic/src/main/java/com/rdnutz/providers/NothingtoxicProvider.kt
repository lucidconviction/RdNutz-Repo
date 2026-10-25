package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class NothingtoxicProvider : MainAPI() {
    override var mainUrl = "https://www.nothingtoxic.com"
    override var name = "NothingToxic"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl/" else "$mainUrl/?start=${(page - 1) * 20}"
        val document = app.get(pageUrl).document
        
        val postLinks = mutableSetOf<String>()
        
        document.select("a[href^=https://www.nothingtoxic.com/]")
            .forEach { postLinks.add(fixUrl(it.attr("href"))!!) }
        
        document.select("a.post-card, a[class*=post]")
            .forEach { 
                val href = fixUrlNull(it.attr("href"))
                if (href != null) postLinks.add(href)
            }
        
        document.select("[data-link]")
            .forEach { 
                val href = fixUrlNull(it.attr("data-link"))
                if (href != null) postLinks.add(href)
            }

        val limitedLinks = postLinks.take(30).toList()

        val home = limitedLinks.mapNotNull { postUrl ->
            val postDoc = app.get(postUrl).document
            val videoUrl = postDoc.selectFirst("video source, source[src]")?.attr("src")
                ?: postDoc.selectFirst("video[src]")?.attr("src")
                ?: postDoc.selectFirst("[data-src]")?.attr("data-src")
                ?: ""
            if (videoUrl.isEmpty()) return@mapNotNull null
            
            val title = postDoc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: postDoc.selectFirst("title")?.text()
                ?: "NothingToxic Video"
            
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
                ?: postDoc.selectFirst("[data-src]")?.attr("data-src")
                ?: ""
            if (videoUrl.isEmpty()) return@mapNotNull null
            
            val title = postDoc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: postDoc.selectFirst("title")?.text()
                ?: "NothingToxic Video"
            
            val posterUrl = fixUrlNull(postDoc.selectFirst("meta[property='og:image']")?.attr("content"))
            
            newMovieSearchResponse(title, postUrl, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property='og:title']")?.attr("content")
            ?: document.selectFirst("title")?.text()
            ?: "NothingToxic Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val videoUrl = document.selectFirst("video source, source[src]")?.attr("src")
            ?: document.selectFirst("video[src]")?.attr("src")
            ?: document.selectFirst("[data-src]")?.attr("data-src")
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