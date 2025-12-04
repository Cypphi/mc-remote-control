package dev.cypphi.mcrc.util.discord;

/**
 * Utility methods for escaping Discord flavoured Markdown so dynamic content
 * does not accidentally trigger formatting or mentions.
 */
public final class DiscordMarkdown {
    private DiscordMarkdown() {}

    public static String escapeInline(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(input.length() + 8);
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (requiresEscape(ch)) {
                builder.append('\\');
            }
            builder.append(ch);
        }
        return builder.toString();
    }

    private static boolean requiresEscape(char ch) {
        return switch (ch) {
            case '\\', '*', '_', '`', '~', '|', '>', '(', ')', '[' ,']' -> true;
            default -> false;
        };
    }
}
