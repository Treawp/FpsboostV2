package com.fpsboost.mod.memory;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Monitors heap usage and schedules periodic lightweight cleanup tasks.
 * Does NOT call System.gc() directly — that is delegated to GCOptimizer.
 */
public class MemoryManager {

    private static final long  MB              = 1024L * 1024L;
    private static final float ALERT_THRESHOLD = 0.85f; // 85% heap used → alert

    private final ConfigManager             config;
    private final Runtime                   runtime = Runtime.getRuntime();
    private final ScheduledExecutorService  scheduler;

    private ScheduledFuture<?>  monitorTask;
    private long                lastAlertTime   = 0L;
    private long                lastUsedMemory  = 0L;

    // Metrics accessible by HUD
    private volatile long usedMemMB  = 0L;
    private volatile long totalMemMB = 0L;
    private volatile long maxMemMB   = 0L;

    public MemoryManager(ConfigManager config) {
        this.config    = config;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "fpsboost-memory");
            t.setDaemon(true);
            return t;
        });
    }

    public void startScheduler() {
        if (monitorTask != null && !monitorTask.isCancelled()) return;
        long interval = Math.max(10L, config.gcIntervalSeconds / 4L); // sample 4x per GC cycle
        monitorTask = scheduler.scheduleAtFixedRate(this::sampleMemory, 5L, interval, TimeUnit.SECONDS);
        FPSBoost.LOGGER.info("[MemoryManager] Monitoring started (sample every {}s).", interval);
    }

    private void sampleMemory() {
        try {
            long total = runtime.totalMemory();
            long free  = runtime.freeMemory();
            long max   = runtime.maxMemory();
            long used  = total - free;

            usedMemMB  = used  / MB;
            totalMemMB = total / MB;
            maxMemMB   = max   / MB;

            float heapRatio = (float) used / max;

            if (heapRatio >= ALERT_THRESHOLD) {
                long now = System.currentTimeMillis();
                if (now - lastAlertTime > 30_000L) { // throttle alerts to once/30s
                    lastAlertTime = now;
                    FPSBoost.LOGGER.warn("[MemoryManager] High heap usage: {}/{} MB ({:.1f}%)",
                            usedMemMB, maxMemMB, heapRatio * 100f);
                }
            }

            lastUsedMemory = used;

            if (config.isDebug()) {
                FPSBoost.LOGGER.debug("[MemoryManager] Heap: {}/{} MB (max {})",
                        usedMemMB, totalMemMB, maxMemMB);
            }
        } catch (Exception e) {
            FPSBoost.LOGGER.error("[MemoryManager] Sample error", e);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Lightweight tick hook — intentionally empty for now.
        // Subsystems that need per-tick memory checks can be wired here.
    }

    public void stopScheduler() {
        if (monitorTask != null) monitorTask.cancel(false);
        scheduler.shutdown();
    }

    // ─── Public metrics ───────────────────────────────────────────────
    public long getUsedMemMB()  { return usedMemMB;  }
    public long getTotalMemMB() { return totalMemMB; }
    public long getMaxMemMB()   { return maxMemMB;   }

    /** Heap utilisation in 0..1 range. */
    public float getHeapRatio() {
        if (maxMemMB == 0) return 0f;
        return (float) usedMemMB / maxMemMB;
    }
}
