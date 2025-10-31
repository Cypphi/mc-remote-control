package dev.cypphi.mcrc.util.chat;

import java.util.Locale;

public final class ChatMentionUtil {
    private ChatMentionUtil() {}

    public static boolean containsUsernameMention(String content, String username) {
        if (content == null || content.isBlank() || username == null || username.isBlank()) {
            return false;
        }

        String haystack = content.toLowerCase(Locale.ROOT);
        String needle = username.toLowerCase(Locale.ROOT);

        int index = haystack.indexOf(needle);
        while (index >= 0) {
            boolean startBoundary = index == 0 || !ChatNameUtil.isValidNameChar(haystack.charAt(index - 1));
            int end = index + needle.length();
            boolean endBoundary = end == haystack.length() || !ChatNameUtil.isValidNameChar(haystack.charAt(end));
            if (startBoundary && endBoundary) {
                return true;
            }
            index = haystack.indexOf(needle, index + 1);
        }

        return false;
    }
}
