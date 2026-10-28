# RdNutz CloudStream Extensions — Version & Knowledge Base

## Current Release: v0.8.4
**Date:** October 28, 2026  
**Branch:** `builds`  
**Repo:** https://github.com/lucidconviction/RdNutz-Repo  
**CloudStream Repo URL:** `https://raw.githubusercontent.com/lucidconviction/RdNutz-Repo/master/repo.json`

---

## Progress / Status

| Area | Status | Notes |
|------|--------|-------|
| **CF-bypass tooling** | ✅ Ready | SeleniumBase (Pure CDP + UC Mode) + nodriver (`verify_cf()`) installed and tested |
| **BasketballVideo** | ❌ Dead source | CF bypass works but article pages have zero video embeds (JS-loaded only) |
| **NFL-Video** | ❌ Dead source | HTTP 522 origin error even after CF bypass |
| **MLBLive** | ❌ Dead source | iframe chain `bysesukior.com` → `n1mwq.org` unresolved |
| **WatchProWrestling** | ✅ Working | Fixed `?paged=N` pagination |
| **AllTheBestFights** | ✅ Working | Fixed `<article>` tag selectors |
| **123Wrestling** | ✅ Working | `/page/N/` pagination confirmed |
| **DaddyLive API** | ✅ Working | Live events endpoint functional |
| **Sportsreplays .cs3** | ✅ Built | 22.3 KB — released as v0.8.4 |

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
| 11 | sportsreplays | Movie | 22.3 KB | WatchProWrestling, AllTheBestFights, 123Wrestling, DaddyLive | ✅ Working |
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

**Problem:** We tried adding StreamSurge (`streamsurge.org`), Sportsurge (`sportsurge.net`), and StreamEast (`streameast.bz`) to our sources list. All three returned Cloudflare challenge pages instead of content, making them unusable in CloudStream. Later, BasketballVideo (`basketball-video.com`), NFL-Video (`nfl-video.com`), and MLBLive (`mlblive.net`) proved to be CF-protected as well.

**Lesson:** Test a new source with a simple `curl` check BEFORE writing any code:
```bash
curl -s -m 10 -o /dev/null -w "%{http_code}" "https://streamsurge.org/" -A "Mozilla/5.0"
```
If the response is a Cloudflare HTML page (not JSON, not HTML with video links), skip it.

**When a CF-protected source is worth keeping, use SeleniumBase:**

Reference repo: **https://github.com/seleniumbase/SeleniumBase.git**

SeleniumBase is the primary tool for testing Cloudflare bypasses against new sources. Escalate in this order:
1. `curl` / plain HTTP request — fastest, catches sites that aren't actually protected
2. SeleniumBase **Pure CDP Mode** (`sb_cdp`) — headless, fast
3. SeleniumBase **UC Mode** (`uc=True`) — most aggressive; `solve_captcha()` works best in GUI (non-headless) mode
4. **nodriver** — undetected-chromedriver successor; `verify_cf()` uses OpenCV template matching to click the Turnstile checkbox

Setup (tested Oct 2026, Python 3.14, macOS arm64):
```bash
python3 -m venv /tmp/sb_venv
/tmp/sb_venv/bin/pip install seleniumbase          # Pure CDP + UC Mode
pip3 install nodriver opencv-python-headless --break-system-packages  # nodriver needs system Python + OpenCV
```

Quick CF detection script (nodriver):
```python
import asyncio, nodriver as uc
CF_MARKERS = ['turnstile', 'challenges.cloudflare', 'just a moment',
              'verify you are human', 'checking your browser', 'cf-browser-verification']

async def check(url):
    browser = await uc.start(headless=False)
    tab = await browser.get(url)
    await asyncio.sleep(3)
    if any(m in (await tab.get_content() or "").lower() for m in CF_MARKERS):
        await tab.verify_cf()
        await asyncio.sleep(5)
    html = await tab.get_content() or ""
    browser.stop()
    return not any(m in html.lower() for m in CF_MARKERS)
```

