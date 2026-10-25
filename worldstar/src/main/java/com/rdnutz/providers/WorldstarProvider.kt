package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class WorldstarProvider : MainAPI() {
    override var mainUrl = "https://worldstarhiphop.com"
    override var name = "Worldstar Hip Hop"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl/videos/" else "$mainUrl/videos/page/$page/"
        val document = app.get(pageUrl).document
        
        // Python: extracts from __NEXT_DATA__ JSON and __next_f chunks
        // Also falls back to regex on stripped HTML
        val allVideos = mutableMapOf<String, Pair<String, String>>() // mp4 -> (title, mp4)
        
        // Try to extract from __NEXT_DATA__
        val nextData = document.selectFirst("script#__NEXT_DATA__")?.html()
        if (nextData != null) {
            extractFromJson(nextData, allVideos)
        }
        
        // Try __next_f chunks
        document.select("script").forEach { script ->
            val html = script.html()
            if (html.contains("__next_f.push")) {
                // Extract the chunk data
                val chunks = html.split("__next_f.push(\\[").drop(1)
                chunks.forEach { chunk ->
                    val endIdx = chunk.indexOf("])")
                    if (endIdx > 0) {
                        val data = chunk.substring(0, endIdx)
                            .replace("\\\"", "\"")
                            .replace("\\n", "\n")
                            .replace("\\\\", "\\")
                        extractFromJson(data, allVideos)
                    }
                }
            }
        }
        
        // Fallback: regex on stripped HTML for direct mp4 URLs
        if (allVideos.isEmpty()) {
            val stripped = document.html().replace("<[^>]+>".toRegex(), " ")
            "https?://[^\\s\\\"]+\\.mp4".toRegex().findAll(stripped)
                .map { it.value }
                .forEach { url ->
                    if (!allVideos.containsKey(url)) {
                        allVideos[url] = "Worldstar Video" to url
                    }
                }
        }

        val home = allVideos.entries
            .take(30)
            .map { (mp4, pair) ->
                val (title, _) = pair
                val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
                newMovieSearchResponse(title, mp4, TvType.NSFW) {
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

    private fun extractFromJson(json: String, allVideos: MutableMap<String, Pair<String, String>>) {
        // Python extracts videos by finding "title" and "utLocation" fields
        var pos = 0
        while (pos < json.length) {
            val tIdx = json.indexOf("\"title\"", pos)
            if (tIdx == -1) break
            val vStart = json.indexOf("\"", tIdx + 7)
            if (vStart == -1) break
            var vEnd = vStart + 1
            while (vEnd < json.length && (json[vEnd] != '"' || json[vEnd - 1] == '\\')) vEnd++
            val title = json.substring(vStart + 1, vEnd).replace("\\\"", "\"")

            val uIdx = json.indexOf("\"utLocation\"", vEnd)
            var url: String? = null
            if (uIdx != -1 && uIdx - vEnd < 2000) {
                val uStart = json.indexOf("\"", uIdx + 13)
                if (uStart != -1) {
                    var uEnd = uStart + 1
                    while (uEnd < json.length && (json[uEnd] != '"' || json[uEnd - 1] == '\\')) uEnd++
                    url = json.substring(uStart + 1, uEnd)
                }
            } else {
                val mp4Idx = json.indexOf("mp4", vEnd)
                if (mp4Idx != -1 && mp4Idx - vEnd < 3000) {
                    var uEnd = mp4Idx + 4
                    while (uEnd < json.length && (json[uEnd] != '"' || json[uEnd - 1] == '\\')) uEnd++
                    url = json.substring(mp4Idx, uEnd).split("?")[0].split("#")[0]
                }
            }

            if (url != null && url.endsWith(".mp4")) {
                allVideos[url] = title to url
            }
            pos = vEnd + 1
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        // Python doesn't implement search for Worldstar
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        // Worldstar URLs are direct mp4 links
        val title = url.split("/").last().replace(".mp4", "").replace("-", " ")
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = ""
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        // data is the direct mp4 URL
        if (data.endsWith(".mp4")) {
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