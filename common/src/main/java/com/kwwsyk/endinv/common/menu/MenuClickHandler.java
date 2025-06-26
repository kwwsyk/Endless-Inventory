package com.kwwsyk.endinv.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

import static net.minecraft.world.inventory.AbstractContainerMenu.*;

public abstract class MenuClickHandler {


    //quick_craft: dragging items through slots.
    //doClick(quick_craft) only raised in slotClicked <- mouseReleased in Screen when player finished his item dragging.
    //Container ItemDisplay do not allow quick_craft
    public static void handleQuickCraft(EndlessInventoryMenu menu, int slotId, int button,Player player) {
        //if(slotId<0 || menu.slots.get(slotId).container instanceof ItemDisplay)return;
        int i = menu.quickcraftStatus; // 0:start drag 1:add slot 2/0b10:end drag
        menu.quickcraftStatus = getQuickcraftHeader(button);//=button&0b11
        if (((i != 1 || menu.quickcraftStatus != 2) && i != menu.quickcraftStatus)
            || menu.getCarried().isEmpty()) {
            resetQuickCraft(menu); //reset: menu.quickCraftStatus=0;menu.quickCraftSlots.clear
        }else switch (menu.quickcraftStatus) {
            case 0->{ //starting dragging
                //Extracts the drag mode. Args : eventButton. Return (0 : evenly split, 1 : one item by slot, 2 : creative clone items)
                menu.quickcraftType = getQuickcraftType(button);
                //type=0or1->true type=2&creative->true
                if (isValidQuickcraftType(menu.quickcraftType, player)) {
                    menu.quickcraftStatus = 1; //Following quick_craft are status_1
                    menu.quickcraftSlots.clear();
                } else { //try middle button drag item
                    resetQuickCraft(menu);
                }
            }
            case 1->{//dragging
                if(slotId<0) return;
                Slot slot = menu.slots.get(slotId);
                ItemStack itemstack = menu.getCarried();
                //Checks if it's possible to add the given itemstack to the given slot.
                if (canItemQuickReplace(slot, itemstack, true)
                        && slot.mayPlace(itemstack)
                        && (menu.quickcraftType == 2 || itemstack.getCount() > menu.quickcraftSlots.size()) //either clone or having enough items
                        && menu.canDragTo(slot)) {
                    menu.quickcraftSlots.add(slot);
                }

            }
            case 2->{//finish dragging
                if (!menu.quickcraftSlots.isEmpty()) {
                    if (menu.quickcraftSlots.size() == 1) {//drag 1 slot = pickup
                        int i1 = menu.quickcraftSlots.iterator().next().index;
                        resetQuickCraft(menu);
                        handlePickup(menu,i1, menu.quickcraftType, player);
                        return;
                    }

                    ItemStack copiedCarried = menu.getCarried().copy();
                    if (copiedCarried.isEmpty()) {
                        resetQuickCraft(menu);
                        return;
                    }


                    int count = menu.getCarried().getCount();

                    for (Slot slot1 : menu.quickcraftSlots) {
                        ItemStack carried = menu.getCarried();
                        if (slot1 != null
                                && canItemQuickReplace(slot1, carried, true)
                                && slot1.mayPlace(carried)
                                && (menu.quickcraftType == 2 || carried.getCount() >= menu.quickcraftSlots.size())
                                && menu.canDragTo(slot1)) { //I never know why check twice...
                            int j = slot1.hasItem() ? slot1.getItem().getCount() : 0;//j=itemCount
                            int k = Math.min(copiedCarried.getMaxStackSize(), slot1.getMaxStackSize(copiedCarried));//k=minSlotCapSize
                            int l = Math.min(getQuickCraftPlaceCount(menu.quickcraftSlots, menu.quickcraftType, copiedCarried) + j, k);//finalCountAfterPlace
                            count -= l - j;//=count-itemCountDecrease = remainCount

                            slot1.setByPlayer(copiedCarried.copyWithCount(l));
                        }
                    }

                    copiedCarried.setCount(count);
                    menu.setCarried(copiedCarried);
                }

                resetQuickCraft(menu);
            }
            default -> resetQuickCraft(menu);
        }
    }

