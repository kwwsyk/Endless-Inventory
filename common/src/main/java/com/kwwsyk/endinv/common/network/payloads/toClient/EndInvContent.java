package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.AbstractModInitializer;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.page.ItemDisplay;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemState;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Map;

public record EndInvContent(Map<ItemKey, ItemState> itemMap) implements ModPacketPayload {

    public static void encode(RegistryFriendlyByteBuf o, EndInvContent content){
        ByteBufCodecs.writeCount(o, content.itemMap.size(), Integer.MAX_VALUE);
        content.itemMap.forEach((key, value) -> {
            ItemKey.STREAM_CODEC.encode(o, key);
            ItemState.encode(o, value);
        });
    }

    public static EndInvContent decode(RegistryFriendlyByteBuf o){
        int size = ByteBufCodecs.readCount(o, Integer.MAX_VALUE);
        Map<ItemKey, ItemState> itemMap = new Object2ObjectLinkedOpenHashMap<>(Math.min(size, 65536));
        for(int i = 0; i < size; i++){
            itemMap.put(ItemKey.STREAM_CODEC.decode(o), ItemState.decode(o));
        }
        return new EndInvContent(itemMap);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, EndInvContent> STREAM_CODEC = StreamCodec.of(EndInvContent::encode, EndInvContent::decode);

    public static final CustomPacketPayload.Type<EndInvContent> TYPE =
            new CustomPacketPayload.Type<>(AbstractModInitializer.withModLocation("endinv_content"));

    @Override
    public String id() {
        return "endinv_content";
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ModPacketContext context){
        CachedSrcInv.INSTANCE.initializeContents(this.itemMap());

        ModPacketPayload.getClientPageMeta().ifPresent(
                mng->{
                    if(mng.getDisplayingPage() instanceof ItemDisplay itemPage){
                        itemPage.readCachedItems();//9.25 update: packet invoke page item refresh, item refresh should not send packet (ItemPageContext)
                    }
                }
        );
    }
}
