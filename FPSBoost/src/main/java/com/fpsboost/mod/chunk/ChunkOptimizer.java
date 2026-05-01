package com.fpsboost.mod.chunk;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.config.ConfigManager;
import com.fpsboost.mod.fps.FPSMonitor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Dynamically adjusts the chunk render distance based on current FPS.
 * Hooks into the client tick to read FPS and update render settings.
 */
public class ChunkOptimizer {

    private final ConfigManager config;
    private final FPSMonitor    fpsMonitor;
    private final Minecraft     mc = Minecraft.getMinecraft();

    // Tracks the render distance we last applied to avoid redundant calls
    private int  lastAppliedRenderDist = -1;
    // Cooldown to avoid thrashing the render distance every tick
    private int  tickCooldown          = 0;
    private static final int COOLDOWN_TICKS = 60; // ~3 seconds

    public ChunkOptimizer(ConfigManager config, FPSMonitor fpsMonitor) {
        this.config     = config;
        this.fpsMonitor = fpsMonitor;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!config.isEnabled() || !config.dynamicChunkReduce) return;
        if (mc.world == null || mc.player == null) return;

        if (tickCooldown > 0) {
            tickCooldown--;
            return;
        }
        tickCooldown = COOLDOWN_TICKS;

        int targetDist = calculateTargetRenderDistance();
        applyRenderDistance(targetDist);
    }

    /**
     * Calculates a target render distance using a simple proportional
     * step from min to max based on how far FPS is above the threshold.
     */
    private int calculateTargetRenderDistance() {
        int   currentFPS = fpsMonitor.getCurrentFPS();
        int   threshold  = config.fpsThresholdChunk;
        int   minDist    = config.minRenderDistance;
        int   maxDist    = config.maxRenderDistance;

        if (currentFPS <= threshold) {
            // FPS too low — step render distance down by 1
            int current = mc.gameSettings.renderDistanceChunks;
            return Math.max(minDist, current - 1);
        } else {
            // FPS healthy — slowly recover render distance
            int current = mc.gameSettings.renderDistanceChunks;
            return Math.min(maxDist, current + 1);
        }
    }

    private void applyRenderDistance(int dist) {
        if (dist == lastAppliedRenderDist) return;

        lastAppliedRenderDist = dist;
        // gameSettings.renderDistanceChunks must be written on the main thread
        mc.addScheduledTask(() -> {
            if (mc.gameSettings.renderDistanceChunks != dist) {
                mc.gameSettings.renderDistanceChunks = dist;
                if (mc.renderGlobal != null) {
                    mc.renderGlobal.setDisplayListEntitiesDirty();
                }
                if (config.isDebug()) {
                    FPSBoost.LOGGER.debug("[ChunkOptimizer] Render distance -> {}", dist);
                }
            }
        });
    }

    /** Called externally (e.g. on profile switch) to reset to max distance */
    public void resetToMax() {
        applyRenderDistance(config.maxRenderDistance);
    }
}
