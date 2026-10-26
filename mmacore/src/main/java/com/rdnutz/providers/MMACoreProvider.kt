package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class MMACoreProvider : MainAPI() {
    override var mainUrl = "https://www.mma-core.com"
    override var name = "MMACore"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/recent" to "Recent",
        "${mainUrl}/videos/trending" to "Trending",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = when (request.name) {
            "Recent" -> if (page == 1) "${mainUrl}/videos/recent" else "${mainUrl}/videos/recent/page/$page"
            "Trending" -> if (page == 1) "${mainUrl}/videos/trending" else "${mainUrl}/videos/trending/page/$page"
            else -> mainUrl
        }
        val document = app.get(pageUrl).document

        // Video links: /videos/{slug}/{id} where id is numeric
        val videoLinks = document.select("a[href]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.startsWith("/videos/") &&
                    Regex("""^/videos/[^/]+/\d+$""").matches(href)) href else null
            }
            .distinct()
            .take(30)

        val home = videoLinks.mapNotNull { videoPath ->
            val videoUrl = "$mainUrl$videoPath"
            val videoDoc = app.get(videoUrl).document

            // Extract YouTube video ID from iframe src
            val ytId = Regex("""youtube\.com/embed/([a-zA-Z0-9_-]+)""")
                .find(videoDoc.html())?.groupValues?.getOrNull(1)

            // Title from <title> tag - decode HTML entities
            val title = videoDoc.selectFirst("title")?.text()
                ?.replace("&#x1F6A8;", "🥊")
                ?.trim()
                ?: "MMACore Video"

            // Thumbnail: pattern is /images/videos/original/{id}.jpg
            val imageMatch = Regex("""images/videos/original/(\d+)\.jpg""").find(videoDoc.html())
            val posterUrl = if (imageMatch != null) {
                "https://images.mma-core.com/images/videos/original/${imageMatch.groupValues[1]}.jpg"
            } else {
                fixUrlNull(videoDoc.selectFirst("meta[property='og:image']")?.attr("content"))
            }

            newMovieSearchResponse(title, videoUrl, TvType.Movie) {
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
        return extractVideos(document)
    }

    private suspend fun extractVideos(document: org.jsoup.nodes.Document): List<SearchResponse> {
        val videoLinks = document.select("a[href]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.startsWith("/videos/") &&
                    Regex("""^/videos/[^/]+/\d+$""").matches(href)) href else null
            }
            .distinct()
            .take(30)

        return videoLinks.mapNotNull { videoPath ->
            val videoUrl = "$mainUrl$videoPath"
            val videoDoc = app.get(videoUrl).document
            val title = videoDoc.selectFirst("title")?.text()
                ?.replace("&#x1F6A8;", "🥊")
                ?.trim()
                ?: "MMACore Video"
            val imageMatch = Regex("""images/videos/original/(\d+)\.jpg""").find(videoDoc.html())
            val posterUrl = if (imageMatch != null) {
                "https://images.mma-core.com/images/videos/original/${imageMatch.groupValues[1]}.jpg"
            } else null

            newMovieSearchResponse(title, videoUrl, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()
            ?.replace("&#x1F6A8;", "🥊")
            ?.trim()
            ?: "MMACore Video"
        val imageMatch = Regex("""images/videos/original/(\d+)\.jpg""").find(document.html())
        val posterUrl = if (imageMatch != null) {
            "https://images.mma-core.com/images/videos/original/${imageMatch.groupValues[1]}.jpg"
        } else {
            fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val youtubeId = Regex("""youtube\.com/embed/([a-zA-Z0-9_-]+)""")
            .find(document.html())?.groupValues?.getOrNull(1)

        if (youtubeId != null) {
            val ytUrl = "https://www.youtube.com/embed/$youtubeId"
            newExtractorLink(
                name,
                "YouTube",
                ytUrl,
                INFER_TYPE
            ) { callback(this) }
            return true
        }
        return false
    }
}
