package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.client.event.AutoPickTipper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record ItemPickedUpPayload(ItemStack stack) implements ToClientPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf,ItemPickedUpPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,ItemPickedUpPayload::stack,
            ItemPickedUpPayload::new
    );

    @Override
    public String id() {
        return "auto_picked";
    }

    public void handle(ToClientPacketContext iPayloadContext) {
        AutoPickTipper.addItem(stack());
    }
}
