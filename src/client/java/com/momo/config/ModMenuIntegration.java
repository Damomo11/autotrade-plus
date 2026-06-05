package com.momo.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            try {
                return ModConfigManager.createConfigScreen(parent);
            } catch (Exception e) {
                return new ErrorScreen(Text.translatable("text.autotrade-plus.error.config"), Text.of(e.getMessage()));
            }
        };
    }

    private static class ErrorScreen extends Screen {
        private final Text message;

        protected ErrorScreen(Text title, Text message) {
            super(title);
            this.message = message;
        }

        @Override
        public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 50, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, this.message, this.width / 2, 70, 0xFF5555);
        }

        @Override
        public void close() {
            this.client.setScreen(null);
        }
    }
}
