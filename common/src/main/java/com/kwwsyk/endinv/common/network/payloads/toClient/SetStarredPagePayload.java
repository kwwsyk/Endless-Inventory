package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.util.ItemStackLike;
import com.kwwsyk.endinv.neoforge.ModInitializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record SetStarredPagePayload(List<ItemStackLike> stacks) implements CustomPacketPayload {

    public static final Type<SetStarredPagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"starred_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetStarredPagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ItemStackLike.STREAM_CODEC),SetStarredPagePayload::stacks,
            SetStarredPagePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
