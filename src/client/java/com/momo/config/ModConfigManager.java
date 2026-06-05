package com.momo.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ActionResult;

public class ModConfigManager {
    private static ConfigHolder<ModConfig> configHolder;

    static {
        configHolder = AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        configHolder.save();

        configHolder.registerSaveListener((holder, config) -> {
            System.out.println("[autotrade-plus] Config saved");
            return ActionResult.SUCCESS;
        });
    }

    public static void init() {
        if (configHolder != null) {
            configHolder.load();
        }
    }

    public static ModConfig get() {
        return configHolder != null ? configHolder.getConfig() : null;
    }

    public static void save() {
        if (configHolder != null) configHolder.save();
    }

    public static Screen createConfigScreen(Screen parent) {
        return new AutoTradePlusConfigScreen(parent);
    }
}
