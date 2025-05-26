package com.kwwsyk.endinv.common.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public enum SortType {
    DEFAULT("sorttype.endinv.default"),
    COUNT("sorttype.endinv.count"),
    SPACE_AND_NAME("sorttype.endinv.name"),
    ID("sorttype.endinv.id"),
    LAST_MODIFIED("sorttype.endinv.last_modified");

    public final String translationKey;

    SortType(String translationKey){
        this.translationKey = translationKey;
    }

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
    public static final Codec<SortType> CODEC = Codec.STRING.xmap(
            name -> {
                try {
                    return SortType.valueOf(name.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return SortType.DEFAULT;
                }
            },
            SortType::name
    );
}
