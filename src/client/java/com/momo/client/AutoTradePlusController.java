package com.momo.client;

import com.momo.AutoTradePlusClient;
import com.momo.compat.ItemScrollerFavoriteTrader;
import com.momo.config.ModConfig;
import com.momo.config.ModConfigManager;
import com.momo.config.VillagerProfessionCatalog;
import com.momo.utils.InventoryDropper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class AutoTradePlusController {
    private int roundCooldown;
    private int perTargetCooldown;
    private int currentIndex;
    private int roundStartTick = -1;
    private int totalGameTick;
    private int pendingDropTicks;
    private int pendingTradeTicks;
    private int lastCompletedIndex = -1;
    private boolean enabledStateInitialized;
    private boolean lastKnownEnabled;
    private TradeContext pendingTrade = TradeContext.empty();

    private final Map<UUID, Integer> villagerOrderMap = new HashMap<>();
    private final List<UUID> orderedVillagers = new ArrayList<>();
    private final ItemScrollerFavoriteTrader favoriteTrader = new ItemScrollerFavoriteTrader();

    private List<Item> cachedDropItems = List.of();
    private String cachedDropItemsString = "";

    public void tick() {
        totalGameTick++;

        ModConfig config = ModConfigManager.get();
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        MultiPlayerGameMode interaction = client.gameMode;

        if (config == null || player == null || interaction == null) {
            return;
        }

        processPendingDrop(config);
        syncEnabledState(config);

        if (client.gui.screen() != null) {
            drainKeyPresses(AutoTradePlusClient.toggleKey);
            drainKeyPresses(AutoTradePlusClient.openConfigKey);
            return;
        }

        if (waitForPendingTrade(config)) {
            return;
        }

        if (consumeKeyPress(AutoTradePlusClient.openConfigKey)) {
            client.gui.setScreen(ModConfigManager.createConfigScreen(null));
            return;
        }

        if (consumeKeyPress(AutoTradePlusClient.toggleKey)) {
            toggleEnabled(config, player);
        }

        if (!config.enabled || shouldPauseForSneaking(config, player)) {
            return;
        }

        if (roundCooldown-- > 0 || perTargetCooldown-- > 0) {
            return;
        }

        if (config.fishingMode) {
            handleFishingMode(config, player, interaction);
        } else {
            handleVillagerTrading(config, player, interaction);
        }
    }

    public void onMerchantOffersUpdated() {
        ModConfig config = ModConfigManager.get();
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;

        if (config == null || player == null) {
            return;
        }

        if (!config.enabled || config.fishingMode || shouldPauseForSneaking(config, player)) {
            return;
        }

        ItemScrollerFavoriteTrader.Result result = favoriteTrader.tryTrade(client, config.autoCloseMerchantScreen);
        if (!result.merchantScreen() || !result.handled()) {
            return;
        }

        if (result.failed()) {
            debug(config, player, Component.translatable("text.autotrade-plus.debug.itemscroller_failed").withStyle(ChatFormatting.RED));
        } else if (result.traded()) {
            pendingDropTicks = 5;
            sendTradeActionbar(player, "text.autotrade-plus.actionbar.trade_complete");
            debug(config, player, Component.translatable("text.autotrade-plus.debug.favorite_trades_done").withStyle(ChatFormatting.GREEN));
        } else {
            sendTradeActionbar(player, "text.autotrade-plus.actionbar.no_favorite");
            debug(config, player, Component.translatable("text.autotrade-plus.debug.no_favorite_trades").withStyle(ChatFormatting.YELLOW));
        }
        completePendingTrade(config);
    }

    private boolean waitForPendingTrade(ModConfig config) {
        if (!pendingTrade.isActive()) {
            return false;
        }
        pendingTradeTicks++;
        if (pendingTradeTicks <= Math.max(20, config.villagerCooldownTicks + 20)) {
            return true;
        }
        completePendingTrade(config);
        perTargetCooldown = 5;
        return true;
    }

    private void completePendingTrade(ModConfig config) {
        lastCompletedIndex = currentIndex;
        advanceCurrentIndex(config);
        pendingTrade = TradeContext.empty();
        pendingTradeTicks = 0;
    }

    private void toggleEnabled(ModConfig config, Player player) {
        config.enabled = !config.enabled;
        ModConfigManager.save();
        handleEnabledStateChanged(config);
        player.sendSystemMessage(
                Component.translatable(
                        "text.autotrade-plus.message.toggled",
                        Component.translatable(config.fishingMode ? "text.autotrade-plus.mode.fishing" : "text.autotrade-plus.mode.trading"),
                        Component.translatable(config.enabled ? "text.autotrade-plus.state.enabled" : "text.autotrade-plus.state.disabled")
                )
        );
    }

    private void syncEnabledState(ModConfig config) {
        if (!enabledStateInitialized) {
            enabledStateInitialized = true;
            lastKnownEnabled = config.enabled;
            return;
        }
        if (lastKnownEnabled != config.enabled) {
            handleEnabledStateChanged(config);
        }
    }

    private void handleEnabledStateChanged(ModConfig config) {
        lastKnownEnabled = config.enabled;
        if (!config.enabled && config.resumeTradeProgress) {
            pauseRoundStateForResume();
        } else if (!config.enabled) {
            resetRoundState();
        }
    }

    private boolean shouldPauseForSneaking(ModConfig config, Player player) {
        if (!player.isShiftKeyDown() || !config.sneak) {
            return false;
        }
        if (config.debug) {
            player.sendSystemMessage(
                    Component.translatable(
                            "text.autotrade-plus.debug.sneak_paused",
                            Component.translatable(config.fishingMode ? "text.autotrade-plus.mode.fishing" : "text.autotrade-plus.mode.trading")
                    ).withStyle(ChatFormatting.RED)
            );
        }
        return true;
    }

    private void handleVillagerTrading(ModConfig config, Player player, MultiPlayerGameMode interaction) {
        Level world = player.level();
        Vec3 center = new Vec3(player.getX(), player.getY(), player.getZ());
        AABB box = new AABB(
                center.add(-config.tradeRange, -config.tradeRange, -config.tradeRange),
                center.add(config.tradeRange, config.tradeRange, config.tradeRange)
        );

        List<Villager> visibleVillagers = world.getEntitiesOfClass(
                Villager.class,
                box,
                villager -> villager.isAlive()
                        && villager.distanceTo(player) <= config.tradeRange
                        && isAllowedProfession(villager, config)
        );

        for (Villager villager : visibleVillagers) {
            UUID uuid = villager.getUUID();
            if (!villagerOrderMap.containsKey(uuid)) {
                villagerOrderMap.put(uuid, villagerOrderMap.size());
                orderedVillagers.add(uuid);
                debug(config, player, Component.translatable("text.autotrade-plus.debug.villager_queued", uuid.toString().substring(0, 8)));
            }
        }

        if (orderedVillagers.isEmpty()) {
            return;
        }

        int checked = 0;
        while (checked < orderedVillagers.size()) {
            startRoundIfNeeded(config, player);
            UUID targetUuid = orderedVillagers.get(currentIndex);
            Optional<Villager> target = visibleVillagers.stream()
                    .filter(villager -> villager.getUUID().equals(targetUuid))
                    .findFirst();

            if (target.isPresent()) {
                tradeWith(config, player, interaction, target.get(), targetUuid);
                return;
            }

            currentIndex = (currentIndex + 1) % orderedVillagers.size();
            checked++;
        }

        perTargetCooldown = 5;
    }

    private void startRoundIfNeeded(ModConfig config, Player player) {
        if (currentIndex == 0 && roundStartTick == -1) {
            roundStartTick = totalGameTick;
            debug(config, player, Component.translatable("text.autotrade-plus.debug.round_started").withStyle(ChatFormatting.GREEN));
        }
    }

    private void tradeWith(
            ModConfig config,
            Player player,
            MultiPlayerGameMode interaction,
            Villager target,
            UUID targetUuid
    ) {
        int targetPosition = currentIndex + 1;
        int targetTotal = orderedVillagers.size();
        pendingTrade = TradeContext.from(target, targetPosition, targetTotal);
        pendingTradeTicks = 0;
        favoriteTrader.rememberInteractionTarget(targetUuid);
        interaction.interact(player, target, new EntityHitResult(target), InteractionHand.MAIN_HAND);
        debug(config, player, Component.translatable(
                "text.autotrade-plus.debug.trade_villager",
                targetPosition,
                targetTotal,
                targetUuid.toString().substring(0, 8),
                String.format("%.2f", target.distanceTo(player))
        ).withStyle(ChatFormatting.GREEN));

        perTargetCooldown = config.villagerCooldownTicks;
    }

    private void advanceCurrentIndex(ModConfig config) {
        currentIndex++;
        if (currentIndex >= orderedVillagers.size()) {
            currentIndex = 0;
            roundCooldown = config.tradeCooldownTicks;
            roundStartTick = -1;
        }
    }

    private void sendTradeActionbar(Player player, String translationKey) {
        Minecraft.getInstance().gui.hud.setOverlayMessage(
                Component.translatable(
                        translationKey,
                        pendingTrade.professionText(),
                        pendingTrade.position(),
                        pendingTrade.total()
                ),
                false
        );
    }

    private void executeDropAfterTrade(ModConfig config) {
        if (config.dropMode != ModConfig.DropMode.AFTER_TRADE) {
            return;
        }
        for (Item item : getDropItems(config.dropItems)) {
            InventoryDropper.keepOnlyOne(item);
        }
    }

    private void processPendingDrop(ModConfig config) {
        if (pendingDropTicks <= 0) {
            return;
        }
        pendingDropTicks--;
        if (pendingDropTicks == 0) {
            executeDropAfterTrade(config);
        }
    }

    private List<Item> getDropItems(String configuredItems) {
        String currentItems = configuredItems == null ? "" : configuredItems;
        if (currentItems.isBlank()) {
            cachedDropItems = List.of();
            cachedDropItemsString = "";
            return cachedDropItems;
        }

        if (currentItems.equals(cachedDropItemsString)) {
            return cachedDropItems;
        }

        List<Item> items = new ArrayList<>();
        for (String idText : currentItems.split("[,，]")) {
            Identifier id = Identifier.tryParse(idText.trim());
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                Item item = BuiltInRegistries.ITEM.getValue(id);
                if (item != Items.AIR) {
                    items.add(item);
                }
            }
        }

        cachedDropItems = items;
        cachedDropItemsString = currentItems;
        return cachedDropItems;
    }

    private void handleFishingMode(ModConfig config, Player player, MultiPlayerGameMode interaction) {
        ItemStack mainHandStack = player.getMainHandItem();
        if (mainHandStack.getItem() != Items.WATER_BUCKET) {
            perTargetCooldown = 20;
            return;
        }

        Level world = player.level();
        Vec3 center = new Vec3(player.getX(), player.getY(), player.getZ());
        AABB box = new AABB(
                center.add(-config.tradeRange, -config.tradeRange, -config.tradeRange),
                center.add(config.tradeRange, config.tradeRange, config.tradeRange)
        );

        AbstractFish nearest = world.getEntitiesOfClass(
                        AbstractFish.class,
                        box,
                        fish -> fish.isAlive() && fish.distanceTo(player) <= config.tradeRange
                )
                .stream()
                .min(Comparator.comparingDouble(fish -> fish.distanceTo(player)))
                .orElse(null);

        if (nearest == null) {
            perTargetCooldown = 10;
            return;
        }

        interaction.interact(player, nearest, new EntityHitResult(nearest), InteractionHand.MAIN_HAND);
        perTargetCooldown = config.villagerCooldownTicks;
    }

    private boolean isAllowedProfession(Villager villager, ModConfig config) {
        if (VillagerProfessionCatalog.allowsAll(config.villagerProfession)) {
            return true;
        }

        Set<String> allowedIds = new HashSet<>(VillagerProfessionCatalog.selectedProfessionIds(config.villagerProfession));
        if (allowedIds.isEmpty()) {
            return true;
        }

        Holder<VillagerProfession> professionEntry = villager.getVillagerData().profession();
        Identifier id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(professionEntry.value());
        return id != null && allowedIds.contains(id.toString());
    }

    private void resetRoundState() {
        roundCooldown = 0;
        perTargetCooldown = 0;
        currentIndex = 0;
        lastCompletedIndex = -1;
        roundStartTick = -1;
        villagerOrderMap.clear();
        orderedVillagers.clear();
        cachedDropItems = List.of();
        cachedDropItemsString = "";
        pendingDropTicks = 0;
        pendingTradeTicks = 0;
        pendingTrade = TradeContext.empty();
    }

    private void pauseRoundStateForResume() {
        roundCooldown = 0;
        perTargetCooldown = 0;
        pendingTradeTicks = 0;
        pendingTrade = TradeContext.empty();

        if (orderedVillagers.isEmpty()) {
            currentIndex = 0;
            lastCompletedIndex = -1;
            roundStartTick = -1;
            return;
        }

        int resumeIndex = lastCompletedIndex >= 0 ? lastCompletedIndex : currentIndex;
        currentIndex = Math.max(0, Math.min(resumeIndex, orderedVillagers.size() - 1));
        if (currentIndex == 0) {
            roundStartTick = -1;
        }
    }

    private void debug(ModConfig config, Player player, Component message) {
        if (config.debug) {
            player.sendSystemMessage(message);
        }
    }

    private boolean consumeKeyPress(KeyMapping keyBinding) {
        boolean pressed = false;
        while (keyBinding != null && keyBinding.consumeClick()) {
            pressed = true;
        }
        return pressed;
    }

    private void drainKeyPresses(KeyMapping keyBinding) {
        while (keyBinding != null && keyBinding.consumeClick()) {
        }
    }

    private record TradeContext(UUID targetUuid, String professionId, int position, int total) {
        private static TradeContext empty() {
            return new TradeContext(null, "", 0, 0);
        }

        private static TradeContext from(Villager villager, int position, int total) {
            Holder<VillagerProfession> professionEntry = villager.getVillagerData().profession();
            Identifier id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(professionEntry.value());
            return new TradeContext(villager.getUUID(), id == null ? "" : id.toString(), position, total);
        }

        private boolean isActive() {
            return targetUuid != null;
        }

        private Component professionText() {
            return professionId.isBlank()
                    ? Component.translatable("text.autotrade-plus.profession.unknown")
                    : VillagerProfessionCatalog.displayTextForValue(professionId);
        }
    }
}
