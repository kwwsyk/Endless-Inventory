package com.kwwsyk.endinv.common.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ItemState(int count, long lastModTime) {

    public static final StreamCodec<FriendlyByteBuf,ItemState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,ItemState::count,
            ByteBufCodecs.VAR_LONG,ItemState::lastModTime,
            ItemState::new
    );
}
