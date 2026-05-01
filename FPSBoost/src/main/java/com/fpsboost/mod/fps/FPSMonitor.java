package com.fpsboost.mod.fps;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks current FPS using a moving average over a configurable window.
 * Other subsystems read from this rather than calling Minecraft.getDebugFPS()
 * directly to avoid redundant per-frame computations.
 */
public class FPSMonitor {

    private static final int   SAMPLE_WINDOW  = 20;    // ticks (≈1 second)
    private static final long  NANOS_PER_SEC  = 1_000_000_000L;

    private final Deque<Long> frameTimes   = new ArrayDeque<>(SAMPLE_WINDOW + 1);
    private       long        lastFrameNs  = System.nanoTime();

    // Computed values
    private volatile int   currentFPS    = 0;
    private volatile float averageFPS    = 0f;
    private volatile int   minFPS1s      = Integer.MAX_VALUE;
    private volatile int   maxFPS1s      = 0;

    // Frame counter for min/max window
    private final Deque<Integer> fpsHistory = new ArrayDeque<>(60);

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        long now       = System.nanoTime();
        long frameDelta = now - lastFrameNs;
        lastFrameNs    = now;

        if (frameDelta <= 0) return;

        // Instant FPS from last frame time
        currentFPS = (int) (NANOS_PER_SEC / frameDelta);

        // Push sample and maintain window
        frameTimes.addLast(frameDelta);
        if (frameTimes.size() > SAMPLE_WINDOW) frameTimes.pollFirst();

        // Moving average
        long sumNs = 0;
        for (long ft : frameTimes) sumNs += ft;
        averageFPS = frameTimes.isEmpty() ? 0f
                : (float) NANOS_PER_SEC / (sumNs / (float) frameTimes.size());

        // 1-second min/max rolling window (60-sample history)
        fpsHistory.addLast(currentFPS);
        if (fpsHistory.size() > 60) fpsHistory.pollFirst();
        minFPS1s = fpsHistory.stream().mapToInt(i -> i).min().orElse(0);
        maxFPS1s = fpsHistory.stream().mapToInt(i -> i).max().orElse(0);
    }

    // ─── Accessors ───────────────────────────────────────────────────
    /**
     * Returns the smoothed moving-average FPS as an integer.
     * This is what subsystems should use for threshold comparisons.
     */
    public int getCurrentFPS() {
        int avg = Math.round(averageFPS);
        // Fall back to Minecraft's own counter if monitor hasn't warmed up yet
        if (avg <= 0) return Minecraft.getDebugFPS();
        return avg;
    }

    public int getInstantFPS()   { return currentFPS; }
    public float getAverageFPS() { return averageFPS;  }
    public int getMin1s()        { return minFPS1s == Integer.MAX_VALUE ? 0 : minFPS1s; }
    public int getMax1s()        { return maxFPS1s; }

    /**
     * Returns a performance tier:
     *   GOOD   ≥ 60 fps
     *   OK     30–59 fps
     *   BAD    < 30 fps
     */
    public PerformanceTier getTier() {
        int fps = getCurrentFPS();
        if (fps >= 60) return PerformanceTier.GOOD;
        if (fps >= 30) return PerformanceTier.OK;
        return PerformanceTier.BAD;
    }

    public enum PerformanceTier { GOOD, OK, BAD }
}
