package com.fpsboost.mod.config;

import com.fpsboost.mod.FPSBoost;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

/**
 * Manages all FPSBoost configuration via the Forge Configuration API.
 * Supports hot-reload on config change and three built-in profiles.
 */
public class ConfigManager {

    // ── General ──────────────────────────────────────────────────────
    public boolean enableMod           = true;
    public String  activeProfile       = "Balanced";  // "Max FPS" | "Balanced" | "Quality"
    public boolean debugMode           = false;

    // ── Chunk ─────────────────────────────────────────────────────────
    public int     maxRenderDistance   = 12;
    public int     minRenderDistance   = 4;
    public boolean dynamicChunkReduce  = true;
    public int     fpsThresholdChunk   = 40;
    public boolean asyncChunkLoading   = true;
    public boolean smartChunkCulling   = true;

    // ── Memory ────────────────────────────────────────────────────────
    public boolean gcOptimization      = true;
    public int     gcIntervalSeconds   = 120;
    public boolean textureCompression  = true;
    public boolean entityCulling       = true;
    public int     entityCullDistance  = 64;
    public boolean particleLimiter     = true;
    public int     particleMaxCount    = 500;
    public int     fpsThresholdParticle= 35;
    public boolean leakDetection       = true;

    // ── Render ────────────────────────────────────────────────────────
    public boolean fastMath            = true;
    public boolean vSyncToggle         = false;
    public boolean adaptiveFrameLimiter= true;
    public int     targetFPS           = 120;
    public boolean reduceTicks         = true;
    public int     tickReduceDistance  = 48;
    public boolean optimizedLighting   = true;
    public boolean fastBlockRender     = true;
    public boolean frustumCulling      = true;

    // ── Advanced ──────────────────────────────────────────────────────
    public boolean minimalHUD          = false;
    public boolean showFPSCounter      = true;
    public boolean showPerfOverlay     = false;
    public boolean hotReloadConfig     = true;

    // ─────────────────────────────────────────────────────────────────
    private final Configuration forge;

    public ConfigManager(File configFile) {
        this.forge = new Configuration(configFile);
    }

    public void load() {
        try {
            forge.load();
            syncFromFile();
        } catch (Exception e) {
            FPSBoost.LOGGER.error("Failed to load config, using defaults", e);
        } finally {
            if (forge.hasChanged()) {
                forge.save();
            }
        }
    }

    private void syncFromFile() {
        forge.setCategoryComment(ConfigCategories.GENERAL,  "General FPSBoost settings");
        forge.setCategoryComment(ConfigCategories.CHUNK,    "Chunk render optimisation settings");
        forge.setCategoryComment(ConfigCategories.MEMORY,   "Memory and GC optimisation settings");
        forge.setCategoryComment(ConfigCategories.RENDER,   "Render optimisation settings");
        forge.setCategoryComment(ConfigCategories.ADVANCED, "Advanced / HUD settings");

        // ── General ──────────────────────────────────────────────────
        enableMod     = forge.getBoolean("enableMod",     ConfigCategories.GENERAL,  true,
                            "Master toggle for the entire mod.");
        activeProfile = forge.getString( "activeProfile", ConfigCategories.GENERAL,  "Balanced",
                            "Active performance profile. Values: Max FPS | Balanced | Quality");
        debugMode     = forge.getBoolean("debugMode",     ConfigCategories.GENERAL,  false,
                            "Enable debug logging.");

        // ── Chunk ─────────────────────────────────────────────────────
        maxRenderDistance  = forge.getInt("maxRenderDistance",  ConfigCategories.CHUNK, 12, 2, 32,
                                "Maximum chunk render distance.");
        minRenderDistance  = forge.getInt("minRenderDistance",  ConfigCategories.CHUNK,  4, 2, 16,
                                "Minimum render distance allowed during dynamic reduction.");
        dynamicChunkReduce = forge.getBoolean("dynamicChunkReduce", ConfigCategories.CHUNK, true,
                                "Reduce render distance automatically when FPS drops.");
        fpsThresholdChunk  = forge.getInt("fpsThresholdChunk",  ConfigCategories.CHUNK, 40, 10, 120,
                                "FPS threshold that triggers dynamic chunk reduction.");
        asyncChunkLoading  = forge.getBoolean("asyncChunkLoading",  ConfigCategories.CHUNK, true,
                                "Load chunks on a background thread to reduce lag spikes.");
        smartChunkCulling  = forge.getBoolean("smartChunkCulling",  ConfigCategories.CHUNK, true,
                                "Skip rendering chunks outside the view frustum.");

        // ── Memory ────────────────────────────────────────────────────
        gcOptimization       = forge.getBoolean("gcOptimization",       ConfigCategories.MEMORY, true,
                                   "Enable periodic garbage collection nudge.");
        gcIntervalSeconds    = forge.getInt("gcIntervalSeconds",         ConfigCategories.MEMORY, 120, 30, 600,
                                   "Seconds between GC nudges.");
        textureCompression   = forge.getBoolean("textureCompression",    ConfigCategories.MEMORY, true,
                                   "Enable texture atlas compression.");
        entityCulling        = forge.getBoolean("entityCulling",         ConfigCategories.MEMORY, true,
                                   "Skip rendering entities outside the view frustum.");
        entityCullDistance   = forge.getInt("entityCullDistance",        ConfigCategories.MEMORY, 64, 16, 128,
                                   "Distance (blocks) beyond which entities are culled.");
        particleLimiter      = forge.getBoolean("particleLimiter",       ConfigCategories.MEMORY, true,
                                   "Limit the number of active particles.");
        particleMaxCount     = forge.getInt("particleMaxCount",          ConfigCategories.MEMORY, 500, 50, 4000,
                                   "Max particles allowed when limiter is active.");
        fpsThresholdParticle = forge.getInt("fpsThresholdParticle",      ConfigCategories.MEMORY, 35, 10, 120,
                                   "FPS threshold that activates the particle limiter.");
        leakDetection        = forge.getBoolean("leakDetection",         ConfigCategories.MEMORY, true,
                                   "Enable memory leak monitoring.");

        // ── Render ────────────────────────────────────────────────────
        fastMath             = forge.getBoolean("fastMath",             ConfigCategories.RENDER, true,
                                   "Use lookup-table sin/cos instead of java.lang.Math.");
        vSyncToggle          = forge.getBoolean("vSyncToggle",          ConfigCategories.RENDER, false,
                                   "Force VSync off for lower latency.");
        adaptiveFrameLimiter = forge.getBoolean("adaptiveFrameLimiter", ConfigCategories.RENDER, true,
                                   "Enable smart adaptive frame limiter.");
        targetFPS            = forge.getInt("targetFPS",                ConfigCategories.RENDER, 120, 30, 300,
                                   "Target FPS for adaptive frame limiter.");
        reduceTicks          = forge.getBoolean("reduceTicks",          ConfigCategories.RENDER, true,
                                   "Reduce tick frequency for distant entities.");
        tickReduceDistance   = forge.getInt("tickReduceDistance",       ConfigCategories.RENDER, 48, 16, 128,
                                   "Distance (blocks) beyond which entity ticks are throttled.");
        optimizedLighting    = forge.getBoolean("optimizedLighting",    ConfigCategories.RENDER, true,
                                   "Batch and defer lighting updates.");
        fastBlockRender      = forge.getBoolean("fastBlockRender",      ConfigCategories.RENDER, true,
                                   "Skip redundant render state checks for blocks.");
        frustumCulling       = forge.getBoolean("frustumCulling",       ConfigCategories.RENDER, true,
                                   "Enable view frustum culling for blocks and entities.");

        // ── Advanced ──────────────────────────────────────────────────
        minimalHUD           = forge.getBoolean("minimalHUD",     ConfigCategories.ADVANCED, false,
                                   "Hide non-essential HUD elements.");
        showFPSCounter       = forge.getBoolean("showFPSCounter", ConfigCategories.ADVANCED, true,
                                   "Show FPS counter overlay.");
        showPerfOverlay      = forge.getBoolean("showPerfOverlay",ConfigCategories.ADVANCED, false,
                                   "Show detailed performance metrics overlay.");
        hotReloadConfig      = forge.getBoolean("hotReloadConfig",ConfigCategories.ADVANCED, true,
                                   "Reload config without restarting the game.");
    }

