package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.menu.page.ItemPage;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Map;

public record EndInvContent(Map<ItemKey, ItemState> itemMap) implements ToClientPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, EndInvContent> STREAM_CODEC = StreamCodec.composite(
            EndlessInventory.ITEM_MAP_STREAM_CODEC,EndInvContent::itemMap,
            EndInvContent::new
    );

    @Override
    public String id() {
        return "endinv_content";
    }

    public void handle(ToClientPacketContext context){
        CachedSrcInv.INSTANCE.initializeContents(this.itemMap());

        ToClientPayload.getClientPageMeta().ifPresent(
                mng->{
                    if(mng.getDisplayingPage() instanceof ItemPage itemPage){
                        itemPage.initializeContents(CachedSrcInv.INSTANCE);
                    }
                }
        );
    }
}
