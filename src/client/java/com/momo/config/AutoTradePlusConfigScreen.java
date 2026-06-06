package com.momo.config;

import com.mojang.blaze3d.platform.InputConstants;
import com.momo.AutoTradePlusClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class AutoTradePlusConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;

    private EditBox tradeRangeField;
    private EditBox tradeCooldownField;
    private EditBox villagerCooldownField;
    private Button professionButton;
    private Button dropItemsButton;
    private Button enabledButton;
    private Button sneakButton;
    private Button fishingButton;
    private Button debugButton;
    private Button autoCloseMerchantScreenButton;
    private Button resumeTradeProgressButton;
    private Button dropModeButton;
    private Button toggleKeyButton;
    private Button openConfigKeyButton;
    private KeyMapping editingKeyBinding;
    private Button editingKeyButton;

    public AutoTradePlusConfigScreen(Screen parent) {
        super(Component.translatable("text.autoconfig.autotrade-plus.title"));
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
        int buttonY = this.height - 28;
        int rowGap = Math.min(29, Math.max(22, (buttonY - 52) / 6));
        int y = Math.min(42, Math.max(24, buttonY - 28 - rowGap * 6));

        addRenderableWidget(new StringWidget(0, 12, this.width, 18, this.title, this.font));

        this.professionButton = addButtonRow(
                leftColumn,
                y,
                columnWidth,
                Component.translatable("text.autoconfig.autotrade-plus.option.villagerProfession"),
                summarizeProfession(config.villagerProfession),
                button -> this.minecraft.setScreen(OptionMultiSelectScreen.professions(this, config.villagerProfession, value -> {
                    config.villagerProfession = value;
                    updateDynamicMessages();
                }))
        );
        this.enabledButton = addButtonRow(
                rightColumn,
                y,
                columnWidth,
                Component.translatable("text.autoconfig.autotrade-plus.option.enabled"),
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
                Component.translatable("text.autoconfig.autotrade-plus.option.tradeRange"),
                Double.toString(config.tradeRange)
        );
        this.sneakButton = addButtonRow(
                rightColumn,
                y,
                columnWidth,
                Component.translatable("text.autoconfig.autotrade-plus.option.sneak"),
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
                Component.translatable("text.autoconfig.autotrade-plus.option.tradeCooldownTicks"),
                Integer.toString(config.tradeCooldownTicks)
        );
        this.fishingButton = addButtonRow(
                rightColumn,
                y,
                columnWidth,
                Component.translatable("text.autoconfig.autotrade-plus.option.fishingMode"),
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
                Component.translatable("text.autoconfig.autotrade-plus.option.villagerCooldownTicks"),
                Integer.toString(config.villagerCooldownTicks)
        );
        this.debugButton = addButtonRow(
                rightColumn,
                y,
                columnWidth,
                Component.translatable("text.autoconfig.autotrade-plus.option.debug"),
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
                Component.translatable("text.autoconfig.autotrade-plus.option.dropMode"),
                Component.translatable(config.dropMode.translationKey()),
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
                Component.translatable("text.autoconfig.autotrade-plus.option.dropItems"),
                summarize(config.dropItems, Component.translatable("text.autotrade-plus.none").getString()),
                button -> this.minecraft.setScreen(OptionMultiSelectScreen.items(this, config.dropItems, value -> {
                    config.dropItems = value;
                    updateDynamicMessages();
                }))
        );

        y += rowGap;
        this.autoCloseMerchantScreenButton = addButtonRow(
                leftColumn,
                y,
                columnWidth,
                Component.translatable("text.autoconfig.autotrade-plus.option.autoCloseMerchantScreen"),
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
                Component.translatable("text.autoconfig.autotrade-plus.option.resumeTradeProgress"),
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
                Component.translatable("key.autotrade-plus.toggle"),
                getKeyText(AutoTradePlusClient.toggleKey),
                button -> startEditingKey(AutoTradePlusClient.toggleKey, button)
        );
        this.openConfigKeyButton = addButtonRow(
                rightColumn,
                y,
                columnWidth,
                Component.translatable("key.autotrade-plus.open_config"),
                getKeyText(AutoTradePlusClient.openConfigKey),
                button -> startEditingKey(AutoTradePlusClient.openConfigKey, button)
        );

        buttonY = Math.min(buttonY, y + rowGap + 12);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> finish())
                .bounds(this.width / 2 - 104, buttonY, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.minecraft.setScreen(this.parent))
                .bounds(this.width / 2 + 4, buttonY, 100, 20)
                .build());
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (this.editingKeyBinding != null) {
            this.editingKeyBinding.setKey(input.key() == 256 ? InputConstants.UNKNOWN : InputConstants.getKey(input));
            KeyMapping.resetMapping();
            this.minecraft.options.save();
            this.editingKeyBinding = null;
            this.editingKeyButton = null;
            updateDynamicMessages();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (this.editingKeyBinding != null) {
            this.editingKeyBinding = null;
            this.editingKeyButton = null;
            updateDynamicMessages();
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void onClose() {
        finish();
    }

    private EditBox addTextFieldRow(int x, int y, int width, Component label, String value) {
        addLabelWidget(x, y, label);
        EditBox field = new EditBox(this.font, x + 128, y, width - 128, 20, label);
        field.setValue(value);
        field.setMaxLength(16);
        return addRenderableWidget(field);
    }

    private Button addButtonRow(int x, int y, int width, Component label, Component value, Button.OnPress action) {
        return addRenderableWidget(Button.builder(rowText(label, value), action)
                .bounds(x, y, width, 20)
                .build());
    }

    private void addLabelWidget(int x, int y, Component label) {
        Button labelWidget = Button.builder(label, button -> {
                })
                .bounds(x, y, 124, 20)
                .build();
        labelWidget.active = false;
        addRenderableWidget(labelWidget);
    }

    private void startEditingKey(KeyMapping keyBinding, Button button) {
        if (keyBinding == null) {
            return;
        }
        this.editingKeyBinding = keyBinding;
        this.editingKeyButton = button;
        button.setMessage(Component.translatable("text.autotrade-plus.keybind.press"));
    }

    private void updateDynamicMessages() {
        if (this.professionButton != null) {
            this.professionButton.setMessage(rowText(
                    Component.translatable("text.autoconfig.autotrade-plus.option.villagerProfession"),
                    summarizeProfession(config.villagerProfession)
            ));
        }
        if (this.dropItemsButton != null) {
            this.dropItemsButton.setMessage(rowText(
                    Component.translatable("text.autoconfig.autotrade-plus.option.dropItems"),
                    summarize(config.dropItems, Component.translatable("text.autotrade-plus.none").getString())
            ));
        }
        if (this.enabledButton != null) {
            this.enabledButton.setMessage(rowText(Component.translatable("text.autoconfig.autotrade-plus.option.enabled"), boolText(config.enabled)));
        }
        if (this.sneakButton != null) {
            this.sneakButton.setMessage(rowText(Component.translatable("text.autoconfig.autotrade-plus.option.sneak"), boolText(config.sneak)));
        }
        if (this.fishingButton != null) {
            this.fishingButton.setMessage(rowText(Component.translatable("text.autoconfig.autotrade-plus.option.fishingMode"), boolText(config.fishingMode)));
        }
        if (this.debugButton != null) {
            this.debugButton.setMessage(rowText(Component.translatable("text.autoconfig.autotrade-plus.option.debug"), boolText(config.debug)));
        }
        if (this.autoCloseMerchantScreenButton != null) {
            this.autoCloseMerchantScreenButton.setMessage(rowText(
                    Component.translatable("text.autoconfig.autotrade-plus.option.autoCloseMerchantScreen"),
                    boolText(config.autoCloseMerchantScreen)
            ));
        }
        if (this.resumeTradeProgressButton != null) {
            this.resumeTradeProgressButton.setMessage(rowText(
                    Component.translatable("text.autoconfig.autotrade-plus.option.resumeTradeProgress"),
                    boolText(config.resumeTradeProgress)
            ));
        }
        if (this.dropModeButton != null) {
            this.dropModeButton.setMessage(rowText(
                    Component.translatable("text.autoconfig.autotrade-plus.option.dropMode"),
                    Component.translatable(config.dropMode.translationKey())
            ));
        }
        if (this.toggleKeyButton != null && this.editingKeyButton != this.toggleKeyButton) {
            this.toggleKeyButton.setMessage(rowText(Component.translatable("key.autotrade-plus.toggle"), getKeyText(AutoTradePlusClient.toggleKey)));
        }
        if (this.openConfigKeyButton != null && this.editingKeyButton != this.openConfigKeyButton) {
            this.openConfigKeyButton.setMessage(rowText(Component.translatable("key.autotrade-plus.open_config"), getKeyText(AutoTradePlusClient.openConfigKey)));
        }
    }

    private void finish() {
        applyNumericFields();
        ModConfigManager.save();
        this.minecraft.options.save();
        this.minecraft.setScreen(this.parent);
    }

    private void applyNumericFields() {
        config.tradeRange = Math.max(0.0, parseDouble(this.tradeRangeField.getValue(), config.tradeRange));
        config.tradeCooldownTicks = Math.max(0, parseInt(this.tradeCooldownField.getValue(), config.tradeCooldownTicks));
        config.villagerCooldownTicks = Math.max(0, parseInt(this.villagerCooldownField.getValue(), config.villagerCooldownTicks));
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

    private Component getKeyText(KeyMapping keyBinding) {
        return keyBinding == null ? Component.translatable("text.autotrade-plus.none") : keyBinding.getTranslatedKeyMessage();
    }

    private static Component boolText(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private Component summarize(String value, String emptyValue) {
        if (value == null || value.isBlank()) {
            return Component.literal(emptyValue);
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
            return Component.literal(first.isEmpty() ? emptyValue : first);
        }
        return Component.translatable("text.autotrade-plus.selection.summary", first, count);
    }

    private Component summarizeProfession(String value) {
        String normalized = VillagerProfessionCatalog.normalizeSelection(value);
        if (VillagerProfessionCatalog.ALL_VALUE.equals(normalized)) {
            return VillagerProfessionCatalog.displayTextForValue(VillagerProfessionCatalog.ALL_VALUE);
        }
        String[] parts = normalized.split("[,，]");
        int count = 0;
        Component first = Component.empty();
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
        return Component.translatable("text.autotrade-plus.selection.summary", first.getString(), count);
    }

    private static Component rowText(Component label, Component value) {
        return Component.literal(label.getString() + ": " + value.getString());
    }

}
