package com.fpsboost.mod.memory;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.config.ConfigManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Schedules periodic System.gc() hints on a background daemon thread,
 * timed to occur during low-activity windows.  We only request a GC; the
 * JVM is free to ignore it, so this is a "nudge" rather than a force.
 */
public class GCOptimizer {

    private final ConfigManager            config;
    private final ScheduledExecutorService scheduler;
    private       ScheduledFuture<?>       gcTask;

    // Track total GC nudges for metrics
    private volatile int nudgeCount = 0;

    public GCOptimizer(ConfigManager config) {
        this.config    = config;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "fpsboost-gc");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    public void schedulePeriodicGC() {
        if (!config.gcOptimization) {
            FPSBoost.LOGGER.info("[GCOptimizer] GC optimisation disabled by config.");
            return;
        }
        if (gcTask != null && !gcTask.isCancelled()) gcTask.cancel(false);

        long interval = config.gcIntervalSeconds;
        gcTask = scheduler.scheduleAtFixedRate(this::performGCNudge,
                interval, interval, TimeUnit.SECONDS);
        FPSBoost.LOGGER.info("[GCOptimizer] Scheduled GC nudge every {}s.", interval);
    }

    private void performGCNudge() {
        try {
            long before = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            // Hint to the JVM
            System.gc();

            // Give GC a moment to run before measuring
            Thread.sleep(200);

            long after  = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long freed  = Math.max(0L, before - after) / (1024L * 1024L);
            nudgeCount++;

            FPSBoost.LOGGER.info("[GCOptimizer] GC nudge #{} — freed ~{} MB", nudgeCount, freed);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            FPSBoost.LOGGER.warn("[GCOptimizer] GC nudge error: {}", e.getMessage());
        }
    }

    public void reschedule(int newIntervalSeconds) {
        config.gcIntervalSeconds = newIntervalSeconds;
        schedulePeriodicGC();
    }

    public void shutdown() {
        if (gcTask != null) gcTask.cancel(false);
        scheduler.shutdown();
    }

    public int getNudgeCount() { return nudgeCount; }
}
