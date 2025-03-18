package dev.cypphi.mcrc.screen;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class JDADownloadPromptScreen extends Screen {
    private final Screen parent;

    public JDADownloadPromptScreen(Screen parent) {
        super(Text.of("JDA Dependency Missing"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height /2;

        this.addDrawableChild(ButtonWidget.builder(
                        Text.of("Yes"),
                        button -> {
                            MinecraftRemoteControl.startJDADownload();
                            MinecraftClient.getInstance().setScreen(parent);
                        })
                .dimensions(centerX - 82, centerY, 80, 20)
                .build()
        );

        // FREE WILL RAHHHHH
        this.addDrawableChild(ButtonWidget.builder(
                        Text.of("No"),
                        button -> MinecraftClient.getInstance().setScreen(parent))
                .dimensions(centerX + 2, centerY, 80, 20)
                .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.fill(0, 0, this.width, this.height, 0x80000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.of("The mod requires Discord JDA."), centerX, centerY - 28, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.of("Download now?"), centerX, centerY - 18, 0xCCCCCC);
    }
}
