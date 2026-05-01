package com.fpsboost.mod.render;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Applies miscellaneous render optimisations:
 *  - Deferred / batched lighting updates
 *  - VSync enforcement via game settings
 *  - Fast block rendering flag
 */
public class RenderOptimizer {

    private final ConfigManager config;
    private final Minecraft     mc = Minecraft.getMinecraft();

    // Track last applied settings to avoid redundant writes
    private Boolean lastVSync     = null;
    private Boolean lastFancyGfx  = null;

    // Lighting update counter — used to batch deferred updates
    private int deferredLightingTick = 0;
    private static final int LIGHTING_BATCH_TICKS = 3; // coalesce updates across 3 ticks

    public RenderOptimizer(ConfigManager config) {
        this.config = config;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!config.isEnabled()) return;

        applyVSyncSetting();
        applyFancyGraphicsSetting();
        handleDeferredLighting();
    }

    private void applyVSyncSetting() {
        if (!config.vSyncToggle) return; // user hasn't set a preference
        boolean desired = false; // always disable VSync for best FPS
        if (lastVSync != null && lastVSync == desired) return;
        mc.addScheduledTask(() -> {
            mc.gameSettings.enableVsync = desired;
            net.minecraft.client.renderer.OpenGlHelper.openGlRenderer.setVSync(desired);
            lastVSync = desired;
            FPSBoost.LOGGER.info("[RenderOptimizer] VSync set to {}", desired);
        });
    }

    private void applyFancyGraphicsSetting() {
        if (!config.fastBlockRender) return;
        // Fast graphics = disable fancy leaves / water
        boolean desired = false;
        if (lastFancyGfx != null && lastFancyGfx == desired) return;
        mc.addScheduledTask(() -> {
            mc.gameSettings.fancyGraphics = desired;
            mc.renderGlobal.loadRenderers();
            lastFancyGfx = desired;
            FPSBoost.LOGGER.info("[RenderOptimizer] fancyGraphics -> {}", desired);
        });
    }

    private void handleDeferredLighting() {
        if (!config.optimizedLighting) return;
        deferredLightingTick++;
        if (deferredLightingTick >= LIGHTING_BATCH_TICKS) {
            deferredLightingTick = 0;
            // Mark light map dirty so Minecraft processes queued updates in one pass
            mc.addScheduledTask(() -> {
                if (mc.entityRenderer != null) {
                    mc.entityRenderer.updateLightmap(1.0f);
                }
            });
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        // Hook point for any per-frame render optimisations
        if (!config.isEnabled()) return;
        // Currently used as a timing anchor; extend as needed
    }
}
