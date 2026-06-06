package com.momo.mixin.client;

import com.momo.AutoTradePlusClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onSetTradeOffers", at = @At("RETURN"))
    private void autoTrade$onSetTradeOffers(SetTradeOffersS2CPacket packet, CallbackInfo info) {
        AutoTradePlusClient.controller().onMerchantOffersUpdated();
    }
}
