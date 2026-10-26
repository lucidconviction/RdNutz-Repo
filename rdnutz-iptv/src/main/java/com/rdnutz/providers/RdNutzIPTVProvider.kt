package com.rdnutz.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class RdNutzIPTVProvider : MainAPI() {
    override var mainUrl = "https://iptv.rdnutz.us/All_Resources_Combined_By_Language.m3u"
    override var name = "RdNutz IPTV"
    override val supportedTypes = setOf(TvType.Live)
    override var lang = "en"
    override val hasMainPage = true

    // Main region groups extracted from the M3U
    private val regionGroups = listOf(
        "UK" to "United Kingdom",
        "US" to "United States",
        "CA" to "Canada",
        "FR" to "France",
        "DE" to "Germany",
        "ES" to "Spain",
        "IT" to "Italy",
        "PT" to "Portugal",
        "NL" to "Netherlands",
        "ARAB" to "Arabic",
        "ASIA" to "Asia",
        "EXYU" to "Ex-Yugoslavia",
        "LAME" to "Latin America",
        "OCEA" to "Oceania",
        "PPV" to "PPV Events",
        "24/7" to "24/7 Channels",
        "VIP" to "VIP Channels",
        "CINE" to "Cinema",
        "SPORT" to "Sports",
        "KIDS" to "Kids",
        "NEWS" to "News",
        "MUSIC" to "Music",
        "DOCU" to "Documentary",
    )

    override val mainPage = mainPageOf(
        "all" to "All Channels",
        "UK" to "United Kingdom",
        "US" to "United States",
        "CA" to "Canada",
        "FR" to "France",
        "DE" to "Germany",
        "ES" to "Spain",
        "IT" to "Italy",
        "PT" to "Portugal",
        "NL" to "Netherlands",
        "ARAB" to "Arabic",
        "ASIA" to "Asia",
        "EXYU" to "Ex-Yugoslavia",
        "LAME" to "Latin America",
        "OCEA" to "Oceania",
        "PPV" to "PPV Events",
        "247" to "24/7 Channels",
        "VIP" to "VIP Channels",
        "CINE" to "Cinema/Movies",
        "SPORT" to "Sports",
        "KIDS" to "Kids",
        "NEWS" to "News",
        "MUSIC" to "Music",
        "DOCU" to "Documentary",
    )

    private suspend fun parseM3U(): List<M3UChannel> {
        return try {
            val response = app.get(mainUrl, headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ))
            val text = response.text
            val channels = mutableListOf<M3UChannel>()
            val lines = text.lines().toList()
            
            var currentTitle = ""
            var currentGroup = ""
            var currentLogo = ""
            var currentTvgId = ""
            
            for (i in 0 until lines.size) {
                val line = lines[i]
                if (line.startsWith("#EXTINF:")) {
                    val extinf = line.substring(8)
                    val commaIdx = extinf.lastIndexOf(',')
                    if (commaIdx > 0) {
                        currentTitle = extinf.substring(commaIdx + 1).trim()
                        val attrs = extinf.substring(0, commaIdx)
                        
                        val groupMatch = Regex("""group-title="([^"]+)"""").find(attrs)
                        currentGroup = groupMatch?.groupValues?.get(1) ?: ""
                        
                        val logoMatch = Regex("""tvg-logo="([^"]+)"""").find(attrs)
                        currentLogo = logoMatch?.groupValues?.get(1) ?: ""
                        
                        val idMatch = Regex("""tvg-id="([^"]+)"""").find(attrs)
                        currentTvgId = idMatch?.groupValues?.get(1) ?: ""
                    }
                } else if (line.startsWith("http") && currentTitle.isNotEmpty()) {
                    val url = line.trim()
                    channels.add(M3UChannel(
                        title = currentTitle,
                        group = currentGroup,
                        logo = currentLogo,
                        url = url,
                        tvgId = currentTvgId
                    ))
                    currentTitle = ""
                    currentGroup = ""
                    currentLogo = ""
                    currentTvgId = ""
                }
            }
            channels
        } catch (e: Exception) {
            emptyList()
        }
    }

    private data class M3UChannel(
        val title: String,
        val group: String,
        val logo: String,
        val url: String,
        val tvgId: String
    )

    private fun matchesGroup(channelGroup: String, filter: String): Boolean {
        val g = channelGroup.uppercase()
        val f = filter.uppercase()
        return when (f) {
            "ALL" -> true
            "UK" -> g.contains("UK") || g.contains("ENGLAND")
            "US" -> g.contains("US") || g.contains("USA") || g.contains("AMERICA")
            "CA" -> g.contains("CANADA") || g.contains("CA |")
            "FR" -> g.contains("FR |") || g.contains("FRANCE")
            "DE" -> g.contains("DE |") || g.contains("GERMAN")
            "ES" -> g.contains("ES |") || g.contains("SPAIN")
            "IT" -> g.contains("IT |") || g.contains("ITALY")
            "PT" -> g.contains("PT |") || g.contains("PORTUGAL")
            "NL" -> g.contains("NL |") || g.contains("NETHERLANDS")
            "ARAB" -> g.contains("ARAB")
            "ASIA" -> g.contains("ASIA") || g.contains("INDIA") || g.contains("PAKISTAN") || g.contains("JAPAN") || g.contains("MALAYSIA") || g.contains("INDONESIA") || g.contains("IRAN") || g.contains("KURDISTAN") || g.contains("ARMENIA") || g.contains("TAIWAN")
            "EXYU" -> g.contains("EXYU") || g.contains("BIH") || g.contains("SRB") || g.contains("DECA")
            "LAME" -> g.contains("LAME") || g.contains("LATINO") || g.contains("BRAZIL") || g.contains("ARGENTINA") || g.contains("CHILE") || g.contains("COLOMBIA") || g.contains("MEXICO") || g.contains("CARIBBEAN")
            "OCEA" -> g.contains("OCEA") || g.contains("NEWZEALAND") || g.contains("AUSTRALIA")
            "PPV" -> g.contains("PPV")
            "247" -> g.contains("24/7") || g.contains("247")
            "VIP" -> g.contains("VIP")
            "CINE" -> g.contains("CINE") || g.contains("MOVIE") || g.contains("FILM")
            "SPORT" -> g.contains("SPORT") || g.contains("DEPORTES") || g.contains("ESPORTES") || g.contains("SPORT") || g.contains("BALONCESTO") || g.contains("LALIGA") || g.contains("PREMIER") || g.contains("BUNDESLIGA")
            "KIDS" -> g.contains("KIDS") || g.contains("CHILDREN") || g.contains("CRIANÇAS")
            "NEWS" -> g.contains("NEWS") || g.contains("NOTÍCIAS") || g.contains("INFORMATION") || g.contains("INFORMATION")
            "MUSIC" -> g.contains("MUSIC") || g.contains("MÚSICA") || g.contains("MUSIQUE")
            "DOCU" -> g.contains("DOCU") || g.contains("DOCUMENTARY") || g.contains("DOCUMENTÁRIO")
            else -> channelGroup.uppercase().contains(f)
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val allChannels = parseM3U()
        
        val filteredChannels = allChannels.filter { channel ->
            matchesGroup(channel.group, request.name)
        }
        
        val start = (page - 1) * 100
        val end = minOf(start + 100, filteredChannels.size)
        val pageChannels = if (start < filteredChannels.size) filteredChannels.subList(start, end) else emptyList()

        val home = pageChannels.map { channel ->
            newMovieSearchResponse(channel.title, channel.url, TvType.Live) {
                this.posterUrl = channel.logo
            }
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = end < filteredChannels.size
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val allChannels = parseM3U()
        val q = query.uppercase()
        
        return allChannels
            .filter { it.title.uppercase().contains(q) || it.group.uppercase().contains(q) }
            .take(50)
            .map { channel ->
                newMovieSearchResponse(channel.title, channel.url, TvType.Live) {
                    this.posterUrl = channel.logo
                }
            }
    }

    override suspend fun load(url: String): LoadResponse {
        return newMovieLoadResponse("IPTV Stream", url, TvType.Live, url)
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        if (data.startsWith("http")) {
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