package dev.cypphi.mcrc.mixin;

import dev.cypphi.mcrc.discord.util.chat.IncomingMessageTracker;
import dev.cypphi.mcrc.discord.util.chat.PlayerMessageTracker;
import dev.cypphi.mcrc.discord.util.chat.ServerMessageTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void mcrc$rememberChatSender(ChatMessageS2CPacket packet, CallbackInfo ci) {
        IncomingMessageTracker.record(packet.signature(), packet.sender(), packet.unsignedContent());
        PlayerMessageTracker.record(packet.unsignedContent(), packet.sender());
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void mcrc$recordGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        ServerMessageTracker.record(packet.content());
    }

}
