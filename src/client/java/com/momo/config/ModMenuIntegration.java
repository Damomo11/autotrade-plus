package com.momo.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            try {
                return ModConfigManager.createConfigScreen(parent);
            } catch (Exception e) {
                return new ErrorScreen(Component.translatable("text.autotrade-plus.error.config"), Component.nullToEmpty(e.getMessage()));
            }
        };
    }

    private static class ErrorScreen extends Screen {
        private final Component message;

        protected ErrorScreen(Component title, Component message) {
            super(title);
            this.message = message;
        }

        @Override
        protected void init() {
            addRenderableWidget(new StringWidget(0, 50, this.width, 18, this.title, this.font));
            addRenderableWidget(new StringWidget(0, 70, this.width, 18, this.message, this.font));
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(null);
        }
    }
}
