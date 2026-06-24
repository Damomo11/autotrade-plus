package com.momo.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TimedTradeTimesScreen extends Screen {
    private static final int ROW_HEIGHT = 24;

    private final Screen parent;
    private final Set<Integer> selected;
    private final Consumer<String> saveConsumer;
    private final List<Button> timeButtons = new ArrayList<>();

    private EditBox timeField;
    private Button addButton;
    private StringWidget selectedCountWidget;
    private List<Integer> orderedTimes = List.of();
    private int scroll;

    public TimedTradeTimesScreen(Screen parent, String currentValue, Consumer<String> saveConsumer) {
        super(Component.translatable("text.autotrade-plus.selector.timed_trade_times"));
        this.parent = parent;
        this.selected = new LinkedHashSet<>(TimedTradeSchedule.parse(currentValue));
        this.saveConsumer = saveConsumer;
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(420, this.width - 32);
        int left = (this.width - contentWidth) / 2;
        addRenderableWidget(new StringWidget(0, 10, this.width, 18, this.title, this.font));

        this.timeField = new EditBox(
                this.font,
                left,
                32,
                contentWidth - 86,
                20,
                Component.translatable("text.autotrade-plus.selector.timed_trade_time")
        );
        this.timeField.setHint(Component.translatable("text.autotrade-plus.selector.timed_trade_time"));
        this.timeField.setResponder(value -> updateAddButton());
        addRenderableWidget(this.timeField);

        this.addButton = addRenderableWidget(Button.builder(Component.translatable("text.autotrade-plus.selector.add_time"), button -> addTime())
                .bounds(left + contentWidth - 80, 32, 80, 20)
                .build());

        int listTop = 68;
        int visibleRows = getVisibleRows();
        for (int row = 0; row < visibleRows; row++) {
            final int rowIndex = row;
            Button timeButton = Button.builder(Component.empty(), button -> removeTime(rowIndex))
                    .bounds(left, listTop + row * ROW_HEIGHT, contentWidth, 20)
                    .build();
            this.timeButtons.add(addRenderableWidget(timeButton));
        }

        int buttonY = this.height - 28;
        int actionWidth = Math.max(80, (contentWidth - 12) / 3);
        addRenderableWidget(Button.builder(Component.translatable("text.autotrade-plus.selector.clear"), button -> {
                    this.selected.clear();
                    updateTimes();
                })
                .bounds(left, buttonY, actionWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> finish())
                .bounds(left + actionWidth + 6, buttonY, actionWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.minecraft.setScreen(this.parent))
                .bounds(left + (actionWidth + 6) * 2, buttonY, actionWidth, 20)
                .build());

        this.selectedCountWidget = addRenderableWidget(new StringWidget(
                0,
                this.height - 45,
                this.width,
                18,
                Component.empty(),
                this.font
        ));

        updateTimes();
        setInitialFocus(this.timeField);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, this.orderedTimes.size() - getVisibleRows());
        if (maxScroll <= 0) {
            return false;
        }
        this.scroll = clamp(this.scroll - (int) Math.signum(verticalAmount), 0, maxScroll);
        updateRowButtons();
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private void addTime() {
        Integer tick = parseTimeField();
        if (tick == null) {
            return;
        }
        this.selected.add(tick);
        this.timeField.setValue("");
        updateTimes();
    }

    private void removeTime(int row) {
        int index = this.scroll + row;
        if (index >= 0 && index < this.orderedTimes.size()) {
            this.selected.remove(this.orderedTimes.get(index));
            updateTimes();
        }
    }

    private void updateTimes() {
        this.orderedTimes = new ArrayList<>(this.selected);
        this.orderedTimes.sort(Integer::compareTo);
        this.scroll = clamp(this.scroll, 0, Math.max(0, this.orderedTimes.size() - getVisibleRows()));
        updateAddButton();
        updateRowButtons();
    }

    private void updateAddButton() {
        if (this.addButton != null) {
            Integer tick = parseTimeField();
            this.addButton.active = tick != null && !this.selected.contains(tick);
        }
    }

    private void updateRowButtons() {
        int visibleRows = this.timeButtons.size();
        for (int row = 0; row < visibleRows; row++) {
            Button button = this.timeButtons.get(row);
            int index = this.scroll + row;
            if (index >= this.orderedTimes.size()) {
                button.visible = row == 0 && this.orderedTimes.isEmpty();
                button.active = false;
                button.setMessage(row == 0
                        ? Component.translatable("text.autotrade-plus.selector.no_timed_trade_times")
                        : Component.empty());
                continue;
            }
            int tick = this.orderedTimes.get(index);
            button.visible = true;
            button.active = true;
            button.setMessage(Component.translatable("text.autotrade-plus.selector.remove_time", tick));
        }
        if (this.selectedCountWidget != null) {
            this.selectedCountWidget.setMessage(Component.translatable("text.autotrade-plus.selector.timed_trade_time_count", this.selected.size()));
        }
    }

    private void finish() {
        this.saveConsumer.accept(TimedTradeSchedule.join(this.orderedTimes));
        this.minecraft.setScreen(this.parent);
    }

    private Integer parseTimeField() {
        if (this.timeField == null) {
            return null;
        }
        try {
            int tick = Integer.parseInt(this.timeField.getValue().trim());
            return tick >= 0 && tick < TimedTradeSchedule.DAY_TICKS ? tick : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int getVisibleRows() {
        return Math.max(1, (this.height - 126) / ROW_HEIGHT);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
