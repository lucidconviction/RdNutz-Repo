# RDNutz CloudStream Extensions

**RdNutz CloudStream extensions** — a curated collection of providers for CloudStream v4 (pre-release), covering viral/shock content, adult entertainment, and live sports streaming.

## 🚀 Install in CloudStream

1. Open **CloudStream** on your Android device
2. Go to **Extensions** tab (puzzle piece icon at bottom)
3. Tap **+** (top-left corner)
4. Select **"Add CloudStream repo"**
5. Paste this URL and tap Add:
   ```
   https://raw.githubusercontent.com/lucidconviction/RdNutz-Repo/master/repo.json
   ```
6. Tap the **"RdNutz Nett Smutt"** card that appears
7. Install the providers you want

## 📦 All Extensions (23 total)

| # | Extension | Display Name | Content | Status |
|---|-----------|-------------|---------|--------|
| 1 | crazyshit | Crazyshit | Viral/shocking videos | ✅ Works |
| 2 | daftporn | DaftPorn | Extreme adult content | ✅ Works |
| 3 | efukt | eFukt | Funny/shocking clips | ✅ Works |
| 4 | kaotic | Kaotic | Shocking/extreme videos | ✅ Works |
| 5 | livegore | LiveGore | Raw gore footage | ✅ Works |
| 6 | sexyandfunny | Sexyandfunny | Adult/comedy videos | ✅ Works |
| 7 | theync | TheYNC | Shock/gore content | ✅ Works |
| 8 | xrares | Xrares | Rare/extreme content | ✅ Works |
| 9 | worldstar | Worldstar | Hip hop/street fight videos | ⚠️ Partial |
| 10 | horriblevideos | HorribleVideos | Shocking clips | ⚠️ Partial |
| 11 | inhumanity | Inhumanity | Shock content | ⚠️ Partial |
| 12 | zfilmzoriginals | ZFilmzOriginals | Adult films | ⚠️ Partial |
| 13 | fullfightreplays | FullFightReplays | UFC/MMA fight replays | ❌ Needs fix |
| 14 | heavyr | Heavy-R | Extreme adult content | ❌ Needs fix |
| 15 | honeydippedcream | HoneyDippedCream | Adult content | ❌ Needs fix |
| 16 | jeffsmodels | JeffModels | Adult previews | ❌ Needs fix |
| 17 | plumperd | PlumperD | BBW adult content | ❌ Needs fix |
| 18 | sickjunk | SickJunk | Extreme shock content | ❌ Needs fix |
| 19 | youporn | YouPorn | Adult content | ❌ Needs fix |
| 20 | mmacore | MMA Core | MMA news/videos | ❌ Needs fix |
| 21 | mmareplays | MMA Replays | MMA fight replays | ❌ Needs fix |
| 22 | nothingtoxic | NothingToxic | Shock videos | ❌ Needs fix |
| 23 | rdnutz-sports | **RdNutz TV Sports n More** | Live sports (Streamed.pk + ZLive.st) | ✅ Works |

## 🏆 RdNutz TV Sports n More

An all-in-one sports streaming extension combining two data sources:

**Streamed.pk API** — REST API with 15 sports categories:
- Live/upcoming matches with team badges and posters
- Stream URLs via `/api/stream/{source}/{id}`
- Categories: Football, Basketball, NFL, Hockey, Baseball, Motor Sports, UFC/MMA, Tennis, Rugby, Golf, Cricket, and more

**ZLive.st** — 22+ live TV channels:
- Sky Sports, ESPN, beIN Sports, Fox Sports, TNT Sports, Eurosport, and more
- Embed format: `https://zlive.st/embed/{channel-slug}`

### Catalog Tabs
- **Live Now** — Currently live matches + ZLive channels
- **All Matches** — All upcoming/ongoing matches with kickoff times
- **ZLive Channels** — 22+ live sports TV channels
- **Football / Basketball / NFL / Hockey / Baseball / Motor Sports / UFC-MMA / Tennis / Rugby / Golf / Cricket** — Sport-specific listings

## 🔧 Build from Source

### Prerequisites
- Android Studio
- JDK 17+
- Gradle 8+

### Steps
```bash
# Clone the repository
git clone https://github.com/lucidconviction/RdNutz-Repo.git
cd RdNutz-Repo

# Build all extensions
./gradlew make --no-daemon

# .cs3 files will be in each module's build/ directory
```

### Output Files
Each module produces a `.cs3` file in its `build/` directory (e.g., `crazyshit/build/crazyshit.cs3`)

## 📁 File Structure
```
RdNutz-Repo/
├── repo.json                 # Repository manifest for CloudStream
├── plugins.json              # Plugin list → .cs3 URLs in releases
├── version.md                # Version changelog
├── README.md                 # This file
├── build.gradle.kts          # Root build config
├── settings.gradle.kts       # Module inclusion
├── gradle.properties         # Gradle/JVM settings
├── gradle/                   # Gradle wrapper
├── crazyshit/                # Crazyshit provider module
├── daftporn/                 # DaftPorn provider module
├── efukt/                    # eFukt provider module
├── kaotic/                   # Kaotic provider module
├── livegore/                 # LiveGore provider module
├── sexyandfunny/             # Sexyandfunny provider module
├── theync/                   # TheYNC provider module
├── xrares/                   # Xrares provider module
├── worldstar/                # Worldstar provider module
├── horriblevideos/           # HorribleVideos provider module
├── inhumanity/               # Inhumanity provider module
├── zfilmzoriginals/          # ZFilmzOriginals provider module
├── fullfightreplays/         # FullFightReplays provider module
├── heavyr/                   # Heavy-R provider module
├── honeydippedcream/         # HoneyDippedCream provider module
├── jeffsmodels/              # JeffModels provider module
├── plumperd/                 # PlumperD provider module
├── sickjunk/                 # SickJunk provider module
├── youporn/                  # YouPorn provider module
├── mmacore/                  # MMACore provider module
├── mmareplays/               # MMA Replays provider module
├── nothingtoxic/             # NothingToxic provider module
└── sports/                   # RdNutz TV Sports n More provider module
```

## 🛠️ Build System
- **Kotlin 2.4.0** / **Gradle 8.10** / **AGP 8.5.2**
- CloudStream Gradle Plugin (master-SNAPSHOT)
- Each provider is an Android Library module
- Output: `.cs3` plugin files (DEX + resources)

## 📦 Dependencies
- **CloudStream SDK** (pre-release) — Base provider classes
- **JSoup** — HTML parsing
- **Jackson** — JSON serialization

## 📜 License
MIT — Free to use and modify.

## 📝 Notes
- Repo uses `master` branch (not `main`)
- Release assets hosted at `github.com/lucidconviction/RdNutz-Repo/releases/download/v0.4.0/`
- Metadata served via `raw.githubusercontent.com/lucidconviction/RdNutz-Repo/master/`
- CS3 files currently accessible via GitHub API; direct download URLs may have propagation delay
