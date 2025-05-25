package com.kwwsyk.endinv.network.payloads.toServer.page.op;

import com.kwwsyk.endinv.ModInitializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record PageStatePayload(boolean holdOn) implements CustomPacketPayload{

    public static final Type<PageStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"page_state"));

    public static final StreamCodec<FriendlyByteBuf,PageStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,PageStatePayload::holdOn,
            PageStatePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
