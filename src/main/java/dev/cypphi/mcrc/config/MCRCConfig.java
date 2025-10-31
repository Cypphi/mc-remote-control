package dev.cypphi.mcrc.config;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import java.util.List;

public class MCRCConfig {
    public static ConfigClassHandler<MCRCConfig> HANDLER = ConfigClassHandler.createBuilder(MCRCConfig.class)
            .id(Identifier.of("mcrc"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("mc-remote-control.json"))
                    .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                    .build())
            .build();

    // -----------------------------------------------------------------
    // Main settings
    // -----------------------------------------------------------------
    @SerialEntry
    public boolean autoStart = false;
    @SerialEntry
    public boolean useEmbeds = true;
    @SerialEntry
    public boolean useEmbedColors = true;
    @SerialEntry
    public boolean allowPublicCommands = false;
    @SerialEntry
    public boolean allowCommandsAnywhere = false;

    // -----------------------------------------------------------------
    // Notifications
    // -----------------------------------------------------------------
    @SerialEntry
    public boolean notifyOnBotReady = true;
    @SerialEntry
    public boolean logChatMessages = true;
    @SerialEntry
    public boolean notifyOnJoinSingleplayer = true;
    @SerialEntry
    public boolean notifyOnJoinMultiplayer = true;
    @SerialEntry
    public boolean notifyOnDisconnectSingleplayer = true;
    @SerialEntry
    public boolean notifyOnDisconnectMultiplayer = true;
    @SerialEntry
    public boolean pingOnDisconnectMultiplayer = true;
    @SerialEntry
    public boolean pingOnMention = false;
    @SerialEntry
    public boolean messageQueueEnabled = false;
    @SerialEntry
    public int messageQueueDelaySeconds = 2;
    @SerialEntry
    public int messageQueueMaxMessages = 5;

    // -----------------------------------------------------------------
    // Remote View
    // -----------------------------------------------------------------
    @SerialEntry
    public boolean remoteViewEnabled = false;
    @SerialEntry
    public int remoteViewFps = 30;
    @SerialEntry
    public String remoteViewBindAddress = "0.0.0.0";
    @SerialEntry
    public int remoteViewPort = 47823;
    @SerialEntry
    public String remoteViewPublicBaseUrl = "";
    @SerialEntry
    public int remoteViewLinkTimeoutSeconds = 120;

    // -----------------------------------------------------------------
    // Discord credentials
    // -----------------------------------------------------------------
    @SerialEntry
    public String botToken = "";
    @SerialEntry
    public String discordChannel = "";
    @SerialEntry
    public String commandGuildId = "";
    @SerialEntry
    public String allowedUserId = "";


