package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONArray
import org.json.JSONObject

class SportsReplaysProvider : MainAPI() {
    override var mainUrl = "https://basketball-video.com"
    override var name = "SportsReplays"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = true

    // ── Main page entries ──────────────────────────────────────────────────────
    override val mainPage = mainPageOf(
        "basketball-video|||2025-26" to "NBA (BasketballVideo)",
        "basketball-video|||wnba-video" to "WNBA (BasketballVideo)",
        "basketball-video|||college-basketball" to "NCAA (BasketballVideo)",
        "nfl-video" to "NFL Replays",
        "mlb-live" to "MLB Replays",
        "watchprowrestling|||mma" to "MMA (WatchProWrestling)",
        "watchprowrestling|||boxing" to "Boxing (WatchProWrestling)",
        "watchprowrestling|||wwe" to "WWE (WatchProWrestling)",
        "watchprowrestling|||aew" to "AEW (WatchProWrestling)",
        "allthebestfights" to "AllTheBestFights",
        "daddylive" to "DaddyLive Events",
        "123wrestling" to "123Wrestling",
    )

    // ── getMainPage ────────────────────────────────────────────────────────────
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val parts = request.name.split("|||")
        val source = parts[0]
        val payload = parts.getOrNull(1) ?: ""

        return when (source) {
            "basketball-video" -> {
                val catLabel = when (payload) {
                    "2025-26" -> "NBA"
                    "wnba-video" -> "WNBA"
                    "college-basketball" -> "NCAA"
                    else -> "Basketball"
                }
                getBasketballVideoMainPage(payload, page, catLabel)
            }
            "nfl-video" -> getNFLVideoMainPage(page)
            "mlb-live" -> getMLBLiveMainPage(page)
            "watchprowrestling" -> getWatchProWrestlingMainPage(payload, page)
            "allthebestfights" -> getAllTheBestFightsMainPage(page)
            "daddylive" -> getDaddyLiveMainPage()
            "123wrestling" -> get123WrestlingMainPage(page)
            else -> newHomePageResponse(emptyList())
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  BASKETBALLVIDEO.COM  (NBA / WNBA / NCAA replays)
    // ═══════════════════════════════════════════════════════════════════════════
    private suspend fun getBasketballVideoMainPage(category: String, page: Int, label: String): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl/$category" else "$mainUrl/$category/page/$page"
        val document = app.get(pageUrl).document

        val videoLinks = document.select("a[href]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.startsWith(mainUrl) &&
                    href.contains("/videos/") &&
                    Regex("""\d+$""").containsMatchIn(href)) href
                else null
            }
            .distinct()
            .take(30)

