package com.kwwsyk.endinv.network.payloads;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.SortType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record EndInvRequestContentPayload(int startIndex, int length, SortType sortType) implements CustomPacketPayload {
    
    public static final Type<EndInvRequestContentPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"endinv_request"));

    public static final StreamCodec<ByteBuf, EndInvRequestContentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, EndInvRequestContentPayload::startIndex,
            ByteBufCodecs.INT, EndInvRequestContentPayload::length,
            SortType.STREAM_CODEC, EndInvRequestContentPayload::sortType,
            EndInvRequestContentPayload::new
    );



    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
