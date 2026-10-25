package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class CrazyshitProvider : MainAPI() {
    override var mainUrl = "https://crazyshit.com"
    override var name = "Crazyshit"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    private fun getRequest(url: String): org.jsoup.nodes.Document {
        return Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .timeout(15000)
            .get()
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl/videos/" else "$mainUrl/videos/$page/"
        val doc = getRequest(pageUrl)
        
        val items = doc.select("a[href*=/cnt/medias/]").mapNotNull { el: org.jsoup.nodes.Element ->
            val title = el.text().trim()
            val href = fixUrl(el.attr("href"))
            val poster = el.select("img").attr("src")
            
            if (title.isNotEmpty() && href.isNotEmpty()) {
                newMovieSearchResponse(title, href, TvType.NSFW, false) {
                    posterUrl = poster
                }
            } else null
        }.distinctBy { it.url }

        return newHomePageResponse(name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/search/$query/"
        val doc = getRequest(searchUrl)
        
        return doc.select("a[href*=/cnt/medias/]").mapNotNull { el: org.jsoup.nodes.Element ->
            val title = el.text().trim()
            val href = fixUrl(el.attr("href"))
            val poster = el.select("img").attr("src")
            
            if (title.isNotEmpty() && href.isNotEmpty()) {
                newMovieSearchResponse(title, href, TvType.NSFW, false) {
                    posterUrl = poster
                }
            } else null
        }.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = getRequest(url)
        val title = doc.select("h1, property=og:title").text().trim()
        val poster = doc.select("meta[property=og:image]").attr("content")
        val description = doc.select("meta[property=og:description]").attr("content")

        return newMovieLoadResponse(name, url, TvType.NSFW, url) {
            posterUrl = poster
            plot = description
        }
    }

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
            ) {
                callback(this)
            }
            return true
        }
        return false
    }
}
