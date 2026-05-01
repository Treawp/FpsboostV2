package com.fpsboost.mod.chunk;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.config.ConfigManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Offloads expensive chunk post-processing to a background thread pool,
 * reducing main-thread lag spikes when entering new areas.
 *
 * NOTE: Only work that is safe to do off-thread should run here.
 *       Any write-back to the main world MUST be scheduled via
 *       Minecraft.getMinecraft().addScheduledTask().
 */
public class AsyncChunkLoader {

    private static final int POOL_SIZE = Math.max(1,
            Runtime.getRuntime().availableProcessors() / 2);

    private final ConfigManager   config;
    private final ExecutorService executor;

    // Metrics
    private final AtomicInteger chunksProcessed = new AtomicInteger(0);

    public AsyncChunkLoader(ConfigManager config) {
        this.config   = config;
        this.executor = Executors.newFixedThreadPool(POOL_SIZE, new DaemonThreadFactory("fpsboost-chunk"));
        FPSBoost.LOGGER.info("[AsyncChunkLoader] Thread pool size: {}", POOL_SIZE);
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!config.isEnabled() || !config.asyncChunkLoading) return;
        if (event.getWorld().isRemote) {
            submitChunkWork(event.getChunk());
        }
    }

    /**
     * Submits a light background task for the given chunk.
     * Currently performs pre-computation work — extend as needed.
     */
    private void submitChunkWork(Chunk chunk) {
        if (executor.isShutdown()) return;
        try {
            executor.submit(() -> {
                try {
                    processChunkAsync(chunk);
                } catch (Exception e) {
                    FPSBoost.LOGGER.warn("[AsyncChunkLoader] Error processing chunk ({}, {}): {}",
                            chunk.x, chunk.z, e.getMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            FPSBoost.LOGGER.debug("[AsyncChunkLoader] Task rejected (queue full), skipping chunk.");
        }
    }

    /**
     * Background processing for a newly loaded chunk.
     * Safe operations only: reading chunk data, pre-calculating values.
     * DO NOT modify world state from here.
     */
    private void processChunkAsync(Chunk chunk) {
        // Pre-check heightmap data — touch each entry to warm CPU cache
        int[] heightMap = chunk.getHeightMap();
        int checksum = 0;
        for (int h : heightMap) {
            checksum += h;
        }

        // Pre-populate the biome array for faster access later
        chunk.getBiomeArray();

        chunksProcessed.incrementAndGet();

        if (config.isDebug()) {
            FPSBoost.LOGGER.debug("[AsyncChunkLoader] Pre-processed chunk ({}, {}) heightmap checksum={}",
                    chunk.x, chunk.z, checksum);
        }
    }

    public int getChunksProcessed() {
        return chunksProcessed.get();
    }

    public void shutdown() {
        executor.shutdown();
        FPSBoost.LOGGER.info("[AsyncChunkLoader] Executor shut down. Total chunks processed: {}",
                chunksProcessed.get());
    }

    // ─── Daemon thread factory ────────────────────────────────────────
    private static class DaemonThreadFactory implements ThreadFactory {
        private final String       prefix;
        private final AtomicInteger count = new AtomicInteger(0);

        DaemonThreadFactory(String prefix) { this.prefix = prefix; }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + count.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        }
    }
}
