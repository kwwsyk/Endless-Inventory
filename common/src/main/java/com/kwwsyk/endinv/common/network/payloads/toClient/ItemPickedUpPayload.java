package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.neoforge.ModInitializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record ItemPickedUpPayload(ItemStack stack) implements CustomPacketPayload {

    public static final Type<ItemPickedUpPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"auto_picked"));

    public static final StreamCodec<RegistryFriendlyByteBuf,ItemPickedUpPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,ItemPickedUpPayload::stack,
            ItemPickedUpPayload::new
    );

    @Override
    public @NotNull Type<ItemPickedUpPayload> type() {
        return TYPE;
    }
}
