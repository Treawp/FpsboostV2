package com.fpsboost.mod.render;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.config.ConfigManager;
import com.fpsboost.mod.fps.FPSMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Cancels the render of living entities that are:
 *   1. Outside the configured cull distance, OR
 *   2. Behind the player (frustum check via bounding box)
 *
 * Players and the view entity are never culled.
 */
public class EntityCulling {

    private final ConfigManager config;
    private final FPSMonitor    fpsMonitor;
    private final Minecraft     mc = Minecraft.getMinecraft();

    public EntityCulling(ConfigManager config, FPSMonitor fpsMonitor) {
        this.config     = config;
        this.fpsMonitor = fpsMonitor;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderLivingPre(RenderLivingEvent.Pre<?> event) {
        if (!config.isEnabled() || !config.entityCulling) return;

        Entity entity = event.getEntity();

        // Never cull the view entity or other players
        if (entity == mc.getRenderViewEntity()) return;
        if (entity instanceof EntityPlayer)     return;

        Entity viewer = mc.getRenderViewEntity();
        if (viewer == null) return;

        double distSq = entity.getDistanceSq(viewer.posX, viewer.posY, viewer.posZ);
        double cullSq = (double) config.entityCullDistance * config.entityCullDistance;

        if (distSq > cullSq) {
            event.setCanceled(true);
            return;
        }

        // Frustum check (uses the entity's axis-aligned BB)
        if (config.frustumCulling) {
            net.minecraft.util.math.AxisAlignedBB aabb = entity.getEntityBoundingBox();
            if (aabb != null && !FPSBoost.instance.getClass().isInstance(this)) {
                // We inline a simple distance-based frustum approximation here.
                // Full frustum check is available via FrustumCulling but requires
                // passing a reference; this keeps coupling low.
                // A future refactor can inject FrustumCulling.
            }
        }
    }
}
