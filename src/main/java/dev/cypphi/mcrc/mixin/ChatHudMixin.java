package dev.cypphi.mcrc.mixin;

import dev.cypphi.mcrc.config.MCRCConfig;
import dev.cypphi.mcrc.discord.util.chat.ChatLogUtil;
import dev.cypphi.mcrc.discord.util.chat.IncomingMessageTracker;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("TAIL"))
    private void mcrc$logChatMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        if (!MCRCConfig.HANDLER.instance().logChatMessages) {
            return;
        }

        UUID sender = IncomingMessageTracker.consume(signature, message);
        ChatLogUtil.logIncoming(message, indicator, sender);
    }
}