**Critical caveat (learned the hard way):** bypassing the challenge is NOT enough. After passing CF, always verify the article pages contain extractable video embeds (iframes/YouTube/OK.ru/Vimeo) in the static HTML. Our three sports sources all passed CF via nodriver but were still dead ends:
- BasketballVideo — 44 KB of HTML, 26 game links, **zero embeds** (videos JS-loaded)
- NFL-Video — HTTP 522 (origin down) even after bypass
- MLBLive — iframe chain `bysesukior.com/e/{id}` → `n1mwq.org/8eu3/{id}` → unresolved

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
| **v0.8.4** | Oct 28, 2026 | **sportsreplays rewrite:** Removed BasketballVideo, NFL-Video, and MLBLive — all three are behind Cloudflare protection that blocks automated requests (confirmed via curl, SeleniumBase CDP+UC, and nodriver tests). BasketballVideo article pages contain zero video embeds (no iframes/YouTube/OK.ru/Vimeo). MLBLive iframe chain resolves to `bysesukior.com` → `n1mwq.org` → unresolved. NFL-Video returns HTTP 522 (origin down). Kept WatchProWrestling (fixed `?paged=N` pagination), AllTheBestFights (fixed `<article>` tag selectors), 123Wrestling, and DaddyLive. |
| **v0.8.3** | Oct 28, 2026 | **Bug fixes:** Fixed sportsreplays 404s — corrected URL paths for BasketballVideo (NBA→`/2025-26`, WNBA→`/wnba-video`, NCAA→`/college-basketball`), NFL (`/`→`/2026/`), MLB (`/`→`/2026-mlb-full-game-replays/`). Removed dead `mirrors.st` API (connection refused) from streamedlive — kept StrmFree, DaddyLive, NTV Falcon, CricHD. Updated `settings.gradle.kts` to include `sportsreplays`. |
| **v0.8.2** | Oct 27, 2026 | **New extensions:** Added `sportsreplays` (multi-source NBA/NFL/MLB/MMA/Boxing/Wrestling replays from BasketballVideo, NFL-Video, MLBLive, WatchProWrestling, AllTheBestFights, 123Wrestling, DaddyLive). Added `streamedlive` (live sports from StrmFree, DaddyLive, NTV Falcon, CricHD). |
| **v0.8.1** | Oct 27, 2026 | **Type fix:** Corrected `crazyshit` and `zfilmzoriginals` type from `Movie` to `NSFW` in both `.csmeta` files and `plugins.json`. This ensures they appear in the correct CloudStream browser tab instead of the Movies section. |
| **v0.8.0** | Oct 27, 2026 | **Initial public release** with 14 NSFW + MMA extensions (crazyshit, daftporn, efukt, fullfightreplays, horriblevideos, jeffsmodels, kaotic, livegore, plumperd, sexyandfunny, theync, watchmmafull, worldstar, zfilmzoriginals). |

---

## Fixes Log

### Fix #1 — sportsreplays 404 on all categories (Oct 28, 2026)
- **Symptom:** All sportsreplays categories returned empty results or 404
- **Root cause:** Sites changed their URL paths; old paths no longer exist
- **What changed:**
  - `basketball-video.com/nba` → `/2025-26`
  - `basketball-video.com/wnba` → `/wnba-video`
  - `basketball-video.com/ncaa` → `/college-basketball`
  - `nfl-video.com/` → `/2026/`
  - `mlblive.net/` → `/2026-mlb-full-game-replays/`
- **Resolution:** Updated all category URLs in `SportsReplaysProvider.kt` `mainPageOf()` and search loop
- **Prevention:** Always test URLs with `curl -s -o /dev/null -w "%{http_code}"` before adding to `mainPageOf()`

### Fix #2 — mirrors.st API dead in streamedlive (Oct 28, 2026)
- **Symptom:** `mirrors` source returned connection refused (HTTP 000), caused errors in logs
- **Root cause:** `api.mirrors.st` is completely offline (no server responding)
- **What was removed:** `getMirrorsMainPage()`, mirrors entry from `mainPageOf()`, mirrors branch in `when`, mirrors handler in `loadLinks()`, mirrors search block
- **Resolution:** Removed all mirrors.st code; kept 4 working sources: StrmFree, DaddyLive, NTV Falcon, CricHD
- **Prevention:** Test API endpoints before shipping; set a health-check deadline

### Fix #3 — NSFW type misclassification (Oct 27, 2026)
- **Symptom:** `crazyshit` and `zfilmzoriginals` appeared in the Movies tab instead of NSFW
- **Root cause:** `.csmeta` files used old format; `plugins.json` had `"type": "Movie"`
- **Resolution:** Migrated both `.csmeta` files to new format with `supportedTypes: ["NSFW"]`; updated `plugins.json` entries
- **Prevention:** Use `"type": "NSFW"` in `plugins.json` for any extension that serves adult content; verify with `cat plugins.json | python3 -c "..."` before release

