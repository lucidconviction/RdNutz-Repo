package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class SportsProvider : MainAPI() {
    override var mainUrl = "https://nuvio.moaqeel6679.my.id"
    override var name = "Live Sports"
    override val supportedTypes = setOf(TvType.Movie, TvType.Live)
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "catalog" to "Live"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val baseUrl = mainUrl
        val doc = app.get("$baseUrl/catalog/tv/nuvio_sports_live.json").document
        val catalogHtml = doc.html()

        val posts = mutableListOf<SearchResponse>()

        """\{"id":"([^"]+)","name":"([^"]+)"\}""".toRegex().findAll(catalogHtml)
            .map { match ->
                val id = match.groupValues[1]
                val name = match.groupValues[2]
                newMovieSearchResponse(name, "$baseUrl/watch/$id", TvType.Live)
            }
            .toList()
            .let { posts.addAll(it) }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = posts,
                isHorizontalImages = false
            ),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val baseUrl = mainUrl
        val doc = app.get("$baseUrl/catalog/tv/nuvio_sports_live.json").document
        val catalogHtml = doc.html()

        return """\{"id":"([^"]+)","name":"([^"]+)"\}""".toRegex().findAll(catalogHtml)
            .map { match ->
                val id = match.groupValues[1]
                val name = match.groupValues[2]
                if (name.contains(query, ignoreCase = true)) {
                    newMovieSearchResponse(name, "$baseUrl/watch/$id", TvType.Live)
                } else null
            }
            .filterNotNull()
            .distinctBy { it.url }
            .toList()
    }

    override suspend fun load(url: String): LoadResponse = newMovieLoadResponse(name, url, TvType.Live, url)

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document

        val videoUrl = document.selectFirst("video source[src$='.m3u8']")?.attr("src")
            ?: document.selectFirst("video source")?.attr("src")
            ?: document.selectFirst("video[src]")?.attr("src")
            ?: document.selectFirst("iframe[src*='player']")?.attr("src")
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
