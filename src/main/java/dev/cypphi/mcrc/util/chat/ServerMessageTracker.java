package dev.cypphi.mcrc.util.chat;

import net.minecraft.text.Text;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerMessageTracker {
    private static final Duration TTL = Duration.ofSeconds(10);
    private static final Map<String, Instant> TRACKED = new ConcurrentHashMap<>();

    private ServerMessageTracker() {}

    public static synchronized void record(Text message) {
        String key = normalize(message);
        if (key == null) {
            return;
        }
        prune();
        TRACKED.put(key, Instant.now().plus(TTL));
    }

    public static synchronized boolean consume(Text message) {
        String key = normalize(message);
        if (key == null) {
            return false;
        }
        prune();
        return TRACKED.remove(key) != null;
    }

    private static void prune() {
        Instant now = Instant.now();
        TRACKED.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }

    private static String normalize(Text message) {
        if (message == null) {
            return null;
        }
        ChatFormattingUtil.ChatAnalysis analysis = ChatFormattingUtil.analyze(message);
        String content = analysis.content().strip();
        return content.isEmpty() ? null : content.toLowerCase(Locale.ROOT);
    }
}
