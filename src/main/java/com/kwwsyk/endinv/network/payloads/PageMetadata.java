package com.kwwsyk.endinv.network.payloads;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.util.SortType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record PageMetadata(int startIndex, int length, PageData pageData) implements CustomPacketPayload {
    
    public static final Type<PageMetadata> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"page_metadata"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PageMetadata> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PageMetadata::startIndex,
            ByteBufCodecs.INT, PageMetadata::length,
            PageData.STREAM_CODEC,PageMetadata::pageData,
            PageMetadata::new
    );


    public SortType sortType() {
        return pageData.sortType();
    }

    public String search() {
        return pageData.search();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
