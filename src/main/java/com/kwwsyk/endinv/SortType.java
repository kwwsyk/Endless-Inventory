package com.kwwsyk.endinv;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public enum SortType {
    DEFAULT,
    COUNT,
    ID,
    LAST_MODIFIED;

    public static final StreamCodec<ByteBuf,SortType> STREAM_CODEC = new StreamCodec<ByteBuf, SortType>() {
        @Override
        public SortType decode(ByteBuf byteBuf) {
            byte b = byteBuf.readByte();
            SortType[] types = SortType.values();
            if(b<0||b>=types.length) b=0;
            return types[b];
        }

        @Override
        public void encode(ByteBuf o, SortType sortType) {
            o.writeByte(sortType.ordinal());
        }
    };
}
