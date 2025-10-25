package dev.cypphi.mcrc.discord.util;

import java.awt.Color;

public enum DiscordMessageKind {
    SUCCESS(new Color(0x00FF6C)),
    INFO(new Color(0x0098FF)),
    WARNING(new Color(0xFF7900)),
    ERROR(new Color(0xFF1600));

    private final Color color;

    DiscordMessageKind(Color color) {
        this.color = color;
    }

    public Color color() {
        return color;
    }

    public static DiscordMessageKind of(String type) {
        if (type == null) {
            return INFO;
        }
        return switch (type.toLowerCase()) {
            case "ready", "success" -> SUCCESS;
            case "warn", "warning" -> WARNING;
            case "error", "fail", "failure" -> ERROR;
            default -> INFO;
        };
    }
}
