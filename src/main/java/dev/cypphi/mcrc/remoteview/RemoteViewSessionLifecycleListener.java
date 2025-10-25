package dev.cypphi.mcrc.remoteview;

public interface RemoteViewSessionLifecycleListener {
    default void onSessionStarted(RemoteViewSession session) {}

    default void onSessionEnded(RemoteViewSession session, RemoteViewSessionEndReason reason) {}

    default void onViewerConnected(RemoteViewSession session) {}

    default void onViewerDisconnected(RemoteViewSession session) {}
}
