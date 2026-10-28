# RdNutz CloudStream Extensions — Version & Knowledge Base

## Current Release: v0.8.3
**Date:** October 28, 2026  
**Branch:** `builds`  
**Repo:** https://github.com/lucidconviction/RdNutz-Repo  
**CloudStream Repo URL:** `https://raw.githubusercontent.com/lucidconviction/RdNutz-Repo/master/repo.json`

---

## Published Extensions (16)

| # | Extension | Type | Size | Source(s) | Status |
|---|-----------|------|------|-----------|--------|
| 1 | crazyshit | NSFW | 11.8 KB | crazyshit.com | ✅ Working |
| 2 | daftporn | NSFW | 12.4 KB | daftporn.com | ✅ Working |
| 3 | efukt | NSFW | 12.3 KB | efukt.com | ✅ Working |
| 4 | fullfightreplays | Movie | 14.4 KB | fullfightreplays.com | ✅ Working |
| 5 | horriblevideos | NSFW | 12.9 KB | horriblevideos.com | ✅ Working |
| 6 | jeffsmodels | NSFW | 11.1 KB | jeffsmodels.com | ✅ Working |
| 7 | kaotic | NSFW | 11.6 KB | kaotic.com | ✅ Working |
| 8 | livegore | NSFW | 12.0 KB | livegore.com | ✅ Working |
| 9 | plumperd | NSFW | 10.0 KB | plumperd.com | ✅ Working |
| 10 | sexyandfunny | NSFW | 13.4 KB | sexyandfunny.com | ✅ Working |
| 11 | sportsreplays | Movie | 34.1 KB | BasketballVideo, NFL-Video, MLBLive, WatchProWrestling, AllTheBestFights, 123Wrestling, DaddyLive | ✅ Working |
| 12 | streamedlive | Live | 28.5 KB | StrmFree, DaddyLive, NTV Falcon, CricHD | ✅ Working |
| 13 | theync | NSFW | 10.3 KB | theync.com | ✅ Working |
| 14 | watchmmafull | Movie | 15.2 KB | watchmmafull.com | ✅ Working |
| 15 | worldstar | NSFW | 10.2 KB | worldstarhiphop.com | ✅ Working |
| 16 | zfilmzoriginals | NSFW | 8.7 KB | z-filmz-originals.com | ✅ Working |

## Unpublished / Excluded (4 modules in source but not in releases)
| Extension | Reason |
|-----------|--------|
| rdnutz-iptv | Not published — no `.cs3` on builds branch |
| xrares | Not published — not included in settings.gradle.kts |
| sports | Not published — legacy, replaced by streamedlive + sportsreplays |
| YouPorn | Deprecated — source structure changed, no working release |

---

## Knowledge Base — What We Learned (Trial & Error)

### 1. URL Path Drift Is the #1 Source of 404s

**Problem:** Sites change their URL structure without warning. BasketballVideo, NFL-Video, and MLBLive all shifted their category paths between when we first scraped them and now:

| Site | Old URL (broken) | New URL (fixed) |
|------|-------------------|-----------------|
| BasketballVideo | `/nba` | `/2025-26` |
| BasketballVideo | `/wnba` | `/wnba-video` |
| BasketballVideo | `/ncaa` | `/college-basketball` |
| NFL-Video | `/` (homepage) | `/2026/` |
| MLBLive | `/` (homepage) | `/2026-mlb-full-game-replays/` |

**How to catch it early:** Always test category URLs with `curl -s -o /dev/null -w "%{http_code}"` before assuming a path works. Sites rotate URLs seasonally — what worked in August may break in October.

---

### 2. API Endpoints Die Without Notice

**Problem:** `api.mirrors.st` was returning valid JSON one day and connection-refused (HTTP 000) the next. No deprecation notice, no warning.

**How to catch it early:** Test all API endpoints in a loop before releasing:
```bash
for url in "https://api.mirrors.st/api/streams" "https://streamfree.top/api/v1/streams?category=soccer"; do
    code=$(curl -s -m 12 -o /dev/null -w "%{http_code}" -A "Mozilla/5.0" "$url")
    echo "$code  $url"
done
```
Any `000` or `404` means the source is dead and should be removed before building.

