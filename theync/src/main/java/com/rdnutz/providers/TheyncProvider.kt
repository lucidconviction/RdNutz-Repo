package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import kotlin.math.max

class TheyncProvider : MainAPI() {
    override var mainUrl = "https://theync.com"
    override var name = "TheYNC"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/latest-updates/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl/latest-updates/" else "$mainUrl/latest-updates/page/$page/"
        val document = app.get(pageUrl).document
        
        val resolved = mutableMapOf<String, String>() // get_file_url -> title
        val getFileRegex = "https://theync\\.com/get_file/\\d+/[^\"'\\s]+".toRegex()
        
        // Python: regex for get_file URLs
        getFileRegex.findAll(document.html()).forEach { match ->
            val url = match.groupValues[0]
            if (!resolved.containsKey(url)) {
                val pos = document.html().indexOf(url)
                val nearby = document.html().substring(max(0, pos - 500), pos)
                val title = "alt=\"([^\"]+)\"".toRegex().find(nearby)?.groupValues?.get(1)
                    ?: "title>([^<]*)<".toRegex().find(nearby)?.groupValues?.get(1)
                    ?: "TheYNC Video"
                resolved[url] = title
            }
        }
        
        // Python: also finds video pages and resolves their get_file URLs
        val videoUrls = document.select("a[href*=/video/]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.contains("/video/")) href else null
            }
            .distinct()
            .take(20)
        
        videoUrls.forEach { videoUrl ->
            if (resolved.size >= 30) return@forEach
            val videoDoc = app.get(videoUrl).document
            val gf = "https://theync\\.com/get_file/\\d+/[^\"'\\s]+".toRegex().find(videoDoc.html())
                ?: return@forEach
            if (!resolved.containsKey(gf.groupValues[0])) {
                val title = videoDoc.selectFirst("title")?.text()
                    ?.replace(" — TheYNC", "")
                    ?.trim()
                    ?: "TheYNC Video"
                resolved[gf.groupValues[0]] = title
            }
        }

        val home = resolved.entries
            .take(30)
            .map { (url, title) ->
                val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
                newMovieSearchResponse(title, url, TvType.NSFW) {
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
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val title = url.split("/").last().replace("-", " ")
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = ""
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        if (data.contains("get_file")) {
            newExtractorLink(
                name,
                name,
                fixUrl(data),
                INFER_TYPE
            ) { callback(this) }
            return true
        }
        return false
    }
}