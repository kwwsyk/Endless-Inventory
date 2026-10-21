package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.AbstractModInitializer;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.page.StarredItemPage;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.util.ItemStackLike;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record SetStarredPagePayload(List<ItemStackLike> stacks) implements ModPacketPayload {

    public static void encode(SetStarredPagePayload payload, FriendlyByteBuf o){
        ByteBufCodecs.writeCount(o, payload.stacks.size(), Integer.MAX_VALUE);
        payload.stacks.forEach(like -> ItemStackLike.STREAM_CODEC.encode((RegistryFriendlyByteBuf) o, like));
    }

    public static SetStarredPagePayload decode(FriendlyByteBuf o){
        int size = ByteBufCodecs.readCount(o, Integer.MAX_VALUE);
        List<ItemStackLike> stacks = new java.util.ArrayList<>(Math.min(size, 65536));
        for(int i = 0; i < size; i++){
            stacks.add(ItemStackLike.STREAM_CODEC.decode((RegistryFriendlyByteBuf) o));
        }
        return new SetStarredPagePayload(stacks);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SetStarredPagePayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> encode(value, buf), SetStarredPagePayload::decode);

    public static final CustomPacketPayload.Type<SetStarredPagePayload> TYPE =
            new CustomPacketPayload.Type<>(AbstractModInitializer.withModLocation("starred_item"));

    @Override
    public String id() {
        return "starred_item";
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(ModPacketContext context) {
        CachedSrcInv.INSTANCE.affinities.syncStarredItems(stacks.stream().map(ItemStackLike::toKey).toList());
        ModPacketPayload.getClientPageMeta().ifPresent(mng->{
            if(mng.getDisplayingPage() instanceof StarredItemPage page){
                page.initializeAsMap(stacks);
            }
        });
    }
}
