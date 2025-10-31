package dev.cypphi.mcrc.util.chat;

public final class ChatNameUtil {
    private static final int MAX_NAME_LENGTH = 16;

    private ChatNameUtil() {}

    public static boolean isValidNameChar(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    public static String extractLeadingName(String content) {
        if (content == null) {
            return null;
        }

        String remaining = content.stripLeading();

        boolean trimmed = true;
        while (trimmed) {
            trimmed = false;
            if (remaining.startsWith("[")) {
                int end = remaining.indexOf(']');
                if (end > 0) {
                    remaining = remaining.substring(end + 1).stripLeading();
                    trimmed = true;
                    continue;
                }
            }
            if (remaining.startsWith("(")) {
                int end = remaining.indexOf(')');
                if (end > 0) {
                    remaining = remaining.substring(end + 1).stripLeading();
                    trimmed = true;
                }
            }
        }

        if (remaining.isEmpty()) {
            return null;
        }

        int index = 0;
        int len = remaining.length();
        while (index < len && isValidNameChar(remaining.charAt(index))) {
            index++;
        }

        if (index == 0 || index > MAX_NAME_LENGTH) {
            return null;
        }

        return remaining.substring(0, index);
    }

    public static String extractBracketedName(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return null;
        }

        String trimmed = rawContent.stripLeading();
        if (!trimmed.startsWith("<")) {
            return null;
        }

        int closing = trimmed.indexOf('>');
        if (closing <= 1) {
            return null;
        }

        String candidate = trimmed.substring(1, closing).trim();
        if (candidate.isEmpty() || candidate.length() > MAX_NAME_LENGTH) {
            return null;
        }

        for (int i = 0; i < candidate.length(); i++) {
            if (!isValidNameChar(candidate.charAt(i))) {
                return null;
            }
        }

        return candidate;
    }

}
