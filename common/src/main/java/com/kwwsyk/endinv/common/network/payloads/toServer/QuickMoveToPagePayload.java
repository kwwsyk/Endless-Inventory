package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.AbstractModInitializer;
import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Optional;

public record QuickMoveToPagePayload(IntList slots) implements ModPacketPayload {

    private static final Logger LOGGER = LogUtils.getLogger();

    public QuickMoveToPagePayload(int slotId) {
        this(IntList.of(slotId));
    }

    public static void encode(QuickMoveToPagePayload payload, FriendlyByteBuf o){
        o.writeVarInt(payload.slots.size());
        payload.slots.forEach((int slot) -> o.writeVarInt(slot));
    }

    public static QuickMoveToPagePayload decode(FriendlyByteBuf o){
        int size = o.readVarInt();
        IntList slots = new IntArrayList(size);
        for(int i = 0; i < size; i++){
            slots.add(o.readVarInt());
        }
        return new QuickMoveToPagePayload(slots);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, QuickMoveToPagePayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> encode(value, buf), QuickMoveToPagePayload::decode);

    public static final CustomPacketPayload.Type<QuickMoveToPagePayload> TYPE =
            new CustomPacketPayload.Type<>(AbstractModInitializer.withModLocation("quick_move_page"));

    @Override
    public String id() {
        return "quick_move_page";
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ModPacketContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        Optional<EndlessInventory> oendinv = ServerLevelEndInv.getEndInvForPlayer(player);
        {
            Optional<PageMetaDataManager> optional = ServerLevelEndInv.checkAndGetManagerForPlayer(player);
            LOGGER.debug("Test: Optional AttachingManager Status: {}", optional);
        }
        if(oendinv.isEmpty()){
            LOGGER.warn("{}: Player who has not an EndInv quick-moved item to EndInv's page.", id()+" payload");
        }else {
            EndlessInventory endinv = oendinv.get();
            AbstractContainerMenu menu = player.containerMenu;
            for(int slotId : slots) {
                if (slotId >= 0 && slotId < menu.slots.size()) {
                    Slot slot = menu.getSlot(slotId);
                    ItemStack itemStack = slot.getItem();
                    ItemStack remain = endinv.addItem(itemStack);
                    slot.setByPlayer(remain);
                    slot.onTake(player, itemStack);
                } else {
                    LOGGER.warn("{}: SlotId in payload exceeded menu's slots.", id()+" payload");
                }
            }
        }
    }
}
