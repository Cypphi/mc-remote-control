package dev.cypphi.mcrc.mixin;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (MinecraftRemoteControl.getHasPrompted()) return;
        MinecraftRemoteControl.onTitleScreenOpened();
        MinecraftRemoteControl.setHasPrompted();
    }
}
