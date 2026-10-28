package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONArray
import org.json.JSONObject

class SportsReplaysProvider : MainAPI() {
    override var mainUrl = "https://watchprowrestling.store"
    override var name = "SportsReplays"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = true

    // ── Main page entries ──────────────────────────────────────────────────────
    override val mainPage = mainPageOf(
        "watchprowrestling|||mma" to "MMA (WatchProWrestling)",
        "watchprowrestling|||boxing" to "Boxing (WatchProWrestling)",
        "watchprowrestling|||wwe" to "WWE (WatchProWrestling)",
        "watchprowrestling|||aew" to "AEW (WatchProWrestling)",
        "allthebestfights" to "AllTheBestFights",
        "123wrestling" to "123Wrestling",
        "daddylive" to "DaddyLive Events",
    )

    // ── getMainPage ────────────────────────────────────────────────────────────
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val parts = request.name.split("|||")
        val source = parts[0]
        val payload = parts.getOrNull(1) ?: ""

        return when (source) {
            "watchprowrestling" -> getWatchProWrestlingMainPage(payload, page)
            "allthebestfights" -> getAllTheBestFightsMainPage(page)
            "123wrestling" -> get123WrestlingMainPage(page)
            "daddylive" -> getDaddyLiveMainPage()
            else -> newHomePageResponse(emptyList())
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  WATCHPROWRESTLING.STORE  (MMA / Boxing / WWE / AEW)
    // ═══════════════════════════════════════════════════════════════════════════
    private suspend fun getWatchProWrestlingMainPage(category: String, page: Int): HomePageResponse {
        // Pagination: site uses both ?paged=N and /page/N/
        val pageUrl = if (page == 1)
            "https://watchprowrestling.store/categorys/$category/"
        else
            "https://watchprowrestling.store/categorys/$category/?paged=$page"

        val document = app.get(pageUrl).document
        val html = document.html()

        // Extract article links: /watch-slug/
        val links = Regex("""href=["'](/watch-[^"']+?)["']""")
            .findAll(html)
            .map { "https://watchprowrestling.store$it.groupValues[1]" }
            .distinct()
            .take(30)
            .toList()

        val items = mutableListOf<SearchResponse>()
        for (url in links) {
            val doc = app.get(url).document
            val title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("title")?.text()?.trim()
                ?: doc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "${category.uppercase()} Replay"
            val poster = fixUrlNull(doc.selectFirst("meta[property='og:image']")?.attr("content"))
            items.add(newMovieSearchResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
            })
        }

        return newHomePageResponse(
            list = HomePageList(name = "${category.uppercase()} (WatchProWrestling)", list = items, isHorizontalImages = true),
            hasNext = items.size >= 15
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ALLTHEBESTFIGHTS.COM  (MMA / Boxing)
    // ═══════════════════════════════════════════════════════════════════════════
    private suspend fun getAllTheBestFightsMainPage(page: Int): HomePageResponse {
        val pageUrl = if (page == 1)
            "https://www.allthebestfights.com/"
        else
            "https://www.allthebestfights.com/page/$page/"

        val document = app.get(pageUrl).document
        val html = document.html()

        // Extract article links from <article> tags
        val articleMatches = Regex("""<article[^>]*>.*?href=["']([^"']*allthebestfights\.com/[^"']+)["'][^>]*>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(html)
            .mapNotNull { match ->
                val href = match.groupValues[1]
                if (!href.contains("/page/") && !href.contains("/category/") &&
                    !href.contains("/author/") && href.contains("/full-fight-video/")) href
                else null
            }
            .distinct()
            .take(30)
            .toList()

        val items = mutableListOf<SearchResponse>()
        for (url in articleMatches) {
            val doc = app.get(url).document
            val title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("title")?.text()?.trim()
                ?: doc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "Fight Replay"
            val poster = fixUrlNull(doc.selectFirst("meta[property='og:image']")?.attr("content"))
            items.add(newMovieSearchResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
            })
        }

        return newHomePageResponse(
            list = HomePageList(name = "AllTheBestFights", list = items, isHorizontalImages = true),
            hasNext = items.size >= 15
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DADDYLIVE.APP API  (Live events)
    // ═══════════════════════════════════════════════════════════════════════════
    private suspend fun getDaddyLiveMainPage(): HomePageResponse {
        val streams = mutableListOf<SearchResponse>()
        try {
            val response = app.get("https://daddylive.app/api/events")
            val body = response.body.string()
            val json = JSONObject(body)
            val categories = json.optJSONObject("categories") ?: JSONObject()
            val keys = categories.names()
            if (keys != null) {
                for (i in 0 until keys.length()) {
                    val catName = keys.getString(i)
                    val events = categories.optJSONArray(catName) ?: continue
                    for (j in 0 until events.length()) {
                        val ev = events.getJSONObject(j)
                        val eventName = ev.optString("event", "").trim()
                        val channels = ev.optJSONArray("channels") ?: continue
                        for (k in 0 until channels.length()) {
                            val ch = channels.getJSONObject(k)
                            val chUrl = ch.optString("url", "").trim()
                            if (chUrl.isEmpty()) continue
                            streams.add(newMovieSearchResponse(eventName, chUrl, TvType.Movie))
                        }
                        if (streams.size >= 80) break
                    }
                    if (streams.size >= 80) break
                }
            }
        } catch (e: Exception) {
            println("SportsReplays[daddylive] error: ${e.message}")
        }

        return newHomePageResponse(
            list = HomePageList(name = "DaddyLive", list = streams, isHorizontalImages = false),
            hasNext = false
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  123WRESTLING.COM
    // ═══════════════════════════════════════════════════════════════════════════
    private suspend fun get123WrestlingMainPage(page: Int): HomePageResponse {
        val pageUrl = if (page == 1)
            "https://123wrestling.com/"
        else
            "https://123wrestling.com/page/$page/"

        val document = app.get(pageUrl).document
        val html = document.html()

        // Extract article links: /watch-slug/
        val links = Regex("""href=["'](https://123wrestling\.com/watch-[^"']+?)["']""")
            .findAll(html)
            .map { it.groupValues[1] }
            .distinct()
            .take(30)
            .toList()

        val items = mutableListOf<SearchResponse>()
        for (url in links) {
            val doc = app.get(url).document
            val title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("title")?.text()?.trim()
                ?: doc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "Wrestling Replay"
            val poster = fixUrlNull(doc.selectFirst("meta[property='og:image']")?.attr("content"))
            items.add(newMovieSearchResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
            })
        }

        return newHomePageResponse(
            list = HomePageList(name = "123Wrestling", list = items, isHorizontalImages = true),
            hasNext = items.size >= 15
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SEARCH
    // ═══════════════════════════════════════════════════════════════════════════
    override suspend fun search(query: String): List<SearchResponse> {
        val all = mutableListOf<SearchResponse>()

        // Search watchprowrestling.store
        try {
            val doc = app.get("https://watchprowrestling.store/?s=${query.replace(" ", "+")}").document
            val html = doc.html()
            val links = Regex("""href=["'](https://watchprowrestling\.store/watch-[^"']+?)["']""")
                .findAll(html)
                .map { it.groupValues[1] }
                .distinct()
                .take(20)
                .toList()
            for (url in links) {
                val doc2 = app.get(url).document
                val title = doc2.selectFirst("h1")?.text()?.trim() ?: doc2.selectFirst("title")?.text()?.trim() ?: continue
                if (title.lowercase().contains(query.lowercase())) {
                    val poster = fixUrlNull(doc2.selectFirst("meta[property='og:image']")?.attr("content"))
                    all.add(newMovieSearchResponse(title, url, TvType.Movie) {
                        if (poster != null) this.posterUrl = poster
                    })
                }
            }
        } catch (_: Exception) {}

        // Search allthebestfights.com
        try {
            val doc = app.get("https://www.allthebestfights.com/?s=${query.replace(" ", "+")}").document
            val html = doc.html()
            val links = Regex("""href=["'](https://www\.allthebestfights\.com/[^"']*full-fight-video[^"']+)["']""")
                .findAll(html)
                .map { it.groupValues[1] }
                .distinct()
                .take(20)
                .toList()
            for (url in links) {
                val doc2 = app.get(url).document
                val title = doc2.selectFirst("h1")?.text()?.trim() ?: doc2.selectFirst("title")?.text()?.trim() ?: continue
                if (title.lowercase().contains(query.lowercase())) {
                    val poster = fixUrlNull(doc2.selectFirst("meta[property='og:image']")?.attr("content"))
                    all.add(newMovieSearchResponse(title, url, TvType.Movie) {
                        if (poster != null) this.posterUrl = poster
                    })
                }
            }
        } catch (_: Exception) {}

        // Search 123wrestling.com
        try {
            val doc = app.get("https://123wrestling.com/?s=${query.replace(" ", "+")}").document
            val html = doc.html()
            val links = Regex("""href=["'](https://123wrestling\.com/watch-[^"']+?)["']""")
                .findAll(html)
                .map { it.groupValues[1] }
                .distinct()
                .take(20)
                .toList()
            for (url in links) {
                val doc2 = app.get(url).document
                val title = doc2.selectFirst("h1")?.text()?.trim() ?: doc2.selectFirst("title")?.text()?.trim() ?: continue
                if (title.lowercase().contains(query.lowercase())) {
                    val poster = fixUrlNull(doc2.selectFirst("meta[property='og:image']")?.attr("content"))
                    all.add(newMovieSearchResponse(title, url, TvType.Movie) {
                        if (poster != null) this.posterUrl = poster
                    })
                }
            }
        } catch (_: Exception) {}

        return all.distinctBy { it.url }.take(50)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LOAD (episode/detail page)
    // ═══════════════════════════════════════════════════════════════════════════
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()?.trim()
            ?: document.selectFirst("meta[property='og:title']")?.attr("content")
            ?: "Sports Replay"
        val poster = fixUrlNull(document.selectFirst("meta[property='og:image']")?.attr("content"))

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LOAD LINKS (extract playable video links)
    // ═══════════════════════════════════════════════════════════════════════════
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var found = false

        // WatchProWrestling / AllTheBestFights / 123Wrestling — extract YouTube embeds
        if (data.contains("watchprowrestling.store") ||
            data.contains("allthebestfights.com") ||
            data.contains("123wrestling.com")) {
            found = loadVideoEmbedLinks(data, callback) || found
        }

        // DaddyLive — resolve iframe → route to appropriate handler
        if (data.contains("daddylive.app/player/embed.php")) {
            found = loadDaddyLiveLinks(data, callback) || found
        }

        return found
    }

    // ── Wrestling/fight sites: extract YouTube embeds ───────────────────────────
    private suspend fun loadVideoEmbedLinks(url: String, callback: (ExtractorLink) -> Unit): Boolean {
        try {
            val document = app.get(url).document
            val html = document.html()

            // YouTube embed: youtube.com/embed/{id} or youtu.be/{id}
            val ytIds = Regex("""(?:youtube\.com/embed/|youtu\.be/)([a-zA-Z0-9_-]+)""")
                .findAll(html)
                .map { it.groupValues[1] }
                .toList()
                .distinct()

            for (id in ytIds) {
                newExtractorLink(name, "YouTube", "https://www.youtube.com/embed/$id", INFER_TYPE) {
                    callback(this)
                }
            }

            return ytIds.isNotEmpty()
        } catch (e: Exception) {
            println("SportsReplays[video_embed] error: ${e.message}")
            return false
        }
    }

    // ── DaddyLive embed resolver ────────────────────────────────────────────────
    private suspend fun loadDaddyLiveLinks(daddyliveUrl: String, callback: (ExtractorLink) -> Unit): Boolean {
        try {
            val embedDoc = app.get(daddyliveUrl).document
            val html = embedDoc.html()

            // Extract inner iframe src
            val iframeMatch = Regex("""<iframe[^>]+src=["']([^"']+)["']""").find(html)
            val innerUrl = iframeMatch?.groupValues?.get(1)
                ?: Regex("""src=["']([^"']*embed["'][^"']*)["']""").find(html)?.groupValues?.get(1)
                ?: return false

            val resolvedUrl = if (innerUrl.startsWith("//")) "https:$innerUrl" else innerUrl

            // Route to appropriate handler based on the resolved URL
            return when {
                resolvedUrl.contains("ok.ru/videoembed/") -> {
                    val okId = Regex("""ok\.ru/videoembed/(\d+)""").find(resolvedUrl)?.groupValues?.get(1)
                    if (okId != null) {
                        newExtractorLink(name, "OK.ru", "https://ok.ru/videoembed/$okId", INFER_TYPE) {
                            callback(this)
                        }
                        true
                    } else false
                }
                resolvedUrl.contains("youtube.com/embed/") || resolvedUrl.contains("youtu.be/") -> {
                    val ytId = Regex("""(?:youtube\.com/embed/|youtu\.be/)([a-zA-Z0-9_-]+)""")
                        .find(resolvedUrl)?.groupValues?.get(1)
                    if (ytId != null) {
                        newExtractorLink(name, "YouTube", "https://www.youtube.com/embed/$ytId", INFER_TYPE) {
                            callback(this)
                        }
                        true
                    } else false
                }
                else -> {
                    newExtractorLink(name, "DaddyLive", resolvedUrl, INFER_TYPE) {
                        this.referer = daddyliveUrl
                        callback(this)
                    }
                    true
                }
            }
        } catch (e: Exception) {
            println("SportsReplays[daddylive] error: ${e.message}")
            return false
        }
    }
}
