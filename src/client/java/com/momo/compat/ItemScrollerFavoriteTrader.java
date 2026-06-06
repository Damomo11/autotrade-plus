package com.momo.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.screen.MerchantScreenHandler;

import java.lang.reflect.Method;
import java.util.UUID;

public final class ItemScrollerFavoriteTrader {
    private int handledSyncId = -1;
    private boolean warned;

    public void rememberInteractionTarget(UUID villagerUuid) {
        try {
            Class<?> storageClass = Class.forName("fi.dy.masa.itemscroller.villager.VillagerDataStorage");
            Object storage = storageClass.getMethod("getInstance").invoke(null);
            storageClass.getMethod("setLastInteractedUUID", UUID.class).invoke(storage, villagerUuid);
        } catch (ReflectiveOperationException | LinkageError error) {
            if (!warned) {
                warned = true;
                error.printStackTrace();
            }
        }
    }

    public Result tryTrade(MinecraftClient client, boolean closeScreen) {
        if (client.player == null || !(client.currentScreen instanceof MerchantScreen screen)) {
            handledSyncId = -1;
            return Result.notMerchant();
        }

        MerchantScreenHandler handler = screen.getScreenHandler();
        if (handler.syncId == handledSyncId) {
            return Result.alreadyHandled();
        }

        handledSyncId = handler.syncId;
        try {
            boolean hasFavorites = hasFavoriteTrades(handler);
            if (hasFavorites) {
                tradeFavorites();
            }

            if (closeScreen) {
                client.player.closeHandledScreen();
            }
            return Result.handled(hasFavorites);
        } catch (ReflectiveOperationException | LinkageError error) {
            if (!warned) {
                warned = true;
                error.printStackTrace();
            }
            return Result.failure();
        }
    }

    private boolean hasFavoriteTrades(MerchantScreenHandler handler) throws ReflectiveOperationException {
        Class<?> storageClass = Class.forName("fi.dy.masa.itemscroller.villager.VillagerDataStorage");
        Object storage = storageClass.getMethod("getInstance").invoke(null);
        Method getFavorites = storageClass.getMethod("getFavoritesForCurrentVillager", MerchantScreenHandler.class);
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
