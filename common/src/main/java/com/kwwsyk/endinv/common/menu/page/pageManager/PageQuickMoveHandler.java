package com.kwwsyk.endinv.common.menu.page.pageManager;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PageQuickMoveHandler {
    private final AbstractContainerMenu menu;
    private final PageMetaDataManager manager;

    /**
     * To add custom quick move action for custom menus.
     */
    public interface PageQuickMoveOverride{
        ItemStack quickMoveFromPage(ItemStack stack);
    }

    public PageQuickMoveHandler(PageMetaDataManager manager){
        this.manager = manager;
        this.menu = manager.getMenu();
    }

    public ItemStack quickMoveFromPage(ItemStack stack){
        switch (menu){
            case PageQuickMoveOverride override -> stack = override.quickMoveFromPage(stack);
            //add extends
            case InventoryMenu ignored -> moveItemStackTo(stack,0,menu.slots.size()-1,true);
            default -> moveItemStackTo(stack,0,menu.slots.size()-1,false);
        }

        return stack;
    }

    /**
     * Vanilla method of handling quick move item to area of menu.
     * @param startIndex the start Index of the area.
     * @param endIndex the end Index of the area. Should not below than startIndex.
     * @param reverseDirection if true, iteration is from {@code endIndex} to {@code startIndex}
     * @param stack to move stack, attention it will be changed
     * @return true if item was moved.
     */
    public boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean flag = false;
        int i = startIndex;
        if (reverseDirection) {
            i = endIndex - 1;
        }

        if (stack.isStackable()) {
            while(!stack.isEmpty()) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                Slot slot = menu.slots.get(i);
                ItemStack itemstack = slot.getItem();
                if (!itemstack.isEmpty() && ItemStack.isSameItemSameComponents(stack, itemstack)) {
                    int j = itemstack.getCount() + stack.getCount();
                    int k = slot.getMaxStackSize(itemstack);
                    if (j <= k) {
                        stack.setCount(0);
                        itemstack.setCount(j);
                        slot.setChanged();
                        flag = true;
                    } else if (itemstack.getCount() < k) {
                        stack.shrink(k - itemstack.getCount());
                        itemstack.setCount(k);
                        slot.setChanged();
                        flag = true;
                    }
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        if (!stack.isEmpty()) {
            if (reverseDirection) {
                i = endIndex - 1;
            } else {
                i = startIndex;
            }

            while(true) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                Slot slot1 = menu.slots.get(i);
                ItemStack itemstack1 = slot1.getItem();
                if (itemstack1.isEmpty() && slot1.mayPlace(stack)) {
                    int l = slot1.getMaxStackSize(stack);
                    slot1.setByPlayer(stack.split(Math.min(stack.getCount(), l)));
                    slot1.setChanged();
                    flag = true;
                    break;
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return flag;
    }
}
