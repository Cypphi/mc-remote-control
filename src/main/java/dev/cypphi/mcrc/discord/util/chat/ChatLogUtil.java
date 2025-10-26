package dev.cypphi.mcrc.discord.util.chat;

import com.mojang.authlib.GameProfile;
import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.discord.util.DiscordMessageSpec;
import dev.cypphi.mcrc.discord.util.DiscordMessageUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

public final class ChatLogUtil {
    private ChatLogUtil() {}

    public static void logOutgoing(String rawContent, boolean command) {
        ChatFormattingUtil.ChatAnalysis analysis = ChatFormattingUtil.analyze(rawContent);

        DiscordMessageSpec spec = DiscordMessageSpec.builder()
                .title(command ? "Command Sent" : "Chat Message Sent")
                .description(codeBlock(analysis.content()))
                .colorOverride(analysis.color())
                .timestamp(true)
                .build();

        DiscordMessageUtil.sendMessage(spec);
    }

    public static void logIncoming(Text message, MessageIndicator indicator, UUID sender) {
        ChatFormattingUtil.ChatAnalysis analysis = ChatFormattingUtil.analyze(message);
        String channel = resolveChannel(indicator);

        String title = "Chat: " + channel;
        String senderName = resolveSenderName(sender);
        String footerText = "Sender: " + senderName;
        String avatarUrl = sender != null ? buildAvatarUrl(sender) : null;

        DiscordMessageSpec.Builder builder = DiscordMessageSpec.builder()
                .title(title)
                .description(codeBlock(analysis.content()))
                .colorOverride(analysis.color())
                .timestamp(true)
                .footer(footerText)
                .footerIconUrl(avatarUrl);

        if (sender != null) {
            builder.addField("Sender UUID", sender.toString(), true);
        }

        DiscordMessageUtil.sendMessage(builder.build());
    }

    private static String codeBlock(String content) {
        return "```" + (content == null ? "" : content) + "```";
    }

    private static String resolveChannel(MessageIndicator indicator) {
        if (indicator == null) {
            return "chat";
        }

        MessageIndicator.Icon icon = indicator.icon();
        if (icon == null) {
            return "chat";
        }

        return icon.name().toLowerCase(Locale.ROOT);
    }

    private static final Method GAME_PROFILE_GET_NAME = resolveProfileAccessor("getName");
    private static final Method GAME_PROFILE_NAME = resolveProfileAccessor("name");

    private static String resolveSenderName(UUID sender) {
        if (sender == null) {
            return "unknown";
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            ClientPlayNetworkHandler handler = client.getNetworkHandler();
            if (handler != null) {
                PlayerListEntry entry = handler.getPlayerListEntry(sender);
                if (entry != null) {
                    Text displayName = entry.getDisplayName();
                    if (displayName != null) {
                        String literal = displayName.getString();
                        if (!literal.isBlank()) {
                            return literal;
                        }
                    }

                    String profileName = extractProfileName(entry.getProfile());
                    if (profileName != null && !profileName.isBlank()) {
                        return profileName;
                    }
                }
            }
        }

        return sender.toString();
    }

    private static Method resolveProfileAccessor(String methodName) {
        try {
            return GameProfile.class.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static String extractProfileName(GameProfile profile) {
        if (profile == null) {
            return null;
        }

        try {
            if (GAME_PROFILE_GET_NAME != null) {
                Object value = GAME_PROFILE_GET_NAME.invoke(profile);
                if (value instanceof String str) {
                    return str;
                }
            }

            if (GAME_PROFILE_NAME != null) {
                Object value = GAME_PROFILE_NAME.invoke(profile);
                if (value instanceof String str) {
                    return str;
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            MinecraftRemoteControl.LOGGER.debug("Failed to read profile name", e);
        }

        return null;
    }

    private static String buildAvatarUrl(UUID sender) {
        String uuid = sender.toString().replace("-", "");
        return "https://crafatar.com/avatars/" + uuid + "?size=32&overlay";
    }
}
