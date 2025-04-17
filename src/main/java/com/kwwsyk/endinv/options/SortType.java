package com.kwwsyk.endinv.options;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public enum SortType {
    DEFAULT,
    COUNT,
    ID,
    LAST_MODIFIED;

    public static final StreamCodec<ByteBuf,SortType> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull SortType decode(ByteBuf byteBuf) {
            byte b = byteBuf.readByte();
            SortType[] types = SortType.values();
            if (b < 0 || b >= types.length) b = 0;
            return types[b];
        }

        @Override
        public void encode(ByteBuf o, SortType sortType) {
            o.writeByte(sortType.ordinal());
        }
    };
}