    private static void resetQuickCraft(EndlessInventoryMenu EIM) {
        EIM.quickcraftStatus = 0;
        EIM.quickcraftSlots.clear();
    }

    public static void handlePickup(EndlessInventoryMenu EIM, int slotId, int button, Player player){
        ClickAction clickaction = button == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
        //switch by: slotId range
        if (slotId == -999) {//outside menu, regardless of pressing shift
            if (!EIM.getCarried().isEmpty()) {//drop all item when left-clicked
                if (clickaction == ClickAction.PRIMARY) {
                    player.drop(EIM.getCarried(), true);
                    EIM.setCarried(ItemStack.EMPTY);
                } else {//drop 1 item when right-clicked
                    player.drop(EIM.getCarried().split(1), true);
                }
            }
        } else if (slotId>=0) {
            Slot clickedSlot = EIM.slots.get(slotId);
            Container container = clickedSlot.container;
            ItemStack clickedSlotItem = clickedSlot.getItem();
            ItemStack carried = EIM.getCarried();
            if(!EIM.tryItemClickBehaviourOverride(player, clickaction, clickedSlot, clickedSlotItem, carried)){
                handleVanillaPickup(EIM,slotId,clickaction,player);
                clickedSlot.setChanged();
            }
            //clickedSlot.setChanged();
        }
        
    }

    
    public static void handleVanillaPickup(AbstractContainerMenu menu, int slotId, ClickAction clickaction, Player player){
        Slot clickedSlot = menu.slots.get(slotId);
        ItemStack clickedSlotItem = clickedSlot.getItem();
        ItemStack carried = menu.getCarried();
        if (clickedSlotItem.isEmpty()) {//when slot item is empty
            if (!carried.isEmpty()) {//when carrying
                int i3 = clickaction == ClickAction.PRIMARY ? carried.getCount() : 1;
                menu.setCarried(clickedSlot.safeInsert(carried, i3));//place item
            }
        } else if (clickedSlot.mayPickup(player)) {
            if (carried.isEmpty()) {
                int j3 = clickaction == ClickAction.PRIMARY ? clickedSlotItem.getCount() : (clickedSlotItem.getCount() + 1) / 2;
                Optional<ItemStack> optional1 = clickedSlot.tryRemove(j3, Integer.MAX_VALUE, player);
                optional1.ifPresent((p_150421_) -> {
                    menu.setCarried(p_150421_);
                    clickedSlot.onTake(player, p_150421_);
                });
            } else if (clickedSlot.mayPlace(carried)) {
                if (ItemStack.isSameItemSameComponents(clickedSlotItem, carried)) {
                    int k3 = clickaction == ClickAction.PRIMARY ? carried.getCount() : 1;
                    menu.setCarried(clickedSlot.safeInsert(carried, k3));
                } else if (carried.getCount() <= clickedSlot.getMaxStackSize(carried)) {
                    menu.setCarried(clickedSlotItem);
                    clickedSlot.setByPlayer(carried);
                }
            } else if (ItemStack.isSameItemSameComponents(clickedSlotItem, carried)) {
                Optional<ItemStack> optional = clickedSlot.tryRemove(clickedSlotItem.getCount(), carried.getMaxStackSize() - carried.getCount(), player);
                optional.ifPresent((p_150428_) -> {
                    carried.grow(p_150428_.getCount());
                    clickedSlot.onTake(player, p_150428_);
                });
            }
        }
    }