**Decision made:** Removed mirrors.st from streamedlive entirely. Kept StrmFree, DaddyLive, NTV Falcon, and CricHD which are all still live.

---

### 3. Cloudflare-Blocked Sources Should Be Excluded Upfront

**Problem:** We tried adding StreamSurge (`streamsurge.org`), Sportsurge (`sportsurge.net`), and StreamEast (`streameast.bz`) to our sources list. All three returned Cloudflare challenge pages instead of content, making them unusable in CloudStream.

**Lesson:** Test a new source with a simple `curl` check BEFORE writing any code:
```bash
curl -s -m 10 -o /dev/null -w "%{http_code}" "https://streamsurge.org/" -A "Mozilla/5.0"
```
If the response is a Cloudflare HTML page (not JSON, not HTML with video links), skip it.

---

### 4. Type Classification Matters for CloudStream UI

**Problem:** We initially classified `crazyshit` and `zfilmzoriginals` as `Movie` type, but CloudStream's browser uses the `supportedTypes` field to group content. NSFW content should be tagged `NSFW` so it doesn't appear in the main Movies tab.

**How we fixed it:** Updated both `.csmeta` files:
- `CrazyshitProvider.csmeta` → `supportedTypes: ["NSFW"]`
- `ZFilmzOriginalsProvider.csmeta` → `supportedTypes: ["NSFW"]`
- Updated `plugins.json` entries to include `"type": "NSFW"`

**General rule:**
- `TvType.Movie` = recorded content (replays, movies, clips)
- `TvType.Live` = live streams (sports, events)
- `["NSFW"]` = adult/explicit content (controls browser grouping)

---

### 5. Gradle Module Inclusion Must Match .cs3 Build

**Problem:** Added `sportsreplays` to `settings.gradle.kts` but forgot to rebuild and update `plugins.json`. The release contained the source code but not the compiled `.cs3`.

