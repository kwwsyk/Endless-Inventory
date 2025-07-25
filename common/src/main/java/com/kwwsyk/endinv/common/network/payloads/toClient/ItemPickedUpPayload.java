package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.client.event.AutoPickTipper;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record ItemPickedUpPayload(ItemStack stack) implements ModPacketPayload {
    
    public static void encode(ItemPickedUpPayload payload, FriendlyByteBuf o){
        o.writeItem(payload.stack);
    }
    
    public static ItemPickedUpPayload decode(FriendlyByteBuf o){
        return new ItemPickedUpPayload(o.readItem());
    }

    @Override
    public String id() {
        return "auto_picked";
    }

    public void handle(ModPacketContext iPayloadContext) {
        AutoPickTipper.addItem(stack());
    }
}
