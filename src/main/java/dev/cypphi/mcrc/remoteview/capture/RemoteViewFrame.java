package dev.cypphi.mcrc.remoteview.capture;

public record RemoteViewFrame(long timestampNanos, int width, int height, byte[] jpegData) {}