    public static void handleQuickMove(AbstractContainerMenu menu, int slotId, int button, Player player){
        ClickAction clickaction = button == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
        if (slotId == -999) {//outside menu, regardless of pressing shift
            if (!menu.getCarried().isEmpty()) {//drop all item when left-clicked
                if (clickaction == ClickAction.PRIMARY) {
                    player.drop(menu.getCarried(), true);
                    menu.setCarried(ItemStack.EMPTY);
                } else {//drop 1 item when right-clicked
                    player.drop(menu.getCarried().split(1), true);
                }
            }
        } else if (slotId < 0) {//on the frame of menu...
                return;
            } else {

            Slot clickedSlot = menu.slots.get(slotId);
            if (!clickedSlot.mayPickup(player)) {
                return;
            }

            ItemStack itemstack8 = menu.quickMoveStack(player, slotId);

            while (!itemstack8.isEmpty() && ItemStack.isSameItem(clickedSlot.getItem(), itemstack8)) {//originally works in crafting table...
                itemstack8 = menu.quickMoveStack(player, slotId);
            }//reserve
            //clickedSlot.setChanged();
        }
    }

    public static void handleSwap(EndlessInventoryMenu EIM, int slotId, int button, Player player){
        if(!(button >= 0 && button < 9 || button == 40) || slotId<=0) return;
        Slot clickedSlot = EIM.slots.get(slotId);
        Container container = clickedSlot.container;
        handleVanillaSwap(EIM,slotId,button,player);

    }

    public static void handleVanillaSwap(AbstractContainerMenu menu, int slotId, int button, Player player){
        Inventory inventory = player.getInventory();
        ItemStack inventoryItem = inventory.getItem(button);
        Slot hoveringSlot = menu.slots.get(slotId);
        ItemStack hoveringSlotItem = hoveringSlot.getItem();
        if (!inventoryItem.isEmpty() || !hoveringSlotItem.isEmpty()) {
            if (inventoryItem.isEmpty()) {
                if (hoveringSlot.mayPickup(player)) {
                    inventory.setItem(button, hoveringSlotItem);
                    //hoveringSlot.onSwapCraft(hoveringSlotItem.getCount()); How can vanilla ACM can access it ?
                    hoveringSlot.setByPlayer(ItemStack.EMPTY);
                    hoveringSlot.onTake(player, hoveringSlotItem);
                }
            } else if (hoveringSlotItem.isEmpty()) {
                if (hoveringSlot.mayPlace(inventoryItem)) {
                    int j2 = hoveringSlot.getMaxStackSize(inventoryItem);
                    if (inventoryItem.getCount() > j2) {
                        hoveringSlot.setByPlayer(inventoryItem.split(j2));
                    } else {
                        inventory.setItem(button, ItemStack.EMPTY);
                        hoveringSlot.setByPlayer(inventoryItem);
                    }
                }
            } else if (hoveringSlot.mayPickup(player) && hoveringSlot.mayPlace(inventoryItem)) {
                int k2 = hoveringSlot.getMaxStackSize(inventoryItem);
                if (inventoryItem.getCount() > k2) {
                    hoveringSlot.setByPlayer(inventoryItem.split(k2));
                    hoveringSlot.onTake(player, hoveringSlotItem);
                    if (!inventory.add(hoveringSlotItem)) {
                        player.drop(hoveringSlotItem, true);
                    }
                } else {
                    inventory.setItem(button, hoveringSlotItem);
                    hoveringSlot.setByPlayer(inventoryItem);
                    hoveringSlot.onTake(player, hoveringSlotItem);
                }
            }
        }
    }


