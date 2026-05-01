package com.fpsboost.mod.fps;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Smart adaptive frame limiter.
 * Writes Minecraft.limitFramerate to throttle the render loop when the game
 * is running well above the target FPS, saving power and reducing heat.
 * Backs off quickly when FPS drops below target.
 */
public class AdaptiveLimiter {

    private static final int HEADROOM        = 5;   // fps above target before limiting kicks in
    private static final int CHECK_INTERVAL  = 40;  // ticks between adjustments (~2 sec)

    private final ConfigManager config;
    private final FPSMonitor    fpsMonitor;
    private final Minecraft     mc = Minecraft.getMinecraft();

    private int  tickCounter    = 0;
    private int  lastApplied    = -1;

    public AdaptiveLimiter(ConfigManager config, FPSMonitor fpsMonitor) {
        this.config     = config;
        this.fpsMonitor = fpsMonitor;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!config.isEnabled() || !config.adaptiveFrameLimiter) return;

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        int fps    = fpsMonitor.getCurrentFPS();
        int target = config.targetFPS;
        int newLimit;

        if (fps >= target + HEADROOM) {
            // We have headroom — gently cap just above target
            newLimit = target + HEADROOM / 2;
        } else {
            // FPS is at or below target — lift the cap (use Minecraft's own setting)
            newLimit = target;
        }

        if (newLimit != lastApplied) {
            final int limit = newLimit;
            mc.addScheduledTask(() -> {
                mc.gameSettings.limitFramerate = limit;
                if (config.isDebug()) {
                    FPSBoost.LOGGER.debug("[AdaptiveLimiter] frameLimit -> {}", limit);
                }
            });
            lastApplied = newLimit;
        }
    }

    public int getLastAppliedLimit() { return lastApplied; }
}
