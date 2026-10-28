package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class JeffsModelsProvider : MainAPI() {
    override var mainUrl = "https://jeffsmodels.com"
    override var name = "JeffsModels"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python: extracts 10-second preview clips from listing page
        // Previews: https://fast-media.roguebucks.com/jeffsmodels.com/tour03/updates/{id}/10_sec.mp4
        // Titles from: href="/update/{id}/"
        val pageUrl = if (page == 1) mainUrl else "$mainUrl/page/$page/"
        val document = app.get(pageUrl).document

        // Find all preview MP4 URLs
        val previewUrls = document.select("source[src*=/10_sec.mp4], video source[src*=/10_sec.mp4]")
            .map { fixUrlNull(it.attr("src")) }
            .filterNotNull()
            .distinct()
            .take(50)

        // Build title map from update links
        val titleMap = mutableMapOf<String, String>()
        document.select("a[href*=/update/]").forEach { el ->
            val href = el.attr("href")
            val title = el.text().trim()
            if (href.contains("/update/") && title.isNotBlank()) {
                val path = href.split("?")[0]
                titleMap[path] = title
            }
        }

        val home = previewUrls.mapNotNull { videoUrl ->
            val vidId = videoUrl.split("/").filter { it.matches(Regex("^\\d+$")) }.lastOrNull()
            if (vidId == null) return@mapNotNull null
            
            val title = titleMap["/update/$vidId/"] ?: "JeffsModels Update $vidId"
            newMovieSearchResponse(title, videoUrl, TvType.NSFW) {
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
        val document = app.get("$mainUrl/search?q=${query.replace(" ", "+")}").document
        val previewUrls = document.select("source[src*=/10_sec.mp4], video source[src*=/10_sec.mp4]")
            .map { fixUrlNull(it.attr("src")) }
            .filterNotNull()
            .distinct()
            .take(30)

        val titleMap = mutableMapOf<String, String>()
        document.select("a[href*=/update/]").forEach { el ->
            val href = el.attr("href")
            val title = el.text().trim()
            if (href.contains("/update/") && title.isNotBlank()) {
                val path = href.split("?")[0]
                titleMap[path] = title
            }
        }

        return previewUrls.mapNotNull { videoUrl ->
            val vidId = videoUrl.split("/").filter { it.matches(Regex("^\\d+$")) }.lastOrNull()
            if (vidId == null) return@mapNotNull null
            
            val title = titleMap["/update/$vidId/"] ?: "JeffsModels Update $vidId"
            newMovieSearchResponse(title, videoUrl, TvType.NSFW) {
                this.posterUrl = "$mainUrl/favicon.ico"
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        return newMovieLoadResponse("JeffsModels Preview", url, TvType.NSFW, url)
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        // URL is already a direct preview MP4
        if (data.endsWith(".mp4")) {
            newExtractorLink(
                name,
                name,
                fixUrl(data),
                INFER_TYPE
            ) { callback(this) }
            return true
        }

        val document = app.get(data).document
        val videoUrl = document.selectFirst("source[src*=/10_sec.mp4], video source[src*=/10_sec.mp4]")?.attr("src")
            ?: document.selectFirst("video[src]")?.attr("src")
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