package com.kwwsyk.endinv.common.util;

import net.minecraft.network.FriendlyByteBuf;

public record ItemState(int count, long lastModTime) {


    public static void encode(FriendlyByteBuf o,ItemState state){
        o.writeInt(state.count);
        o.writeLong(state.lastModTime);
    }

    public static ItemState decode(FriendlyByteBuf o){
        return new ItemState(o.readInt(),o.readLong());
    }
}
