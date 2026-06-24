package com.momo.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "autotrade-plus")
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public String villagerProfession = VillagerProfessionCatalog.ALL_VALUE;

    @ConfigEntry.Gui.Tooltip
    public double tradeRange = 5.0;

    @ConfigEntry.Gui.Tooltip
    public int tradeCooldownTicks = 200;

    @ConfigEntry.Gui.Tooltip
    public int villagerCooldownTicks = 20;

    @ConfigEntry.Gui.Tooltip
    public boolean timedMode = false;

    @ConfigEntry.Gui.Tooltip
    public String timedTradeTimes = TimedTradeSchedule.DEFAULT_TIMES;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DropMode dropMode = DropMode.DISABLED;

    @ConfigEntry.Gui.Tooltip
    public String dropItems = "";

    public enum DropMode {
        DISABLED("Disabled", "text.autotrade-plus.drop_mode.disabled"),
        AFTER_TRADE("Drop after trade", "text.autotrade-plus.drop_mode.after_trade");

        private final String displayName;
        private final String translationKey;

        DropMode(String displayName, String translationKey) {
            this.displayName = displayName;
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @ConfigEntry.Gui.Tooltip
    public boolean sneak = false;

    @ConfigEntry.Gui.Tooltip
    public boolean fishingMode = false;

    @ConfigEntry.Gui.Tooltip
    public boolean debug = false;

    @ConfigEntry.Gui.Tooltip
    public boolean autoCloseMerchantScreen = true;

    @ConfigEntry.Gui.Tooltip
    public boolean resumeTradeProgress = false;

    @ConfigEntry.Gui.Tooltip
    public boolean enabled = false;

    @Override
    public void validatePostLoad() {
        villagerProfession = VillagerProfessionCatalog.normalizeSelection(villagerProfession);
        timedTradeTimes = TimedTradeSchedule.normalize(timedTradeTimes);
        if (tradeRange < 0) tradeRange = 0;
        if (tradeCooldownTicks < 0) tradeCooldownTicks = 0;
        if (villagerCooldownTicks < 0) villagerCooldownTicks = 0;
    }
}
