package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.network.payloads.PageData;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**In page context used on Page operations.
 * @param startIndex
 * @param length
 * @param pageData
 */
public record PageContext(int startIndex, int length, PageData pageData) implements ToServerPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, PageContext> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PageContext::startIndex,
            ByteBufCodecs.INT, PageContext::length,
            PageData.STREAM_CODEC, PageContext::pageData,
            PageContext::new
    );


    public SortType sortType() {
        return pageData.sortType();
    }

    public String search() {
        return pageData.search();
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PageContext(int index, int length1, PageData data))) return false;
        return length == length1 && startIndex == index && Objects.equals(pageData,data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startIndex, length, pageData);
    }

    @Override
    public String id() {
        return "page_context";
    }

    public void handle(ToServerPacketContext iPayloadContext){
        ServerPlayer serverPlayer = (ServerPlayer) iPayloadContext.player();
        var optional = ServerLevelEndInv.checkAndGetManagerForPlayer(serverPlayer);
        optional.ifPresent(manager -> ToServerPayload.syncPageContext(manager, this, true));
    }
}
