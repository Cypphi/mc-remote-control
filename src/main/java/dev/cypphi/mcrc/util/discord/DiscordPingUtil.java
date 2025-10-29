package dev.cypphi.mcrc.util.discord;

import dev.cypphi.mcrc.config.MCRCConfig;

public final class DiscordPingUtil {
    private DiscordPingUtil() {}

    public static String allowedUserId() {
        MCRCConfig config = MCRCConfig.HANDLER.instance();
        if (config == null) {
            return null;
        }

        String allowedUserId = config.allowedUserId;
        if (allowedUserId == null) {
            return null;
        }

        String trimmed = allowedUserId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed;
    }

    public static String allowedUserMention() {
        String allowedId = allowedUserId();
        return allowedId == null ? null : "<@" + allowedId + ">";
    }

    public static void applyAllowedUserMention(DiscordMessageSpec.Builder builder, String trigger) {
        applyAllowedUserMention(builder, true, trigger);
    }

    public static void applyAllowedUserMention(DiscordMessageSpec.Builder builder, boolean condition, String trigger) {
        if (!condition || builder == null) {
            return;
        }

        String mention = allowedUserMention();
        if (mention == null) {
            return;
        }

        String triggerLabel = trigger == null ? "" : trigger.trim();
        if (triggerLabel.isEmpty()) {
            triggerLabel = "unspecified";
        }

        String mentionLine = mention + " **" + triggerLabel + "**";

        String existing = builder.content();
        if (existing == null || existing.isBlank()) {
            builder.content(mentionLine);
        } else if (!existing.contains(mentionLine)) {
            builder.content(existing + "\n" + mentionLine);
        }
    }
}
