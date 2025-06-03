package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.ServerLevelEndInv;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record StarItemPayload(ItemStack stack,boolean isAdding) implements ToServerPayload {


    public static final StreamCodec<RegistryFriendlyByteBuf,StarItemPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,StarItemPayload::stack,
            ByteBufCodecs.BOOL,StarItemPayload::isAdding,
            StarItemPayload::new
    );

    @Override
    public String id() {
        return "star_item";
    }

    public void handle(ToServerPacketContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        ServerLevelEndInv.getEndInvForPlayer(player).ifPresent(endInv->{
            if(isAdding()) {
                endInv.affinities.addStarredItem(stack());
            }else {
                endInv.affinities.removeStarredItem(stack());
            }
        });
    }
}
