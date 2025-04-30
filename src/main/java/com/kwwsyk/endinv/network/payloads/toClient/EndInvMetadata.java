package com.kwwsyk.endinv.network.payloads.toClient;

import com.kwwsyk.endinv.ModInitializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record EndInvMetadata(int itemSize, int maxStackSize, boolean infinityMode) implements CustomPacketPayload {
    public static final Type<EndInvMetadata> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"endinv_meta"));
    public static final StreamCodec<FriendlyByteBuf,EndInvMetadata> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,EndInvMetadata::itemSize,
            ByteBufCodecs.INT,EndInvMetadata::maxStackSize,
            ByteBufCodecs.BOOL,EndInvMetadata::infinityMode,
            EndInvMetadata::new
    );

    public @NotNull Type<EndInvMetadata> type(){
        return TYPE;
    }
}
