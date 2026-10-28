package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class PlumperDProvider : MainAPI() {
    override var mainUrl = "https://plumperd.com"
    override var name = "PlumperD"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python: extracts low-res preview clips from listing page
        // Previews: https://cdnstatic.imctransfer.com/static_01/.../preview_320.mp4
        val document = app.get(mainUrl).document

        // Find all preview MP4 URLs — PlumperD uses data-source on <video> tags, not <source src>
        val previewUrls = document.select("video[data-source*=preview_320.mp4]")
            .map { fixUrlNull(it.attr("data-source")) }
            .filterNotNull()
            .distinct()
            .take(50)

        val home = previewUrls.mapNotNull { videoUrl ->
            // Extract numeric ID from URL path
            val parts = videoUrl.split("/")
            val vidId = parts.filter { it.matches(Regex("^\\d+$")) }.lastOrNull() 
                ?: parts.dropLast(1).lastOrNull() 
                ?: ""
            val title = if (vidId.isNotBlank()) "PlumperD Preview $vidId" else "PlumperD Preview"
            
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
        val previewUrls = document.select("video[data-source*=preview_320.mp4]")
            .map { fixUrlNull(it.attr("data-source")) }
            .filterNotNull()
            .distinct()
            .take(30)

        return previewUrls.mapNotNull { videoUrl ->
            val parts = videoUrl.split("/")
            val vidId = parts.filter { it.matches(Regex("^\\d+$")) }.lastOrNull() 
                ?: parts.dropLast(1).lastOrNull() 
                ?: ""
            val title = if (vidId.isNotBlank()) "PlumperD Preview $vidId" else "PlumperD Preview"
            
            newMovieSearchResponse(title, videoUrl, TvType.NSFW) {
                this.posterUrl = "$mainUrl/favicon.ico"
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        return newMovieLoadResponse("PlumperD Preview", url, TvType.NSFW, url)
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
        val videoUrl = document.selectFirst("video[data-source*=preview_320.mp4]")?.attr("data-source")
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