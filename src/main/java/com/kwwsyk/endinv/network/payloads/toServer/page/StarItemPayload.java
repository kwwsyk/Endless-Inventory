package com.kwwsyk.endinv.network.payloads.toServer.page;

import com.kwwsyk.endinv.ModInitializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record StarItemPayload(ItemStack stack,boolean isAdding) implements CustomPacketPayload {

    public static final Type<StarItemPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"star_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf,StarItemPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,StarItemPayload::stack,
            ByteBufCodecs.BOOL,StarItemPayload::isAdding,
            StarItemPayload::new
    );

    @Override
    public @NotNull Type<StarItemPayload> type() {
        return TYPE;
    }
}
