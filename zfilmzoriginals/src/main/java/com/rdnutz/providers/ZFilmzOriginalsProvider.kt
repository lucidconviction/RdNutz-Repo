package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject

class ZFilmzOriginalsProvider : MainAPI() {
    override var mainUrl = "https://z-filmz-originals.com"
    override var name = "ZFilmzOriginals"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/videos" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python: Next.js site with JSON in <script id="__NEXT_DATA__" type="application/json">
        // Videos: data.props.pageProps.contents.data[i].trailer_url and title
        // Pagination: ?page={n}
        val pageUrl = if (page == 1) "$mainUrl/videos" else "$mainUrl/videos?page=$page"
        val document = app.get(pageUrl).document

        val nextDataScript = document.selectFirst("script#__NEXT_DATA__")?.html() ?: ""
        if (nextDataScript.isEmpty()) {
            return newHomePageResponse(
                list = HomePageList(name = request.name, list = emptyList(), isHorizontalImages = true),
                hasNext = false
            )
        }

        val videos = mutableListOf<Pair<String, String>>()
        try {
            val json = JSONObject(nextDataScript)
            val contents = json.getJSONObject("props")
                .getJSONObject("pageProps")
                .getJSONObject("contents")
                .getJSONArray("data")
            
            for (i in 0 until contents.length()) {
                val v = contents.getJSONObject(i)
                val title = v.optString("title", "")
                val trailerUrl = v.optString("trailer_url", "")
                if (title.isNotBlank() && trailerUrl.isNotBlank()) {
                    videos.add(Pair(title, trailerUrl))
                }
            }
        } catch (e: Exception) {
            // JSON parsing failed
        }

        val home = videos.take(30).mapNotNull { (title, trailerUrl) ->
            newMovieSearchResponse(title, trailerUrl, TvType.Movie) {
                this.posterUrl = "$mainUrl/favicon.ico"
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
        val document = app.get("$mainUrl/videos?search=${query.replace(" ", "+")}").document

        val nextDataScript = document.selectFirst("script#__NEXT_DATA__")?.html() ?: ""
        if (nextDataScript.isEmpty()) return emptyList()

        val videos = mutableListOf<Pair<String, String>>()
        try {
            val json = JSONObject(nextDataScript)
            val contents = json.getJSONObject("props")
                .getJSONObject("pageProps")
                .getJSONObject("contents")
                .getJSONArray("data")
            
            for (i in 0 until contents.length()) {
                val v = contents.getJSONObject(i)
                val title = v.optString("title", "")
                val trailerUrl = v.optString("trailer_url", "")
                if (title.isNotBlank() && trailerUrl.isNotBlank()) {
                    videos.add(Pair(title, trailerUrl))
                }
            }
        } catch (e: Exception) {
            // JSON parsing failed
        }

        return videos.take(30).mapNotNull { (title, trailerUrl) ->
            newMovieSearchResponse(title, trailerUrl, TvType.Movie) {
                this.posterUrl = ""
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        // For trailer URLs, we just pass them through
        return newMovieLoadResponse("ZFilmzOriginals Trailer", url, TvType.Movie, url)
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        // The URL is already the direct MP4/trailer URL
        newExtractorLink(
            name,
            name,
            fixUrl(data),
            INFER_TYPE
        ) { callback(this) }
        return true
    }
}