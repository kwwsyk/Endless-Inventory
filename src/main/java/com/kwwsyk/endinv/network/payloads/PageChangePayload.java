package com.kwwsyk.endinv.network.payloads;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.options.ItemClassify;
import com.kwwsyk.endinv.options.SortType;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record PageChangePayload(int startIndex, int length, SortType sortType, Holder<ItemClassify> classify, String search) implements CustomPacketPayload {
    
    public static final Type<PageChangePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"page_change"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PageChangePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PageChangePayload::startIndex,
            ByteBufCodecs.INT, PageChangePayload::length,
            SortType.STREAM_CODEC, PageChangePayload::sortType,
            ByteBufCodecs.holderRegistry(ModInitializer.CLASSIFY_REGISTRY_KEY), PageChangePayload::classify,
            ByteBufCodecs.STRING_UTF8, PageChangePayload::search,
            PageChangePayload::new
    );



    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
