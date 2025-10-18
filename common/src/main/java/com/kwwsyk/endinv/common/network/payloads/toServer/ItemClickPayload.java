package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.client.gui.page.ItemPage;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageQuickMoveHandler;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**Sent when
 * slot/item clicked in {@link ItemPage}.
 * @param stack if count of stack is too big, error will occur as the count is parsed to 0;
 * @param button
 * @param clickType
 */
public record ItemClickPayload(ItemStack stack, int button, ClickType clickType) implements ModPacketPayload {

    private static final Logger LOGGER = LogUtils.getLogger();



    public static ItemClickPayload decode(FriendlyByteBuf buf) {
        return new ItemClickPayload(
                buf.readItem(),
                buf.readInt(),
                buf.readEnum(ClickType.class)
        );
    }


    public static void encode(ItemClickPayload itemClickPayload,FriendlyByteBuf o) {
        o.writeItem(itemClickPayload.stack);
        o.writeInt(itemClickPayload.button);
        o.writeEnum(itemClickPayload.clickType);
    }

    @Override
    public void handle(ModPacketContext context) {
        Player player = context.player();
        AbstractContainerMenu menu = player.containerMenu;
        ItemStack carried = menu.getCarried();
        LOGGER.debug("EI:ItemClickPayload.handle: player={} clickType={} button={} carriedEmpty={} stack={}", player.getName().getString(), clickType, button, carried.isEmpty(), stack);
        var opt = ServerLevelEndInv.getEndInvForPlayer(player);
        if(opt.isEmpty()) {
            LOGGER.warn("ItemClickPayload.handle: no EndInv for player={}", player.getName().getString());
            return;
        }
        EndlessInventory endInv = opt.get();

        switch (clickType){
            case PICKUP -> {
                if(!carried.isEmpty()){
                    ItemStack remain = endInv.addItem(carried);
                    menu.setCarried(remain);
                    endInv.setChanged();
                } else {
                    int count = Math.min(stack.getCount(),stack.getMaxStackSize());
                    int takenCount = button==0 ? count : (count + 1) / 2;
                    ItemStack taken = endInv.takeItem(stack,takenCount);
                    LOGGER.debug("ItemClickPayload.PICKUP: taken={} from stack={}", taken, stack);
                    if(player.isCreative() && (menu instanceof InventoryMenu)) {
                        LOGGER.info("Ignored taken item on server thread in Creative mode to prevent duplication.");
                    }else menu.setCarried(taken);
                    if(!stack.isEmpty()) endInv.setChanged();
                }
            }
            case SWAP -> {
                Inventory inventory = player.getInventory();
                ItemStack inventoryItem = inventory.getItem(button);
                boolean a = !inventoryItem.isEmpty();
                boolean b = !stack.isEmpty();
                LOGGER.debug("ItemClickPayload.SWAP: inventoryItem={} clickedStack={}", inventoryItem, stack);
                if( a && !b ){
                    ItemStack remain = endInv.addItem(inventoryItem);
                    inventory.setItem(button, remain);
                    LOGGER.debug("ItemClickPayload.SWAP: added inventoryItem, remain={}", remain);
                }
                if( !a && b ){
                    ItemStack swapping = endInv.takeItem(stack); //take most
                    inventory.setItem(button,swapping);
                    LOGGER.debug("ItemClickPayload.SWAP: took from endInv swapping={}", swapping);
                }
                if( a && b ){
                    ItemStack remain =  endInv.addItem(inventoryItem);
                    LOGGER.debug("ItemClickPayload.SWAP: both non-empty, remainFromAdd={}", remain);
                    if(remain.isEmpty()) {
                        ItemStack swapping =  endInv.takeItem(stack); //take most
                        inventory.setItem(button, swapping);
                        LOGGER.debug("ItemClickPayload.SWAP: swap success swapping={}", swapping);
                    }else {
                        inventory.setItem(button,remain);
                    }
                }
                endInv.setChanged();
            }
            case THROW -> {
                ItemStack thrown = endInv.takeItem(stack);
                LOGGER.debug("ItemClickPayload.THROW: thrown={}", thrown);
                player.drop(thrown,true);
                endInv.setChanged();
            }
            case PICKUP_ALL -> {
                int startIndex = menu.slots.size() - 1; //changed: reversed button==0 condition
                for(int index = startIndex; index>=0 ; --index){
                    Slot scanning = menu.slots.get(index);
                    if(!(scanning.container instanceof Inventory)) break;
                    ItemStack scanningItem =scanning.getItem();
                    if (ItemStack.isSameItemSameComponents(carried, scanningItem)) {
                        ItemStack taken = scanning.safeTake(scanningItem.getCount(), scanningItem.getCount(), player);
                        LOGGER.debug("ItemClickPayload.PICKUP_ALL: took {} from slot index={}", taken, index);
                        ItemStack remain = endInv.addItem(taken);
                        if(!remain.isEmpty()) scanning.set(remain);
                        endInv.setChanged();
                    }
                }
            }
            case CLONE -> {
                if(player.isCreative() && carried.isEmpty()){
                    menu.setCarried(stack.copyWithCount(stack.getMaxStackSize()));
                    LOGGER.debug("ItemClickPayload.CLONE: cloned stack={}", stack);
                }
            }
            case QUICK_MOVE -> {
                ItemStack taken = endInv.takeItem(stack);
                LOGGER.debug("ItemClickPayload.QUICK_MOVE: taken={}", taken);
                ItemStack remain = new PageQuickMoveHandler(menu).quickMoveFromPage(taken);
                LOGGER.debug("ItemClickPayload.QUICK_MOVE: remain after quickMove={}", remain);
                endInv.addItem(remain);
                endInv.setChanged();
            }
        }


    }

    @Override
    public String id() {
        return "item_click";
    }
}
