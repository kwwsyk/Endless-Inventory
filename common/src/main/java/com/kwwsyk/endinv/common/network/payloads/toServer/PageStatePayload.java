package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public record PageStatePayload(boolean holdOn) implements ToServerPayload {


    public static final StreamCodec<FriendlyByteBuf,PageStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,PageStatePayload::holdOn,
            PageStatePayload::new
    );

    @Override
    public String id() {
        return "page_state";
    }

    public void handle(ToServerPacketContext context){
        ServerPlayer player = (ServerPlayer) context.player();
        var optional = ServerLevelEndInv.checkAndGetManagerForPlayer(player);
        if(optional.isPresent()){
            PageMetaDataManager manager = optional.get();
            if(holdOn){
                manager.getDisplayingPage().setHoldOn();
            }else {
                manager.getDisplayingPage().release();
            }
        }
    }
}
