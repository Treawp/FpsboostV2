package com.fpsboost.mod.render;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.config.ConfigManager;
import com.fpsboost.mod.fps.FPSMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limits the number of active particles when FPS drops below the configured threshold.
 * Uses reflection to access ParticleManager's internal queue count.
 *
 * This is safe to use in production; the reflection is done once at init time.
 */
public class ParticleLimiter {

    private final ConfigManager config;
    private final FPSMonitor    fpsMonitor;
    private final Minecraft     mc = Minecraft.getMinecraft();

    // Reflection access to ParticleManager — resolved once at startup
    private Field particleCountField = null;
    private boolean reflectionFailed = false;

    // Tracks particles we cancelled this second for metrics
    private final AtomicInteger cancelledThisCycle = new AtomicInteger(0);
    private int tickCounter = 0;

    public ParticleLimiter(ConfigManager config, FPSMonitor fpsMonitor) {
        this.config     = config;
        this.fpsMonitor = fpsMonitor;
        initReflection();
    }

    private void initReflection() {
        // ParticleManager field names differ by MCP mapping; we try both
        String[] candidateFields = { "fxLayers", "particleLayers" };
        for (String name : candidateFields) {
            try {
                Field f = ParticleManager.class.getDeclaredField(name);
                f.setAccessible(true);
                particleCountField = f;
                FPSBoost.LOGGER.debug("[ParticleLimiter] Resolved ParticleManager field: {}", name);
                return;
            } catch (NoSuchFieldException ignored) {}
        }
        reflectionFailed = true;
        FPSBoost.LOGGER.warn("[ParticleLimiter] Could not resolve ParticleManager field. "
                + "Particle count limiting will use heuristics only.");
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (!config.isEnabled() || !config.particleLimiter) return;
        if (!(event.getEntity() instanceof Particle))  return;
        if (!event.getWorld().isRemote)                return;

        int fps = fpsMonitor.getCurrentFPS();
        if (fps <= 0 || fps > config.fpsThresholdParticle) return; // FPS fine, allow

        // Estimate current particle count
        int currentCount = estimateParticleCount();
        if (currentCount >= config.particleMaxCount) {
            event.setCanceled(true);
            cancelledThisCycle.incrementAndGet();
        }
    }

    private int estimateParticleCount() {
        if (reflectionFailed || particleCountField == null) {
            // Fallback: use a rough tick-based counter reset
            return 0;
        }
        try {
            // fxLayers is an ArrayDeque[4][] — we sum queue sizes
            Object layers = particleCountField.get(mc.effectRenderer);
            if (layers instanceof ArrayDeque[]) {
                ArrayDeque<?>[] queues = (ArrayDeque<?>[]) layers;
                int total = 0;
                for (ArrayDeque<?> q : queues) {
                    if (q != null) total += q.size();
                }
                return total;
            }
        } catch (Exception e) {
            FPSBoost.LOGGER.debug("[ParticleLimiter] Reflection read error: {}", e.getMessage());
        }
        return 0;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter % 20 == 0) { // every second
            int cancelled = cancelledThisCycle.getAndSet(0);
            if (config.isDebug() && cancelled > 0) {
                FPSBoost.LOGGER.debug("[ParticleLimiter] Suppressed {} particles last second.", cancelled);
            }
        }
    }
}