    public static void handleThrow(AbstractContainerMenu menu, int slotId, int button, Player player){
        if(!menu.getCarried().isEmpty() || slotId < 0 ) return;
        Slot throwingSlot = menu.slots.get(slotId);
        int throwingCount = button == 0 ? 1 : throwingSlot.getItem().getCount(); // Q : ctrl+Q
        ItemStack thrown = throwingSlot.safeTake(throwingCount, Integer.MAX_VALUE, player);
        player.drop(thrown, true);

        if (button == 1) {
            while (!thrown.isEmpty() && ItemStack.isSameItem(throwingSlot.getItem(), thrown)) {

                thrown = throwingSlot.safeTake(throwingCount, Integer.MAX_VALUE, player);
                player.drop(thrown, true);
            }
        }
    }
    public static void handleClone(AbstractContainerMenu menu, int slotId, int button,  Player player){
        if(player.hasInfiniteMaterials() && menu.getCarried().isEmpty() && slotId >= 0){
            Slot slot4 = menu.slots.get(slotId);
            if (slot4.hasItem()) {
                ItemStack itemstack5 = slot4.getItem();
                menu.setCarried(itemstack5.copyWithCount(itemstack5.getMaxStackSize()));
            }
        }
    }
    public static void handlePickupAll(EndlessInventoryMenu EIM, int slotId, int button,  Player player){
        if(slotId<0) return;

        Slot clickedSlot = EIM.slots.get(slotId);
        Container slotContainer = clickedSlot.container;
        ItemStack carried = EIM.getCarried();

        if (carried.isEmpty()) return;
        if(!clickedSlot.hasItem() || !clickedSlot.mayPickup(player)) {
            int startIndex = button == 0 ? 0 : EIM.slots.size() - 1;
            int inc = button == 0 ? 1 : -1;

            for (int l2 = 0; l2 < 2; l2++) {
                for (int index = startIndex; index >= 0 && index < EIM.slots.size() && carried.getCount() < carried.getMaxStackSize(); index += inc) {
                    Slot scanningSlot = EIM.slots.get(index);
                    if (canItemQuickReplace(scanningSlot, carried, true)
                            && scanningSlot.mayPickup(player)
                            && EIM.canTakeItemForPickAll(carried, scanningSlot)
                            ) {
                        ItemStack scanningItem = scanningSlot.getItem();
                        if (l2 != 0 || scanningItem.getCount() != scanningItem.getMaxStackSize()) {
                            ItemStack taken = scanningSlot.safeTake(scanningItem.getCount(), carried.getMaxStackSize() - carried.getCount(), player);
                            carried.grow(taken.getCount());
                        }
                    }
                }
                if(carried.getCount() < carried.getMaxStackSize()){
                    ItemStack taken = EIM.getDisplayingPage().tryExtractItem(carried,carried.getMaxStackSize()-carried.getCount());
                    carried.grow(taken.getCount());
                    carried.limitSize(carried.getMaxStackSize());
                }
            }
        }
    }
    
    public static void vanillaPickupAll(AbstractContainerMenu menu, int slotId, int button,  Player player){
        if (slotId >= 0) {
            Slot slot2 = menu.slots.get(slotId);
            ItemStack itemstack4 = menu.getCarried();
            if (!itemstack4.isEmpty() && (!slot2.hasItem() || !slot2.mayPickup(player))) {
                int l1 = button == 0 ? 0 : menu.slots.size() - 1;
                int i2 = button == 0 ? 1 : -1;

                for(int l2 = 0; l2 < 2; ++l2) {
                    for(int l3 = l1; l3 >= 0 && l3 < menu.slots.size() && itemstack4.getCount() < itemstack4.getMaxStackSize(); l3 += i2) {
                        Slot slot8 = menu.slots.get(l3);
                        if (slot8.hasItem() && canItemQuickReplace(slot8, itemstack4, true) && slot8.mayPickup(player) && menu.canTakeItemForPickAll(itemstack4, slot8)) {
                            ItemStack itemstack11 = slot8.getItem();
                            if (l2 != 0 || itemstack11.getCount() != itemstack11.getMaxStackSize()) {
                                ItemStack itemstack12 = slot8.safeTake(itemstack11.getCount(), itemstack4.getMaxStackSize() - itemstack4.getCount(), player);
                                itemstack4.grow(itemstack12.getCount());
                            }
                        }
                    }
                }
            }
        }
    }
}
