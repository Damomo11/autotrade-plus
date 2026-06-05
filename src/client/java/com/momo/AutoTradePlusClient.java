package com.momo;

import com.mojang.blaze3d.platform.InputConstants;
import com.momo.client.AutoTradePlusController;
import com.momo.config.ModConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class AutoTradePlusClient implements ClientModInitializer {
    public static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(AutoTradePlus.MOD_ID, "keybindings"));
    public static KeyMapping toggleKey;
    public static KeyMapping openConfigKey;
    private static final AutoTradePlusController CONTROLLER = new AutoTradePlusController();

	@Override
	public void onInitializeClient() {
        ModConfigManager.init();
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.autotrade-plus.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                KEY_CATEGORY
        ));
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.autotrade-plus.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KEY_CATEGORY
        ));
	}

    public static AutoTradePlusController controller() {
        return CONTROLLER;
    }
}
