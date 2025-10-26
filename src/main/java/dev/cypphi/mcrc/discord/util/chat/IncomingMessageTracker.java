package dev.cypphi.mcrc.discord.util.chat;

import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Associates incoming {@link MessageSignatureData} instances with the UUID of the sender so that
 * chat logging can decorate embeds with player context (e.g., avatar icons).
 */
public final class IncomingMessageTracker {
    private static final int MAX_ENTRIES = 128;
    private static final Map<SignatureKey, UUID> SENDERS = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<SignatureKey, UUID> eldest) {
            return size() > MAX_ENTRIES;
        }
    };
    private static final Map<String, UUID> UNSIGNED = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, UUID> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private IncomingMessageTracker() {}

    public static void record(MessageSignatureData signature, UUID sender, Text rawContent) {
        if (sender == null) {
            return;
        }

        if (signature != null) {
            SignatureKey key = SignatureKey.of(signature);
            synchronized (SENDERS) {
                SENDERS.put(key, sender);
            }
            return;
        }

        storeFallback(sender, rawContent);
    }

    public static UUID consume(MessageSignatureData signature, Text rawContent) {
        if (signature != null) {
            SignatureKey key = SignatureKey.of(signature);
            synchronized (SENDERS) {
                UUID sender = SENDERS.remove(key);
                if (sender != null) {
                    return sender;
                }
            }
        }

        if (rawContent == null) {
            return null;
        }

        return consumeFallback(rawContent);
    }

    private static void storeFallback(UUID sender, Text rawContent) {
        if (rawContent == null) {
            return;
        }

        String normalized = normalize(rawContent);
        if (normalized.isEmpty()) {
            return;
        }

        UNSIGNED.put(normalized, sender);
    }

    private static UUID consumeFallback(Text rawContent) {
        String normalized = normalize(rawContent);
        if (normalized.isEmpty()) {
            return null;
        }

        return UNSIGNED.remove(normalized);
    }

    private static String normalize(Text rawContent) {
        ChatFormattingUtil.ChatAnalysis analysis = ChatFormattingUtil.analyze(rawContent);
        return analysis.content().strip();
    }

    private record SignatureKey(byte[] data) {
        static SignatureKey of(MessageSignatureData signature) {
            byte[] bytes = signature.data();
            return new SignatureKey(bytes != null ? Arrays.copyOf(bytes, bytes.length) : new byte[0]);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SignatureKey other)) {
                return false;
            }
            return Arrays.equals(this.data, other.data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.data);
        }
    }
}
