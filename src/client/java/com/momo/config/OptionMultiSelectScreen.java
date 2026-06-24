package com.momo.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class OptionMultiSelectScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_ITEM_RESULTS = 500;
    private static final int COLUMN_GAP = 10;

    private final Screen parent;
    private final List<Option> allOptions;
    private final Set<String> selected;
    private final Consumer<String> saveConsumer;
    private final boolean professionMode;
    private final Component emptyText;

    private EditBox searchField;
    private StringWidget selectedCountWidget;
    private Button addCustomButton;
    private List<Option> availableOptions = List.of();
    private List<Option> selectedOptions = List.of();
    private final List<Button> availableButtons = new ArrayList<>();
    private final List<Button> selectedButtons = new ArrayList<>();
    private int availableScroll;
    private int selectedScroll;

    public static OptionMultiSelectScreen professions(Screen parent, String currentValue, Consumer<String> saveConsumer) {
        List<Option> options = new ArrayList<>();
        Set<String> knownValues = new LinkedHashSet<>();
        addOption(options, knownValues, new Option(
                VillagerProfessionCatalog.ALL_VALUE,
                VillagerProfessionCatalog.displayTextForValue(VillagerProfessionCatalog.ALL_VALUE).copy()
                        .append("  ")
                        .append(VillagerProfessionCatalog.ALL_VALUE),
                VillagerProfessionCatalog.searchTextForValue(VillagerProfessionCatalog.ALL_VALUE),
                null
        ));
        for (var profession : BuiltInRegistries.VILLAGER_PROFESSION.stream().toList()) {
            Identifier id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
            if (id == null) {
                continue;
            }
            String value = id.toString();
            addOption(options, knownValues, new Option(
                    value,
                    VillagerProfessionCatalog.displayTextForValue(value).copy().append("  ").append(value),
                    VillagerProfessionCatalog.searchTextForValue(value),
                    null
            ));
        }
        Set<String> selected = VillagerProfessionCatalog.normalizedValues(currentValue);
        if (selected.isEmpty()) {
            selected.add(VillagerProfessionCatalog.ALL_VALUE);
        }
        return new OptionMultiSelectScreen(
                parent,
                Component.translatable("text.autotrade-plus.selector.professions"),
                options,
                selected,
                saveConsumer,
                true,
                Component.translatable("text.autotrade-plus.selector.no_professions")
        );
    }

    public static OptionMultiSelectScreen items(Screen parent, String currentValue, Consumer<String> saveConsumer) {
        List<Option> options = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM.stream().toList()) {
            if (item == Items.AIR) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) {
                continue;
            }
            Component name = item.getName(new net.minecraft.world.item.ItemStack(item));
            String searchText = (id + " " + name.getString() + " " + item.getDescriptionId()).toLowerCase(Locale.ROOT);
            options.add(new Option(id.toString(), name, searchText, item));
        }
        options.sort(Comparator.comparing(option -> option.value));
        return new OptionMultiSelectScreen(
                parent,
                Component.translatable("text.autotrade-plus.selector.items"),
                options,
                parseCsv(currentValue),
                saveConsumer,
                false,
                Component.translatable("text.autotrade-plus.selector.no_items")
        );
    }

    private OptionMultiSelectScreen(
            Screen parent,
            Component title,
            List<Option> allOptions,
            Set<String> selected,
            Consumer<String> saveConsumer,
            boolean professionMode,
            Component emptyText
    ) {
        super(title);
        this.parent = parent;
        this.allOptions = allOptions;
        this.selected = new LinkedHashSet<>(selected);
        this.saveConsumer = saveConsumer;
        this.professionMode = professionMode;
        this.emptyText = emptyText;
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(420, this.width - 32);
        int left = (this.width - contentWidth) / 2;
        addRenderableWidget(new StringWidget(0, 10, this.width, 18, this.title, this.font));
        this.searchField = new EditBox(
                this.font,
                left,
                32,
                contentWidth,
                20,
                Component.translatable("text.autotrade-plus.selector.search")
        );
        this.searchField.setHint(Component.translatable("text.autotrade-plus.selector.search"));
        this.searchField.setResponder(value -> {
            this.availableScroll = 0;
            this.selectedScroll = 0;
            updateOptions();
        });
        addRenderableWidget(this.searchField);

        int columnWidth = getColumnWidth();
        int listTop = getListTop();
        int listLeft = getListLeft();
        addRenderableWidget(Button.builder(Component.translatable("text.autotrade-plus.selector.available"), button -> {
                })
                .bounds(listLeft, listTop - 24, columnWidth, 20)
                .build()).active = false;
        addRenderableWidget(Button.builder(Component.translatable("text.autotrade-plus.selector.selected"), button -> {
                })
                .bounds(listLeft + columnWidth + COLUMN_GAP, listTop - 24, columnWidth, 20)
                .build()).active = false;

        int buttonY = this.height - 28;
        int actionWidth = Math.max(40, (contentWidth - 24) / 5);
        addRenderableWidget(Button.builder(Component.translatable("text.autotrade-plus.selector.add_all"), button -> addAllVisible())
                .bounds(left, buttonY, actionWidth, 20)
                .build());
        this.addCustomButton = addRenderableWidget(Button.builder(Component.translatable("text.autotrade-plus.selector.add_custom"), button -> {
                    addCustomSearchValue();
                    updateOptions();
                })
                .bounds(left + actionWidth + 6, buttonY, actionWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("text.autotrade-plus.selector.clear"), button -> {
                    this.selected.clear();
                    updateOptions();
                })
                .bounds(left + (actionWidth + 6) * 2, buttonY, actionWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> finish())
                .bounds(left + (actionWidth + 6) * 3, buttonY, actionWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.minecraft.gui.setScreen(this.parent))
                .bounds(left + (actionWidth + 6) * 4, buttonY, actionWidth, 20)
                .build());

        int leftColumn = listLeft;
        int rightColumn = listLeft + columnWidth + COLUMN_GAP;
        int visibleRows = getVisibleRows();
        for (int row = 0; row < visibleRows; row++) {
            final int rowIndex = row;
            Button availableButton = Button.builder(Component.empty(), button -> addAvailable(rowIndex))
                    .bounds(leftColumn, listTop + row * ROW_HEIGHT, columnWidth, 20)
                    .build();
            this.availableButtons.add(addRenderableWidget(availableButton));

            Button selectedButton = Button.builder(Component.empty(), button -> removeSelected(rowIndex))
                    .bounds(rightColumn, listTop + row * ROW_HEIGHT, columnWidth, 20)
                    .build();
            this.selectedButtons.add(addRenderableWidget(selectedButton));
        }
        this.selectedCountWidget = addRenderableWidget(new StringWidget(
                0,
                this.height - 45,
                this.width,
                18,
                Component.empty(),
                this.font
        ));

        updateOptions();
        setInitialFocus(this.searchField);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int left = getListLeft();
        int columnWidth = getColumnWidth();
        if (mouseX >= left && mouseX < left + columnWidth) {
            return scrollAvailable(verticalAmount);
        }
        if (mouseX >= left + columnWidth + COLUMN_GAP && mouseX < left + columnWidth * 2 + COLUMN_GAP) {
            return scrollSelected(verticalAmount);
        }
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    private void updateOptions() {
        String query = this.searchField == null ? "" : this.searchField.getValue().trim().toLowerCase(Locale.ROOT);
        List<Option> available = new ArrayList<>();
        List<Option> selectedResult = new ArrayList<>();
        Set<String> knownValues = new LinkedHashSet<>();
        for (Option option : this.allOptions) {
            knownValues.add(option.value);
            if (!query.isEmpty() && !option.searchText.contains(query)) {
                continue;
            }
            if (this.selected.contains(option.value)) {
                selectedResult.add(option);
            } else {
                available.add(option);
                if (!this.professionMode && available.size() >= MAX_ITEM_RESULTS) {
                    break;
                }
            }
        }
        for (String value : this.selected) {
            if (!knownValues.contains(value) && (query.isEmpty() || value.toLowerCase(Locale.ROOT).contains(query))) {
                selectedResult.add(customOption(value));
            }
        }
        this.availableOptions = available;
        this.selectedOptions = selectedResult;
        this.availableScroll = clamp(this.availableScroll, 0, Math.max(0, this.availableOptions.size() - getVisibleRows()));
        this.selectedScroll = clamp(this.selectedScroll, 0, Math.max(0, this.selectedOptions.size() - getVisibleRows()));
        updateAddCustomButton();
        updateRowButtons();
    }

    private void updateRowButtons() {
        int visibleRows = this.availableButtons.size();
        for (int row = 0; row < visibleRows; row++) {
            updateColumnButton(this.availableButtons.get(row), this.availableOptions, this.availableScroll, row, "+ ", this.emptyText);
            updateColumnButton(this.selectedButtons.get(row), this.selectedOptions, this.selectedScroll, row, "- ", Component.translatable("text.autotrade-plus.selector.no_selected"));
        }
        if (this.selectedCountWidget != null) {
            this.selectedCountWidget.setMessage(Component.translatable("text.autotrade-plus.selector.selected_count", this.selected.size()));
        }
    }

    private void updateColumnButton(Button button, List<Option> options, int scroll, int row, String prefix, Component emptyText) {
        int index = scroll + row;
        if (index >= options.size()) {
            button.visible = row == 0 && options.isEmpty();
            button.active = false;
            button.setMessage(row == 0 ? emptyText : Component.empty());
            return;
        }
        button.visible = true;
        button.active = true;
        button.setMessage(Component.literal(prefix + optionLabel(options.get(index))));
    }

    private void addAvailable(int row) {
        int index = this.availableScroll + row;
        if (index >= 0 && index < this.availableOptions.size()) {
            add(this.availableOptions.get(index));
            updateOptions();
        }
    }

    private void removeSelected(int row) {
        int index = this.selectedScroll + row;
        if (index >= 0 && index < this.selectedOptions.size()) {
            remove(this.selectedOptions.get(index));
            updateOptions();
        }
    }

    private String optionLabel(Option option) {
        String label = option.item == null
                ? option.display.getString()
                : option.display.getString() + "  " + option.value;
        return this.font.plainSubstrByWidth(label, getColumnWidth() - 24);
    }

    private void add(Option option) {
        if (this.professionMode && VillagerProfessionCatalog.ALL_VALUE.equals(option.value)) {
            this.selected.clear();
            this.selected.add(VillagerProfessionCatalog.ALL_VALUE);
            return;
        }
        if (this.professionMode) {
            this.selected.remove(VillagerProfessionCatalog.ALL_VALUE);
        }
        this.selected.add(option.value);
    }

    private void addCustomSearchValue() {
        String value = customValueFromSearch();
        if (value != null) {
            add(customOption(value));
        }
    }

    private void updateAddCustomButton() {
        if (this.addCustomButton == null) {
            return;
        }
        String value = customValueFromSearch();
        this.addCustomButton.active = value != null && !this.selected.contains(value);
    }

    private String customValueFromSearch() {
        return this.searchField == null ? null : normalizeCustomValue(this.searchField.getValue());
    }

    private static String normalizeCustomValue(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        if (!value.contains(":")) {
            value = "minecraft:" + value;
        }
        return Identifier.tryParse(value) == null ? null : value;
    }

    private void remove(Option option) {
        this.selected.remove(option.value);
        if (this.professionMode && this.selected.isEmpty()) {
            this.selected.add(VillagerProfessionCatalog.ALL_VALUE);
        }
    }

    private void addAllVisible() {
        if (this.professionMode) {
            this.selected.clear();
            for (Option option : this.availableOptions) {
                if (!VillagerProfessionCatalog.ALL_VALUE.equals(option.value)) {
                    this.selected.add(option.value);
                }
            }
            if (this.selected.isEmpty()) {
                this.selected.add(VillagerProfessionCatalog.ALL_VALUE);
            }
        } else {
            for (Option option : this.availableOptions) {
                this.selected.add(option.value);
            }
        }
        updateOptions();
    }

    private boolean scrollAvailable(double verticalAmount) {
        int maxScroll = Math.max(0, this.availableOptions.size() - getVisibleRows());
        if (maxScroll <= 0) {
            return false;
        }
        this.availableScroll = clamp(this.availableScroll - (int) Math.signum(verticalAmount), 0, maxScroll);
        updateRowButtons();
        return true;
    }

    private boolean scrollSelected(double verticalAmount) {
        int maxScroll = Math.max(0, this.selectedOptions.size() - getVisibleRows());
        if (maxScroll <= 0) {
            return false;
        }
        this.selectedScroll = clamp(this.selectedScroll - (int) Math.signum(verticalAmount), 0, maxScroll);
        updateRowButtons();
        return true;
    }

    private void finish() {
        if (this.professionMode && (this.selected.isEmpty() || this.selected.contains(VillagerProfessionCatalog.ALL_VALUE))) {
            this.saveConsumer.accept(VillagerProfessionCatalog.ALL_VALUE);
        } else {
            this.saveConsumer.accept(String.join(",", this.selected));
        }
        this.minecraft.gui.setScreen(this.parent);
    }

    private int getListLeft() {
        return (this.width - getListWidth()) / 2;
    }

    private int getListTop() {
        return 84;
    }

    private int getListWidth() {
        return Math.min(720, this.width - 32);
    }

    private int getColumnWidth() {
        return (getListWidth() - COLUMN_GAP) / 2;
    }

    private int getVisibleRows() {
        return Math.max(1, (this.height - getListTop() - 58) / ROW_HEIGHT);
    }

    private static Set<String> parseCsv(String value) {
        Set<String> result = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return result;
        }
        for (String part : value.split("[,，]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void addOption(List<Option> options, Set<String> knownValues, Option option) {
        if (knownValues.add(option.value)) {
            options.add(option);
        }
    }

    private static Option customOption(String value) {
        return new Option(value, Component.literal(value), value.toLowerCase(Locale.ROOT), null);
    }

    private record Option(String value, Component display, String searchText, Item item) {
    }
}
