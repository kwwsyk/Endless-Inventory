package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageQuickMoveHandler;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public record ItemClickPayload(ItemStack stack, int button, ClickType clickType) implements ModPacketPayload {



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
        var opt = ServerLevelEndInv.getEndInvForPlayer(player);
        if(opt.isEmpty()) return;
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
                    menu.setCarried(endInv.takeItem(stack,takenCount));
                    if(!stack.isEmpty()) endInv.setChanged();
                }
            }
            case SWAP -> {
                Inventory inventory = player.getInventory();
                ItemStack inventoryItem = inventory.getItem(button);
                boolean a = !inventoryItem.isEmpty();
                boolean b = !stack.isEmpty();
                if( a && !b ){
                    ItemStack remain = endInv.addItem(inventoryItem);
                    inventory.setItem(button, remain);
                }
                if( !a && b ){
                    ItemStack swapping = endInv.takeItem(stack); //take most
                    inventory.setItem(button,swapping);
                }
                if( a && b ){
                    ItemStack remain =  endInv.addItem(inventoryItem);
                    if(remain.isEmpty()) {
                        ItemStack swapping =  endInv.takeItem(stack); //take most
                        inventory.setItem(button, swapping);
                    }else {
                        inventory.setItem(button,remain);
                    }
                }
                endInv.setChanged();
            }
            case THROW -> {
                ItemStack thrown = endInv.takeItem(stack);
                player.drop(thrown,true);
                endInv.setChanged();
            }
            case PICKUP_ALL -> {
                int startIndex = menu.slots.size() - 1; //changed: reversed button==0 condition
                for(int index = startIndex; index>=0 ; --index){
                    Slot scanning = menu.slots.get(index);
                    if(!(scanning.container instanceof Inventory)) break;
                    ItemStack scanningItem =scanning.getItem();
                    if(ItemStack.isSameItemSameTags(carried,scanningItem)){
                        ItemStack taken = scanning.safeTake(scanningItem.getCount(), scanningItem.getCount(), player);
                        ItemStack remain = endInv.addItem(taken);
                        if(!remain.isEmpty()) scanning.set(remain);
                        endInv.setChanged();
                    }
                }
            }
            case CLONE -> {
                if(player.isCreative() && carried.isEmpty()){
                    menu.setCarried(stack.copyWithCount(stack.getMaxStackSize()));
                }
            }
            case QUICK_MOVE -> {
                ItemStack taken = endInv.takeItem(stack);
                ItemStack remain = new PageQuickMoveHandler(menu).quickMoveFromPage(taken);
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
