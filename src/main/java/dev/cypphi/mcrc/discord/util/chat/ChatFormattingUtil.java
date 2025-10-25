package dev.cypphi.mcrc.discord.util.chat;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TextVisitFactory;

import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ChatFormattingUtil {
    private static final Color DEFAULT_COLOR = new Color(0xFFFFFF);
    private static final Map<Character, Color> LEGACY_COLORS = Map.ofEntries(
            Map.entry('0', new Color(0x000000)),
            Map.entry('1', new Color(0x0000AA)),
            Map.entry('2', new Color(0x00AA00)),
            Map.entry('3', new Color(0x00AAAA)),
            Map.entry('4', new Color(0xAA0000)),
            Map.entry('5', new Color(0xAA00AA)),
            Map.entry('6', new Color(0xFFAA00)),
            Map.entry('7', new Color(0xAAAAAA)),
            Map.entry('8', new Color(0x555555)),
            Map.entry('9', new Color(0x5555FF)),
            Map.entry('a', new Color(0x55FF55)),
            Map.entry('b', new Color(0x55FFFF)),
            Map.entry('c', new Color(0xFF5555)),
            Map.entry('d', new Color(0xFF55FF)),
            Map.entry('e', new Color(0xFFFF55)),
            Map.entry('f', new Color(0xFFFFFF))
    );

    private ChatFormattingUtil() {}

    public static ChatAnalysis analyze(Text text) {
        if (text == null) {
            return new ChatAnalysis("", DEFAULT_COLOR);
        }

        Map<Color, Integer> counts = new HashMap<>();
        StringBuilder plain = new StringBuilder();

        TextVisitFactory.visitFormatted(text, Style.EMPTY, (index, style, codePoint) -> {
            plain.appendCodePoint(codePoint);
            if (!Character.isISOControl(codePoint)) {
                counts.merge(colorFromStyle(style), 1, Integer::sum);
            }
            return true;
        });

        return new ChatAnalysis(plain.toString(), selectDominantColor(counts));
    }

    public static ChatAnalysis analyze(String raw) {
        if (raw == null || raw.isEmpty()) {
            return new ChatAnalysis("", DEFAULT_COLOR);
        }

        Map<Color, Integer> counts = new HashMap<>();
        Color current = DEFAULT_COLOR;
        StringBuilder plain = new StringBuilder();

        char[] chars = raw.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (ch == '§' && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[++i]);
                if (code == 'x' && i + 12 < chars.length) {
                    Color hex = parseHexColor(chars, i + 1);
                    if (hex != null) {
                        current = hex;
                        i += 12;
                        continue;
                    }
                }
                if (code == 'r') {
                    current = DEFAULT_COLOR;
                } else {
                    current = LEGACY_COLORS.getOrDefault(code, current);
                }
                continue;
            }

            plain.append(ch);
            if (!Character.isISOControl(ch)) {
                counts.merge(current, 1, Integer::sum);
            }
        }

        return new ChatAnalysis(plain.toString(), selectDominantColor(counts));
    }

    public static String toHex(Color color) {
        Color resolved = color != null ? color : DEFAULT_COLOR;
        return String.format(Locale.ROOT, "#%02X%02X%02X", resolved.getRed(), resolved.getGreen(), resolved.getBlue());
    }

    private static Color colorFromStyle(Style style) {
        if (style == null) {
            return DEFAULT_COLOR;
        }
        TextColor textColor = style.getColor();
        if (textColor == null) {
            return DEFAULT_COLOR;
        }
        return new Color(textColor.getRgb() | 0xFF000000, true);
    }

    private static Color parseHexColor(char[] chars, int start) {
        StringBuilder hex = new StringBuilder(6);
        int index = start;
        for (int part = 0; part < 6; part++) {
            if (index + 1 >= chars.length || chars[index] != '§') {
                return null;
            }
            char digit = chars[index + 1];
            if (!isHexDigit(digit)) {
                return null;
            }
            hex.append(digit);
            index += 2;
        }
        try {
            return new Color(Integer.parseInt(hex.toString(), 16));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isHexDigit(char ch) {
        return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
    }

    private static Color selectDominantColor(Map<Color, Integer> counts) {
        if (counts.isEmpty()) {
            return DEFAULT_COLOR;
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DEFAULT_COLOR);
    }

    public record ChatAnalysis(String content, Color color) {}
}
