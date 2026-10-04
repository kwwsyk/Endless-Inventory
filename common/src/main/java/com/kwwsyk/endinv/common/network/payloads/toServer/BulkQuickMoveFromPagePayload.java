package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.AbstractModInitializer;
import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageQuickMoveHandler;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.mojang.logging.LogUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Server-side bulk quick-move from the Endless Inventory to the open container.
 * Moves as many stacks of the same item as possible until either the container
 * can no longer accept or the source has none left.
 */
public record BulkQuickMoveFromPagePayload(ItemKey prototype) implements ModPacketPayload {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final StreamCodec<RegistryFriendlyByteBuf, BulkQuickMoveFromPagePayload> STREAM_CODEC =
            StreamCodec.of((buf, value) -> encode(value, buf), BulkQuickMoveFromPagePayload::decode);

    public static final CustomPacketPayload.Type<BulkQuickMoveFromPagePayload> TYPE =
            new CustomPacketPayload.Type<>(AbstractModInitializer.withModLocation("bulk_quick_move_from_page"));

    public static BulkQuickMoveFromPagePayload decode(RegistryFriendlyByteBuf buf) {
        return new BulkQuickMoveFromPagePayload(ItemKey.STREAM_CODEC.decode(buf));
    }

    public static void encode(BulkQuickMoveFromPagePayload payload, RegistryFriendlyByteBuf buf) {
        ItemKey.STREAM_CODEC.encode(buf, payload.prototype);
    }

    @Override
    public void handle(ModPacketContext context) {
        Player player = context.player();
        Optional<EndlessInventory> opt = ServerLevelEndInv.getEndInvForPlayer(player);
        if (opt.isEmpty()) {
            LOGGER.warn("BulkQuickMoveFromPage: no EndInv for player={}", player.getName().getString());
            return;
        }
        EndlessInventory endInv = opt.get();
        AbstractContainerMenu menu = player.containerMenu;
        PageQuickMoveHandler mover = new PageQuickMoveHandler(menu);

        // Safety cap to avoid infinite loop on odd overrides
        int iterations = 0;
        int movedTotal = 0;
        while (iterations++ < 32768) {
            ItemStack taken = endInv.takeItem(prototype.toStack(prototype.item().getDefaultMaxStackSize()));
            if (taken.isEmpty()) break;

            ItemStack remain = mover.quickMoveFromPage(taken);
            int moved = taken.getCount() - remain.getCount();
            movedTotal += Math.max(moved, 0);

            if (!remain.isEmpty()) {
                // Could not insert fully; put the remainder back and stop.
                endInv.addItem(remain);
                break;
            }
        }
        if (movedTotal > 0) endInv.setChanged();
        LOGGER.debug("BulkQuickMoveFromPage: moved {} of {} for player {}", movedTotal, prototype.item(), player.getName().getString());
    }

    @Override
    public String id() {
        return "bulk_quick_move_from_page";
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

