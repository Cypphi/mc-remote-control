package dev.cypphi.mcrc.discord.command.commands;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.discord.command.SlashCommand;
import dev.cypphi.mcrc.discord.util.DiscordMessageKind;
import dev.cypphi.mcrc.discord.util.DiscordMessageSpec;
import dev.cypphi.mcrc.discord.util.MessageFormatterManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;

public class ScreenshotCommand implements SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("mcrc-screenshot-command");

    @Override
    public String getName() {
        return "screenshot";
    }

    @Override
    public String getDescription() {
        return "Capture a one-off screenshot from the Minecraft client.";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> captureScreenshot()
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error("Failed to capture screenshot", throwable);
                        String message = throwable.getMessage();
                        if (message == null || message.isBlank()) {
                            message = throwable.getClass().getSimpleName();
                        }
                        DiscordMessageSpec spec = DiscordMessageSpec.builder()
                                .description("Failed to capture screenshot: " + message)
                                .kind(DiscordMessageKind.ERROR)
                                .build();
                        hook.editOriginal(MessageEditData.fromCreateData(MessageFormatterManager.format(spec))).queue();
                        return;
                    }

                    if (result == null || result.data().length == 0) {
                        DiscordMessageSpec spec = DiscordMessageSpec.builder()
                                .description("Screenshot capture returned no data.")
                                .kind(DiscordMessageKind.WARNING)
                                .build();
                        hook.editOriginal(MessageEditData.fromCreateData(MessageFormatterManager.format(spec))).queue();
                        return;
                    }

                    DiscordMessageSpec spec = DiscordMessageSpec.builder()
                            .title("Screenshot Captured")
                            .description("Latest frame from the Minecraft client.")
                            .kind(DiscordMessageKind.SUCCESS)
                            .addField("Resolution", result.width() + "×" + result.height(), true)
                            .imageUrl("attachment://mcrc-screenshot.png")
                            .timestamp(true)
                            .build();

                    hook.editOriginal(MessageEditData.fromCreateData(MessageFormatterManager.format(spec)))
                            .setFiles(FileUpload.fromData(result.data(), "mcrc-screenshot.png"))
                            .queue();
                }));
    }

    private CompletableFuture<ScreenshotResult> captureScreenshot() {
        CompletableFuture<ScreenshotResult> future = new CompletableFuture<>();
        MinecraftClient client = MinecraftRemoteControl.mc;

        if (client == null) {
            future.completeExceptionally(new IllegalStateException("Minecraft client is not ready."));
            return future;
        }

        client.execute(() -> captureScreenshotAsync(client, future));
        return future;
    }

    private void captureScreenshotAsync(MinecraftClient client, CompletableFuture<ScreenshotResult> future) {
        try {
            Framebuffer framebuffer = client.getFramebuffer();
            if (framebuffer == null) {
                future.completeExceptionally(new IllegalStateException("No active framebuffer to capture."));
                return;
            }

            ScreenshotRecorder.takeScreenshot(framebuffer, nativeImage -> {
                try {
                    int width = nativeImage.getWidth();
                    int height = nativeImage.getHeight();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    BufferedImage bufferedImage = convertToBufferedImage(nativeImage);
                    ImageIO.write(bufferedImage, "png", baos);
                    future.complete(new ScreenshotResult(baos.toByteArray(), width, height));
                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    nativeImage.close();
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    private BufferedImage convertToBufferedImage(NativeImage nativeImage) {
        int width = nativeImage.getWidth();
        int height = nativeImage.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = nativeImage.getColorArgb(x, y);
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

    private record ScreenshotResult(byte[] data, int width, int height) {}
}
