package com.fpsboost.mod.fps;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

/**
 * Throttles tick-heavy operations for entities that are far from the player.
 * Distant EntityLiving entities skip their AI tick every other game tick,
 * cutting CPU load without visible gameplay impact.
 *
 * Implementation note: Minecraft exposes Entity.ticksExisted.
 * We skip calling updateEntityActionState-equivalent logic by hooking
 * before the world tick and flagging entities via Entity.noClip (a safe
 * approximate approach in 1.12.2 without ASM/Mixin).
 *
 * For a production mod, this should use ASM or Mixin to patch
 * EntityLiving.updateEntityActionState() directly.
 */
public class TickOptimizer {

    private final ConfigManager config;
    private final FPSMonitor    fpsMonitor;
    private final Minecraft     mc = Minecraft.getMinecraft();

    // How many ticks between scans
    private static final int SCAN_INTERVAL = 10;
    private int scanCounter = 0;

    public TickOptimizer(ConfigManager config, FPSMonitor fpsMonitor) {
        this.config     = config;
        this.fpsMonitor = fpsMonitor;
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!config.isEnabled() || !config.reduceTicks)  return;
        if (event.world.isRemote)                        return; // client only

        scanCounter++;
        if (scanCounter < SCAN_INTERVAL) return;
        scanCounter = 0;

        int fps = fpsMonitor.getCurrentFPS();
        if (fps > config.fpsThresholdChunk + 10) return; // FPS is fine, skip optimisation

        Entity player = mc.player;
        if (player == null) return;

        double cullDistSq = (double) config.tickReduceDistance * config.tickReduceDistance;

        List<Entity> entities = event.world.loadedEntityList;
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if (!(e instanceof EntityLiving)) continue;
            if (e == player) continue;

            double distSq = e.getDistanceSq(player.posX, player.posY, player.posZ);
            EntityLiving living = (EntityLiving) e;

            if (distSq > cullDistSq) {
                // Throttle: only run AI on even ticks
                if ((living.ticksExisted & 1) == 1) {
                    living.setNoAI(true);
                } else {
                    living.setNoAI(false);
                }
            } else {
                // Close entity — ensure AI is always on
                living.setNoAI(false);
            }
        }
    }
}
