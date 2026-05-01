package com.fpsboost.mod;

import com.fpsboost.mod.chunk.AsyncChunkLoader;
import com.fpsboost.mod.chunk.ChunkOptimizer;
import com.fpsboost.mod.chunk.FrustumCulling;
import com.fpsboost.mod.config.ConfigManager;
import com.fpsboost.mod.fps.AdaptiveLimiter;
import com.fpsboost.mod.fps.FPSMonitor;
import com.fpsboost.mod.fps.TickOptimizer;
import com.fpsboost.mod.gui.FPSOverlay;
import com.fpsboost.mod.gui.PerformanceHud;
import com.fpsboost.mod.memory.GCOptimizer;
import com.fpsboost.mod.memory.LeakDetector;
import com.fpsboost.mod.memory.MemoryManager;
import com.fpsboost.mod.render.EntityCulling;
import com.fpsboost.mod.render.FastMath;
import com.fpsboost.mod.render.ParticleLimiter;
import com.fpsboost.mod.render.RenderOptimizer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid       = FPSBoost.MOD_ID,
    name        = FPSBoost.MOD_NAME,
    version     = FPSBoost.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    guiFactory  = "com.fpsboost.mod.gui.ConfigGui"
)
public class FPSBoost {

    public static final String MOD_ID   = "fpsboost";
    public static final String MOD_NAME = "FPS Boost";
    public static final String VERSION  = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.Instance(MOD_ID)
    public static FPSBoost instance;

    // Subsystem references
    private ConfigManager    configManager;
    private FPSMonitor       fpsMonitor;
    private ChunkOptimizer   chunkOptimizer;
    private AsyncChunkLoader asyncChunkLoader;
    private FrustumCulling   frustumCulling;
    private MemoryManager    memoryManager;
    private GCOptimizer      gcOptimizer;
    private LeakDetector     leakDetector;
    private FastMath         fastMath;
    private EntityCulling    entityCulling;
    private ParticleLimiter  particleLimiter;
    private RenderOptimizer  renderOptimizer;
    private AdaptiveLimiter  adaptiveLimiter;
    private TickOptimizer    tickOptimizer;
    private FPSOverlay       fpsOverlay;
    private PerformanceHud   performanceHud;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("=== FPSBoost {} PreInit ===", VERSION);

        // Initialize config first — everything else reads from it
        configManager = new ConfigManager(event.getSuggestedConfigurationFile());
        configManager.load();

        // FastMath lookup tables — no MC dependency, safe to init early
        fastMath = new FastMath();
        FastMath.init();

        LOGGER.info("Config loaded. Profile: {}", configManager.getActiveProfile());
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("=== FPSBoost {} Init ===", VERSION);

        // Build subsystems
        fpsMonitor       = new FPSMonitor();
        chunkOptimizer   = new ChunkOptimizer(configManager, fpsMonitor);
        asyncChunkLoader = new AsyncChunkLoader(configManager);
        frustumCulling   = new FrustumCulling(configManager);
        memoryManager    = new MemoryManager(configManager);
        gcOptimizer      = new GCOptimizer(configManager);
        leakDetector     = new LeakDetector(configManager);
        entityCulling    = new EntityCulling(configManager, fpsMonitor);
        particleLimiter  = new ParticleLimiter(configManager, fpsMonitor);
        renderOptimizer  = new RenderOptimizer(configManager);
        adaptiveLimiter  = new AdaptiveLimiter(configManager, fpsMonitor);
        tickOptimizer    = new TickOptimizer(configManager, fpsMonitor);
        fpsOverlay       = new FPSOverlay(fpsMonitor);
        performanceHud   = new PerformanceHud(configManager, fpsMonitor, memoryManager);

        // Register all event listeners on the Forge event bus
        registerEventListeners();

        LOGGER.info("All subsystems registered.");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("=== FPSBoost {} PostInit ===", VERSION);

        // Apply profile settings after all mods have initialised
        configManager.applyActiveProfile();
        memoryManager.startScheduler();
        leakDetector.startMonitoring();
        gcOptimizer.schedulePeriodicGC();

        LOGGER.info("FPSBoost fully loaded. Enjoy smooth frames!");
    }

    // ---------------------------------------------------------------
    private void registerEventListeners() {
        MinecraftForge.EVENT_BUS.register(fpsMonitor);
        MinecraftForge.EVENT_BUS.register(chunkOptimizer);
        MinecraftForge.EVENT_BUS.register(asyncChunkLoader);
        MinecraftForge.EVENT_BUS.register(frustumCulling);
        MinecraftForge.EVENT_BUS.register(memoryManager);
        MinecraftForge.EVENT_BUS.register(entityCulling);
        MinecraftForge.EVENT_BUS.register(particleLimiter);
        MinecraftForge.EVENT_BUS.register(renderOptimizer);
        MinecraftForge.EVENT_BUS.register(adaptiveLimiter);
        MinecraftForge.EVENT_BUS.register(tickOptimizer);
        MinecraftForge.EVENT_BUS.register(fpsOverlay);
        MinecraftForge.EVENT_BUS.register(performanceHud);
        MinecraftForge.EVENT_BUS.register(configManager); // hot-reload listener
    }

    // ---------------------------------------------------------------
    // Static accessors used by subsystems
    // ---------------------------------------------------------------
    public static ConfigManager  getConfig()       { return instance.configManager; }
    public static FPSMonitor     getFPSMonitor()   { return instance.fpsMonitor; }
    public static MemoryManager  getMemory()       { return instance.memoryManager; }
}
