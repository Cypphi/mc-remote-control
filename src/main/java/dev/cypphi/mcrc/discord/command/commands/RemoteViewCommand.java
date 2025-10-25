package dev.cypphi.mcrc.discord.command.commands;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.config.MCRCConfig;
import dev.cypphi.mcrc.discord.command.SlashCommand;
import dev.cypphi.mcrc.remoteview.RemoteViewSessionManager;
import dev.cypphi.mcrc.remoteview.RemoteViewUrlHelper;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

public class RemoteViewCommand implements SlashCommand {
    @Override
    public String getName() {
        return "remoteview";
    }

    @Override
    public String getDescription() {
        return "Generate a secure Remote View link for this client.";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MCRCConfig config = MCRCConfig.HANDLER.instance();
        if (!config.remoteViewEnabled) {
            event.reply("Remote View is disabled in the client settings. Enable it in-game to use this command.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        var urlsOpt = RemoteViewUrlHelper.resolveUrls(config);
        if (urlsOpt.isEmpty()) {
            event.reply("""
                    Remote View couldn't determine an address for streaming.
                    Set the Remote View Public URL in the config (or ensure the client has a LAN IP) and try again.
                    """.stripIndent())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        RemoteViewSessionManager sessionManager = MinecraftRemoteControl.getRemoteViewSessionManager();
        sessionManager.setSessionTtlSeconds(config.remoteViewLinkTimeoutSeconds);
        RemoteViewSessionManager.SessionLink sessionLink = sessionManager.createEphemeralSession();
        long remainingSeconds = Math.max(5, Duration.between(Instant.now(), sessionLink.expiresAt()).toSeconds());

        var urls = urlsOpt.get();
        String signalBase = urls.signalBase();
        String viewerBase = urls.viewerBase();

        String viewerUrl = viewerBase + "?session=" + sessionLink.sessionId()
                + "&auth=" + sessionLink.authToken()
                + "&expires=" + sessionLink.expiresAt().toEpochMilli()
                + "&signal=" + URLEncoder.encode(signalBase, StandardCharsets.UTF_8);

        String response = """
                Remote View link (valid for %d seconds):
                %s
                
                **Do not share this link. A viewer must connect within %d seconds or the session will self-destruct.**
                
                Streaming base: %s
                """.formatted(remainingSeconds, viewerUrl, remainingSeconds, signalBase);

        event.reply(response)
                .setEphemeral(true)
                .queue();
    }
}