    public static ConfigCategory getMainCategory() {
        ConfigCategory.Builder builder = ConfigCategory.createBuilder()
                .name(Text.of("Main"));

        builder.option(Option.<Boolean>createBuilder()
                .name(Text.of("Start on Launch"))
                .description(OptionDescription.of(Text.of("Start the Remote Control Discord client automatically on launch. To see effects please restart your Minecraft client.")))
                .binding(
                        false,
                        () -> HANDLER.instance().autoStart,
                        value -> HANDLER.instance().autoStart = value
                )
                .controller(BooleanControllerBuilder::create)
                .build());

        Option<Boolean> useEmbedsOption = Option.<Boolean>createBuilder()
                .name(Text.of("Use Embeds"))
                .description(OptionDescription.of(Text.of("If the bot should use embeds instead of plain text.")))
                .binding(
                        false,
                        () -> HANDLER.instance().useEmbeds,
                        value -> HANDLER.instance().useEmbeds = value
                )
                .controller(BooleanControllerBuilder::create)
                .build();
        builder.option(useEmbedsOption);

        Option<Boolean> embedColorsOption = Option.<Boolean>createBuilder()
                .name(Text.of("Embed Colors"))
                .description(OptionDescription.of(Text.of("Whether to give embeds a color.")))
                .binding(
                        false,
                        () -> HANDLER.instance().useEmbedColors,
                        value -> HANDLER.instance().useEmbedColors = value
                )
                .controller(BooleanControllerBuilder::create)
                .build();
        builder.option(embedColorsOption);

        useEmbedsOption.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE) {
                boolean enabled = option.pendingValue();
                embedColorsOption.setAvailable(enabled);
            }
        });
        embedColorsOption.setAvailable(HANDLER.instance().useEmbeds);

        builder.option(Option.<Boolean>createBuilder()
                .name(Text.of("Allow Public Commands"))
                .description(OptionDescription.of(
                        Text.literal("DON'T ENABLE IF YOU DON'T WANT OTHER PEOPLE USING THE BOT.")
                                .formatted(Formatting.RED)))
                .binding(
                        false,
                        () -> HANDLER.instance().allowPublicCommands,
                        value -> HANDLER.instance().allowPublicCommands = value
                )
                .controller(option -> BooleanControllerBuilder.create(option).coloured(true))
                .build());

        builder.option(Option.<Boolean>createBuilder()
                .name(Text.of("Allow Commands Anywhere"))
                .description(OptionDescription.of(Text.of("Permit slash commands outside the configured log channel.")))
                .binding(
                        false,
                        () -> HANDLER.instance().allowCommandsAnywhere,
                        value -> HANDLER.instance().allowCommandsAnywhere = value
                )
                .controller(BooleanControllerBuilder::create)
                .build());

        return builder.build();
    }

    public static ConfigCategory getNotificationsCategory() {
        ConfigCategory.Builder builder = ConfigCategory.createBuilder()
                .name(Text.of("Notifications"));

        builder.option(Option.<Boolean>createBuilder()
                .name(Text.of("Bot Ready Event"))
                .description(OptionDescription.of(Text.of("Send a status notification when the Discord bot reports ready.")))
                .binding(
                        true,
                        () -> HANDLER.instance().notifyOnBotReady,
                        value -> HANDLER.instance().notifyOnBotReady = value
                )
                .controller(BooleanControllerBuilder::create)
                .build());

        builder.option(Option.<Boolean>createBuilder()
                .name(Text.of("Chat Logging"))
                .description(OptionDescription.of(Text.of("Mirror in-game chat activity to Discord.")))
                .binding(
                        true,
                        () -> HANDLER.instance().logChatMessages,
                        value -> HANDLER.instance().logChatMessages = value
                )
                .controller(BooleanControllerBuilder::create)
                .build());

        builder.option(Option.<Boolean>createBuilder()
                .name(Text.of("Join Notification (Singleplayer)"))
                .description(OptionDescription.of(Text.of("Send a notification when the singleplayer world session starts.")))
                .binding(
                        true,
                        () -> HANDLER.instance().notifyOnJoinSingleplayer,
                        value -> HANDLER.instance().notifyOnJoinSingleplayer = value
                )
                .controller(BooleanControllerBuilder::create)
                .build());

        builder.option(Option.<Boolean>createBuilder()
                .name(Text.of("Join Notification (Multiplayer)"))
                .description(OptionDescription.of(Text.of("Send a notification when the player joins a multiplayer server.")))
                .binding(
                        true,
                        () -> HANDLER.instance().notifyOnJoinMultiplayer,
                        value -> HANDLER.instance().notifyOnJoinMultiplayer = value
                )
                .controller(BooleanControllerBuilder::create)
                .build());

        builder.option(Option.<Boolean>createBuilder()
                .name(Text.of("Disconnect Notification (Singleplayer)"))
                .description(OptionDescription.of(Text.of("Send a notification when the singleplayer world is closed.")))
                .binding(
                        true,
                        () -> HANDLER.instance().notifyOnDisconnectSingleplayer,
                        value -> HANDLER.instance().notifyOnDisconnectSingleplayer = value
                )
                .controller(BooleanControllerBuilder::create)
                .build());

        builder.option(Option.<Boolean>createBuilder()
                .name(Text.of("Disconnect Notification (Multiplayer)"))
                .description(OptionDescription.of(Text.of("Send a notification when the player leaves a multiplayer server.")))
                .binding(
                        true,
                        () -> HANDLER.instance().notifyOnDisconnectMultiplayer,
                        value -> HANDLER.instance().notifyOnDisconnectMultiplayer = value
                )
                .controller(BooleanControllerBuilder::create)
                .build());

        builder.option(Option.<Boolean>createBuilder()
                .name(Text.of("Ping on Kick (Multiplayer)"))
                .description(OptionDescription.of(Text.of("Mention the configured allowed Discord user when kicked from a multiplayer server.")))
                .binding(
                        true,
                        () -> HANDLER.instance().pingOnDisconnectMultiplayer,
                        value -> HANDLER.instance().pingOnDisconnectMultiplayer = value
                )
                .controller(BooleanControllerBuilder::create)
                .build());

        builder.option(Option.<Boolean>createBuilder()
                .name(Text.of("Ping on Mention"))
                .description(OptionDescription.of(Text.of("Mention the configured Discord user when your Minecraft username appears in chat. (Still work in progress, may be unstable or not working altogether.)")))
                .binding(
                        false,
                        () -> HANDLER.instance().pingOnMention,
                        value -> HANDLER.instance().pingOnMention = value
                )
                .controller(BooleanControllerBuilder::create)
                .build());

        Option<Boolean> messageQueueEnabledOption = Option.<Boolean>createBuilder()
                .name(Text.of("Enable Message Queue"))
                .description(OptionDescription.of(Text.of("Batch outgoing Discord messages to reduce spam and allow grouped delivery.")))
                .binding(
                        false,
                        () -> HANDLER.instance().messageQueueEnabled,
                        value -> HANDLER.instance().messageQueueEnabled = value
                )
                .controller(BooleanControllerBuilder::create)
                .build();

        Option<Integer> messageQueueDelayOption = Option.<Integer>createBuilder()
                .name(Text.of("Queue Delay (seconds)"))
                .description(OptionDescription.of(Text.of("How long to wait before sending the queued batch of messages.")))
                .binding(
                        2,
                        () -> HANDLER.instance().messageQueueDelaySeconds,
                        value -> HANDLER.instance().messageQueueDelaySeconds = value
                )
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                        .range(0, 10)
                        .step(1))
                .build();

        Option<Integer> messageQueueSizeOption = Option.<Integer>createBuilder()
                .name(Text.of("Queue Size Limit"))
                .description(OptionDescription.of(Text.of("How many messages to accumulate before sending them immediately.")))
                .binding(
                        5,
                        () -> HANDLER.instance().messageQueueMaxMessages,
                        value -> HANDLER.instance().messageQueueMaxMessages = value
                )
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                        .range(1, 10)
                        .step(1))
                .build();

        messageQueueEnabledOption.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE) {
                boolean enabled = option.pendingValue();
                messageQueueDelayOption.setAvailable(enabled);
                messageQueueSizeOption.setAvailable(enabled);
            }
        });
        boolean queueEnabled = HANDLER.instance().messageQueueEnabled;
        messageQueueDelayOption.setAvailable(queueEnabled);
        messageQueueSizeOption.setAvailable(queueEnabled);

        builder.option(messageQueueEnabledOption);
        builder.option(messageQueueDelayOption);
        builder.option(messageQueueSizeOption);

        return builder.build();
    }

    public static ConfigCategory getRemoteViewCategory() {
        ConfigCategory.Builder builder = ConfigCategory.createBuilder()
                .name(Text.of("Remote View (WIP)"));

        Option<Boolean> remoteViewEnabledOption = Option.<Boolean>createBuilder()
                .name(Text.of("Enable Remote View"))
                .description(OptionDescription.of(Text.of("Allow the bot to stream your client to authenticated viewers via the /remoteview command.")))
                .binding(
                        false,
                        () -> HANDLER.instance().remoteViewEnabled,
                        value -> HANDLER.instance().remoteViewEnabled = value
                )
                .controller(BooleanControllerBuilder::create)
                .build();
        builder.option(remoteViewEnabledOption);

        Option<Integer> remoteViewFpsOption = Option.<Integer>createBuilder()
                .name(Text.of("Remote View FPS"))
                .description(OptionDescription.of(Text.of("Target FPS for Remote View captures. Higher values impact performance more.")))
                .binding(
                        30,
                        () -> HANDLER.instance().remoteViewFps,
                        value -> HANDLER.instance().remoteViewFps = value
                )
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                        .range(5, 60)
                        .step(5))
                .build();
        builder.option(remoteViewFpsOption);

        Option<String> remoteViewBindAddressOption = Option.<String>createBuilder()
                .name(Text.of("Remote View Bind Address"))
                .description(OptionDescription.of(Text.of("IP address/interface the embedded stream server should bind to (advanced users only).")))
                .binding(
                        "0.0.0.0",
                        () -> HANDLER.instance().remoteViewBindAddress,
                        value -> HANDLER.instance().remoteViewBindAddress = value
                )
                .controller(StringControllerBuilder::create)
                .build();
        builder.option(remoteViewBindAddressOption);

        Option<Integer> remoteViewPortOption = Option.<Integer>createBuilder()
                .name(Text.of("Remote View Port"))
                .description(OptionDescription.of(Text.of("TCP port for the stream HTTP server.")))
                .binding(
                        47823,
                        () -> HANDLER.instance().remoteViewPort,
                        value -> HANDLER.instance().remoteViewPort = value
                )
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                        .range(1024, 65535)
                        .step(1))
                .build();
        builder.option(remoteViewPortOption);

        Option<Integer> remoteViewLinkTimeoutOption = Option.<Integer>createBuilder()
                .name(Text.of("Link Timeout (seconds)"))
                .description(OptionDescription.of(Text.of("How long viewer invite links stay valid before they self-destruct.")))
                .binding(
                        120,
                        () -> HANDLER.instance().remoteViewLinkTimeoutSeconds,
                        value -> HANDLER.instance().remoteViewLinkTimeoutSeconds = value
                )
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                        .range(30, 300)
                        .step(10))
                .build();
        builder.option(remoteViewLinkTimeoutOption);

        Option<String> remoteViewPublicUrlOption = Option.<String>createBuilder()
                .name(Text.of("Remote View Public URL"))
                .description(OptionDescription.of(Text.of("Optional https:// URL to share when exposing Remote View over the internet (VPN/reverse proxy). Leave blank for LAN auto-detect.")))
                .binding(
                        "",
                        () -> HANDLER.instance().remoteViewPublicBaseUrl,
                        value -> HANDLER.instance().remoteViewPublicBaseUrl = value
                )
                .controller(StringControllerBuilder::create)
                .build();
        builder.option(remoteViewPublicUrlOption);

        List<Option<?>> remoteViewDependentOptions = List.of(
                remoteViewFpsOption,
                remoteViewBindAddressOption,
                remoteViewPortOption,
                remoteViewLinkTimeoutOption,
                remoteViewPublicUrlOption
        );

        remoteViewEnabledOption.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE) {
                boolean enabled = option.pendingValue();
                for (Option<?> dependent : remoteViewDependentOptions) {
                    dependent.setAvailable(enabled);
                }
            }
        });

        boolean remoteViewEnabled = HANDLER.instance().remoteViewEnabled;
        for (Option<?> dependent : remoteViewDependentOptions) {
            dependent.setAvailable(remoteViewEnabled);
        }

        return builder.build();
    }

    public static ConfigCategory getCredentialsCategory() {
        ConfigCategory.Builder builder = ConfigCategory.createBuilder()
                .name(Text.of("CREDENTIALS (DO NOT SHARE)"));

        builder.option(Option.<String>createBuilder()
                .name(Text.of("Discord Bot token"))
                .description(OptionDescription.of(Text.of("The Discord bot token. DO NOT SHARE THIS WITH ANYONE ELSE!")))
                .binding(
                        "BOT TOKEN",
                        () -> HANDLER.instance().botToken,
                        value -> HANDLER.instance().botToken = value
                )
                .controller(StringControllerBuilder::create)
                .build());

        builder.option(Option.<String>createBuilder()
                .name(Text.of("Discord Channel ID"))
                .description(OptionDescription.of(Text.of("The channel ID the bot will send messages to. This will be the only channel the bot will work in for safety reasons.")))
                .binding(
                        "CHANNEL ID",
                        () -> HANDLER.instance().discordChannel,
                        value -> HANDLER.instance().discordChannel = value
                )
                .controller(StringControllerBuilder::create)
                .build());

        builder.option(Option.<String>createBuilder()
                .name(Text.of("Allowed Discord User ID"))
                .description(OptionDescription.of(Text.of("Only this Discord user can run bot commands when public commands are disabled.")))
                .binding(
                        "USER ID",
                        () -> HANDLER.instance().allowedUserId,
                        value -> HANDLER.instance().allowedUserId = value
                )
                .controller(StringControllerBuilder::create)
                .build());

        builder.option(Option.<String>createBuilder()
                .name(Text.of("Discord Guild ID"))
                .description(OptionDescription.of(Text.of("Optional guild/server ID used to push slash commands instantly. Leave empty to register globally (may take up to an hour).")))
                .binding(
                        "GUILD ID",
                        () -> HANDLER.instance().commandGuildId,
                        value -> HANDLER.instance().commandGuildId = value
                )
                .controller(StringControllerBuilder::create)
                .build());

        return builder.build();
    }
}
