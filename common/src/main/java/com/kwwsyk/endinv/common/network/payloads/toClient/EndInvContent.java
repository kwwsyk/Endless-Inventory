package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.menu.page.ItemPage;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemState;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;

public record EndInvContent(Map<ItemKey, ItemState> itemMap) implements ModPacketPayload {

    public static void encodeItemMap(Map<ItemKey,ItemState> map, FriendlyByteBuf o){
        o.writeMap(map,ItemKey::encode,ItemState::encode);
    }

    public static Map<ItemKey,ItemState> decodeItemMap(FriendlyByteBuf o){
        return o.readMap(Object2ObjectLinkedOpenHashMap::new,ItemKey::decode,ItemState::decode);
    }

    public static void encode(EndInvContent content,FriendlyByteBuf o){
        encodeItemMap(content.itemMap,o);
    }

    public static EndInvContent decode(FriendlyByteBuf o){
        return new EndInvContent(decodeItemMap(o));
    }

    @Override
    public String id() {
        return "endinv_content";
    }

    public void handle(ModPacketContext context){
        CachedSrcInv.INSTANCE.initializeContents(this.itemMap());

        ModPacketPayload.getClientPageMeta().ifPresent(
                mng->{
                    if(mng.getDisplayingPage() instanceof ItemPage itemPage){
                        itemPage.initializeContents(CachedSrcInv.INSTANCE);
                    }
                }
        );
    }
}