        val items = videoLinks.mapNotNull { url ->
            val doc = app.get(url).document
            val title = doc.selectFirst("title")?.text()?.trim()
                ?: doc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: label + " Replay"
            val poster = fixUrlNull(doc.selectFirst("meta[property='og:image']")?.attr("content"))
            newMovieSearchResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
            }
        }

        return newHomePageResponse(
            list = HomePageList(name = label, list = items, isHorizontalImages = true),
            hasNext = items.size >= 15
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  NFL-VIDEO.COM
    // ═══════════════════════════════════════════════════════════════════════════
    private suspend fun getNFLVideoMainPage(page: Int): HomePageResponse {
        val pageUrl = if (page == 1) "https://nfl-video.com/2026/" else "https://nfl-video.com/2026/page/$page/"
        val document = app.get(pageUrl).document

        val postLinks = document.select("a[href]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.startsWith("https://nfl-video.com") &&
                    !href.contains("/page/") && !href.contains("/css") && !href.contains("/js") &&
                    href.split("/").last().length > 5) href
                else null
            }
            .distinct()
            .take(30)

        val items = postLinks.mapNotNull { url ->
            val doc = app.get(url).document
            val title = doc.selectFirst("title")?.text()?.trim()
                ?: doc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "NFL Replay"
            val poster = fixUrlNull(doc.selectFirst("meta[property='og:image']")?.attr("content"))
            newMovieSearchResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
            }
        }

        return newHomePageResponse(
            list = HomePageList(name = "NFL Replays", list = items, isHorizontalImages = true),
            hasNext = items.size >= 15
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  MLBLIVE.NET  (MLB full game replays)
    // ═══════════════════════════════════════════════════════════════════════════
    private suspend fun getMLBLiveMainPage(page: Int): HomePageResponse {
        val pageUrl = if (page == 1) "https://mlblive.net/2026-mlb-full-game-replays/" else "https://mlblive.net/2026-mlb-full-game-replays/page/$page/"
        val document = app.get(pageUrl).document

        val postLinks = document.select("a[href]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.startsWith("https://mlblive.net") &&
                    !href.contains("/page/") && !href.contains("/css") && !href.contains("/js") &&
                    href.split("/").last().length > 5) href
                else null
            }
            .distinct()
            .take(30)

        val items = postLinks.mapNotNull { url ->
            val doc = app.get(url).document
            val title = doc.selectFirst("title")?.text()?.trim()
                ?: doc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "MLB Replay"
            val poster = fixUrlNull(doc.selectFirst("meta[property='og:image']")?.attr("content"))
            newMovieSearchResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
            }
        }

        return newHomePageResponse(
            list = HomePageList(name = "MLB Replays", list = items, isHorizontalImages = true),
            hasNext = items.size >= 15
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  WATCHPROWRESTLING.STORE  (MMA / Boxing / Wrestling)
    // ═══════════════════════════════════════════════════════════════════════════
    private suspend fun getWatchProWrestlingMainPage(category: String, page: Int): HomePageResponse {
        val catPath = when (category) {
            "mma" -> "mma"
            "boxing" -> "boxing"
            "wwe" -> "wwe"
            "aew" -> "aew"
            else -> category
        }
        val pageUrl = if (page == 1)
            "https://watchprowrestling.store/categorys/$catPath/"
        else
            "https://watchprowrestling.store/categorys/$catPath/page/$page/"

        val document = app.get(pageUrl).document

        val postLinks = document.select("a[href]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.startsWith("https://watchprowrestling.store") &&
                    !href.contains("/categorys/") && !href.contains("/page/") &&
                    !href.contains("/css") && !href.contains("/js") &&
                    href.split("/").last().length > 5) href
                else null
            }
            .distinct()
            .take(30)

        val items = postLinks.mapNotNull { url ->
            val doc = app.get(url).document
            val title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("title")?.text()?.trim()
                ?: doc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "${category.uppercase()} Replay"
            val poster = fixUrlNull(doc.selectFirst("meta[property='og:image']")?.attr("content"))
            newMovieSearchResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
            }
        }

        return newHomePageResponse(
            list = HomePageList(name = "${category.uppercase()} (WatchProWrestling)", list = items, isHorizontalImages = true),
            hasNext = items.size >= 15
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ALLTHEBESTFIGHTS.COM  (MMA / Boxing / Kickboxing)
    // ═══════════════════════════════════════════════════════════════════════════
    private suspend fun getAllTheBestFightsMainPage(page: Int): HomePageResponse {
        val pageUrl = if (page == 1)
            "https://www.allthebestfights.com/"
        else
            "https://www.allthebestfights.com/page/$page/"

        val document = app.get(pageUrl).document

        val postLinks = document.select("a[href]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.startsWith("https://www.allthebestfights.com") &&
                    !href.contains("/page/") && !href.contains("/category/") &&
                    !href.contains("/css") && !href.contains("/js") &&
                    href.split("/").last().length > 5) href
                else null
            }
            .distinct()
            .take(30)

        val items = postLinks.mapNotNull { url ->
            val doc = app.get(url).document
            val title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("title")?.text()?.trim()
                ?: doc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "Fight Replay"
            val poster = fixUrlNull(doc.selectFirst("meta[property='og:image']")?.attr("content"))
            newMovieSearchResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
            }
        }

        return newHomePageResponse(
            list = HomePageList(name = "AllTheBestFights", list = items, isHorizontalImages = true),
            hasNext = items.size >= 15
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DADDYLIVE.APP API  (Live events — Basketball, American Football, etc.)
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

        val postLinks = document.select("a[href]")
            .mapNotNull { el ->
                val href = fixUrlNull(el.attr("href"))
                if (href != null && href.startsWith("https://123wrestling.com") &&
                    !href.contains("/page/") && !href.contains("/category/") &&
                    !href.contains("/tag/") && !href.contains("/css") && !href.contains("/js") &&
                    href.split("/").last().length > 5) href
                else null
            }
            .distinct()
            .take(30)

        val items = postLinks.mapNotNull { url ->
            val doc = app.get(url).document
            val title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("title")?.text()?.trim()
                ?: doc.selectFirst("meta[property='og:title']")?.attr("content")
                ?: "Wrestling Replay"
            val poster = fixUrlNull(doc.selectFirst("meta[property='og:image']")?.attr("content"))
            newMovieSearchResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
            }
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

        // Search basketball-video.com
        try {
            for (cat in listOf("2025-26", "wnba-video", "college-basketball")) {
                val doc = app.get("$mainUrl/$cat").document
                val links = doc.select("a[href]")
                    .mapNotNull { el ->
                        val href = fixUrlNull(el.attr("href"))
                        if (href != null && href.startsWith(mainUrl) &&
                            href.contains("/videos/") &&
                            Regex("""\d+$""").containsMatchIn(href)) href
                        else null
                    }
                    .distinct()
                    .take(20)
                for (url in links) {
                    val doc2 = app.get(url).document
                    val title = doc2.selectFirst("title")?.text()?.trim() ?: continue
                    if (title.lowercase().contains(query.lowercase())) {
                        val poster = fixUrlNull(doc2.selectFirst("meta[property='og:image']")?.attr("content"))
                        all.add(newMovieSearchResponse(title, url, TvType.Movie) {
                            if (poster != null) this.posterUrl = poster
                        })
                    }
                }
            }
        } catch (_: Exception) {}

        // Search nfl-video.com
        try {
            val doc = app.get("https://nfl-video.com/?s=${query.replace(" ", "+")}").document
            val links = doc.select("a[href]")
                .mapNotNull { el ->
                    val href = fixUrlNull(el.attr("href"))
                    if (href != null && href.startsWith("https://nfl-video.com") &&
                        !href.contains("/page/") && !href.contains("/css") &&
                        href.split("/").last().length > 5) href
                    else null
                }
                .distinct()
                .take(20)
            for (url in links) {
                val doc2 = app.get(url).document
                val title = doc2.selectFirst("title")?.text()?.trim() ?: continue
                if (title.lowercase().contains(query.lowercase())) {
                    val poster = fixUrlNull(doc2.selectFirst("meta[property='og:image']")?.attr("content"))
                    all.add(newMovieSearchResponse(title, url, TvType.Movie) {
                        if (poster != null) this.posterUrl = poster
                    })
                }
            }
        } catch (_: Exception) {}

        // Search mlblive.net
        try {
            val doc = app.get("https://mlblive.net/?s=${query.replace(" ", "+")}").document
            val links = doc.select("a[href]")
                .mapNotNull { el ->
                    val href = fixUrlNull(el.attr("href"))
                    if (href != null && href.startsWith("https://mlblive.net") &&
                        !href.contains("/page/") && !href.contains("/css") &&
                        href.split("/").last().length > 5) href
                    else null
                }
                .distinct()
                .take(20)
            for (url in links) {
                val doc2 = app.get(url).document
                val title = doc2.selectFirst("title")?.text()?.trim() ?: continue
                if (title.lowercase().contains(query.lowercase())) {
                    val poster = fixUrlNull(doc2.selectFirst("meta[property='og:image']")?.attr("content"))
                    all.add(newMovieSearchResponse(title, url, TvType.Movie) {
                        if (poster != null) this.posterUrl = poster
                    })
                }
            }
        } catch (_: Exception) {}

        // Search watchprowrestling.store
        try {
            val doc = app.get("https://watchprowrestling.store/?s=${query.replace(" ", "+")}").document
            val links = doc.select("a[href]")
                .mapNotNull { el ->
                    val href = fixUrlNull(el.attr("href"))
                    if (href != null && href.startsWith("https://watchprowrestling.store") &&
                        !href.contains("/categorys/") && !href.contains("/page/") &&
                        !href.contains("/css") && !href.contains("/js") &&
                        href.split("/").last().length > 5) href
                    else null
                }
                .distinct()
                .take(20)
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
            val links = doc.select("a[href]")
                .mapNotNull { el ->
                    val href = fixUrlNull(el.attr("href"))
                    if (href != null && href.startsWith("https://www.allthebestfights.com") &&
                        !href.contains("/page/") && !href.contains("/category/") &&
                        !href.contains("/css") && !href.contains("/js") &&
                        href.split("/").last().length > 5) href
                    else null
                }
                .distinct()
                .take(20)
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

        // BasketballVideo / NFL-Video / MLBLive — extract OK.ru embeds or direct embeds
        if (data.contains("basketball-video.com") || data.contains("nfl-video.com") || data.contains("mlblive.net")) {
            found = loadBlogEmbedLinks(data, callback) || found
        }

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

    // ── Blog-style sites: extract OK.ru / generic embeds ────────────────────────
    private suspend fun loadBlogEmbedLinks(url: String, callback: (ExtractorLink) -> Unit): Boolean {
        try {
            val document = app.get(url).document
            val html = document.html()

            // OK.ru embed: ok.ru/videoembed/{id}
            val okIds = Regex("""ok\.ru/videoembed/(\d+)""")
                .findAll(html)
                .map { it.groupValues[1] }
                .toList()
                .distinct()

            for (id in okIds) {
                newExtractorLink(name, "OK.ru", "https://ok.ru/videoembed/$id", INFER_TYPE) {
                    callback(this)
                }
            }

            // Generic iframe src extraction
            val iframes = Regex("""<iframe[^>]+src=["']([^"']+)["']""")
                .findAll(html)
                .map { it.groupValues[1] }
                .toList()
                .distinct()

            for (iframeUrl in iframes) {
                val resolved = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl
                if (resolved.contains("ok.ru")) continue // already handled above
                newExtractorLink(name, "Embed", resolved, INFER_TYPE) {
                    this.referer = url
                    callback(this)
                }
            }

            return okIds.isNotEmpty() || iframes.isNotEmpty()
        } catch (e: Exception) {
            println("SportsReplays[blog_embed] error: ${e.message}")
            return false
        }
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
