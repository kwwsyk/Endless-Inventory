package com.kwwsyk.endinv.network.payloads.toClient;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.ModInitializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * holds various attributes that influent ItemDisplay,
 *  compared to {@link EndInvConfig}
 */
public record EndInvMetadata(int itemSize, int maxStackSize, boolean infinityMode, EndInvConfig config) implements CustomPacketPayload {
    public static final Type<EndInvMetadata> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"endinv_meta"));
    public static final StreamCodec<FriendlyByteBuf,EndInvMetadata> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,EndInvMetadata::itemSize,
            ByteBufCodecs.INT,EndInvMetadata::maxStackSize,
            ByteBufCodecs.BOOL,EndInvMetadata::infinityMode,
            EndInvConfig.STREAM_CODEC,EndInvMetadata::config,
            EndInvMetadata::new
    );

    public static EndInvMetadata getWith(EndlessInventory endInv) {
        return new EndInvMetadata(
                endInv.getItemSize(),
                endInv.getMaxItemStackSize(),
                endInv.isInfinityMode(),
                EndInvConfig.getWith(endInv)
        );
    }

    public @NotNull Type<EndInvMetadata> type(){
        return TYPE;
    }
}
