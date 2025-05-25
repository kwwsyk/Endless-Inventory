package com.kwwsyk.endinv.network.payloads.toServer.page.op;

import com.kwwsyk.endinv.ModInitializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record QuickMoveToPagePayload(int slotId) implements CustomPacketPayload {

    public static final Type<QuickMoveToPagePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"quick_move_to_page"));

    public static final StreamCodec<FriendlyByteBuf,QuickMoveToPagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,QuickMoveToPagePayload::slotId,
            QuickMoveToPagePayload::new
    );

    @Override
    public @NotNull Type<QuickMoveToPagePayload> type() {
        return TYPE;
    }
}
