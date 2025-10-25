package dev.cypphi.mcrc.remoteview;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public class RemoteViewSession {
    private final String sessionId;
    private final String authTokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final AtomicBoolean viewerConnected = new AtomicBoolean(false);

    RemoteViewSession(String sessionId, String authTokenHash, Instant createdAt, Instant expiresAt) {
        this.sessionId = sessionId;
        this.authTokenHash = authTokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAuthTokenHash() {
        return authTokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean markViewerConnected() {
        return viewerConnected.compareAndSet(false, true);
    }

    public boolean isViewerConnected() {
        return viewerConnected.get();
    }
}
