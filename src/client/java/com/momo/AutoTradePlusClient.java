package com.momo;

import com.momo.client.AutoTradePlusController;
import com.momo.config.ModConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class AutoTradePlusClient implements ClientModInitializer {
    public static final KeyBinding.Category KEY_CATEGORY =
            KeyBinding.Category.create(Identifier.of(AutoTradePlus.MOD_ID, "keybindings"));
    public static KeyBinding toggleKey;
    public static KeyBinding openConfigKey;
    private static final AutoTradePlusController CONTROLLER = new AutoTradePlusController();

	@Override
	public void onInitializeClient() {
        ModConfigManager.init();
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autotrade-plus.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                KEY_CATEGORY
        ));
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autotrade-plus.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KEY_CATEGORY
        ));
	}

    public static AutoTradePlusController controller() {
        return CONTROLLER;
    }
}
