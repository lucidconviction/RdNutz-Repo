package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document

class JeffModelsProvider : MainAPI() {
    override var mainUrl = "https://jeffsmodels.com"
    override var name = "JeffModels"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    private fun Document.findAllMp4Urls(): List<String> {
        val pattern = "https?://fast-media\\.roguebucks\\.com/jeffsmodels\\.com/tour03/updates/\\d+/10_sec\\.mp4".toRegex()
        return pattern.findAll(this.html()).map { it.value }.distinct().sorted().take(30).toList()
    }

    private fun Document.extractTitlesFromLinks(): Map<String, String> {
        val titleMap = mutableMapOf<String, String>()
        val linkPattern = "<a[^>]*href=\"(/update/\\d+/[^\"]*)\"[^>]*>(.*?)</a>".toRegex()
        linkPattern.findAll(this.html()).forEach { match ->
            val path = match.groupValues[1].split("?")[0]
            var rawTitle = match.groupValues[2].trim()
            rawTitle = rawTitle.replace("<[^>]+>".toRegex(), "")
            rawTitle = rawTitle.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
            if (rawTitle.isNotBlank()) {
                titleMap[path] = rawTitle
            }
        }
        return titleMap
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val mp4s = document.findAllMp4Urls()
        val titleMap = document.extractTitlesFromLinks()

        val home = mutableListOf<SearchResponse>()
        mp4s.forEach { url ->
            val parts = url.split("/")
            val vidId = if (parts.size >= 2) parts[parts.size - 2] else ""
            val title = titleMap["/update/$vidId/"] ?: "JeffModels Update $vidId"
            home.add(
                newMovieSearchResponse(title, url, TvType.NSFW) {
                    this.posterUrl = ""
                }
            )
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val parts = url.split("/")
        val vidId = if (parts.size >= 2) parts[parts.size - 2] else ""
        val title = "JeffModels Update $vidId"
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = ""
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        if (data.contains("10_sec.mp4")) {
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
