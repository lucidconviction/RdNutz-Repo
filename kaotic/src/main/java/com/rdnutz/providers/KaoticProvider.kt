package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.select.Elements

class KaoticProvider : MainAPI() {
    override var mainUrl = "https://kaotic.com"
    override var name = "Kaotic"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    private fun getRequest(url: String): org.jsoup.nodes.Document {
        return Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .timeout(15000)
            .get()
    }

    private fun getPosterUrl(el: org.jsoup.nodes.Element): String {
        return el.select("img")?.firstOrNull()?.attr("data-src")
            ?: el.select("img")?.firstOrNull()?.attr("src")
            ?: el.select("img")?.firstOrNull()?.attr("data-original")
            ?: el.parent()?.select("img")?.firstOrNull()?.attr("src")
            ?: ""
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl/videos/" else "$mainUrl/videos/page/$page/"
        val doc = getRequest(pageUrl)

        val links = doc.select("a[href*=/video/]") ?: Elements()
        val items = links.mapNotNull { el: org.jsoup.nodes.Element ->
            val title = el.text().trim()
            val href = fixUrl(el.attr("href"))
            val poster = getPosterUrl(el)

            if (title.isNotEmpty() && href.isNotEmpty()) {
                newMovieSearchResponse(title, href, TvType.NSFW, false) {
                    this.posterUrl = if (poster.isNotEmpty()) fixUrl(poster) else ""
                }
            } else null
        }.distinctBy { it.url }.take(30)

        return newHomePageResponse(listOf(HomePageList("Videos", items, false)))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/search/$query/"
        val doc = getRequest(searchUrl)
        val links = doc.select("a[href*=/video/]") ?: Elements()

        return links.mapNotNull { el: org.jsoup.nodes.Element ->
            val title = el.text().trim()
            val href = fixUrl(el.attr("href"))
            val poster = getPosterUrl(el)

            if (title.isNotEmpty() && href.isNotEmpty()) {
                newMovieSearchResponse(title, href, TvType.NSFW, false) {
                    this.posterUrl = if (poster.isNotEmpty()) fixUrl(poster) else ""
                }
            } else null
        }.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = getRequest(url)
        val title = doc.select("h1, property=og:title").text().trim()
        val poster = doc.select("meta[property=og:image]").attr("content")
        val description = doc.select("meta[property=og:description]").attr("content")

        val duration = doc.select(".duration, .video-duration, [class*='duration']").text().trim()
        val uploadDate = doc.select(".date, .upload-date, [class*='date']").text().trim()
        val tags = doc.select(".tags a, .categories a, [class*='tag'] a").map { it.text().trim() }.joinToString(", ")

        var plot = description
        if (duration.isNotEmpty()) plot = "Duration: $duration\n$plot"
        if (uploadDate.isNotEmpty()) plot = "Uploaded: $uploadDate\n$plot"
        if (tags.isNotEmpty()) plot = "Tags: $tags\n$plot"

        return newMovieLoadResponse(name, url, TvType.NSFW, url) {
            this.posterUrl = fixUrl(poster)
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = getRequest(data)

        val videoUrl = doc.select("video source, video[src], source[src]").firstOrNull()?.attr("src")
            ?: doc.select("iframe[src*='player'], iframe[src*='embed']").firstOrNull()?.attr("src")
            ?: ""

        if (videoUrl.isNotEmpty()) {
            newExtractorLink(
                name,
                name,
                fixUrl(videoUrl),
                if (videoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
            ) { callback(this) }
            return true
        }

        val scripts = doc.select("script") ?: Elements()
        val scriptContent = scripts.map { it.html() }.joinToString("\n")
        val mp4Urls = """https?://[^"'\s]+\.mp4[^"'\s]*""".toRegex().findAll(scriptContent)
            .map { it.value }.distinct().toList()

        mp4Urls.forEach { url ->
            newExtractorLink(
                name,
                name,
                fixUrl(url),
                ExtractorLinkType.VIDEO
            ) { callback(this) }
        }

        return !mp4Urls.isEmpty()
    }
}