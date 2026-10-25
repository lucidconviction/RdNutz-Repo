package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class SportsProvider : MainAPI() {
    override var mainUrl = "https://nuvio.moaqeel6679.my.id"
    override var name = "Live Sports"
    override val supportedTypes = setOf(TvType.Movie, TvType.Live)
    override val hasMainPage = true

    private fun getRequest(url: String): org.jsoup.nodes.Document {
        return Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .timeout(15000)
            .get()
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val baseUrl = "https://nuvio.moaqeel6679.my.id"
        val doc = getRequest("$baseUrl/catalog/tv/nuvio_sports_live.json")
        val catalogHtml = doc.html()
        
        val posts = mutableListOf<SearchResponse>()
        
        val metas = """\{"id":"([^"]+)","name":"([^"]+)"""".toRegex().findAll(catalogHtml)
            .map { match ->
                val id = match.groupValues[1]
                val name = match.groupValues[2]
                newMovieSearchResponse(name, "$baseUrl/watch/$id", TvType.Live)
            }
            .toList()
        
        return newHomePageResponse(name, posts)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val baseUrl = "https://nuvio.moaqeel6679.my.id"
        val doc = getRequest("$baseUrl/catalog/tv/nuvio_sports_live.json")
        val catalogHtml = doc.html()
        
        return """\{"id":"([^"]+)","name":"([^"]+)"""".toRegex().findAll(catalogHtml)
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

    override suspend fun load(url: String): LoadResponse? = newMovieLoadResponse("", url, TvType.Live, url)

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = false
}