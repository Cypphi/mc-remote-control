package dev.cypphi.mcrc.util.chat;

import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerMessageTracker {
    private static final Duration TTL = Duration.ofSeconds(10);
    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();

    private PlayerMessageTracker() {}

    public static synchronized void record(Text message, UUID sender) {
        if (message == null || sender == null || sender.equals(Util.NIL_UUID)) {
            return;
        }

        String key = normalize(message);
        if (key == null) {
            return;
        }

        prune();
        CACHE.put(key, new Entry(sender, Instant.now().plus(TTL)));
    }

    public static synchronized UUID consume(Text message) {
        if (message == null) {
            return null;
        }

        String key = normalize(message);
        if (key == null) {
            return null;
        }

        prune();
        Entry entry = CACHE.remove(key);
        return entry != null ? entry.uuid() : null;
    }

    private static void prune() {
        Instant now = Instant.now();
        CACHE.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private static String normalize(Text message) {
        ChatFormattingUtil.ChatAnalysis analysis = ChatFormattingUtil.analyze(message);
        String content = analysis.content().strip();
        return content.isEmpty() ? null : content.toLowerCase(Locale.ROOT);
    }

    private record Entry(UUID uuid, Instant expiresAt) {}
}
