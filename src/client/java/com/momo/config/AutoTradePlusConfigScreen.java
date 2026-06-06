package com.momo.config;

import com.momo.AutoTradePlusClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class AutoTradePlusConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;

    private TextFieldWidget tradeRangeField;
    private TextFieldWidget tradeCooldownField;
    private TextFieldWidget villagerCooldownField;
    private ButtonWidget professionButton;
    private ButtonWidget dropItemsButton;
    private ButtonWidget enabledButton;
    private ButtonWidget sneakButton;
    private ButtonWidget fishingButton;
    private ButtonWidget debugButton;
    private ButtonWidget autoCloseMerchantScreenButton;
    private ButtonWidget resumeTradeProgressButton;
    private ButtonWidget dropModeButton;
    private ButtonWidget toggleKeyButton;
    private ButtonWidget openConfigKeyButton;
    private KeyBinding editingKeyBinding;
    private ButtonWidget editingKeyButton;

    public AutoTradePlusConfigScreen(Screen parent) {
        super(Text.translatable("text.autoconfig.autotrade-plus.title"));
        this.parent = parent;
        this.config = ModConfigManager.get();
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(620, this.width - 32);
        int left = (this.width - contentWidth) / 2;
        int columnGap = 22;
        int columnWidth = (contentWidth - columnGap) / 2;
        int leftColumn = left;
        int rightColumn = left + columnWidth + columnGap;
        int y = 42;
        int rowGap = 29;

        addDrawableChild(new TextWidget(0, 12, this.width, 18, this.title, this.textRenderer));

        this.professionButton = addButtonRow(
                leftColumn,
                y,
                columnWidth,
                Text.translatable("text.autoconfig.autotrade-plus.option.villagerProfession"),
                summarizeProfession(config.villagerProfession),
                button -> this.client.setScreen(OptionMultiSelectScreen.professions(this, config.villagerProfession, value -> {
                    config.villagerProfession = value;
                    updateDynamicMessages();
                }))
        );
        this.enabledButton = addButtonRow(
                rightColumn,
                y,
                columnWidth,
                Text.translatable("text.autoconfig.autotrade-plus.option.enabled"),
                boolText(config.enabled),
                button -> {
                    config.enabled = !config.enabled;
                    updateDynamicMessages();
                }
        );

        y += rowGap;
        this.tradeRangeField = addTextFieldRow(
                leftColumn,
                y,
                columnWidth,
                Text.translatable("text.autoconfig.autotrade-plus.option.tradeRange"),
                Double.toString(config.tradeRange)
        );
        this.sneakButton = addButtonRow(
                rightColumn,
                y,
                columnWidth,
                Text.translatable("text.autoconfig.autotrade-plus.option.sneak"),
                boolText(config.sneak),
                button -> {
                    config.sneak = !config.sneak;
                    updateDynamicMessages();
                }
        );

        y += rowGap;
        this.tradeCooldownField = addTextFieldRow(
                leftColumn,
                y,
                columnWidth,
                Text.translatable("text.autoconfig.autotrade-plus.option.tradeCooldownTicks"),
                Integer.toString(config.tradeCooldownTicks)
        );
        this.fishingButton = addButtonRow(
                rightColumn,
                y,
                columnWidth,
                Text.translatable("text.autoconfig.autotrade-plus.option.fishingMode"),
                boolText(config.fishingMode),
                button -> {
                    config.fishingMode = !config.fishingMode;
                    updateDynamicMessages();
                }
        );

        y += rowGap;
        this.villagerCooldownField = addTextFieldRow(
                leftColumn,
                y,
                columnWidth,
                Text.translatable("text.autoconfig.autotrade-plus.option.villagerCooldownTicks"),
                Integer.toString(config.villagerCooldownTicks)
        );
        this.debugButton = addButtonRow(
                rightColumn,
                y,
                columnWidth,
                Text.translatable("text.autoconfig.autotrade-plus.option.debug"),
                boolText(config.debug),
                button -> {
                    config.debug = !config.debug;
                    updateDynamicMessages();
                }
        );

        y += rowGap;
        this.dropModeButton = addButtonRow(
                leftColumn,
                y,
                columnWidth,
                Text.translatable("text.autoconfig.autotrade-plus.option.dropMode"),
                Text.translatable(config.dropMode.translationKey()),
                button -> {
                    config.dropMode = config.dropMode == ModConfig.DropMode.DISABLED
                            ? ModConfig.DropMode.AFTER_TRADE
                            : ModConfig.DropMode.DISABLED;
                    updateDynamicMessages();
                }
        );
        this.dropItemsButton = addButtonRow(
                rightColumn,
                y,
                columnWidth,
                Text.translatable("text.autoconfig.autotrade-plus.option.dropItems"),
                summarize(config.dropItems, Text.translatable("text.autotrade-plus.none").getString()),
                button -> this.client.setScreen(OptionMultiSelectScreen.items(this, config.dropItems, value -> {
                    config.dropItems = value;
                    updateDynamicMessages();
                }))
        );

        y += rowGap;
        this.autoCloseMerchantScreenButton = addButtonRow(
                leftColumn,
                y,
                columnWidth,
                Text.translatable("text.autoconfig.autotrade-plus.option.autoCloseMerchantScreen"),
                boolText(config.autoCloseMerchantScreen),
                button -> {
                    config.autoCloseMerchantScreen = !config.autoCloseMerchantScreen;
                    updateDynamicMessages();
                }
        );
        this.resumeTradeProgressButton = addButtonRow(
                rightColumn,
                y,
                columnWidth,
                Text.translatable("text.autoconfig.autotrade-plus.option.resumeTradeProgress"),
                boolText(config.resumeTradeProgress),
                button -> {
                    config.resumeTradeProgress = !config.resumeTradeProgress;
                    updateDynamicMessages();
                }
        );

        y += rowGap;
        this.toggleKeyButton = addButtonRow(
                leftColumn,
                y,
                columnWidth,
                Text.translatable("key.autotrade-plus.toggle"),
                getKeyText(AutoTradePlusClient.toggleKey),
                button -> startEditingKey(AutoTradePlusClient.toggleKey, button)
        );
        this.openConfigKeyButton = addButtonRow(
                rightColumn,
                y,
                columnWidth,
                Text.translatable("key.autotrade-plus.open_config"),
                getKeyText(AutoTradePlusClient.openConfigKey),
                button -> startEditingKey(AutoTradePlusClient.openConfigKey, button)
        );

        int buttonY = Math.min(this.height - 28, y + rowGap + 12);
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> finish())
                .dimensions(this.width / 2 - 104, buttonY, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> this.client.setScreen(this.parent))
                .dimensions(this.width / 2 + 4, buttonY, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.editingKeyBinding != null) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("text.autotrade-plus.keybind.waiting"),
                    this.width / 2,
                    this.height - 50,
                    0xFFFF55
            );
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.editingKeyBinding != null) {
            this.editingKeyBinding.setBoundKey(input.key() == 256 ? InputUtil.UNKNOWN_KEY : InputUtil.fromKeyCode(input));
            KeyBinding.updateKeysByCode();
            this.client.options.write();
            this.editingKeyBinding = null;
            this.editingKeyButton = null;
            updateDynamicMessages();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.editingKeyBinding != null) {
            this.editingKeyBinding = null;
            this.editingKeyButton = null;
            updateDynamicMessages();
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void close() {
        finish();
    }

    private TextFieldWidget addTextFieldRow(int x, int y, int width, Text label, String value) {
        addLabelWidget(x, y, label);
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x + 128, y, width - 128, 20, label);
        field.setText(value);
        field.setMaxLength(16);
        return addDrawableChild(field);
    }

    private ButtonWidget addButtonRow(int x, int y, int width, Text label, Text value, ButtonWidget.PressAction action) {
        return addDrawableChild(ButtonWidget.builder(rowText(label, value), action)
                .dimensions(x, y, width, 20)
                .build());
    }

    private void addLabelWidget(int x, int y, Text label) {
        ButtonWidget labelWidget = ButtonWidget.builder(label, button -> {
                })
                .dimensions(x, y, 124, 20)
                .build();
        labelWidget.active = false;
        addDrawableChild(labelWidget);
    }

    private void startEditingKey(KeyBinding keyBinding, ButtonWidget button) {
        if (keyBinding == null) {
            return;
        }
        this.editingKeyBinding = keyBinding;
        this.editingKeyButton = button;
        button.setMessage(Text.translatable("text.autotrade-plus.keybind.press"));
    }

    private void updateDynamicMessages() {
        if (this.professionButton != null) {
            this.professionButton.setMessage(rowText(
                    Text.translatable("text.autoconfig.autotrade-plus.option.villagerProfession"),
                    summarizeProfession(config.villagerProfession)
            ));
        }
        if (this.dropItemsButton != null) {
            this.dropItemsButton.setMessage(rowText(
                    Text.translatable("text.autoconfig.autotrade-plus.option.dropItems"),
                    summarize(config.dropItems, Text.translatable("text.autotrade-plus.none").getString())
            ));
        }
        if (this.enabledButton != null) {
            this.enabledButton.setMessage(rowText(Text.translatable("text.autoconfig.autotrade-plus.option.enabled"), boolText(config.enabled)));
        }
        if (this.sneakButton != null) {
            this.sneakButton.setMessage(rowText(Text.translatable("text.autoconfig.autotrade-plus.option.sneak"), boolText(config.sneak)));
        }
        if (this.fishingButton != null) {
            this.fishingButton.setMessage(rowText(Text.translatable("text.autoconfig.autotrade-plus.option.fishingMode"), boolText(config.fishingMode)));
        }
        if (this.debugButton != null) {
            this.debugButton.setMessage(rowText(Text.translatable("text.autoconfig.autotrade-plus.option.debug"), boolText(config.debug)));
        }
        if (this.autoCloseMerchantScreenButton != null) {
            this.autoCloseMerchantScreenButton.setMessage(rowText(
                    Text.translatable("text.autoconfig.autotrade-plus.option.autoCloseMerchantScreen"),
                    boolText(config.autoCloseMerchantScreen)
            ));
        }
        if (this.resumeTradeProgressButton != null) {
            this.resumeTradeProgressButton.setMessage(rowText(
                    Text.translatable("text.autoconfig.autotrade-plus.option.resumeTradeProgress"),
                    boolText(config.resumeTradeProgress)
            ));
        }
        if (this.dropModeButton != null) {
            this.dropModeButton.setMessage(rowText(
                    Text.translatable("text.autoconfig.autotrade-plus.option.dropMode"),
                    Text.translatable(config.dropMode.translationKey())
            ));
        }
        if (this.toggleKeyButton != null && this.editingKeyButton != this.toggleKeyButton) {
            this.toggleKeyButton.setMessage(rowText(Text.translatable("key.autotrade-plus.toggle"), getKeyText(AutoTradePlusClient.toggleKey)));
        }
        if (this.openConfigKeyButton != null && this.editingKeyButton != this.openConfigKeyButton) {
            this.openConfigKeyButton.setMessage(rowText(Text.translatable("key.autotrade-plus.open_config"), getKeyText(AutoTradePlusClient.openConfigKey)));
        }
    }

    private void finish() {
        applyNumericFields();
        ModConfigManager.save();
        this.client.options.write();
        this.client.setScreen(this.parent);
    }

    private void applyNumericFields() {
        config.tradeRange = Math.max(0.0, parseDouble(this.tradeRangeField.getText(), config.tradeRange));
        config.tradeCooldownTicks = Math.max(0, parseInt(this.tradeCooldownField.getText(), config.tradeCooldownTicks));
        config.villagerCooldownTicks = Math.max(0, parseInt(this.villagerCooldownField.getText(), config.villagerCooldownTicks));
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Text getKeyText(KeyBinding keyBinding) {
        return keyBinding == null ? Text.translatable("text.autotrade-plus.none") : keyBinding.getBoundKeyLocalizedText();
    }

    private static Text boolText(boolean value) {
        return Text.translatable(value ? "options.on" : "options.off");
    }

    private Text summarize(String value, String emptyValue) {
        if (value == null || value.isBlank()) {
            return Text.literal(emptyValue);
        }
        String[] parts = value.split("[,，]");
        int count = 0;
        String first = "";
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (first.isEmpty()) {
                first = trimmed;
            }
            count++;
        }
        if (count <= 1) {
            return Text.literal(first.isEmpty() ? emptyValue : first);
        }
        return Text.translatable("text.autotrade-plus.selection.summary", first, count);
    }

    private Text summarizeProfession(String value) {
        String normalized = VillagerProfessionCatalog.normalizeSelection(value);
        if (VillagerProfessionCatalog.ALL_VALUE.equals(normalized)) {
            return VillagerProfessionCatalog.displayTextForValue(VillagerProfessionCatalog.ALL_VALUE);
        }
        String[] parts = normalized.split("[,，]");
        int count = 0;
        Text first = Text.empty();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (count == 0) {
                first = VillagerProfessionCatalog.displayTextForValue(trimmed);
            }
            count++;
        }
        if (count <= 1) {
            return count == 0 ? VillagerProfessionCatalog.displayTextForValue(VillagerProfessionCatalog.ALL_VALUE) : first;
        }
        return Text.translatable("text.autotrade-plus.selection.summary", first.getString(), count);
    }

    private static Text rowText(Text label, Text value) {
        return Text.literal(label.getString() + ": " + value.getString());
    }

}
