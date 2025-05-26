package com.kwwsyk.endinv.common.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public enum Accessibility {

    PUBLIC,
    RESTRICTED,
    PRIVATE;

    public static final StreamCodec<ByteBuf,Accessibility> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull Accessibility decode(ByteBuf byteBuf) {
            byte b = byteBuf.readByte();
            Accessibility[] types = Accessibility.values();
            if (b < 0 || b >= types.length) b = 0;
            return types[b];
        }

        @Override
        public void encode(ByteBuf o, Accessibility accessibility) {
            o.writeByte(accessibility.ordinal());
        }
    };
}