### Fix #4 — Kotlin `request` unresolved reference (Oct 28, 2026)
- **Symptom:** Build failure — `Unresolved reference: request` at line 79 of SportsReplaysProvider.kt
- **Root cause:** When removing the `sportLabel` local variable, left a dangling `request.name` reference inside `getBasketballVideoMainPage()` which doesn't receive a `request` parameter
- **Resolution:** Added `label: String` parameter to `getBasketballVideoMainPage()` and passed the category label from the caller; updated the fallback title to use `label + " Replay"`
- **Prevention:** After refactoring a function signature, search for all references to removed variables

### Fix #5 — .cs3 not committed (gitignore trap) (Oct 27–28, 2026)
- **Symptom:** Release v0.8.2 had no `.cs3` assets — only v0.8.3 had them after manual `cp`
- **Root cause:** `*.cs3` is in `.gitignore`; `git add` silently skipped them
- **Resolution:** Used `git add -f` to force-add; also copied binaries to root before committing
- **Prevention:** Always verify with `ls *.cs3` (on disk) vs `git ls-tree HEAD --name-only | grep '\.cs3$'` (in repo)

### Fix #6 — CF-protected sources removed from sportsreplays (Oct 28, 2026)
- **Symptom:** BasketballVideo, NFL-Video, and MLBLive returned empty results (Cloudflare challenge pages)
- **Root cause:** All three sites serve Cloudflare Turnstile challenges to automated/CDP requests. Even nodriver's `verify_cf()` cannot make the data extractable in CloudStream:
  - **BasketballVideo:** Node bypass works, but article pages contain zero video embeds — only nav/team links, no iframes/YouTube/OK.ru/Vimeo. Videos are likely JS-loaded dynamically.
  - **NFL-Video:** Returns HTTP 522 (origin server down) even after CF bypass
  - **MLBLive:** Bypasses CF, but article iframe points to `bysesukior.com/e/{id}` → redirects to `n1mwq.org/8eu3/{id}` → final destination unresolved
- **Tested:** SeleniumBase Pure CDP (blocked), UC Mode (blocked), nodriver `verify_cf()` (bypasses but no extractable content)
- **What was removed:** BasketballVideo, NFL-Video, MLBLive from `mainPageOf()`, all related `get*` methods, and search blocks
- **What was fixed:** WatchProWrestling pagination → `?paged=N` (not `/page/N/`), AllTheBestFights article extraction → `<article>` tags with `full-fight-video` filter
- **Prevention:** Before adding a CF-protected site, verify that article pages actually contain extractable video embeds in the static HTML

---

## Quick Reference — CF Bypass & Build Commands

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

# CF bypass — quick nodriver check (install: pip install nodriver opencv-python-headless --break-system-packages)
python3 << 'EOF'
import asyncio, nodriver as uc
async def check(url):
    b = await uc.start(headless=False)
    t = await b.get(url)
    await asyncio.sleep(3)
    h = (await t.get_content() or "").lower()
    if any(m in h for m in ['turnstile','challenges.cloudflare','cf-browser-verification']):
        await t.verify_cf()
        await asyncio.sleep(5)
    h = (await t.get_content() or "").lower()
    b.stop()
    return not any(m in h for m in ['turnstile','challenges.cloudflare','cf-browser-verification'])
print(asyncio.run(check("https://basketball-video.com/2025-26")))
EOF

# CF bypass — SeleniumBase Pure CDP
python3 << 'EOF'
from seleniumbase import sb_cdp
sb = sb_cdp.Chrome(headless=True)
sb.goto("https://basketball-video.com/2025-26")
print("CF blocked:", any(m in sb.get_page_source().lower() for m in ["turnstile","cf-browser-verification"]))
sb.quit()
EOF

# CF bypass — SeleniumBase UC Mode (slowest, most reliable, works best in GUI)
python3 << 'EOF'
from seleniumbase import SB
with SB(uc=True, test=True, headless=False) as sb:
    sb.goto("https://basketball-video.com/2025-26")
    sb.solve_captcha()
    print("HTML size:", len(sb.get_page_source()))
EOF
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

*Last updated: October 28, 2026 — v0.8.4*