**Checklist before releasing:**
1. `settings.gradle.kts` includes the module
2. `JAVA_HOME=... ./gradlew :modulename:make` succeeds
3. `plugins.json` has the entry with correct hash and size
4. `.cs3` file is committed with `git add -f` (it's in `.gitignore`)
5. GitHub release has the `.cs3` asset uploaded

---

### 6. `.cs3` Files Are Gitignored — Must Use `-f` to Commit

**Problem:** The root `.gitignore` has `*.cs3` to keep the working tree clean. When committing new builds, `git add` silently skips them.

**Solution:** Always use `git add -f` for `.cs3` files:
```bash
git add -f sportsreplays.cs3 streamedlive.cs3
```

---

### 7. Build Output Lives in `*/build/` — Not the Root

**Problem:** New to CloudStream extension development, I expected `.cs3` files to appear at the project root. They actually land in each module's build directory:
```
sportsreplays/build/sportsreplays.cs3
streamedlive/build/streamedlive.cs3
```

**Workflow:** After building, copy to root for committing and releasing:
```bash
cp sportsreplays/build/sportsreplays.cs3 .
cp streamedlive/build/streamedlive.cs3 .
```

---

### 8. JDK 17 Is Required — Not Available via Default `java`

**Problem:** `./gradlew make` fails with "unspecified is not a valid version" or Kotlin compilation errors when using the wrong JDK.

**Fix:** Always set `JAVA_HOME` explicitly:
```bash
JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home ./gradlew :sportsreplays:make
```

---

### 9. Search Must Cover All Categories, Not Just the First

**Problem:** The sportsreplays `search()` method only searched the first BasketballVideo category, missing content from other sports.

**Fix:** Loop through ALL categories in search:
```kotlin
for (cat in listOf("2025-26", "wnba-video", "college-basketball")) {
    val response = getBasketballVideoMainPage(cat, 1, cat)
    // add results...
}
```

---

### 10. `plugins.json` Must Match Reality

**Problem:** `plugins.json` is the manifest CloudStream uses to discover extensions. If the hash or size doesn't match the actual `.cs3`, installation fails with a checksum error.

**Always run this before releasing:**
```bash
sha256sum *.cs3 | sort
cat plugins.json | python3 -c "import sys,json; [print(f\"{e['internalName']:25s} {e['fileHash']} size={e['fileSize']}\") for e in sorted(json.load(sys.stdin), key=lambda x: x['internalName'])]"
```
Compare the two outputs — they must match.

---

## Build & Release Workflow (Verified)

```bash
# 1. Build
cd /Users/robbdeeze/Documents/projects/rdnutz-cloudstream-extensions
JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home \
    ./gradlew :sportsreplays:make :streamedlive:make

# 2. Copy to root
cp sportsreplays/build/sportsreplays.cs3 .
cp streamedlive/build/streamedlive.cs3 .

# 3. Update plugins.json hashes
#    (manually edit or use python script)

# 4. Commit
git add -f *.cs3 plugins.json
git commit -m "fix: <description>"
git push origin builds

# 5. Upload to GitHub release
gh release upload v<version> *.cs3 --clobber

# 6. Verify
gh release view v<version> --json assets --jq '.assets[].name'
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| v0.8.3 | Oct 28, 2026 | Fixed sportsreplays 404s (URL path corrections); removed dead mirrors.st from streamedlive |
| v0.8.2 | Oct 27, 2026 | Added sportsreplays extension (7 sources); added streamedlive extension (4 sources) |
| v0.8.1 | Oct 27, 2026 | Fixed NSFW type classification for crazyshit and zfilmzoriginals |
| v0.8.0 | Oct 27, 2026 | Initial public release with 14 extensions |

---

## Quick Reference — Common Commands

```bash
# Test if a site is reachable
curl -s -m 12 -o /dev/null -w "%{http_code}\n" "URL" -A "Mozilla/5.0"

# Test API endpoint
curl -s -m 12 "https://api.example.com/endpoint" | python3 -m json.tool | head -30

# Build a single extension
JAVA_HOME=/usr/local/Cellar/openjdk@17/.../home ./gradlew :modulename:make

# Get hash of built .cs3
sha256sum modulename/build/modulename.cs3

# List all extensions in current plugins.json
cat plugins.json | python3 -c "import sys,json; [print(e['internalName']) for e in sorted(json.load(sys.stdin), key=lambda x: x['internalName'])]"

# Check what's in the latest GitHub release
gh release view vLATEST --repo lucidconviction/RdNutz-Repo --json assets --jq '.assets[].name'
```

---

## File Map

```
RdNutz-Repo/
├── plugins.json              # Plugin manifest (hash, size, type) — MUST match releases
├── repo.json                 # CloudStream repo entry point
├── version.md                # This file
├── *.cs3                     # Compiled extension binaries (committed with git add -f)
├── *.csmeta                  # Per-extension metadata files
├── settings.gradle.kts       # Module list — controls what gets built
├── build.gradle.kts          # Root build config
│
├── sportsreplays/            # Multi-source sports replay aggregator
│   └── src/main/java/com/rdnutz/providers/SportsReplaysProvider.kt
├── streamedlive/             # Live sports streaming aggregator
│   └── src/main/java/com/rdnutz/providers/StreamedLiveProvider.kt
├── crazyshit/                # NSFW viral content
├── daftporn/                 # NSFW adult content
├── efukt/                    # NSFW shocking videos
├── fullfightreplays/         # UFC/MMA fight replays
├── horriblevideos/           # NSFW shocking clips
├── jeffsmodels/              # NSFW adult previews
├── kaotic/                   # NSFW shocking content
├── livegore/                 # NSFW raw/gore footage
├── plumperd/                 # NSFW adult previews
├── sexyandfunny/             # NSFW adult/comedy
├── theync/                   # NSFW shocking/gore (CF protected)
├── watchmmafull/             # MMA full fights
├── worldstar/                # NSFW street/fight videos
└── zfilmzoriginals/          # NSFW adult trailers
```

---

*Last updated: October 28, 2026 — v0.8.3*
