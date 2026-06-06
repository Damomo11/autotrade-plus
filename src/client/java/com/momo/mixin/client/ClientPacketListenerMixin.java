package com.momo.mixin.client;

import com.momo.AutoTradePlusClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleMerchantOffers", at = @At("RETURN"))
    private void autoTrade$handleMerchantOffers(ClientboundMerchantOffersPacket packet, CallbackInfo info) {
        AutoTradePlusClient.controller().onMerchantOffersUpdated();
    }
}
