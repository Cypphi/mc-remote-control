package dev.cypphi.mcrc.discord.util.chat;

import net.minecraft.network.message.MessageSignatureData;

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

    private IncomingMessageTracker() {}

    public static void record(MessageSignatureData signature, UUID sender) {
        if (signature == null || sender == null) {
            return;
        }

        SignatureKey key = SignatureKey.of(signature);
        synchronized (SENDERS) {
            SENDERS.put(key, sender);
        }
    }

    public static UUID consume(MessageSignatureData signature) {
        if (signature == null) {
            return null;
        }

        SignatureKey key = SignatureKey.of(signature);
        synchronized (SENDERS) {
            return SENDERS.remove(key);
        }
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
