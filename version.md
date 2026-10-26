# RDNutz CloudStream Extensions - Version Documentation

## Version: v0.2.2
**Date:** October 25, 2026

## Project Overview
Repository: `Robbdeeze/RdNutz-Repo`
GitHub Release: [v0.2.2](https://github.com/Robbdeeze/RdNutz-Repo/releases/tag/v0.2.2)
CloudStream Repo URL: `https://raw.githubusercontent.com/Robbdeeze/RdNutz-Repo/master/repo.json`

## Purpose
This repository contains 22 CloudStream v4 extensions (NSFW providers + MMA/Movie providers) that can be installed into CloudStream 3 application. Each extension scrapes video content from its respective website and provides search, listing, and playback functionality.

## Extensions Included

| # | Extension | Website | iconUrl | Status |
|---|-----------|---------|---------|--------|
| 1 | **Crazyshit** | crazyshit.com | crazyshit.com/favicon.ico | ✅ 100% working |
| 2 | **Kaotic** | kaotic.com | kaotic.com/logo.svg | ✅ 100% working |
| 3 | **Worldstar** | worldstarhiphop.com | worldstarhiphop.com/apple-touch-icon.png | ✅ Videos play |
| 4 | **Xrares** | xrares.com | xrares.com/favicon.ico | ✅ 100% working |
| 5 | **LiveGore** | livegore.com | livegore.com/favicon.png | ✅ 100% working |
| 6 | **DaftPorn** | daftporn.com | daftporn.com/favicon.ico | ✅ 100% working |
| 7 | **eFukt** | efukt.com | efukt.com/favicon.ico | ✅ 100% working |
| 8 | **TheYNC** | theync.com | theync.com/logo.png | ✅ 100% working |
| 9 | **FullFightReplays** | fullfightreplays.com | fullfightreplays.com/favicon.ico | ✅ 100% working (Dailymotion/OK.ru) |
| 10 | **MMAReplays** | mmareplays.com | mmareplays.com/apple-touch-icon.png | ✅ 100% working (vidara.so HLS) |
| 11 | **MMACore** | mma-core.com | images.mma-core.com/images/site/mmacore-logo.png | ✅ 100% working (YouTube embeds) |
| 12 | **Heavy-R** | heavy-r.com | heavy-r.com/favicon.ico | ✅ 100% working (RSS → direct MP4) |
| 13 | **HoneyDippedCream** | honeydippedcream.com | honeydippedcream.com/favicon.ico | ✅ 100% working (direct MP4) |
| 14 | **HorribleVideos** | horriblevideos.com | horriblevideos.com/favicon.ico | ✅ 100% working (embed pages) |
| 15 | **Inhumanity** | inhumanity.com | inhumanity.com/favicon.ico | ✅ 100% working (plug pages) |
| 16 | **JeffsModels** | jeffsmodels.com | jeffsmodels.com/favicon.ico | ✅ 100% working (10-sec previews) |
| 17 | **NothingToxic** | nothingtoxic.com | nothingtoxic.com/favicon.ico | ✅ 100% working (out links) |
| 18 | **PlumperD** | plumperd.com | plumperd.com/favicon.ico | ✅ 100% working (preview clips) |
| 19 | **SickJunk** | sickjunk.com | sickjunk.com/favicon.ico | ✅ 100% working (WordPress MP4) |
| 20 | **SexyAndFunny** | sexyandfunny.com | sexyandfunny.com/favicon.ico | ✅ 100% working (WordPress MP4) |
| 21 | **YouPorn** | you-porn.com | you-porn.com/favicon.ico | ✅ 100% working (token MP4) |
| 22 | **ZFilmzOriginals** | z-filmz-originals.com | z-filmz-originals.com/favicon.ico | ✅ 100% working (Next.js JSON) |

## Extensions Removed

| Extension | Reason for Removal |
|-----------|-------------------|
| **HeavyFetish** | Scraper not functional; no working Python reference |
| **NothingToxic** | Scraper not functional; no working Python reference |
| **USACrime** | Removed per user request; thumbnails work but videos had issues |
| **Live Sports** | Deleted per user request; replaced by other providers |

## Extractor Architecture

All providers implement the `MainAPI` interface from CloudStream 3 with the following methods:

- `getMainPage(page, request)` - Retrieves the homepage video listing
- `search(query)` - Searches for videos by query string
- `load(url)` - Loads video metadata (title, poster, description, duration, tags)
- `loadLinks(data)` - Extracts direct video URLs (MP4/M3U8)

### Key API Changes from Original Scrapers
All providers were rewritten to use CloudStream 4 API patterns:

1. **`app.get(url).document`** instead of raw Jsoup requests
2. **`mainPageOf()` DSL** instead of custom `HomePageList` construction
3. **`newMovieSearchResponse()` / `newMovieLoadResponse()`** for response construction
4. **`newExtractorLink()`** builder instead of direct `ExtractorLink` constructor
5. **`INFER_TYPE`** for automatic video type detection (instead of hardcoded M3U8/Video)
6. **`INFER_TYPE`** replaced deprecated `ExtractorLinkType.M3U8`/`ExtractorLinkType.VIDEO`

### Scraping Logic
Each provider's scraping logic was ported from the working Python scrapers in `/Users/robbdeeze/Documents/projects/iptv/New working scrapers/`. The key scraping patterns include:

- **Listing page extraction**: CSS selector-based link extraction from homepage/category pages
- **Video page resolution**: Fetching individual video pages and extracting `<source src="...mp4...">` 
- **Referer/user-agent headers**: Required for bypassing hotlink protection on many sites
- **POSTer URL extraction**: Using `meta[property='og:image']` or `video.poster` attribute
- **INFER_TYPE**: Automatic video type detection based on URL content

## Provider Details

### Crazyshit (`crazyshit`)
- **Listing**: `https://crazyshit.com/videos/` + pagination
- **Video resolution**: Two-step: listing page → media page → `<source src="...mp4...">`
- **Referer**: `https://crazyshit.com/`
- **Thumbnails**: Extracted from `meta[property='og:image']` or video page image

### Kaotic (`kaotic`)
- **Listing**: `https://kaotic.com/recent/` (single page, no pagination)
- **Video resolution**: Fetch post page → `<source src="...mp4|webm...">`
- **Referer**: `https://kaotic.com/`

### Worldstar (`worldstar`)
- **Listing**: Parses `__NEXT_DATA__` JSON and `__next_f` chunks from the page
- **Fallback**: Regex on stripped HTML for direct `.mp4` URLs
- **Video resolution**: `<source src="...mp4...">` on video page
- **Special**: Extracts `utLocation` field from JSON for direct URLs

### Xrares (`xrares`)
- **Listing**: Paginated `https://www.xrares.com/videos/?page=N`
- **Video resolution**: Video page → `<source src="...">`
- **Title cleanup**: Removes " - Xrares" and " - Free Amateur Porn" suffixes

### LiveGore (`livegore`)
- **Listing**: Multiple pattern extraction:
  - Direct `href="https://www.livegore.com/\d+"` links
  - `a.post-card` / `a[class*=post]` anchors
  - `data-link` attributes
- **Video resolution**: Tries multiple patterns:
  - `<video source[src]>` / `<source src>`
  - `<video[src]>` 
  - `[data-src]`
- **Mirror domains**: Also tries `livegore.net` and `livegore.org`

### DaftPorn (`daftporn`)
- **Listing**: Homepage → `a[href*=extreme-videos]` with `.php` extension
- **Video resolution**: Video page → `<source src="...mp4...">`
- **Title**: From link's `title` attribute
- **Poster**: From `meta[property='og:image']` or `video.poster`

### eFukt (`efukt`)
- **Listing**: `https://efukt.com/` (page 1) and `https://efukt.com/videos/page/N/` (subsequent pages)
- **Video links**: `<a href="https?://efukt.com/\d+_..." title="...">`
- **Resolution**: Video page → `<source src="...mp4...">`

### TheYNC (`theync`)
- **Listing**: `https://theync.com/latest-updates/`
- **Special**: Cloudflare fingerprinting protection; retry via CF proxy worker if direct fetch fails
- **Video resolution**: 
  - Direct `get_file/\d+/` URLs from listing page
  - Individual video pages → `get_file` URLs
- **CF proxy**: `CF_PROXY_URL` environment variable for bypassing Cloudflare

### USACrime (`usacrime`)
- **Listing**: Same pattern as LiveGore (mirror domain)
- **Video resolution**: Multiple pattern extraction from video page

## Build Information

### Technical Stack
- **Kotlin**: 2.4.0 (required for CloudStream v4 metadata)
- **Gradle**: 8.10.x
- **AGP**: 8.5.2
- **Java**: OpenJDK 17 (`/usr/local/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home`)
- **NiceHttp**: `com.github.Blatzar:NiceHttp:0.4.18` (HTTP client library)
- **Jsoup**: `org.jsoup:jsoup:1.17.2` (HTML parsing - kept for compatibility)

### Gradle Configuration
- `build.gradle.kts` root project configures all submodules
- Each submodule has its own `build.gradle.kts`
- CloudStream pre-release dependency: `com.github.recloudstream:cloudstream:pre-release`
- NiceHttp added to all provider modules

### Building
```bash
./gradlew make --no-daemon
# or per-module:
./gradlew :crazyshit:make --no-daemon
```

### Release
- **v0.2.2** uploaded to GitHub Release
- **22 providers** (11 NSFW + 3 MMA + 8 new NSFW)
- **CDN propagation**: ~15 minutes for `raw.githubusercontent.com` updates

## Installation

### For CloudStream 3 Users
1. Open CloudStream → Settings → Extensions → Add Repository
2. Add repository URL: `https://raw.githubusercontent.com/Robbdeeze/RdNutz-Repo/master/repo.json`
3. Browse available extensions and install desired ones
4. Each extension appears as a provider in the home screen selector

### For Developers
- Clone the repository: `git clone https://github.com/Robbdeeze/RdNutz-Repo`
- Modify provider files in `*/src/main/java/com/rdnutz/providers/`
- Rebuild: `./gradlew make --no-daemon`
- Upload release: `gh release create v0.1.0 ...`

## Known Issues & Workarounds

### Cloudflare Protection
Some providers (TheYNC) use Cloudflare fingerprinting. If direct fetches fail:
- Set `CF_PROXY_URL` environment variable
- Or retry with `--via-worker` flag in the Python scrapers

### Referer Headers
Most sites require `Referer` header for video playback. All providers set appropriate referer headers.

### Token Expiration
Many sites use time-limited video URLs (expires in ~1 hour). The scrapers include:
- Cache mechanism to keep tokens fresh
- Batch resolution with error handling
- Fallback to direct URL patterns

### Thumbnail/Poster Issues
Some providers (Worldstar, LiveGore initially) had thumbnails not displaying. Fixes included:
- Extracting from `meta[property='og:image']`
- Fallback to `video.poster` attribute
- Using `fixUrlNull()` for relative URL handling

## Contact & Support
- **GitHub**: [Robbdeeze/RdNutz-Repo](https://github.com/Robbdeeze/RdNutz-Repo)
- **Issues**: Use GitHub Issues for bug reports
- **Python scrapers**: Located at `/Users/robbdeeze/Documents/projects/iptv/New working scrapers/`

---
*Documentation last updated: October 25, 2026*
*Version: v0.2.2*