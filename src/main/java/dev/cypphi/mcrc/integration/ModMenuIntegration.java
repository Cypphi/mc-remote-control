package dev.cypphi.mcrc.integration;

import dev.cypphi.mcrc.screen.ConfigScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.Function;

public class ModMenuIntegration {
    public static Function<Screen, Screen> getConfigScreen() {
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            return ConfigScreen::new;
        }
        return null;
    }
}
