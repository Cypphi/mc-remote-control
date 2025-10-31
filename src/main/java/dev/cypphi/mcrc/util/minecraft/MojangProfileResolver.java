package dev.cypphi.mcrc.util.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class MojangProfileResolver {
    private static final Duration CACHE_TTL = Duration.ofHours(12);
    private static final Duration NEGATIVE_TTL = Duration.ofMinutes(30);

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(5))
            .build();

    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> IN_FLIGHT = new ConcurrentHashMap<>();

    private static final String USER_AGENT = "MCRemoteControl/1.0 (+https://github.com/Cypphi/mc-remote-control)";
    private static final HttpUrl BASE_URL = HttpUrl.get("https://api.mojang.com/users/profiles/minecraft/");

    private MojangProfileResolver() {}

    public static Profile getCachedProfile(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String key = username.toLowerCase(Locale.ROOT);
        CacheEntry cached = CACHE.get(key);
        if (cached == null) {
            return null;
        }

        if (cached.expiresAt().isBefore(Instant.now())) {
            CACHE.remove(key);
            return null;
        }

        return cached.profile();
    }

    public static void queueLookup(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        String key = username.toLowerCase(Locale.ROOT);
        if (CACHE.containsKey(key)) {
            return;
        }

        if (IN_FLIGHT.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Profile profile = fetchProfile(username);
                Duration ttl = profile != null ? CACHE_TTL : NEGATIVE_TTL;
                CACHE.put(key, new CacheEntry(profile, Instant.now().plus(ttl)));
            } finally {
                IN_FLIGHT.remove(key);
            }
        });
    }

    private static Profile fetchProfile(String username) {
        HttpUrl url = BASE_URL.newBuilder()
                .addPathSegment(username)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build();

        try (Response response = HTTP.newCall(request).execute()) {
            if (response.code() == 204 || response.code() == 404) {
                return null;
            }

            if (!response.isSuccessful()) {
                return null;
            }

            String payload = response.body().string();
            if (payload.isBlank()) {
                return null;
            }

            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
            String rawId = getString(obj, "id");
            String canonicalName = getString(obj, "name");

            if (rawId == null || canonicalName == null) {
                return null;
            }

            UUID uuid = parseCompactUuid(rawId);
            if (uuid == null) {
                return null;
            }

            return new Profile(uuid, canonicalName);
        } catch (IOException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        return obj.get(key).isJsonNull() ? null : obj.get(key).getAsString();
    }

    private record CacheEntry(Profile profile, Instant expiresAt) {}

    public record Profile(UUID uuid, String name) {}

    private static UUID parseCompactUuid(String input) {
        String normalized = input.replace("-", "").trim();
        if (normalized.length() != 32) {
            return null;
        }

        try {
            String withHyphens = normalized.substring(0, 8) + "-" +
                    normalized.substring(8, 12) + "-" +
                    normalized.substring(12, 16) + "-" +
                    normalized.substring(16, 20) + "-" +
                    normalized.substring(20);
            return UUID.fromString(withHyphens);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
