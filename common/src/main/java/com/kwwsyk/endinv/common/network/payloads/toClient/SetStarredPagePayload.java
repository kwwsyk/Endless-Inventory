package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.menu.page.StarredItemPage;
import com.kwwsyk.endinv.common.util.ItemStackLike;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record SetStarredPagePayload(List<ItemStackLike> stacks) implements ToClientPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, SetStarredPagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ItemStackLike.STREAM_CODEC),SetStarredPagePayload::stacks,
            SetStarredPagePayload::new
    );

    @Override
    public String id() {
        return "starred_item";
    }

    @Override
    public void handle(ToClientPacketContext context) {
        ToClientPayload.getClientPageMeta().ifPresent(mng->{
            if(mng.getDisplayingPage() instanceof StarredItemPage page){
                page.initializeAsMap(stacks);
            }
        });
    }
}
