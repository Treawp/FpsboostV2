# FPSBoost — Minecraft 1.12.2 Performance Mod

![Build Status](https://github.com/YOUR_USERNAME/FPSBoost/actions/workflows/build.yml/badge.svg)
![MC Version](https://img.shields.io/badge/Minecraft-1.12.2-brightgreen)
![Forge](https://img.shields.io/badge/Forge-14.23.5.2860-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

A lightweight, compile-and-drop Forge mod that squeezes every frame out of Minecraft 1.12.2 without compromising gameplay.

---

## Features

| System | What it does |
|---|---|
| **Dynamic Chunk Reduction** | Automatically lowers render distance when FPS drops below your threshold, then slowly recovers |
| **Async Chunk Loading** | Pre-processes newly loaded chunks on a daemon thread pool to reduce lag spikes |
| **Frustum Culling** | Skips rendering of chunks and entities outside the camera's view cone |
| **Entity Culling** | Stops rendering living entities beyond a configurable distance |
| **Particle Limiter** | Caps particle count when FPS is low |
| **GC Optimiser** | Schedules periodic GC nudges during quiet moments to keep heap pressure low |
| **Memory Leak Detector** | Monitors heap trend and warns you if memory is growing continuously |
| **Fast Math** | Replaces `Math.sin/cos` with 65536-entry lookup tables for hot render paths |
| **Adaptive Frame Limiter** | Gently caps frame rate when you're well above target, reducing GPU heat |
| **Tick Optimiser** | Throttles distant entity AI ticks to halve CPU load in crowded worlds |
| **FPS Counter** | Color-coded overlay (green ≥ 60, yellow ≥ 30, red < 30) |
| **Performance HUD** | Togglable overlay: FPS avg/min/max, RAM usage %, chunk distance, entity count |
| **Profile System** | One-line profile switch: `Max FPS` / `Balanced` / `Quality` |
| **Hot-Reload Config** | Change `config/fpsboost.cfg` without restarting the game |

---

## Installation

1. Install **[Minecraft Forge 1.12.2-14.23.5.2860](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html)**
2. Download the latest `FPSBoost-X.Y.Z.jar` from [Releases](../../releases)
3. Drop it into your `.minecraft/mods/` folder
4. Launch the game — the mod is active immediately

> **Java version:** Java 8 required (same as vanilla 1.12.2)

---

## Configuration

Config file: `.minecraft/config/fpsboost.cfg`

You can also open the GUI in-game via **Mods → FPS Boost → Config**.

### Profiles

Set `activeProfile` in the `[general]` category:

| Profile | Description |
|---|---|
| `Max FPS` | Aggressive settings — lowest quality, highest frames |
| `Balanced` | Uses your individual config values (default) |
| `Quality` | Keeps visuals intact, still applies safe optimisations |

### Key options

```properties
# [general]
enableMod       = true
activeProfile   = Balanced   # Max FPS | Balanced | Quality
debugMode       = false

# [chunk]
maxRenderDistance   = 12     # chunks (2–32)
minRenderDistance   = 4      # floor for dynamic reduction
dynamicChunkReduce  = true
fpsThresholdChunk   = 40     # reduce chunks below this FPS
asyncChunkLoading   = true

# [memory]
gcOptimization      = true
gcIntervalSeconds   = 120
entityCulling       = true
entityCullDistance  = 64     # blocks
particleLimiter     = true
particleMaxCount    = 500
fpsThresholdParticle= 35

# [render]
fastMath             = true
adaptiveFrameLimiter = true
targetFPS            = 120
reduceTicks          = true
tickReduceDistance   = 48    # blocks
frustumCulling       = true

# [advanced]
showFPSCounter   = true
showPerfOverlay  = false     # press nothing, set to true to enable
hotReloadConfig  = true
```

---

## Building from source

**Requirements:** JDK 8, internet connection (ForgeGradle downloads deps)

```bash
git clone https://github.com/YOUR_USERNAME/FPSBoost.git
cd FPSBoost
./gradlew setupDecompWorkspace   # first run only, downloads MC sources
./gradlew build
# Output: build/libs/FPSBoost-1.0.0.jar
```

### IDE setup (IntelliJ)
```bash
./gradlew setupDecompWorkspace genIntellijRuns
```
Then open the project and import the Gradle build.

---

## Compatibility

| Mod | Status |
|---|---|
| OptiFine | ⚠️ Some render hooks may conflict — use one or the other |
| BetterFPS | ✅ Compatible (complementary fast-math implementations) |
| FoamFix | ✅ Compatible |
| VanillaFix | ✅ Compatible |
| Phosphor | ✅ Compatible |

---

## Changelog

### v1.0.0
- Initial release
- Dynamic chunk reduction system
- Async chunk loader (thread-pool based)
- Entity culling + frustum culling
- Particle limiter with FPS threshold
- GC optimiser + memory leak detector
- Fast math (65536-entry sin/cos LUT)
- Adaptive frame limiter
- Tick optimiser for distant entities
- FPS counter overlay + performance HUD
- Profile system (Max FPS / Balanced / Quality)
- Hot-reload config
- GitHub Actions CI/CD

---

## Contributing

Pull requests welcome. Please follow the existing code style and add Javadoc for new public APIs.

## License

MIT — see [LICENSE](LICENSE)
