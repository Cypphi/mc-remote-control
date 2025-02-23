package dev.cypphi.mcrc.screen;

import dev.cypphi.mcrc.config.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ToggleButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final List<TextFieldWidget> textFields = new ArrayList<>();
    private final List<SettingEntry> settingEntries = new ArrayList<>();
    private TextFieldWidget tokenField;
    private TextFieldWidget channelField;
    private String originalToken;
    private String originalChannel;

    private final int fieldWidth = 200;
    private final int fieldHeight = 20;
    private final int labelWidth = 180;
    private final int rowHeight = 24;
    private final int buttonWidth = 100;
    private final int buttonHeight = 20;
    private final int spacing = 8;

    private int contentHeight = 0;

    public ConfigScreen(Screen parent) {
        super(Text.of("Minecraft Remote Control Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int startY = 30;
        contentHeight = startY;

        originalToken = Config.getInstance().getDiscordBotToken();
        originalChannel = Config.getInstance().getDiscordChannelID();

        addTextFieldSetting("Discord Bot Token", "Your bot's token.", originalToken, (field) -> tokenField = field);
        addTextFieldSetting("Discord Channel ID", "The channel ID the bot will use.", originalChannel, (field) -> channelField = field);

        contentHeight += rowHeight;

        int buttonY = this.height - 30;
        int centerX = this.width / 2;
        int totalButtonWidth = (buttonWidth * 3) + (spacing * 2);
        int buttonStartX = centerX - (totalButtonWidth / 2);

        this.addDrawableChild(ButtonWidget.builder(Text.of("Save"), button -> {
            Config.getInstance().setDiscordBotToken(tokenField.getText());
            Config.getInstance().setDiscordChannelID(channelField.getText());
            Config.save();
            assert this.client != null;
            this.client.setScreen(parent);
        }).dimensions(buttonStartX, buttonY, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Reset"), button -> {
            textFields.forEach(field -> field.setText(""));
        }).dimensions(buttonStartX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
            tokenField.setText(originalToken);
            channelField.setText(originalChannel);
            assert this.client != null;
            this.client.setScreen(parent);
        }).dimensions(buttonStartX + (buttonWidth + spacing) * 2, buttonY, buttonWidth, buttonHeight).build());
    }

    private void addTextFieldSetting(String label, String tooltip, String initialValue, SettingFieldSetter setter) {
        int y = contentHeight + rowHeight;

        settingEntries.add(new SettingEntry(label, tooltip, spacing, y));

        TextFieldWidget field = new TextFieldWidget(this.textRenderer, this.width - fieldWidth - spacing, y, fieldWidth, fieldHeight, Text.of(label));
        field.setMaxLength(128);
        field.setText(initialValue);
        this.addDrawableChild(field);
        textFields.add(field);

        setter.set(field);
        contentHeight += rowHeight;
    }

    private void addToggleSetting(String label, String tooltip, boolean defaultValue) {
        int y = contentHeight + rowHeight;

        settingEntries.add(new SettingEntry(label, tooltip, spacing, y));

        ToggleButtonWidget toggleButton = new ToggleButtonWidget(this.width - fieldWidth - spacing, y, fieldWidth / 2, fieldHeight, defaultValue);
        this.addDrawableChild(toggleButton);

        contentHeight += rowHeight;
    }

    @Override
    public void removed() {
        Config.save();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.of("Minecraft Remote Control Config"), this.width / 2, 10, 0xFFFFFF);

        for (SettingEntry entry : settingEntries) {
            context.drawTextWithShadow(this.textRenderer, Text.of(entry.label), entry.x, entry.y + 6, 0xFFFFFF);
            if (mouseX >= entry.x && mouseX <= entry.x + labelWidth && mouseY >= entry.y && mouseY <= entry.y + fieldHeight) {
                context.drawTooltip(this.textRenderer, Text.of(entry.tooltip), mouseX, mouseY);
            }
        }
    }

    private static class SettingEntry {
        private final String label;
        private final String tooltip;
        private final int x;
        private final int y;

        public SettingEntry(String label, String tooltip, int x, int y) {
            this.label = label;
            this.tooltip = tooltip;
            this.x = x;
            this.y = y;
        }
    }

    @FunctionalInterface
    interface SettingFieldSetter {
        void set(TextFieldWidget field);
    }
}
