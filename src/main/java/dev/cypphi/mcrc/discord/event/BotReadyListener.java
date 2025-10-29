package dev.cypphi.mcrc.discord.event;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.util.client.MinecraftClientUtil;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Optional;

public class BotReadyListener extends ListenerAdapter {
    private static final String BIO_TEMPLATE = """
            MC Remote Control **v%s**
            Official Discord:
              https://discord.gg/2b2tism
            GitHub:
              https://github.com/Cypphi/mc-remote-control
            """;

    @Override
    public void onReady(ReadyEvent event) {
        String username = resolveClientUsername();

        event.getJDA().getPresence().setActivity(Activity.watching("over " + username));
        updateBotBio(event);

        MinecraftRemoteControl.LOGGER.info("Discord bot ready as {}", event.getJDA().getSelfUser().getName());
    }

    private void updateBotBio(ReadyEvent event) {
        String desiredBio = BIO_TEMPLATE.formatted(MinecraftRemoteControl.MOD_VERSION).stripTrailing();
        event.getJDA().retrieveApplicationInfo().queue(info -> {
            String currentBio = Optional.of(info.getDescription())
                    .map(String::stripTrailing)
                    .orElse("");

            if (desiredBio.equals(currentBio)) {
                MinecraftRemoteControl.LOGGER.debug("Bot bio already up to date.");
                return;
            }

            event.getJDA().getApplicationManager()
                    .setDescription(desiredBio)
                    .queue(
                            success -> MinecraftRemoteControl.LOGGER.info("Updated bot bio."),
                            error -> MinecraftRemoteControl.LOGGER.warn("Failed to update bot bio: {}", error.getMessage(), error)
                    );
        }, error -> MinecraftRemoteControl.LOGGER.warn("Failed to fetch bot bio: {}", error.getMessage(), error));
    }

    private String resolveClientUsername() {
        return MinecraftClientUtil.getLocalUsername().orElse("your client");
    }
}
