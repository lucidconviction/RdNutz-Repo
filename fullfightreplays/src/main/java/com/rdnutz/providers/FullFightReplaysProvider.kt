package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class FullFightReplaysProvider : MainAPI() {
    override var mainUrl = "https://fullfightreplays.com"
    override var name = "FullFightReplays"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest",
        "${mainUrl}/ufc" to "UFC",
        "${mainUrl}/boxing" to "Boxing",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val isHome = request.name == "Latest"
        val pageUrl = when {
            isHome -> if (page == 1) "$mainUrl/" else "$mainUrl/?page=$page"
            request.name == "UFC" -> if (page == 1) "$mainUrl/ufc" else "$mainUrl/ufc?page=$page"
            else -> if (page == 1) "$mainUrl/boxing" else "$mainUrl/boxing?page=$page"
        }
        val document = app.get(pageUrl).document

        // Ucoz-based site: post links are inside .short_item anchors
        val postLinks = document.select("a[href]").mapNotNull { el ->
            val href = fixUrlNull(el.attr("href"))
            if (href != null && href.startsWith(mainUrl) && href.contains("/")) {
                href
            } else null
        }.filter { it.matches(Regex("""^https?://fullfightreplays\.com/[\w-]+(-\d{4}-\d{2}-\d{2})?(-boxing|-ufc)?$""")) }
         .distinct()
         .take(30)

        val home = postLinks.mapNotNull { postUrl ->
            val postDoc = app.get(postUrl).document
            val title = postDoc.selectFirst("title")?.text()
                ?: postDoc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: postDoc.selectFirst("h1")?.text()
                ?: "FullFightReplays Video"
            val posterUrl = fixUrlNull(postDoc.selectFirst("meta[property='og:image']")?.attr("content"))
                ?: fixUrlNull(postDoc.selectFirst("img[src*=\"/_pu/\"]")?.attr("src"))

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
        val document = app.get("${mainUrl}/search/?q=${query.replace(" ", "+")}").document
        return extractPosts(document)
    }

    private suspend fun extractPosts(document: org.jsoup.nodes.Document): List<SearchResponse> {
        val postLinks = document.select("a[href]").mapNotNull { el ->
            val href = fixUrlNull(el.attr("href"))
            if (href != null && href.startsWith(mainUrl) &&
                !href.contains("/css") && !href.contains("/js") &&
                !href.contains("/images") && !href.contains("/.s") &&
                !href.contains("page") && !href.contains("search") &&
                href.split("/").last().length > 5) {
                href
            } else null
        }.distinct().take(30)

        return postLinks.mapNotNull { postUrl ->
            val postDoc = app.get(postUrl).document
            val title = postDoc.selectFirst("title")?.text()
                ?: postDoc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "FullFightReplays Video"
            val posterUrl = fixUrlNull(postDoc.selectFirst("meta[property='og:image']")?.attr("content"))
                ?: fixUrlNull(postDoc.selectFirst("img[src*=\"/_pu/\"]")?.attr("src"))

            newMovieSearchResponse(title.trim(), postUrl, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?: document.selectFirst("meta[property='og:title']")?.attr("content")
            ?: "FullFightReplays Video"
        val posterUrl = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
            ?: fixUrlNull(document.selectFirst("img[src*=\"/_pu/\"]")?.attr("src"))

        return newMovieLoadResponse(title.trim(), url, TvType.Movie, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document

        // Extract Dailymotion links: dailymotion.com/video/{id}
        val dmIds = Regex("""dailymotion\.com/video/([a-zA-Z0-9]+)""").findAll(document.html())
            .map { it.groupValues[1] }
            .toList()

        // Extract OK.ru embed links: ok.ru/videoembed/{id}
        val okIds = Regex("""ok\.ru/videoembed/(\d+)""").findAll(document.html())
            .map { it.groupValues[1] }
            .toList()

        var addedAny = false

        for (id in dmIds) {
            val dmUrl = "https://www.dailymotion.com/embed/video/$id"
            newExtractorLink(
                name,
                "Dailymotion",
                dmUrl,
                INFER_TYPE
            ) { callback(this); addedAny = true }
        }

        for (id in okIds) {
            val okUrl = "https://ok.ru/videoembed/$id"
            newExtractorLink(
                name,
                "OK.ru",
                okUrl,
                INFER_TYPE
            ) { callback(this); addedAny = true }
        }

        return addedAny
    }
}
