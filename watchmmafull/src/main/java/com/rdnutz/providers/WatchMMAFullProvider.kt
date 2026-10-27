package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject

class WatchMMAFullProvider : MainAPI() {
    override var mainUrl = "https://watchmmafull.com"
    override var name = "WatchMMAFull"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest",
        "${mainUrl}/cate/ufc/" to "UFC",
        "${mainUrl}/cate/one-championship/" to "ONE Championship",
        "${mainUrl}/cate/bellator/" to "Bellator",
        "${mainUrl}/cate/pfl/" to "PFL",
        "${mainUrl}/cate/ksw/" to "KSW",
        "${mainUrl}/cate/rizin/" to "Rizin",
        "${mainUrl}/cate/oktagon/" to "Oktagon",
        "${mainUrl}/cate/lfa/" to "LFA",
        "${mainUrl}/cate/brave-cf/" to "Brave CF",
        "${mainUrl}/cate/cage-warriors/" to "Cage Warriors",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = when {
            request.name == "Latest" -> if (page == 1) "${mainUrl}/" else "${mainUrl}/new/$page"
            else -> if (page == 1) "${request.data}" else "${request.data}${page}"
        }
        val document = app.get(pageUrl).document

        val postLinks = document.select("a[href]").mapNotNull { el ->
            fixUrlNull(el.attr("href"))
        }.filter { it.contains(".html") && it.startsWith(mainUrl) }.distinct().take(30)

        val home = postLinks.mapNotNull { postUrl ->
            val postDoc = app.get(postUrl).document
            val title = postDoc.selectFirst("title")?.text()
                ?: postDoc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "WatchMMAFull Video"
            val posterUrl = fixUrlNull(postDoc.selectFirst("meta[property='og:image']")?.attr("content"))

            newMovieSearchResponse(title.trim(), postUrl, TvType.Movie) {
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
        val document = app.get("${mainUrl}/?s=${query.replace(" ", "+")}").document
        return extractPosts(document)
    }

    private suspend fun extractPosts(document: org.jsoup.nodes.Document): List<SearchResponse> {
        val postLinks = document.select("a[href]").mapNotNull { el ->
            fixUrlNull(el.attr("href"))
        }.filter { it.contains(".html") && it.startsWith(mainUrl) }.distinct().take(30)

        return postLinks.mapNotNull { postUrl ->
            val postDoc = app.get(postUrl).document
            val title = postDoc.selectFirst("title")?.text()
                ?: postDoc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "WatchMMAFull Video"
            val posterUrl = fixUrlNull(postDoc.selectFirst("meta[property='og:image']")?.attr("content"))

            newMovieSearchResponse(title.trim(), postUrl, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?: document.selectFirst("meta[property='og:title']")?.attr("content")
            ?: "WatchMMAFull Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))

        return newMovieLoadResponse(title.trim(), url, TvType.Movie, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val html = document.html()

        // Step 1: Extract embedUrl from JSON-LD VideoObject script
        for (el in document.select("script[type=application/ld+json]")) {
            val jsonStr = el.html().trim()
            try {
                val obj = JSONObject(jsonStr)
                if (obj.getString("@type") == "VideoObject" && obj.has("embedUrl")) {
                    val embedUrl = obj.getString("embedUrl")
                    resolveHlsFromEmbed(embedUrl)?.let { hlsUrl ->
                        newExtractorLink(
                            name,
                            "WatchMMAFull",
                            hlsUrl,
                            INFER_TYPE
                        ) {
                            this.referer = "https://content.st"
                            callback(this)
                        }
                        return true
                    }
                }
            } catch (_: Exception) {
                // Skip malformed JSON-LD
            }
        }

        return false
    }

    private suspend fun resolveHlsFromEmbed(embedUrl: String): String? {
        // Step 2: Fetch embed page -> find iframe pointing to content.st
        val embedDoc = app.get(embedUrl).document
        val iframeSrc = embedDoc.selectFirst("iframe[src*='content.st']")?.attr("src")
            ?: return null

        // Step 3: Fetch player page -> extract pcfg JSON
        val playerPage = app.get(iframeSrc).document
        val pcfgHtml = playerPage.selectFirst("script#pcfg")?.html()
            ?: return null

        val pcfg = JSONObject(pcfgHtml)
        val videoId = pcfg.getString("id")

        // Step 4: POST to /x/k/{id} to obtain the HLS stream URL
        val checkUrl = "https://content.st/x/k/$videoId"
        val response = app.post(
            checkUrl,
            headers = mapOf("Content-Type" to "application/json"),
            json = mapOf("bc" to "")
        )
        val result = JSONObject(response.body.string())

        return if (result.optBoolean("ok", false) && result.has("src")) {
            result.getString("src")
        } else null
    }
}