package com.momo.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public final class InventoryDropper {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    private InventoryDropper() {
    }

    public static void keepOnlyOne(Item item) {
        if (CLIENT.player == null || CLIENT.interactionManager == null || CLIENT.player.isSpectator()) {
            return;
        }

        for (int slot = 0; slot < CLIENT.player.getInventory().size(); slot++) {
            ItemStack stack = CLIENT.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.getItem() == item && stack.getCount() > 1) {
                dropOneAtATime(convertToScreenSlot(slot), stack.getCount() - 1);
            }
        }
    }

    private static void dropOneAtATime(int screenSlot, int count) {
        for (int i = 0; i < count; i++) {
            CLIENT.interactionManager.clickSlot(
                    CLIENT.player.currentScreenHandler.syncId,
                    screenSlot,
                    0,
                    SlotActionType.THROW,
                    CLIENT.player
            );
        }
    }

    private static int convertToScreenSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < 9) {
            return 36 + inventorySlot;
        }
        if (inventorySlot >= 9 && inventorySlot < 36) {
            return inventorySlot;
        }
        if (inventorySlot >= 36 && inventorySlot < 40) {
            return 5 + (inventorySlot - 36);
        }
        if (inventorySlot == 40) {
            return 45;
        }
        return inventorySlot;
    }
}
