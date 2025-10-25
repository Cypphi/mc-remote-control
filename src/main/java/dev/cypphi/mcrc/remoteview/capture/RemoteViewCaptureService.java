package dev.cypphi.mcrc.remoteview.capture;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RemoteViewCaptureService {
    private static final Logger LOGGER = LoggerFactory.getLogger("mcrc-remoteview-capture");
    private final ScheduledExecutorService executor;
    private final ExecutorService encoderExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean captureInFlight = new AtomicBoolean(false);
    private final AtomicBoolean encoderBusy = new AtomicBoolean(false);
    private final AtomicReference<CapturedFrame> nextPendingFrame = new AtomicReference<>();
    private ScheduledFuture<?> captureTask;
    private BufferedImage reusableBufferedImage;
    private byte[] reusableBgrBuffer;
    private ImageWriter jpegWriter;
    private ImageWriteParam jpegWriteParam;
    private final ByteArrayOutputStream jpegBuffer = new ByteArrayOutputStream(131_072);

    public RemoteViewCaptureService() {
        ThreadFactory captureFactory = runnable -> {
            Thread thread = new Thread(runnable, "mcrc-remoteview-capture");
            thread.setDaemon(true);
            return thread;
        };
        executor = Executors.newSingleThreadScheduledExecutor(captureFactory);

        ThreadFactory encoderFactory = runnable -> {
            Thread thread = new Thread(runnable, "mcrc-remoteview-encoder");
            thread.setDaemon(true);
            return thread;
        };
        encoderExecutor = Executors.newSingleThreadExecutor(encoderFactory);
    }

    public synchronized void startCapture(int targetFps, FrameConsumer consumer) {
        if (targetFps <= 0) {
            throw new IllegalArgumentException("targetFps must be positive");
        }

        stopCapture();
        long intervalMillis = Math.max(1000L / targetFps, 10L);
        running.set(true);
        captureTask = executor.scheduleAtFixedRate(
                () -> captureOnce(consumer),
                0,
                intervalMillis,
                TimeUnit.MILLISECONDS
        );
        LOGGER.info("Remote View capture started at {} FPS ({} ms interval)", targetFps, intervalMillis);
    }

    private void captureOnce(FrameConsumer consumer) {
        if (!running.get()) {
            return;
        }

        if (!captureInFlight.compareAndSet(false, true)) {
            return;
        }

        MinecraftClient client = MinecraftRemoteControl.mc;
        if (client == null || client.getWindow() == null) {
            captureInFlight.set(false);
            return;
        }

        client.execute(() -> {
            try {
                Optional<CapturedFrame> frame = captureNativeImage(client);
                frame.ifPresent(captured -> submitForEncoding(captured, consumer));
            } catch (Exception e) {
                LOGGER.error("Failed to capture frame for Remote View", e);
            } finally {
                captureInFlight.set(false);
            }
        });
    }

    private void submitForEncoding(CapturedFrame frame, FrameConsumer consumer) {
        if (encoderBusy.compareAndSet(false, true)) {
            encoderExecutor.execute(() -> encodeLoop(frame, consumer));
            return;
        }

        CapturedFrame previous = nextPendingFrame.getAndSet(frame);
        if (previous != null) {
            previous.image().close();
        }
    }

    private void encodeLoop(CapturedFrame initialFrame, FrameConsumer consumer) {
        CapturedFrame frame = initialFrame;
        try {
            while (frame != null) {
                try {
                    RemoteViewFrame encoded = encodeCapturedFrame(frame);
                    if (encoded != null) {
                        consumer.accept(encoded);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to encode frame for Remote View", e);
                } finally {
                    frame.image().close();
                }

                frame = nextPendingFrame.getAndSet(null);
            }
        } finally {
            encoderBusy.set(false);
            CapturedFrame pending = nextPendingFrame.getAndSet(null);
            if (pending != null && encoderBusy.compareAndSet(false, true)) {
                encodeLoop(pending, consumer);
            } else if (pending != null) {
                submitForEncoding(pending, consumer);
            }
        }
    }

    private Optional<CapturedFrame> captureNativeImage(MinecraftClient client) {
        Framebuffer framebuffer = client.getFramebuffer();
        int width = framebuffer.textureWidth;
        int height = framebuffer.textureHeight;
        if (width <= 0 || height <= 0) {
            return Optional.empty();
        }

        NativeImage screenshot = ScreenshotRecorder.takeScreenshot(framebuffer);
        return Optional.of(new CapturedFrame(System.nanoTime(), width, height, screenshot));
    }

    private RemoteViewFrame encodeCapturedFrame(CapturedFrame frame) {
        BufferedImage bufferedImage = convertToBufferedImage(frame.image());
        byte[] jpeg = encodeToJpeg(bufferedImage);
        if (jpeg.length == 0) {
            return null;
        }
        return new RemoteViewFrame(frame.timestampNanos(), frame.width(), frame.height(), jpeg);
    }

    private BufferedImage convertToBufferedImage(NativeImage nativeImage) {
        int width = nativeImage.getWidth();
        int height = nativeImage.getHeight();
        if (reusableBufferedImage == null
                || reusableBufferedImage.getWidth() != width
                || reusableBufferedImage.getHeight() != height) {
            reusableBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            reusableBgrBuffer = ((DataBufferByte) reusableBufferedImage
                    .getRaster()
                    .getDataBuffer())
                    .getData();
        }

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = nativeImage.getColorArgb(x, y);
                reusableBgrBuffer[index++] = (byte) (argb & 0xFF); // Blue
                reusableBgrBuffer[index++] = (byte) ((argb >> 8) & 0xFF); // Green
                reusableBgrBuffer[index++] = (byte) ((argb >> 16) & 0xFF); // Red
            }
        }

        return reusableBufferedImage;
    }

    private byte[] encodeToJpeg(BufferedImage image) {
        ensureJpegWriter();
        if (jpegWriter == null) {
            return new byte[0];
        }

        jpegBuffer.reset();

        try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(jpegBuffer)) {
            jpegWriter.setOutput(output);
            jpegWriter.write(null, new IIOImage(image, null, null), jpegWriteParam);
            output.flush();
            return jpegBuffer.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Failed to encode frame to JPEG", e);
            return new byte[0];
        } finally {
            if (jpegWriter != null) {
                jpegWriter.setOutput(null);
            }
        }
    }

    public synchronized void stopCapture() {
        running.set(false);
        captureInFlight.set(false);
        if (captureTask != null) {
            captureTask.cancel(false);
            captureTask = null;
            LOGGER.info("Remote View capture stopped.");
        }
        releasePendingFrame();
    }

    public void shutdown() {
        stopCapture();
        executor.shutdownNow();
        encoderExecutor.shutdownNow();
        disposeJpegWriter();
    }

    private void ensureJpegWriter() {
        if (jpegWriter != null) {
            return;
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            LOGGER.error("No JPEG writers available. Cannot encode Remote View frame.");
            return;
        }

        jpegWriter = writers.next();
        jpegWriteParam = jpegWriter.getDefaultWriteParam();
        if (jpegWriteParam.canWriteCompressed()) {
            jpegWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpegWriteParam.setCompressionQuality(0.7f);
        }
    }

    private void disposeJpegWriter() {
        if (jpegWriter != null) {
            jpegWriter.dispose();
            jpegWriter = null;
            jpegWriteParam = null;
        }
    }

    public interface FrameConsumer {
        void accept(RemoteViewFrame frame);
    }

    private record CapturedFrame(long timestampNanos, int width, int height, NativeImage image) {}

    private void releasePendingFrame() {
        CapturedFrame pending = nextPendingFrame.getAndSet(null);
        if (pending != null) {
            pending.image().close();
        }
    }
}
