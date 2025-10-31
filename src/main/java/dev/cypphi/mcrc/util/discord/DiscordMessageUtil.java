package dev.cypphi.mcrc.util.discord;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.config.MCRCConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DiscordMessageUtil {
    private static final Object QUEUE_LOCK = new Object();
    private static final Queue<DiscordMessageSpec> MESSAGE_QUEUE = new ArrayDeque<>();
    private static final ScheduledExecutorService QUEUE_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "MCRC-DiscordMessageQueue");
        thread.setDaemon(true);
        return thread;
    });
    private static ScheduledFuture<?> pendingFlushTask;
    private static final int DISCORD_EMBED_LIMIT = 10;
    private static final int DISCORD_MESSAGE_CONTENT_LIMIT = 2000;

    public static void sendMessage(String message, String type) {
        sendMessage(DiscordMessageSpec.builder()
                .description(message)
                .kind(DiscordMessageKind.of(type))
                .build());
    }

    public static void sendMessage(DiscordMessageSpec spec) {
        if (spec == null) {
            MinecraftRemoteControl.LOGGER.warn("Cannot enqueue Discord message: spec is null.");
            return;
        }

        MCRCConfig config = MCRCConfig.HANDLER.instance();
        if (!config.messageQueueEnabled) {
            flushPendingQueue();
            sendMessageImmediate(spec);
            return;
        }

        enqueue(spec, config);
    }

    private static void enqueue(DiscordMessageSpec spec, MCRCConfig config) {
        int maxMessages = Math.max(1, Math.min(config.messageQueueMaxMessages, 10));
        long delaySeconds = Math.max(0, config.messageQueueDelaySeconds);

        synchronized (QUEUE_LOCK) {
            MESSAGE_QUEUE.add(spec);

            if (MESSAGE_QUEUE.size() >= maxMessages) {
                scheduleFlushLocked(0);
            } else if (pendingFlushTask == null || pendingFlushTask.isDone()) {
                scheduleFlushLocked(delaySeconds);
            }
        }
    }

    private static void scheduleFlushLocked(long delaySeconds) {
        if (pendingFlushTask != null && !pendingFlushTask.isDone()) {
            pendingFlushTask.cancel(false);
        }
        pendingFlushTask = QUEUE_EXECUTOR.schedule(DiscordMessageUtil::flushQueue, delaySeconds, TimeUnit.SECONDS);
    }

    private static void flushQueue() {
        List<DiscordMessageSpec> batch;
        synchronized (QUEUE_LOCK) {
            if (MESSAGE_QUEUE.isEmpty()) {
                pendingFlushTask = null;
                return;
            }
            batch = new ArrayList<>(MESSAGE_QUEUE);
            MESSAGE_QUEUE.clear();
            pendingFlushTask = null;
        }

        dispatchBatch(batch);
    }

    private static void flushPendingQueue() {
        List<DiscordMessageSpec> batch = null;
        synchronized (QUEUE_LOCK) {
            if (pendingFlushTask != null && !pendingFlushTask.isDone()) {
                pendingFlushTask.cancel(false);
            }
            pendingFlushTask = null;

            if (!MESSAGE_QUEUE.isEmpty()) {
                batch = new ArrayList<>(MESSAGE_QUEUE);
                MESSAGE_QUEUE.clear();
            }
        }

        if (batch != null) {
            dispatchBatch(batch);
        }
    }

    private static void sendMessageImmediate(DiscordMessageSpec spec) {
        sendMessageData(MessageFormatterManager.format(spec));
    }

    private static void sendMessageData(MessageCreateData payload) {
        if (payload == null) {
            return;
        }

        boolean hasContent = !payload.getContent().isBlank();
        boolean hasEmbeds = !payload.getEmbeds().isEmpty();
        boolean hasAttachments = !payload.getAttachments().isEmpty();
        boolean hasComponents = !payload.getComponents().isEmpty();
        if (!hasContent && !hasEmbeds && !hasAttachments && !hasComponents) {
            return;
        }

        JDA jda = MinecraftRemoteControl.getJDA();
        if (jda == null) {
            MinecraftRemoteControl.LOGGER.warn("Cannot send message: Discord bot is not connected.");
            return;
        }

        String channelID = MCRCConfig.HANDLER.instance().discordChannel;
        if (channelID == null || channelID.isBlank()) {
            MinecraftRemoteControl.LOGGER.warn("Cannot send message: Discord channel ID is not configured.");
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelID.trim());

        if (channel != null) {
            channel.sendMessage(payload).queue();
        } else {
            MinecraftRemoteControl.LOGGER.error("Failed to send message: Discord channel not found (ID: {})", channelID);
        }
    }

    private static void dispatchBatch(List<DiscordMessageSpec> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        boolean useEmbeds = MCRCConfig.HANDLER.instance().useEmbeds;
        if (useEmbeds) {
            dispatchEmbedBatch(batch);
        } else {
            dispatchPlainTextBatch(batch);
        }
    }

    private static void dispatchEmbedBatch(List<DiscordMessageSpec> batch) {
        DiscordEmbedFormatter formatter = new DiscordEmbedFormatter();
        MessageCreateBuilder builder = new MessageCreateBuilder();
        StringBuilder contentAccumulator = new StringBuilder();
        int embedCount = 0;

        for (DiscordMessageSpec spec : batch) {
            MessageCreateData data = formatter.format(spec);
            List<MessageEmbed> embeds = data.getEmbeds();

            if (embedCount > 0 && !embeds.isEmpty() && embedCount + embeds.size() > DISCORD_EMBED_LIMIT) {
                finalizeAndSendEmbedMessage(builder, contentAccumulator, embedCount);
                builder = new MessageCreateBuilder();
                contentAccumulator.setLength(0);
                embedCount = 0;
            }

            String content = data.getContent();
            if (!content.isBlank()) {
                if (!contentAccumulator.isEmpty()) {
                    contentAccumulator.append("\n\n");
                }
                contentAccumulator.append(content.strip());
            }

            if (!embeds.isEmpty()) {
                if (embeds.size() > DISCORD_EMBED_LIMIT) {
                    for (int start = 0; start < embeds.size(); start += DISCORD_EMBED_LIMIT) {
                        int end = Math.min(start + DISCORD_EMBED_LIMIT, embeds.size());
                        MessageCreateBuilder chunkBuilder = new MessageCreateBuilder();
                        chunkBuilder.addEmbeds(embeds.subList(start, end));
                        if (start == 0 && !contentAccumulator.isEmpty()) {
                            chunkBuilder.addContent(contentAccumulator.toString());
                        }
                        sendMessageData(chunkBuilder.build());
                    }
                    contentAccumulator.setLength(0);
                    builder = new MessageCreateBuilder();
                    embedCount = 0;
                    continue;
                }

                builder.addEmbeds(embeds);
                embedCount += embeds.size();

                if (embedCount == DISCORD_EMBED_LIMIT) {
                    finalizeAndSendEmbedMessage(builder, contentAccumulator, embedCount);
                    builder = new MessageCreateBuilder();
                    contentAccumulator.setLength(0);
                    embedCount = 0;
                }
            }
        }

        finalizeAndSendEmbedMessage(builder, contentAccumulator, embedCount);
    }

    private static void finalizeAndSendEmbedMessage(MessageCreateBuilder builder, StringBuilder contentAccumulator, int embedCount) {
        if (embedCount <= 0 && contentAccumulator.isEmpty()) {
            return;
        }

        if (!contentAccumulator.isEmpty()) {
            builder.addContent(contentAccumulator.toString());
        }

        sendMessageData(builder.build());
    }

    private static void dispatchPlainTextBatch(List<DiscordMessageSpec> batch) {
        PlainTextFormatter formatter = new PlainTextFormatter();
        StringBuilder combined = new StringBuilder();

        for (DiscordMessageSpec spec : batch) {
            MessageCreateData data = formatter.format(spec);
            String content = data.getContent();
            if (!content.isBlank()) {
                if (!combined.isEmpty()) {
                    combined.append("\n\n");
                }
                combined.append(content.strip());
            }
        }

        if (combined.isEmpty()) {
            return;
        }

        sendPlainTextChunks(combined.toString());
    }

    private static void sendPlainTextChunks(String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        int length = content.length();
        int index = 0;
        while (index < length) {
            int end = Math.min(length, index + DISCORD_MESSAGE_CONTENT_LIMIT);
            if (end < length) {
                int newline = content.lastIndexOf('\n', end - 1);
                if (newline > index) {
                    end = newline + 1;
                }
            }

            String chunk = content.substring(index, end);
            if (!chunk.isBlank()) {
                sendMessageData(MessageCreateData.fromContent(chunk));
            }

            index = end;
        }
    }

    public static void editMessage(String messageId, String newContent, String type) {
        editMessage(messageId, DiscordMessageSpec.builder()
                .description(newContent)
                .kind(DiscordMessageKind.of(type))
                .build());
    }

    public static void editMessage(String messageId, DiscordMessageSpec spec) {
        JDA jda = MinecraftRemoteControl.getJDA();
        if (jda == null) {
            MinecraftRemoteControl.LOGGER.warn("Cannot edit message: Discord bot is not connected.");
            return;
        }

        String channelID = MCRCConfig.HANDLER.instance().discordChannel;
        if (channelID == null || channelID.isBlank()) {
            MinecraftRemoteControl.LOGGER.warn("Cannot edit message: Discord channel ID is not configured.");
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelID.trim());

        if (channel != null) {
            channel.retrieveMessageById(messageId).queue(
                    message -> {
                        if (!message.getAuthor().isBot() ||
                                !message.getAuthor().getId().equals(channel.getJDA().getSelfUser().getId())) {
                            MinecraftRemoteControl.LOGGER.warn("Tried to edit message not sent by bot (ID: {})", messageId);
                            return;
                        }

                        MessageCreateData formatted = MessageFormatterManager.format(spec);
                        MessageEditData editData = MessageEditData.fromCreateData(formatted);

                        message.editMessage(editData).queue();
                    },
                    e -> MinecraftRemoteControl.LOGGER.error("Failed to retrieve message (ID: {}): {}", messageId, e.getMessage())
            );
        } else {
            MinecraftRemoteControl.LOGGER.error("Failed to edit message: Discord channel not found (ID: {})", channelID);
        }
    }
}
