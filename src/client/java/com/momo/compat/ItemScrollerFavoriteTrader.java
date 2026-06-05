package com.momo.compat;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.MerchantMenu;

public final class ItemScrollerFavoriteTrader {
    private int handledSyncId = -1;
    private boolean warned;

    public Result tryTradeAndClose(Minecraft client) {
        if (client.player == null || !(client.screen instanceof MerchantScreen screen)) {
            handledSyncId = -1;
            return Result.notMerchant();
        }

        MerchantMenu handler = screen.getMenu();
        if (handler.containerId == handledSyncId) {
            return Result.alreadyHandled();
        }

        handledSyncId = handler.containerId;
        try {
            boolean hasFavorites = hasFavoriteTrades(handler);
            if (hasFavorites) {
                tradeFavorites();
            }

            client.player.closeContainer();
            return Result.handled(hasFavorites);
        } catch (ReflectiveOperationException | LinkageError error) {
            if (!warned) {
                warned = true;
                error.printStackTrace();
            }
            return Result.failure();
        }
    }

    private boolean hasFavoriteTrades(MerchantMenu handler) throws ReflectiveOperationException {
        Class<?> storageClass = Class.forName("fi.dy.masa.itemscroller.villager.VillagerDataStorage");
        Object storage = storageClass.getMethod("getInstance").invoke(null);
        Method getFavorites = storageClass.getMethod("getFavoritesForCurrentVillager", MerchantMenu.class);
        Object favoriteData = getFavorites.invoke(storage, handler);
        Object favorites = favoriteData.getClass().getMethod("favorites").invoke(favoriteData);
        return !(Boolean) favorites.getClass().getMethod("isEmpty").invoke(favorites);
    }

    private void tradeFavorites() throws ReflectiveOperationException {
        Class<?> inventoryUtilsClass = Class.forName("fi.dy.masa.itemscroller.util.InventoryUtils");
        inventoryUtilsClass.getMethod("villagerTradeEverythingPossibleWithAllFavoritedTrades").invoke(null);
    }

    public record Result(boolean merchantScreen, boolean handled, boolean traded, boolean failed) {
        private static Result notMerchant() {
            return new Result(false, false, false, false);
        }

        private static Result alreadyHandled() {
            return new Result(true, false, false, false);
        }

        private static Result handled(boolean traded) {
            return new Result(true, true, traded, false);
        }

        private static Result failure() {
            return new Result(true, true, false, true);
        }
    }
}
