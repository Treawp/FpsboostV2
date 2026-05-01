package com.fpsboost.mod.chunk;

import com.fpsboost.mod.FPSBoost;
import com.fpsboost.mod.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Maintains a view frustum that other subsystems can query to determine
 * whether a given AABB is visible to the player's camera.
 */
public class FrustumCulling {

    private final ConfigManager config;
    private final Frustum       frustum = new Frustum();
    private       boolean       frustumValid = false;

    public FrustumCulling(ConfigManager config) {
        this.config = config;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!config.isEnabled() || !config.frustumCulling) {
            frustumValid = false;
            return;
        }
        updateFrustum(event.getPartialTicks());
    }

    private void updateFrustum(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderViewEntity() == null) {
            frustumValid = false;
            return;
        }
        Entity viewEntity = mc.getRenderViewEntity();
        double x = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * partialTicks;
        double y = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * partialTicks;
        double z = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * partialTicks;
        frustum.setPosition(x, y, z);
        frustumValid = true;
    }

    /**
     * Returns true if the given AABB is inside (or intersects) the view frustum.
     * Always returns true when frustum culling is disabled or frustum is stale.
     */
    public boolean isVisible(AxisAlignedBB aabb) {
        if (!config.frustumCulling || !frustumValid) return true;
        try {
            return frustum.isBoundingBoxInFrustum(aabb);
        } catch (Exception e) {
            FPSBoost.LOGGER.debug("[FrustumCulling] isBoundingBoxInFrustum threw: {}", e.getMessage());
            return true;
        }
    }

    public boolean isFrustumValid() {
        return frustumValid;
    }
}
