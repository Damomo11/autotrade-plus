package com.momo.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class InventoryDropper {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    private InventoryDropper() {
    }

    public static void keepOnlyOne(Item item) {
        if (CLIENT.player == null || CLIENT.gameMode == null || CLIENT.player.isSpectator()) {
            return;
        }

        for (int slot = 0; slot < CLIENT.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = CLIENT.player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item && stack.getCount() > 1) {
                dropOneAtATime(convertToScreenSlot(slot), stack.getCount() - 1);
            }
        }
    }

    private static void dropOneAtATime(int screenSlot, int count) {
        for (int i = 0; i < count; i++) {
            CLIENT.gameMode.handleContainerInput(
                    CLIENT.player.containerMenu.containerId,
                    screenSlot,
                    0,
                    ContainerInput.THROW,
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
