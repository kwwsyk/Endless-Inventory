package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.menu.page.StarredItemPage;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.util.ItemStackLike;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record SetStarredPagePayload(List<ItemStackLike> stacks) implements ModPacketPayload {

    public static void encode(SetStarredPagePayload payload, FriendlyByteBuf o){
        o.writeCollection(payload.stacks,ItemStackLike::encode);
    }

    public static SetStarredPagePayload decode(FriendlyByteBuf o){
        return new SetStarredPagePayload(o.readList(ItemStackLike::decode));
    }

    @Override
    public String id() {
        return "starred_item";
    }

    @Override
    public void handle(ModPacketContext context) {
        ModPacketPayload.getClientPageMeta().ifPresent(mng->{
            if(mng.getDisplayingPage() instanceof StarredItemPage page){
                page.initializeAsMap(stacks);
            }
        });
    }
}
