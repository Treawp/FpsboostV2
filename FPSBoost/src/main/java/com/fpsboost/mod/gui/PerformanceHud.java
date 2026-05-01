package com.fpsboost.mod.gui;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.config.ConfigManager;
import com.fpsboost.mod.fps.FPSMonitor;
import com.fpsboost.mod.memory.MemoryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed performance overlay showing:
 *   - FPS: current / avg / min-max
 *   - RAM: used / max MB + usage %
 *   - Chunk render distance
 *   - Entity count in loaded world
 *
 * Toggle with the showPerfOverlay config flag.
 * Position: top-left, below hotbar area.
 */
public class PerformanceHud extends Gui {

    private static final int BG_COLOR    = 0x99000000; // semi-transparent black
    private static final int TEXT_COLOR  = 0xFFFFFF;
    private static final int PADDING     = 3;
    private static final int LINE_HEIGHT = 10;

    private final ConfigManager configManager;
    private final FPSMonitor    fpsMonitor;
    private final MemoryManager memoryManager;
    private final Minecraft     mc = Minecraft.getMinecraft();

    public PerformanceHud(ConfigManager configManager, FPSMonitor fpsMonitor, MemoryManager memoryManager) {
        this.configManager  = configManager;
        this.fpsMonitor     = fpsMonitor;
        this.memoryManager  = memoryManager;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (!configManager.isEnabled())      return;
        if (!configManager.showPerfOverlay)  return;
        if (mc.gameSettings.showDebugInfo)   return;

        List<String> lines = buildLines();
        int width  = computeMaxWidth(lines);
        int height = lines.size() * LINE_HEIGHT + PADDING * 2;
        int x      = 2;
        int y      = 32; // below minimap/vanilla elements

        drawRect(x, y, x + width + PADDING * 2, y + height, BG_COLOR);

        FontRenderer font = mc.fontRenderer;
        for (int i = 0; i < lines.size(); i++) {
            font.drawStringWithShadow(lines.get(i), x + PADDING, y + PADDING + i * LINE_HEIGHT, TEXT_COLOR);
        }
    }

    private List<String> buildLines() {
        List<String> lines = new ArrayList<>();

        // ── FPS ───────────────────────────────────────────────────────
        int avg = fpsMonitor.getCurrentFPS();
        int min = fpsMonitor.getMin1s();
        int max = fpsMonitor.getMax1s();
        lines.add(String.format("FPS: %d  avg:%d  %d/%d", fpsMonitor.getInstantFPS(), avg, min, max));

        // ── RAM ───────────────────────────────────────────────────────
        long used   = memoryManager.getUsedMemMB();
        long maxMem = memoryManager.getMaxMemMB();
        int  pct    = maxMem > 0 ? (int) (used * 100 / maxMem) : 0;
        lines.add(String.format("RAM: %d/%d MB  (%d%%)", used, maxMem, pct));

        // ── Chunk distance ────────────────────────────────────────────
        int rd = mc.gameSettings.renderDistanceChunks;
        lines.add(String.format("Chunks: %d / %d", rd, configManager.maxRenderDistance));

        // ── Entity count ──────────────────────────────────────────────
        int entities = (mc.world != null) ? mc.world.loadedEntityList.size() : 0;
        lines.add(String.format("Entities: %d", entities));

        // ── Profile ───────────────────────────────────────────────────
        lines.add("Profile: " + configManager.getActiveProfile());

        return lines;
    }

    private int computeMaxWidth(List<String> lines) {
        FontRenderer font = mc.fontRenderer;
        int max = 0;
        for (String s : lines) {
            int w = font.getStringWidth(s);
            if (w > max) max = w;
        }
        return max;
    }
}
