package dev.cypphi.mcrc.remoteview.stream;

import dev.cypphi.mcrc.remoteview.RemoteViewSession;
import dev.cypphi.mcrc.remoteview.RemoteViewSessionEndReason;
import dev.cypphi.mcrc.remoteview.capture.RemoteViewFrame;

public interface RemoteViewStreamPublisher {
    void onSessionStarted(RemoteViewSession session);

    void onViewerConnected(RemoteViewSession session);

    void publishFrame(RemoteViewSession session, RemoteViewFrame frame);

    void onViewerDisconnected(RemoteViewSession session);

    void onSessionEnded(RemoteViewSession session, RemoteViewSessionEndReason reason);
}
