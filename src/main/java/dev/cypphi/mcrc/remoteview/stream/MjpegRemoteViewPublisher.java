package dev.cypphi.mcrc.remoteview.stream;

import dev.cypphi.mcrc.remoteview.RemoteViewSession;
import dev.cypphi.mcrc.remoteview.RemoteViewSessionEndReason;
import dev.cypphi.mcrc.remoteview.capture.RemoteViewFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MjpegRemoteViewPublisher implements RemoteViewStreamPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger("mcrc-remoteview-mjpeg");
    private static final RemoteViewFrame POISON_PILL = new RemoteViewFrame(
            0L, 0, 0, new byte[0]
    );

    private final ConcurrentMap<String, ViewerConnection> connections = new ConcurrentHashMap<>();

    public Optional<ViewerConnection> attachViewer(RemoteViewSession session,
                                                   String boundary,
                                                   Runnable onClosed) {
        ViewerConnection connection = new ViewerConnection(
                session.getSessionId(),
                boundary,
                onClosed
        );
        ViewerConnection existing = connections.putIfAbsent(session.getSessionId(), connection);
        if (existing != null && !existing.isClosed()) {
            return Optional.empty();
        }
        if (existing != null) {
            existing.closeSilently();
            connections.put(session.getSessionId(), connection);
        }
        return Optional.of(connection);
    }

    @Override
    public void onSessionStarted(RemoteViewSession session) {
        LOGGER.info("Remote View session {} ready for MJPEG streaming", session.getSessionId());
    }

    @Override
    public void onViewerConnected(RemoteViewSession session) {
        LOGGER.info("Viewer connected to Remote View session {}", session.getSessionId());
    }

    @Override
    public void publishFrame(RemoteViewSession session, RemoteViewFrame frame) {
        ViewerConnection connection = connections.get(session.getSessionId());
        if (connection == null) {
            return;
        }
        connection.enqueueFrame(frame);
    }

    @Override
    public void onViewerDisconnected(RemoteViewSession session) {
        ViewerConnection connection = connections.remove(session.getSessionId());
        if (connection != null) {
            LOGGER.info("Viewer disconnected from session {}", session.getSessionId());
            connection.closeSilently();
        }
    }

    @Override
    public void onSessionEnded(RemoteViewSession session, RemoteViewSessionEndReason reason) {
        ViewerConnection connection = connections.remove(session.getSessionId());
        if (connection != null) {
            LOGGER.info("Remote View session {} ended (reason: {})", session.getSessionId(), reason);
            connection.closeSilently();
        } else {
            LOGGER.info("Remote View session {} ended (reason: {})", session.getSessionId(), reason);
        }
    }

    public static class ViewerConnection {
        private final String sessionId;
        private final String boundary;
        private final Runnable onClosed;
        private final LinkedBlockingQueue<RemoteViewFrame> queue = new LinkedBlockingQueue<>(2);
        private final AtomicBoolean closed = new AtomicBoolean(false);

        ViewerConnection(String sessionId, String boundary, Runnable onClosed) {
            this.sessionId = sessionId;
            this.boundary = boundary;
            this.onClosed = onClosed;
        }

        public String getSessionId() {
            return sessionId;
        }

        public boolean isClosed() {
            return closed.get();
        }

        public void enqueueFrame(RemoteViewFrame frame) {
            if (closed.get()) {
                return;
            }
            if (!queue.offer(frame)) {
                queue.poll();
                queue.offer(frame);
            }
        }

        public void close() {
            if (closed.compareAndSet(false, true)) {
                queue.offer(POISON_PILL);
                safeRunOnClosed();
            }
        }

        void closeSilently() {
            if (closed.compareAndSet(false, true)) {
                queue.offer(POISON_PILL);
            }
        }

        public void stream(OutputStream output) throws IOException {
            byte[] boundaryBytes = ("--" + boundary + "\r\n").getBytes(StandardCharsets.US_ASCII);
            byte[] newlineBytes = "\r\n".getBytes(StandardCharsets.US_ASCII);

            try {
                while (!closed.get()) {
                    RemoteViewFrame frame = queue.poll(5, TimeUnit.SECONDS);
                    if (frame == null) {
                        continue;
                    }

                    if (frame == POISON_PILL) {
                        break;
                    }

                    byte[] jpeg = frame.jpegData();
                    output.write(boundaryBytes);
                    output.write("Content-Type: image/jpeg\r\n".getBytes(StandardCharsets.US_ASCII));
                    output.write(("Content-Length: " + jpeg.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    output.write(jpeg);
                    output.write(newlineBytes);
                    output.flush();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                if (!closed.get()) {
                    throw e;
                }
            } finally {
                close();
                output.flush();
            }
        }

        private void safeRunOnClosed() {
            if (onClosed != null) {
                try {
                    onClosed.run();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
