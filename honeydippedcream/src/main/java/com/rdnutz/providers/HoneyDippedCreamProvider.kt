package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document

class HoneydippedcreamProvider : MainAPI() {
    override var mainUrl = "https://honeydippedcream.com"
    override var name = "HoneyDippedCream"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    private fun slugToTitle(slug: String): String {
        return slug.replace("-", " ")
            .replace("amp", "&")
            .split(" ")
            .map { it.capitalize() }
            .joinToString(" ")
    }

    private fun Document.findAllMp4Urls(): List<String> {
        val pattern = "https?://[^\\s\\\"'<>]+\\.mp4[^\\s\\\"'<>]*".toRegex()
        return pattern.findAll(this.html())
            .map { it.value }
            .filter { it.contains("_large.mp4") && !it.contains("trailer", ignoreCase = true) }
            .distinct()
            .sorted()
            .toList()
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val mp4s = document.findAllMp4Urls()

        val home = mutableListOf<SearchResponse>()
        mp4s.take(30).forEach { url ->
            val fname = url.substringAfterLast("/")
            val title = slugToTitle(fname.replace(".mp4", "").replace("_honedc_", " "))
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
        val fname = url.substringAfterLast("/")
        val title = slugToTitle(fname.replace(".mp4", "").replace("_honedc_", " "))
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = ""
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
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