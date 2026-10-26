# RDNutz CloudStream Extensions

22 CloudStream extensions for viral, UFC/MMA, and shock content from Crazyshit, LiveGore, Worldstar Hip Hop, FullFightReplays, MMAReplays, MMACore, Kaotic, DaftPorn, eFukt, Xrares, TheYNC, Heavy-R, HoneyDippedCream, HorribleVideos, Inhumanity, JeffsModels, NothingToxic, PlumperD, SickJunk, SexyAndFunny, YouPorn, and ZFilmzOriginals.

## Quick Start

1. **Open CloudStream** on your Android device
2. Go to **Extensions** tab (puzzle piece icon at bottom)
3. Tap **+** (top-left corner)
4. Select **"Add CloudStream repo"**
5. Paste this URL and tap Add:
   ```
   https://raw.githubusercontent.com/Robbdeeze/RdNutz-Repo/master/repo.json
   ```
6. Tap the **"RdNutz Nett Smutt"** card that appears
7. Install the providers you want:
   - **Crazyshit** — Viral/shocking videos
   - **LiveGore** — Raw footage
   - **Worldstar** — Street videos
   - **Kaotic** — Shocking content
   - **DaftPorn** — Adult content
   - **eFukt** — Shocking/funny videos
   - **Xrares** — Rare/extreme content
   - **TheYNC** — Shocking/gore content
   - **FullFightReplays** 🆕 — UFC fight replays (Dailymotion/OK.ru)
   - **MMAReplays** 🆕 — MMA fight replays (gasobni.com HLS)
   - **MMACore** 🆕 — MMA news and videos (YouTube embeds)
   - **Heavy-R** 🆕 — Extreme videos (RSS feed → direct MP4)
   - **HoneyDippedCream** 🆕 — Adult content (direct MP4)
   - **HorribleVideos** 🆕 — Shocking clips (embed pages)
   - **Inhumanity** 🆕 — Shocking content (plug pages)
   - **JeffsModels** 🆕 — Adult previews (10-sec clips)
   - **NothingToxic** 🆕 — Shocking videos (out links)
   - **PlumperD** 🆕 — Adult previews (preview clips)
   - **SickJunk** 🆕 — Extreme videos (WordPress MP4)
   - **SexyAndFunny** 🆕 — Adult/Comedy (WordPress MP4)
   - **YouPorn** 🆕 — Adult content (token-protected MP4)
   - **ZFilmzOriginals** 🆕 — Adult trailers (Next.js JSON)

## Build from Source

### Prerequisites
- Android Studio
- JDK 17+
- Gradle 8+

### Steps
```bash
# Clone the repository
git clone https://github.com/Robbdeeze/RdNutz-Repo.git
cd RdNutz-Repo

# Build all extensions
./gradlew make --no-daemon

# .cs3 files will be in each module's build/ directory
```

### Output Files
Each module produces a `.cs3` file in its `build/` directory (e.g., `crazyshit/build/crazyshit.cs3`)

## Providers

| Provider | Base URL | Content Type |
|----------|----------|--------------|
| Crazyshit | crazyshit.com | Viral/shocking videos |
| LiveGore | livegore.com | Raw/gore footage |
| Worldstar | worldstarhiphop.com | Street/fight videos |
| Kaotic | kaotic.com | Shocking/extreme content |
| DaftPorn | daftporn.com | Adult content |
| eFukt | efukt.com | Shocking/funny videos |
| Xrares | xrares.com | Rare/extreme content |
| TheYNC | theync.com | Shocking/gore content |
| FullFightReplays | fullfightreplays.com | UFC fight replays |
| MMAReplays | mmareplays.com | MMA fight replays |
| MMACore | mma-core.com | MMA news & videos |
| Heavy-R | heavy-r.com | Extreme videos |
| HoneyDippedCream | honeydippedcream.com | Adult content |
| HorribleVideos | horriblevideos.com | Shocking clips |
| Inhumanity | inhumanity.com | Shocking content |
| JeffsModels | jeffsmodels.com | Adult previews |
| NothingToxic | nothingtoxic.com | Shocking videos |
| PlumperD | plumperd.com | Adult previews |
| SickJunk | sickjunk.com | Extreme videos |
| SexyAndFunny | sexyandfunny.com | Adult/Comedy |
| YouPorn | you-porn.com | Adult content |
| ZFilmzOriginals | z-filmz-originals.com | Adult trailers |

## File Structure
```
RdNutz-Repo/
├── repo.json                 # Repository manifest for CloudStream
├── plugins.json              # Plugin list → .cs3 URLs in releases
├── version.md                # Version changelog
├── *.csmeta                  # Individual plugin manifests (metadata)
├── build.gradle.kts          # Root build config
├── settings.gradle.kts       # Module inclusion
├── gradle.properties         # Gradle/JVM settings
├── gradle/                   # Gradle wrapper
├── crazyshit/                # Crazyshit provider module
├── livegore/                 # LiveGore provider module
├── worldstar/                # Worldstar provider module
├── kaotic/                   # Kaotic provider module
├── daftporn/                 # DaftPorn provider module
├── efukt/                    # eFukt provider module
├── xrares/                   # Xrares provider module
├── theync/                   # TheYNC provider module
├── fullfightreplays/         # FullFightReplays provider module
├── mmareplays/               # MMAReplays provider module
├── mmacore/                  # MMACore provider module
├── heavyr/                   # Heavy-R provider module
├── honeydippedcream/         # HoneyDippedCream provider module
├── horriblevideos/           # HorribleVideos provider module
├── inhumanity/               # Inhumanity provider module
├── jeffsmodels/              # JeffsModels provider module
├── nothingtoxic/             # NothingToxic provider module
├── plumperd/                 # PlumperD provider module
├── sickjunk/                 # SickJunk provider module
├── sexyandfunny/             # SexyAndFunny provider module
├── youporn/                  # YouPorn provider module
└── zfilmzoriginals/          # ZFilmzOriginals provider module
```

## Build System
- **Kotlin 2.4.0** / **Gradle 8.10** / **AGP 8.5.2**
- CloudStream Gradle Plugin (master-SNAPSHOT)
- Each provider is an Android Library module
- Output: `.cs3` plugin files (DEX + resources)

## Dependencies
- **CloudStream 3 SDK** (pre-release) — Base provider classes
- **JSoup** — HTML parsing
- **Jackson** — JSON serialization
- **NiceHttp 0.4.18** — HTTP client (JSON POST support)

## License
MIT — Free to use and modify.

## Notes
- Repo uses `master` branch (not `main`)
- Release assets hosted at `github.com/Robbdeeze/RdNutz-Repo/releases/download/v0.2.0/`
- Metadata served via `raw.githubusercontent.com/Robbdeeze/RdNutz-Repo/master/`
- CS3 files currently accessible via GitHub API; direct download URLs may have propagation delay
