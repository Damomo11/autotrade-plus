package com.momo.config;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public class OptionMultiSelectScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_ITEM_RESULTS = 500;
    private static final int COLUMN_GAP = 10;

    private final Screen parent;
    private final List<Option> allOptions;
    private final Set<String> selected;
    private final Consumer<String> saveConsumer;
    private final boolean professionMode;
    private final Text emptyText;

    private TextFieldWidget searchField;
    private TextWidget selectedCountWidget;
    private ButtonWidget addCustomButton;
    private List<Option> availableOptions = List.of();
    private List<Option> selectedOptions = List.of();
    private final List<ButtonWidget> availableButtons = new ArrayList<>();
    private final List<ButtonWidget> selectedButtons = new ArrayList<>();
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
        for (var profession : Registries.VILLAGER_PROFESSION.stream().toList()) {
            Identifier id = Registries.VILLAGER_PROFESSION.getId(profession);
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
                Text.translatable("text.autotrade-plus.selector.professions"),
                options,
                selected,
                saveConsumer,
                true,
                Text.translatable("text.autotrade-plus.selector.no_professions")
        );
    }

    public static OptionMultiSelectScreen items(Screen parent, String currentValue, Consumer<String> saveConsumer) {
        List<Option> options = new ArrayList<>();
        for (Item item : Registries.ITEM.stream().toList()) {
            if (item == Items.AIR) {
                continue;
            }
            Identifier id = Registries.ITEM.getId(item);
            if (id == null) {
                continue;
            }
            Text name = item.getName();
            String searchText = (id + " " + name.getString() + " " + item.getTranslationKey()).toLowerCase(Locale.ROOT);
            options.add(new Option(id.toString(), name, searchText, item));
        }
        options.sort(Comparator.comparing(option -> option.value));
        return new OptionMultiSelectScreen(
                parent,
                Text.translatable("text.autotrade-plus.selector.items"),
                options,
                parseCsv(currentValue),
                saveConsumer,
                false,
                Text.translatable("text.autotrade-plus.selector.no_items")
        );
    }

    private OptionMultiSelectScreen(
            Screen parent,
            Text title,
            List<Option> allOptions,
            Set<String> selected,
            Consumer<String> saveConsumer,
            boolean professionMode,
            Text emptyText
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
        addDrawableChild(new TextWidget(0, 10, this.width, 18, this.title, this.textRenderer));
        this.searchField = new TextFieldWidget(
                this.textRenderer,
                left,
                32,
                contentWidth,
                20,
                Text.translatable("text.autotrade-plus.selector.search")
        );
        this.searchField.setPlaceholder(Text.translatable("text.autotrade-plus.selector.search"));
        this.searchField.setChangedListener(value -> {
            this.availableScroll = 0;
            this.selectedScroll = 0;
            updateOptions();
        });
        addDrawableChild(this.searchField);

        int columnWidth = getColumnWidth();
        int listTop = getListTop();
        int listLeft = getListLeft();
        addDrawableChild(ButtonWidget.builder(Text.translatable("text.autotrade-plus.selector.available"), button -> {
                })
                .dimensions(listLeft, listTop - 24, columnWidth, 20)
                .build()).active = false;
        addDrawableChild(ButtonWidget.builder(Text.translatable("text.autotrade-plus.selector.selected"), button -> {
                })
                .dimensions(listLeft + columnWidth + COLUMN_GAP, listTop - 24, columnWidth, 20)
                .build()).active = false;

        int buttonY = this.height - 28;
        int actionWidth = Math.max(40, (contentWidth - 24) / 5);
        addDrawableChild(ButtonWidget.builder(Text.translatable("text.autotrade-plus.selector.add_all"), button -> addAllVisible())
                .dimensions(left, buttonY, actionWidth, 20)
                .build());
        this.addCustomButton = addDrawableChild(ButtonWidget.builder(Text.translatable("text.autotrade-plus.selector.add_custom"), button -> {
                    addCustomSearchValue();
                    updateOptions();
                })
                .dimensions(left + actionWidth + 6, buttonY, actionWidth, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("text.autotrade-plus.selector.clear"), button -> {
                    this.selected.clear();
                    updateOptions();
                })
                .dimensions(left + (actionWidth + 6) * 2, buttonY, actionWidth, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> finish())
                .dimensions(left + (actionWidth + 6) * 3, buttonY, actionWidth, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> this.client.setScreen(this.parent))
                .dimensions(left + (actionWidth + 6) * 4, buttonY, actionWidth, 20)
                .build());

        int leftColumn = listLeft;
        int rightColumn = listLeft + columnWidth + COLUMN_GAP;
        int visibleRows = getVisibleRows();
        for (int row = 0; row < visibleRows; row++) {
            final int rowIndex = row;
            ButtonWidget availableButton = ButtonWidget.builder(Text.empty(), button -> addAvailable(rowIndex))
                    .dimensions(leftColumn, listTop + row * ROW_HEIGHT, columnWidth, 20)
                    .build();
            this.availableButtons.add(addDrawableChild(availableButton));

            ButtonWidget selectedButton = ButtonWidget.builder(Text.empty(), button -> removeSelected(rowIndex))
                    .dimensions(rightColumn, listTop + row * ROW_HEIGHT, columnWidth, 20)
                    .build();
            this.selectedButtons.add(addDrawableChild(selectedButton));
        }
        this.selectedCountWidget = addDrawableChild(new TextWidget(
                0,
                this.height - 45,
                this.width,
                18,
                Text.empty(),
                this.textRenderer
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
    public void close() {
        this.client.setScreen(this.parent);
    }

    private void updateOptions() {
        String query = this.searchField == null ? "" : this.searchField.getText().trim().toLowerCase(Locale.ROOT);
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
            updateColumnButton(this.selectedButtons.get(row), this.selectedOptions, this.selectedScroll, row, "- ", Text.translatable("text.autotrade-plus.selector.no_selected"));
        }
        if (this.selectedCountWidget != null) {
            this.selectedCountWidget.setMessage(Text.translatable("text.autotrade-plus.selector.selected_count", this.selected.size()));
        }
    }

    private void updateColumnButton(ButtonWidget button, List<Option> options, int scroll, int row, String prefix, Text emptyText) {
        int index = scroll + row;
        if (index >= options.size()) {
            button.visible = row == 0 && options.isEmpty();
            button.active = false;
            button.setMessage(row == 0 ? emptyText : Text.empty());
            return;
        }
        button.visible = true;
        button.active = true;
        button.setMessage(Text.literal(prefix + optionLabel(options.get(index))));
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
        return this.textRenderer.trimToWidth(label, getColumnWidth() - 24);
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
        return this.searchField == null ? null : normalizeCustomValue(this.searchField.getText());
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
        this.client.setScreen(this.parent);
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
        return new Option(value, Text.literal(value), value.toLowerCase(Locale.ROOT), null);
    }

    private record Option(String value, Text display, String searchText, Item item) {
    }
}
