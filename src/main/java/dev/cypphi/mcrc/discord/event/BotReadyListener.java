package dev.cypphi.mcrc.discord.event;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class BotReadyListener extends ListenerAdapter {
    @Override
    public void onReady(ReadyEvent event) {
        MinecraftRemoteControl.LOGGER.info("Discord bot ready as {}", event.getJDA().getSelfUser().getName());
    }
}
