package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * holds various attributes that influent ItemDisplay,
 *  compared to {@link EndInvConfig}
 */
public record EndInvMetadata(int itemSize, int maxStackSize, boolean infinityMode, EndInvConfig config) implements ToClientPayload {

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

    public void handle(ToClientPacketContext context) {
        CachedSrcInv.INSTANCE.syncMetadata(this);

        //todo
    }


    @Override
    public String id() {
        return "endinv_meta";
    }
}
