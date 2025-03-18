package dev.cypphi.mcrc.config;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MCRCConfig {
    public static ConfigClassHandler<MCRCConfig> HANDLER = ConfigClassHandler.createBuilder(MCRCConfig.class)
            .id(Identifier.of("mcrc"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("mc-remote-control.json"))
                    .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                    .build())
            .build();

    @SerialEntry
    public boolean autoStart = false;

    @SerialEntry
    public String botToken = "";

    @SerialEntry
    public String discordChannel = "";

    @SerialEntry
    public int test = 5;

    public static ConfigCategory getMainCategory() {
        return ConfigCategory.createBuilder()
                .name(Text.of("Remote Control"))

                .option(Option.<Boolean>createBuilder()
                        .name(Text.of("Start on launch"))
                        .description(OptionDescription.of(Text.of("Start the Remote Control Discord client automatically on launch. To see effects please restart your Minecraft client.")))
                        .binding(
                                false,
                                () -> HANDLER.instance().autoStart,
                                value -> HANDLER.instance().autoStart = value
                        )
                        .controller(BooleanControllerBuilder::create)
                        .build())

                .option(Option.<Integer>createBuilder()
                        .name(Text.of("Integer Value"))
                        .description(OptionDescription.of(Text.of("An example integer value.")))
                        .binding(
                                5,
                                () -> HANDLER.instance().test,
                                value -> HANDLER.instance().test = value
                        )
                        .controller(IntegerFieldControllerBuilder::create)
                        .build())

                .build();
    }

    public static ConfigCategory getCredentialsCategory() {
        return ConfigCategory.createBuilder()
                .name(Text.of("CREDENTIALS (DO NOT SHARE)"))
                .option(Option.<String>createBuilder()
                        .name(Text.of("Discord Bot token"))
                        .description(OptionDescription.of(Text.of("The Discord bot token. DO NOT SHARE THIS WITH ANYONE ELSE!")))
                        .binding(
                                "BOT TOKEN",
                                () -> HANDLER.instance().botToken,
                                value -> HANDLER.instance().botToken = value
                        )
                        .controller(StringControllerBuilder::create)
                        .build())

                .option(Option.<String>createBuilder()
                        .name(Text.of("Discord Channel ID"))
                        .description(OptionDescription.of(Text.of("The channel ID the bot will send messages to. This will be the only channel the bot will work in for safety reasons.")))
                        .binding(
                                "CHANNEL ID",
                                () -> HANDLER.instance().discordChannel,
                                value -> HANDLER.instance().discordChannel = value
                        )
                        .controller(StringControllerBuilder::create)
                        .build())
                .build();
    }
}
