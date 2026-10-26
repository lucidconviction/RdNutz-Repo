package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class HoneyDippedCreamProvider : MainAPI() {
    override var mainUrl = "https://honeydippedcream.com"
    override var name = "HoneyDippedCream"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Latest"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Python: extract direct MP4s from listing page
        // MP4s: https://thumbs.honeydippedcream.yppcdn.com/.../hdcXXX_honedc_TITLE_large.mp4
        val pageUrl = if (page == 1) mainUrl else "${mainUrl}/page/${page}/"
        val document = app.get(pageUrl).document

        // Find all video source URLs with _large.mp4
        val videoSources = document.select("video source[src*=_large.mp4], source[src*=_large.mp4]")
            .map { fixUrlNull(it.attr("src")) }
            .filterNotNull()
            .distinct()
            .take(50)

        val home = videoSources.mapNotNull { videoUrl ->
            // Extract title from URL: hdc123_honedc_video_title_large.mp4
            val fileName = videoUrl.substringAfterLast("/")
            val titlePart = fileName.removeSuffix(".mp4").replace("_honedc_", " ")
            val title = titlePart.split("_").joinToString(" ").replace("large", "").trim()

            newMovieSearchResponse(title, videoUrl, TvType.NSFW) {
                this.posterUrl = ""
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
        val document = app.get("${mainUrl}/search/${query.replace(" ", "+")}").document
        val videoSources = document.select("video source[src*=_large.mp4], source[src*=_large.mp4]")
            .map { fixUrlNull(it.attr("src")) }
            .filterNotNull()
            .distinct()
            .take(30)

        return videoSources.mapNotNull { videoUrl ->
            val fileName = videoUrl.substringAfterLast("/")
            val titlePart = fileName.removeSuffix(".mp4").replace("_honedc_", " ")
            val title = titlePart.split("_").joinToString(" ").replace("large", "").trim()

            newMovieSearchResponse(title, videoUrl, TvType.NSFW) {
                this.posterUrl = ""
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        return newMovieLoadResponse("HoneyDippedCream Video", url, TvType.NSFW, url)
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val videoUrl = document.selectFirst("video source[src*=_large.mp4], source[src*=_large.mp4]")?.attr("src")
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