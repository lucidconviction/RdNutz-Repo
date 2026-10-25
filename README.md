# CloudStream 3 Nett Smutt Extension

CloudStream 3 plugins for raw/shocking video content from Crazyshit, LiveGore, and Worldstar Hip Hop.

## Quick Start

1. **Open CloudStream** on your Android device
2. Go to **Extensions** tab (puzzle piece icon at bottom)
3. Tap **+** (top-left corner)
4. Select **"Add CloudStream repo"**
5. Paste this URL and tap Add:
   ```
   https://raw.githubusercontent.com/Robbdeeze/rdnutz-cloudstream-extensions/main/repo.json
   ```
6. Tap the **"RdNutz Nett Smutt"** card that appears
7. Install the providers you want:
   - **Crazyshit** — Viral/shocking videos (30 streams)
   - **LiveGore** — Raw footage (100 streams)
   - **Worldstar** — Street videos (30 streams)

## Build from Source

### Prerequisites
- Android Studio
- JDK 17+
- Gradle 8+

### Steps
```bash
# Clone the repository
git clone https://github.com/Robbdeeze/rdnutz-cloudstream-extensions.git
cd rdnutz-cloudstream-extensions

# Build release AAR (use Android Studio)
./gradlew assembleRelease

# Or build a .cs3 file
./gradlew buildCs3
```

### Output Files
- `app/build/outputs/aar/app-release.aar` — Release build
- `.cs3` file created automatically by CloudStream gradle plugin

## Providers

| Provider | Base URL | Content | Max Streams |
|----------|----------|---------|-------------|
| Crazyshit | crazyshit.com | Viral/shocking | 30 |
| LiveGore | livegore.com | Raw footage | 100 |
| Worldstar | worldstarhiphop.com | Street videos | 30 |

## File Structure

```
rdnutz-cloudstream-extensions/
├── providers/
│   ├── CrazyshitProvider.kt
│   ├── LiveGoreProvider.kt
│   └── WorldstarProvider.kt
├── extractors/          # Video host extractors (for future)
│   └── VidplayExtractor.kt
├── settings.gradle.kts
├── build.gradle.kts
├── repo.json            # Repository manifest for CloudStream
├── plugins.json         # Plugin list
├── *.csmeta             # Individual plugin manifests
└── README.md
```

## Dependencies

- **CloudStream 3 SDK** — Base classes for providers
- **JSoup** — HTML parsing
- **Jackson** — JSON parsing

## License

MIT — Free to use and modify.

## Based On

Original Python scrapers from:
- `/home/robbdeeze/Documents/projects/iptv/New working scrapers/crazyshit.py`
- `/home/robbdeeze/Documents/projects/iptv/New working scrapers/livegore.py`
- `/home/robbdeeze/Documents/projects/iptv/New working scrapers/worldstar.py`
