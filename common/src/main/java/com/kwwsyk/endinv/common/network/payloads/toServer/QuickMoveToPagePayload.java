package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;

public record QuickMoveToPagePayload(int slotId) implements ModPacketPayload {

    public static void encode(QuickMoveToPagePayload payload, FriendlyByteBuf o){
        o.writeInt(payload.slotId);
    }

    public static QuickMoveToPagePayload decode(FriendlyByteBuf o){
        return new QuickMoveToPagePayload(o.readInt());
    }

    @Override
    public String id() {
        return "quick_move_page";
    }

    public void handle(ModPacketContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        var optional = ServerLevelEndInv.checkAndGetManagerForPlayer(player);
        optional.ifPresent(manager -> {
            Slot slot = manager.getMenu().getSlot(slotId());
            manager.slotQuickMoved(slot);
        });
    }
}
