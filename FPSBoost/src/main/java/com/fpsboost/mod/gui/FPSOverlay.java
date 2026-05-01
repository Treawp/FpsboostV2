package com.fpsboost.mod.gui;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.fps.FPSMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Renders a compact FPS counter in the top-right corner.
 * Color coding:
 *   GREEN  ≥ 60 fps
 *   YELLOW 30–59 fps
 *   RED    < 30 fps
 */
public class FPSOverlay {

    private static final int COLOR_GREEN  = 0x55FF55;
    private static final int COLOR_YELLOW = 0xFFFF55;
    private static final int COLOR_RED    = 0xFF5555;

    private final FPSMonitor fpsMonitor;
    private final Minecraft  mc = Minecraft.getMinecraft();

    public FPSOverlay(FPSMonitor fpsMonitor) {
        this.fpsMonitor = fpsMonitor;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (!FPSBoost.getConfig().isEnabled())         return;
        if (!FPSBoost.getConfig().showFPSCounter)      return;
        if (mc.gameSettings.showDebugInfo)             return; // vanilla F3 already shows FPS

        int fps   = fpsMonitor.getCurrentFPS();
        int color = fpsColor(fps);

        String label = fps + " fps";

        FontRenderer font = mc.fontRenderer;
        int x = mc.displayWidth  / mc.gameSettings.guiScale - font.getStringWidth(label) - 2;
        int y = 2;

        // Shadow for readability
        font.drawStringWithShadow(label, x, y, color);
    }

    private static int fpsColor(int fps) {
        if (fps >= 60) return COLOR_GREEN;
        if (fps >= 30) return COLOR_YELLOW;
        return COLOR_RED;
    }
}
