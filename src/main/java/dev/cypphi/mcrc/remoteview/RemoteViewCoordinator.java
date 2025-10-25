package dev.cypphi.mcrc.remoteview;

import dev.cypphi.mcrc.config.MCRCConfig;
import dev.cypphi.mcrc.remoteview.capture.RemoteViewCaptureService;
import dev.cypphi.mcrc.remoteview.capture.RemoteViewFrame;
import dev.cypphi.mcrc.remoteview.stream.RemoteViewStreamPublisher;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class RemoteViewCoordinator implements RemoteViewSessionLifecycleListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("mcrc-remoteview-coordinator");
    private final RemoteViewCaptureService captureService;
    private final RemoteViewStreamPublisher streamPublisher;
    private final Supplier<MCRCConfig> configSupplier;
    private final AtomicBoolean captureRunning = new AtomicBoolean(false);

    public RemoteViewCoordinator(RemoteViewCaptureService captureService,
                                 RemoteViewStreamPublisher streamPublisher,
                                 Supplier<MCRCConfig> configSupplier) {
        this.captureService = captureService;
        this.streamPublisher = streamPublisher;
        this.configSupplier = configSupplier;
    }

    @Override
    public void onSessionStarted(RemoteViewSession session) {
        streamPublisher.onSessionStarted(session);
    }

    @Override
    public void onSessionEnded(RemoteViewSession session, RemoteViewSessionEndReason reason) {
        stopCaptureIfNeeded();
        streamPublisher.onSessionEnded(session, reason);
    }

    @Override
    public void onViewerConnected(RemoteViewSession session) {
        streamPublisher.onViewerConnected(session);
        startCaptureIfNeeded(session);
    }

    @Override
    public void onViewerDisconnected(RemoteViewSession session) {
        streamPublisher.onViewerDisconnected(session);
        stopCaptureIfNeeded();
    }

    private void startCaptureIfNeeded(RemoteViewSession session) {
        if (captureRunning.compareAndSet(false, true)) {
            int fps = MathHelper.clamp(configSupplier.get().remoteViewFps, 5, 60);
            captureService.startCapture(fps, frame -> forwardFrame(session, frame));
        }
    }

    private void stopCaptureIfNeeded() {
        if (captureRunning.compareAndSet(true, false)) {
            captureService.stopCapture();
        }
    }

    private void forwardFrame(RemoteViewSession session, RemoteViewFrame frame) {
        try {
            streamPublisher.publishFrame(session, frame);
        } catch (Exception e) {
            LOGGER.error("Failed to publish Remote View frame for session {}", session.getSessionId(), e);
        }
    }
}
