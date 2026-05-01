package com.fpsboost.mod.memory;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Detects potential memory leaks by monitoring heap growth over time.
 * Uses a sliding window of heap samples; if N consecutive samples show
 * monotonically increasing heap usage, a warning is emitted.
 */
public class LeakDetector {

    private static final int    SAMPLE_WINDOW   = 5;   // consecutive rising samples = alert
    private static final long   SAMPLE_INTERVAL = 30L; // seconds between samples
    private static final long   MB              = 1024L * 1024L;

    private final ConfigManager            config;
    private final ScheduledExecutorService scheduler;
    private final List<Long>               samples = new ArrayList<>();
    private       ScheduledFuture<?>       task;
    private       int                      alertCount = 0;

    public LeakDetector(ConfigManager config) {
        this.config    = config;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "fpsboost-leak");
            t.setDaemon(true);
            return t;
        });
    }

    public void startMonitoring() {
        if (!config.leakDetection) {
            FPSBoost.LOGGER.info("[LeakDetector] Disabled by config.");
            return;
        }
        task = scheduler.scheduleAtFixedRate(this::sampleAndCheck,
                SAMPLE_INTERVAL, SAMPLE_INTERVAL, TimeUnit.SECONDS);
        FPSBoost.LOGGER.info("[LeakDetector] Monitoring started.");
    }

    private void sampleAndCheck() {
        try {
            Runtime rt   = Runtime.getRuntime();
            long    used = (rt.totalMemory() - rt.freeMemory()) / MB;

            synchronized (samples) {
                samples.add(used);
                if (samples.size() > SAMPLE_WINDOW) {
                    samples.remove(0);
                }

                if (samples.size() == SAMPLE_WINDOW && isMonotonicallyIncreasing(samples)) {
                    alertCount++;
                    FPSBoost.LOGGER.warn(
                        "[LeakDetector] Possible memory leak detected! "
                        + "Heap has grown for {} consecutive samples. "
                        + "Values (MB): {} (alert #{})",
                        SAMPLE_WINDOW, samples, alertCount);
                }
            }

            if (config.isDebug()) {
                FPSBoost.LOGGER.debug("[LeakDetector] Heap sample: {} MB", used);
            }
        } catch (Exception e) {
            FPSBoost.LOGGER.warn("[LeakDetector] Sample error: {}", e.getMessage());
        }
    }

    private static boolean isMonotonicallyIncreasing(List<Long> list) {
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i) <= list.get(i - 1)) return false;
        }
        return true;
    }

    public void stopMonitoring() {
        if (task != null) task.cancel(false);
        scheduler.shutdown();
    }

    public int getAlertCount() { return alertCount; }
}
