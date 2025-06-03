package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.menu.page.ItemPage;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Used when client item modified in ItemDisplay with {@link net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu}
 * @param isAdding true for add item and false for take item.
 */
public record ItemDisplayItemModPayload(ItemStack stack, boolean isAdding) implements ToServerPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf,ItemDisplayItemModPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,ItemDisplayItemModPayload::stack,
            ByteBufCodecs.BOOL,ItemDisplayItemModPayload::isAdding,
            ItemDisplayItemModPayload::new
    );


    @Override
    public String id() {
        return "item_modify";
    }

    @Override
    public void handle(ToServerPacketContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        if(player.containerMenu.getCarried().isEmpty() && player.hasInfiniteMaterials()){
            var optional = ServerLevelEndInv.checkAndGetManagerForPlayer(player);
            if(optional.isEmpty()) return;
            PageMetaDataManager manager = optional.get();
            if(manager.getDisplayingPage() instanceof ItemPage itemPage){
                if(isAdding()){
                    itemPage.addItem(stack());
                }else {
                    ItemStack taken = itemPage.takeItem(stack());
                    if(taken.isEmpty()) return;
                    player.containerMenu.setCarried(taken);
                }
            }
        }
    }
}
