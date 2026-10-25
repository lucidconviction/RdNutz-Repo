package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class XraresProvider : MainAPI() {
    override var mainUrl = "https://www.xrares.com"
    override var name = "Xrares"
    override val supportedTypes = setOf(TvType.NSFW)
    override val hasMainPage = true

    private fun getRequest(url: String): org.jsoup.nodes.Document {
        return Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .timeout(15000)
            .get()
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val html = getRequest("$mainUrl/videos").html()
        
        val videoLinks = """href="(/video/\d+[^"]*)"""".toRegex()
            .findAll(html)
            .map { "https://www.xrares.com${it.groupValues[1]}" }
            .distinct()
            .take(30)
            .toList()
        
        val posts = videoLinks.map { url ->
            newMovieSearchResponse(url.split("/").last(), url, TvType.NSFW, false)
        }
        
        return newHomePageResponse(name, posts)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse? = newMovieLoadResponse("", url, TvType.NSFW, url)

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = getRequest(data)
        val videoUrl = doc.select("video source, video[src]").attr("src")
        
        if (videoUrl.isNotEmpty()) {
            newExtractorLink(
                name,
                name,
                fixUrl(videoUrl),
                if (videoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
            ) { callback(this) }
            return true
        }
        return false
    }
}