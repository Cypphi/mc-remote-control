package dev.cypphi.mcrc.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import net.minecraft.text.Text;

public class ModMenuEntrypoint implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> YetAnotherConfigLib.createBuilder()
                .title(Text.of("Remote Control"))
                .category(MCRCConfig.getConfigCategory())
                .save(MCRCConfig.HANDLER::save)
                .build()
                .generateScreen(parent);
    }
}
