package com.rdnutz.providers

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class MMAReplaysProvider : MainAPI() {
    override var mainUrl = "https://mmareplays.com"
    override var name = "MMAReplays"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest",
        "${mainUrl}/ufc" to "UFC",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = when {
            page == 1 -> mainUrl
            request.name == "UFC" -> "$mainUrl/ufc?page=$page"
            else -> "$mainUrl?page=$page"
        }
        val document = app.get(pageUrl).document

        // Fight cards: <article class="fight-card"> with <a href="/fight/slug">
        val fightLinks = document.select("a[href^=\"/fight/\"]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.startsWith("/fight/")) href else null
            }
            .distinct()
            .take(30)

        val home = fightLinks.mapNotNull { fightPath ->
            val fightUrl = "$mainUrl$fightPath"
            val fightDoc = app.get(fightUrl).document

            // Get title
            val title = fightDoc.selectFirst("title")?.text()?.trim()
                ?: fightDoc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "MMA Replay"

            // Get poster from og:image or article thumbnail
            val posterUrl = fixUrlNull(fightDoc.selectFirst("meta[property='og:image']")?.attr("content"))
                ?: fightDoc.selectFirst("img[src*=\"thumbnail\"]")?.attr("src")
                ?: fightDoc.selectFirst("img[data-src]")?.attr("data-src")

            newMovieSearchResponse(title, fightUrl, TvType.Movie) {
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
        val document = app.get("${mainUrl}/search?q=${query.replace(" ", "+")}").document
        return extractFights(document)
    }

    private suspend fun extractFights(document: org.jsoup.nodes.Document): List<SearchResponse> {
        val fightLinks = document.select("a[href^=\"/fight/\"]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.startsWith("/fight/")) href else null
            }
            .distinct()
            .take(30)

        return fightLinks.mapNotNull { fightPath ->
            val fightUrl = "$mainUrl$fightPath"
            val fightDoc = app.get(fightUrl).document
            val title = fightDoc.selectFirst("title")?.text()?.trim()
                ?: fightDoc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "MMA Replay"
            val posterUrl = fixUrlNull(fightDoc.selectFirst("meta[property='og:image']")?.attr("content"))
                ?: fightDoc.selectFirst("img[src*=\"thumbnail\"]")?.attr("src")
                ?: fightDoc.selectFirst("img[data-src]")?.attr("data-src")

            newMovieSearchResponse(title, fightUrl, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()?.trim()
            ?: document.selectFirst("meta[property='og:title']")?.attr("content")
            ?: "MMA Replay"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
            ?: document.selectFirst("img[src*=\"thumbnail\"]")?.attr("src")
            ?: document.selectFirst("img[data-src]")?.attr("data-src")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document

        // Extract video IDs from data-href attributes or direct vidara/gasobni URLs
        val videoIds = Regex("""data-href="(https?://[^"]+)"""")
            .findAll(document.html())
            .map { match ->
                match.groupValues[1].substringAfterLast('/')
            }
            .toList()
            .distinct()
            .toMutableList()

        if (videoIds.isEmpty()) {
            val directMatches = Regex("""(?:vidara\.so/v|vidara\.to/e)/([a-zA-Z0-9]+)""")
                .findAll(document.html())
                .map { it.groupValues[1] }
                .toList()
                .distinct()
            videoIds.addAll(directMatches)
        }

        var addedAny = false
        for (videoId in videoIds) {
            // Call vidara.so /api/stream with JSON body {"filecode": videoId}
            val jsonBody = """{"filecode":"$videoId"}"""
            val response = try {
                app.post(
                    "https://vidara.so/api/stream",
                    requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull()),
                    headers = mapOf(
                        "Referer" to "https://vidara.so/v/$videoId",
                        "Origin" to "https://vidara.so"
                    )
                ).parsedSafe<GasobniStreamResponse>()
            } catch (e: Exception) {
                null
            }

            if (response?.streamingUrl != null && response.streamingUrl.isNotBlank()) {
                newExtractorLink(
                    name,
                    "MMAReplays",
                    response.streamingUrl,
                    INFER_TYPE
                ) { callback(this); addedAny = true }
            } else if (response?.streamingUrl == null && response != null) {
                val playerUrl = "https://vidara.so/v/$videoId"
                newExtractorLink(
                    name,
                    "MMAReplays",
                    playerUrl,
                    INFER_TYPE
                ) { callback(this); addedAny = true }
            }
        }

        return addedAny
    }
}

data class GasobniStreamResponse(
    @JsonProperty("streaming_url") val streamingUrl: String? = null,
    @JsonProperty("thumbnail") val thumbnail: String? = null,
    @JsonProperty("title") val title: String? = null,
)
