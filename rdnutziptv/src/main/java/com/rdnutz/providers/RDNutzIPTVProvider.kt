package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class RDNutzIPTVProvider : MainAPI() {
    override var mainUrl = "https://iptv.rdnutz.us"
    override var name = "RDNutz IPTV"
    override val supportedTypes = setOf(TvType.Live)
    override var lang = "en"
    override val hasMainPage = true

    /**
     * Multi-byte XOR obfuscation key.  The two M3U source URLs are stored
     * as encrypted byte arrays in the compiled .cs3 — a plain `strings` dump
     * will never reveal the plaintext.  Decrypt at runtime before fetching.
     */
    private companion object {
        private val KEY = byteArrayOf(0x37.toByte(), 0xA5.toByte(), 0xCC.toByte(), 0x12.toByte())

        private fun encrypt(plain: String): ByteArray = plain.mapIndexed { i, c ->
            ((c.code xor KEY[i % KEY.size].toInt() and 0xFF) and 0xFF).toByte()
        }.toByteArray()

        private fun decrypt(data: ByteArray): String {
            val chars = CharArray(data.size)
            for (i in data.indices) {
                chars[i] = (((data[i].toInt() and 0xFF) xor KEY[i % KEY.size].toInt() and 0xFF) and 0xFF).toChar()
            }
            return String(chars)
        }

        // ── Encrypted M3U source URLs ──────────────────────────────
        private val ALL_RES = encrypt("https://iptv.rdnutz.us/All_Resources_Combined_By_Language.m3u")
        private val FAST    = encrypt("https://iptv.rdnutz.us/fast_combined.m3u")

        // ── In-memory channel cache (24 h TTL) ─────────────────────
        data class Channel(val title: String, val logo: String?, val group: String, val url: String)
        data class SourceCache(val channels: List<Channel>, val groups: Set<String>)
        private val CACHE = ConcurrentHashMap<String, Pair<Long, SourceCache>>()
        private const val TTL_MS = 24L * 60 * 60 * 1000

        suspend fun getOrFetch(sourceKey: String): SourceCache {
            val now = System.currentTimeMillis()
            val entry = CACHE[sourceKey]
            if (entry != null && now - entry.first < TTL_MS) return entry.second

            val url = decrypt(if (sourceKey == "allres") ALL_RES else FAST)
            val text = app.get(url).text
            val parsed = parseM3u(text)
            CACHE[sourceKey] = Pair(now, parsed)
            return parsed
        }

        private fun parseM3u(text: String): SourceCache {
            val channels = mutableListOf<Channel>()
            val groups = mutableSetOf<String>()
            val lines = text.lineSequence()
            val it = lines.iterator()
            while (it.hasNext()) {
                val line = it.next().trim()
                if (!line.startsWith("#EXTINF")) continue
                val logo = Regex("""tvg-logo="([^"]*)""").find(line)?.groupValues?.get(1)
                val group = Regex("""group-title="([^"]*)""").find(line)?.groupValues?.get(1) ?: "Uncategorized"
                val title = line.substringAfterLast(',').trim()
                val url = it.next().trim()
                if (url.isEmpty() || url.startsWith("#")) continue
                groups.add(group)
                channels.add(Channel(title, logo?.takeIf { it.isNotEmpty() }, group, url))
            }
            return SourceCache(channels, groups)
        }
    }

    override val mainPage = mainPageOf(
        "allres" to "All Resources (46K+)",
        "fast"   to "Fast Combined (650+)",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val cache = getOrFetch(request.data)
        val all = cache.channels
        val pageSize = 100
        val from = (page - 1) * pageSize
        if (from >= all.size) return newHomePageResponse(emptyList(), hasNext = false)

        val slice = all.slice(from until all.size.coerceAtMost(from + pageSize))
        val list = slice.map { ch ->
            newMovieSearchResponse(ch.title, jsonEncode(ch), TvType.Live) {
                posterUrl = ch.logo
            }
        }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = list,
                isHorizontalImages = false
            ),
            hasNext = from + pageSize < all.size
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val q = query.lowercase()
        val results = mutableListOf<SearchResponse>()
        for (key in listOf("allres", "fast")) {
            val cache = getOrFetch(key)
            results.addAll(cache.channels.filter { ch ->
                ch.title.contains(q, ignoreCase = true) ||
                ch.group.contains(q, ignoreCase = true)
            }.take(25).map { ch ->
                newMovieSearchResponse(ch.title, jsonEncode(ch), TvType.Live) {
                    posterUrl = ch.logo
                }
            })
        }
        return results.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse {
        val ch = jsonDecode(url)
        return newMovieLoadResponse(ch.title, url, TvType.Live, url) {
            posterUrl = ch.logo
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val ch = jsonDecode(data)
        newExtractorLink(name, "IPTV", ch.url, INFER_TYPE) { callback(this) }
        return true
    }

    // ── Channel JSON helpers ───────────────────────────────────
    private fun jsonEncode(ch: Channel): String = JSONObject().apply {
        put("title", ch.title)
        put("logo", ch.logo ?: "")
        put("group", ch.group)
        put("url", ch.url)
    }.toString()

    private fun jsonDecode(json: String): Channel {
        val o = JSONObject(json)
        return Channel(o.getString("title"), o.optString("logo", ""), o.getString("group"), o.getString("url"))
    }
}
