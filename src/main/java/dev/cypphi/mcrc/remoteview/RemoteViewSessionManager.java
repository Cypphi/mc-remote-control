package dev.cypphi.mcrc.remoteview;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class RemoteViewSessionManager {
    private final SecureRandom secureRandom = new SecureRandom();
    private final ScheduledExecutorService scheduler;
    private Duration sessionTtl = Duration.ofSeconds(120);

    private RemoteViewSessionLifecycleListener lifecycleListener;
    private RemoteViewSession activeSession;
    private ScheduledFuture<?> expiryFuture;

    public RemoteViewSessionManager() {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "mcrc-remoteview-expiry");
            thread.setDaemon(true);
            return thread;
        };
        this.scheduler = Executors.newSingleThreadScheduledExecutor(factory);
    }

    public synchronized void setLifecycleListener(RemoteViewSessionLifecycleListener listener) {
        this.lifecycleListener = listener;
    }

    public synchronized void setSessionTtlSeconds(int seconds) {
        int bounded = Math.max(30, Math.min(600, seconds));
        this.sessionTtl = Duration.ofSeconds(bounded);
    }

    public synchronized Optional<RemoteViewSession> getActiveSession() {
        return Optional.ofNullable(activeSession);
    }

    public synchronized Optional<RemoteViewSession> getSessionById(String sessionId) {
        if (activeSession == null) {
            return Optional.empty();
        }
        return activeSession.getSessionId().equals(sessionId) ? Optional.of(activeSession) : Optional.empty();
    }

    public synchronized boolean validateAuthToken(String sessionId, String authToken) {
        if (activeSession == null || authToken == null || authToken.isBlank()) {
            return false;
        }

        if (!activeSession.getSessionId().equals(sessionId) || activeSession.isExpired(Instant.now())) {
            return false;
        }

        String hashed = hashToken(authToken);
        return hashed.equals(activeSession.getAuthTokenHash());
    }

    public synchronized SessionLink createEphemeralSession() {
        Instant now = Instant.now();
        String sessionId = randomToken(12);
        String authToken = randomToken(32);
        String hashed = hashToken(authToken);
        Instant expiresAt = now.plus(sessionTtl);
        RemoteViewSession session = new RemoteViewSession(sessionId, hashed, now, expiresAt);

        cancelExpiryTask();
        if (activeSession != null) {
            RemoteViewSession previous = activeSession;
            activeSession = null;
            notifySessionEnded(previous, RemoteViewSessionEndReason.REPLACED);
        }

        activeSession = session;
        scheduleExpiry(session);
        notifySessionStarted(session);

        return new SessionLink(sessionId, authToken, expiresAt);
    }

    public synchronized void cancelActiveSession() {
        if (activeSession == null) {
            return;
        }
        RemoteViewSession session = activeSession;
        activeSession = null;
        cancelExpiryTask();
        notifySessionEnded(session, RemoteViewSessionEndReason.CANCELLED);
    }

    public synchronized void markViewerConnected(String sessionId) {
        if (activeSession == null || !activeSession.getSessionId().equals(sessionId)) {
            return;
        }
        if (activeSession.markViewerConnected()) {
            notifyViewerConnected(activeSession);
        }
    }

    public synchronized void markViewerDisconnected(String sessionId) {
        if (activeSession == null || !activeSession.getSessionId().equals(sessionId)) {
            return;
        }
        RemoteViewSession session = activeSession;
        activeSession = null;
        cancelExpiryTask();
        notifyViewerDisconnected(session);
        notifySessionEnded(session, RemoteViewSessionEndReason.VIEWER_DISCONNECTED);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void scheduleExpiry(RemoteViewSession session) {
        long delay = Duration.between(Instant.now(), session.getExpiresAt()).toMillis();
        expiryFuture = scheduler.schedule(() -> expireIfActive(session), delay, TimeUnit.MILLISECONDS);
    }

    private void expireIfActive(RemoteViewSession session) {
        synchronized (this) {
            if (activeSession == null || activeSession != session) {
                return;
            }
            activeSession = null;
            cancelExpiryTask();
        }
        RemoteViewSessionEndReason reason = session.isViewerConnected()
                ? RemoteViewSessionEndReason.EXPIRED_ACTIVE
                : RemoteViewSessionEndReason.EXPIRED_UNUSED;
        notifySessionEnded(session, reason);
    }

    private void cancelExpiryTask() {
        if (expiryFuture != null) {
            expiryFuture.cancel(false);
            expiryFuture = null;
        }
    }

    private void notifySessionStarted(RemoteViewSession session) {
        if (lifecycleListener != null) {
            lifecycleListener.onSessionStarted(session);
        }
    }

    private void notifySessionEnded(RemoteViewSession session, RemoteViewSessionEndReason reason) {
        if (lifecycleListener != null) {
            lifecycleListener.onSessionEnded(session, reason);
        }
    }

    private void notifyViewerConnected(RemoteViewSession session) {
        if (lifecycleListener != null) {
            lifecycleListener.onViewerConnected(session);
        }
    }

    private void notifyViewerDisconnected(RemoteViewSession session) {
        if (lifecycleListener != null) {
            lifecycleListener.onViewerDisconnected(session);
        }
    }

    private String hashToken(String authToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest(authToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash auth token", e);
        }
    }

    private String randomToken(int bytes) {
        byte[] data = new byte[bytes];
        secureRandom.nextBytes(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public record SessionLink(String sessionId, String authToken, Instant expiresAt) {}
}
