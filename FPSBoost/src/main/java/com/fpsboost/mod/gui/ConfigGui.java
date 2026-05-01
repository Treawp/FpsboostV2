package com.fpsboost.mod.gui;

import com.fpsboost.mod.FPSBoost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;

import java.util.Set;

/**
 * Hooks FPSBoost into the Forge "Mod Options" button in the mod list,
 * opening a standard GuiConfig screen backed by our ConfigManager.
 */
public class ConfigGui implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {
        // Nothing to initialise
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiConfig(
                parentScreen,
                FPSBoost.getConfig().getForgeConfig().getCategoryNames(),
                FPSBoost.MOD_ID,
                false,
                false,
                GuiConfig.getAbridgedConfigPath(FPSBoost.getConfig().getForgeConfig().toString())
        );
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }
}
