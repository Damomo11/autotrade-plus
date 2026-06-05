package com.momo.client;

import com.momo.AutoTradePlusClient;
import com.momo.compat.ItemScrollerFavoriteTrader;
import com.momo.config.ModConfig;
import com.momo.config.ModConfigManager;
import com.momo.config.VillagerProfessionCatalog;
import com.momo.utils.InventoryDropper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AutoTradePlusController {
    private int roundCooldown;
    private int perTargetCooldown;
    private int currentIndex;
    private int roundStartTick = -1;
    private int totalGameTick;
    private int pendingDropTicks;
    private int pendingTradeTicks;
    private TradeContext pendingTrade = TradeContext.empty();

    private final Map<UUID, Integer> villagerOrderMap = new HashMap<>();
    private final List<UUID> orderedVillagers = new ArrayList<>();
    private final ItemScrollerFavoriteTrader favoriteTrader = new ItemScrollerFavoriteTrader();

    private List<Item> cachedDropItems = List.of();
    private String cachedDropItemsString = "";

    public void tick() {
        totalGameTick++;

        ModConfig config = ModConfigManager.get();
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        ClientPlayerInteractionManager interaction = client.interactionManager;

        if (config == null || player == null || interaction == null) {
            return;
        }

        processPendingDrop(config);

        if (client.currentScreen != null) {
            drainKeyPresses(AutoTradePlusClient.toggleKey);
            drainKeyPresses(AutoTradePlusClient.openConfigKey);
            handleMerchantScreen(config, client, player);
            return;
        }

        if (waitForPendingTrade(config)) {
            return;
        }

        if (consumeKeyPress(AutoTradePlusClient.openConfigKey)) {
            client.setScreen(ModConfigManager.createConfigScreen(null));
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

    private void handleMerchantScreen(ModConfig config, MinecraftClient client, PlayerEntity player) {
        if (!config.enabled || config.fishingMode || shouldPauseForSneaking(config, player)) {
            return;
        }

        ItemScrollerFavoriteTrader.Result result = favoriteTrader.tryTradeAndClose(client);
        if (!result.merchantScreen() || !result.handled()) {
            return;
        }

        if (result.failed()) {
            debug(config, player, Text.translatable("text.autotrade-plus.debug.itemscroller_failed").formatted(Formatting.RED));
        } else if (result.traded()) {
            pendingDropTicks = 5;
            sendTradeActionbar(player, "text.autotrade-plus.actionbar.trade_complete");
            debug(config, player, Text.translatable("text.autotrade-plus.debug.favorite_trades_done").formatted(Formatting.GREEN));
        } else {
            sendTradeActionbar(player, "text.autotrade-plus.actionbar.no_favorite");
            debug(config, player, Text.translatable("text.autotrade-plus.debug.no_favorite_trades").formatted(Formatting.YELLOW));
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
        advanceCurrentIndex(config);
        pendingTrade = TradeContext.empty();
        pendingTradeTicks = 0;
    }

    private void toggleEnabled(ModConfig config, PlayerEntity player) {
        config.enabled = !config.enabled;
        ModConfigManager.save();
        player.sendMessage(
                Text.translatable(
                        "text.autotrade-plus.message.toggled",
                        Text.translatable(config.fishingMode ? "text.autotrade-plus.mode.fishing" : "text.autotrade-plus.mode.trading"),
                        Text.translatable(config.enabled ? "text.autotrade-plus.state.enabled" : "text.autotrade-plus.state.disabled")
                ),
                false
        );

        if (!config.enabled) {
            resetRoundState();
        }
    }

    private boolean shouldPauseForSneaking(ModConfig config, PlayerEntity player) {
        if (!player.isSneaking() || !config.sneak) {
            return false;
        }
        if (config.debug) {
            player.sendMessage(
                    Text.translatable(
                            "text.autotrade-plus.debug.sneak_paused",
                            Text.translatable(config.fishingMode ? "text.autotrade-plus.mode.fishing" : "text.autotrade-plus.mode.trading")
                    ).formatted(Formatting.RED),
                    false
            );
        }
        return true;
    }

    private void handleVillagerTrading(ModConfig config, PlayerEntity player, ClientPlayerInteractionManager interaction) {
        World world = player.getEntityWorld();
        Vec3d center = new Vec3d(player.getX(), player.getY(), player.getZ());
        Box box = new Box(
                center.add(-config.tradeRange, -config.tradeRange, -config.tradeRange),
                center.add(config.tradeRange, config.tradeRange, config.tradeRange)
        );

        List<VillagerEntity> visibleVillagers = world.getEntitiesByClass(
                VillagerEntity.class,
                box,
                villager -> villager.isAlive()
                        && villager.distanceTo(player) <= config.tradeRange
                        && isAllowedProfession(villager, config)
        );

        for (VillagerEntity villager : visibleVillagers) {
            UUID uuid = villager.getUuid();
            if (!villagerOrderMap.containsKey(uuid)) {
                villagerOrderMap.put(uuid, villagerOrderMap.size());
                orderedVillagers.add(uuid);
                debug(config, player, Text.translatable("text.autotrade-plus.debug.villager_queued", uuid.toString().substring(0, 8)));
            }
        }

        if (orderedVillagers.isEmpty()) {
            return;
        }

        int checked = 0;
        while (checked < orderedVillagers.size()) {
            startRoundIfNeeded(config, player);
            UUID targetUuid = orderedVillagers.get(currentIndex);
            Optional<VillagerEntity> target = visibleVillagers.stream()
                    .filter(villager -> villager.getUuid().equals(targetUuid))
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

    private void startRoundIfNeeded(ModConfig config, PlayerEntity player) {
        if (currentIndex == 0 && roundStartTick == -1) {
            roundStartTick = totalGameTick;
            debug(config, player, Text.translatable("text.autotrade-plus.debug.round_started").formatted(Formatting.GREEN));
        }
    }

    private void tradeWith(
            ModConfig config,
            PlayerEntity player,
            ClientPlayerInteractionManager interaction,
            VillagerEntity target,
            UUID targetUuid
    ) {
        int targetPosition = currentIndex + 1;
        int targetTotal = orderedVillagers.size();
        pendingTrade = TradeContext.from(target, targetPosition, targetTotal);
        pendingTradeTicks = 0;
        interaction.interactEntity(player, target, Hand.MAIN_HAND);
        debug(config, player, Text.translatable(
                "text.autotrade-plus.debug.trade_villager",
                targetPosition,
                targetTotal,
                targetUuid.toString().substring(0, 8),
                String.format("%.2f", target.distanceTo(player))
        ).formatted(Formatting.GREEN));

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

    private void sendTradeActionbar(PlayerEntity player, String translationKey) {
        player.sendMessage(
                Text.translatable(
                        translationKey,
                        pendingTrade.professionText(),
                        pendingTrade.position(),
                        pendingTrade.total()
                ),
                true
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
            if (id != null && Registries.ITEM.containsId(id)) {
                Item item = Registries.ITEM.get(id);
                if (item != Items.AIR) {
                    items.add(item);
                }
            }
        }

        cachedDropItems = items;
        cachedDropItemsString = currentItems;
        return cachedDropItems;
    }

    private void handleFishingMode(ModConfig config, PlayerEntity player, ClientPlayerInteractionManager interaction) {
        ItemStack mainHandStack = player.getMainHandStack();
        if (mainHandStack.getItem() != Items.WATER_BUCKET) {
            perTargetCooldown = 20;
            return;
        }

        World world = player.getEntityWorld();
        Vec3d center = new Vec3d(player.getX(), player.getY(), player.getZ());
        Box box = new Box(
                center.add(-config.tradeRange, -config.tradeRange, -config.tradeRange),
                center.add(config.tradeRange, config.tradeRange, config.tradeRange)
        );

        FishEntity nearest = world.getEntitiesByClass(
                        FishEntity.class,
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

        interaction.interactEntity(player, nearest, Hand.MAIN_HAND);
        perTargetCooldown = config.villagerCooldownTicks;
    }

    private boolean isAllowedProfession(VillagerEntity villager, ModConfig config) {
        if (VillagerProfessionCatalog.allowsAll(config.villagerProfession)) {
            return true;
        }

        Set<String> allowedIds = new HashSet<>(VillagerProfessionCatalog.selectedProfessionIds(config.villagerProfession));
        if (allowedIds.isEmpty()) {
            return true;
        }

        RegistryEntry<VillagerProfession> professionEntry = villager.getVillagerData().profession();
        Identifier id = Registries.VILLAGER_PROFESSION.getId(professionEntry.value());
        return id != null && allowedIds.contains(id.toString());
    }

    private void resetRoundState() {
        roundCooldown = 0;
        perTargetCooldown = 0;
        currentIndex = 0;
        roundStartTick = -1;
        villagerOrderMap.clear();
        orderedVillagers.clear();
        cachedDropItems = List.of();
        cachedDropItemsString = "";
        pendingDropTicks = 0;
        pendingTradeTicks = 0;
        pendingTrade = TradeContext.empty();
    }

    private void debug(ModConfig config, PlayerEntity player, Text message) {
        if (config.debug) {
            player.sendMessage(message, false);
        }
    }

    private boolean consumeKeyPress(KeyBinding keyBinding) {
        boolean pressed = false;
        while (keyBinding != null && keyBinding.wasPressed()) {
            pressed = true;
        }
        return pressed;
    }

    private void drainKeyPresses(KeyBinding keyBinding) {
        while (keyBinding != null && keyBinding.wasPressed()) {
        }
    }

    private record TradeContext(UUID targetUuid, String professionId, int position, int total) {
        private static TradeContext empty() {
            return new TradeContext(null, "", 0, 0);
        }

        private static TradeContext from(VillagerEntity villager, int position, int total) {
            RegistryEntry<VillagerProfession> professionEntry = villager.getVillagerData().profession();
            Identifier id = Registries.VILLAGER_PROFESSION.getId(professionEntry.value());
            return new TradeContext(villager.getUuid(), id == null ? "" : id.toString(), position, total);
        }

        private boolean isActive() {
            return targetUuid != null;
        }

        private Text professionText() {
            return professionId.isBlank()
                    ? Text.translatable("text.autotrade-plus.profession.unknown")
                    : VillagerProfessionCatalog.displayTextForValue(professionId);
        }
    }
}