    // ─── Hot-reload ──────────────────────────────────────────────────
    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(FPSBoost.MOD_ID) && hotReloadConfig) {
            load();
            FPSBoost.LOGGER.info("[FPSBoost] Config hot-reloaded.");
        }
    }

    // ─── Profile system ───────────────────────────────────────────────
    public void applyActiveProfile() {
        switch (activeProfile) {
            case "Max FPS":
                applyMaxFPS();
                break;
            case "Quality":
                applyQuality();
                break;
            default: // Balanced — use file values as-is
                FPSBoost.LOGGER.info("[FPSBoost] Profile: Balanced (file values)");
        }
    }

    private void applyMaxFPS() {
        FPSBoost.LOGGER.info("[FPSBoost] Applying profile: Max FPS");
        maxRenderDistance    = Math.min(maxRenderDistance, 8);
        minRenderDistance    = 2;
        dynamicChunkReduce   = true;
        fpsThresholdChunk    = 60;
        asyncChunkLoading    = true;
        smartChunkCulling    = true;
        entityCulling        = true;
        entityCullDistance   = 32;
        particleLimiter      = true;
        particleMaxCount     = 100;
        fpsThresholdParticle = 60;
        fastMath             = true;
        adaptiveFrameLimiter = true;
        reduceTicks          = true;
        tickReduceDistance   = 32;
        optimizedLighting    = true;
        fastBlockRender      = true;
        frustumCulling       = true;
        gcOptimization       = true;
        gcIntervalSeconds    = 60;
    }

    private void applyQuality() {
        FPSBoost.LOGGER.info("[FPSBoost] Applying profile: Quality");
        dynamicChunkReduce   = false;
        entityCulling        = true;
        entityCullDistance   = 96;
        particleLimiter      = false;
        fastMath             = true;
        adaptiveFrameLimiter = false;
        reduceTicks          = false;
        optimizedLighting    = true;
        fastBlockRender      = false;
        frustumCulling       = true;
        gcOptimization       = true;
        gcIntervalSeconds    = 180;
    }

    // ─── Accessors ───────────────────────────────────────────────────
    public Configuration getForgeConfig()  { return forge; }
    public String  getActiveProfile()      { return activeProfile; }
    public boolean isEnabled()             { return enableMod; }
    public boolean isDebug()               { return debugMode; }

    public void save() {
        if (forge.hasChanged()) forge.save();
    }
}
