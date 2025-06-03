package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.ServerLevelEndInv;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;

public record QuickMoveToPagePayload(int slotId) implements ToServerPayload {

    public static final StreamCodec<FriendlyByteBuf,QuickMoveToPagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,QuickMoveToPagePayload::slotId,
            QuickMoveToPagePayload::new
    );

    @Override
    public String id() {
        return "quick_move_page";
    }

    public void handle(ToServerPacketContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        var optional = ServerLevelEndInv.checkAndGetManagerForPlayer(player);
        optional.ifPresent(manager -> {
            Slot slot = manager.getMenu().getSlot(slotId());
            manager.slotQuickMoved(slot);
        });
    }
}
