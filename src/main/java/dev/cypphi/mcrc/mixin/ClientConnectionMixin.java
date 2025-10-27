package dev.cypphi.mcrc.mixin;

import dev.cypphi.mcrc.discord.notification.SessionNotificationManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Inject(method = "disconnect(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void mcrc$captureDisconnectReason(Text reason, CallbackInfo ci) {
        SessionNotificationManager.handleServerDisconnect(reason);
    }
}
