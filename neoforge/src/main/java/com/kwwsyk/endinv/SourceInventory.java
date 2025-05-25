package com.kwwsyk.endinv;

import net.minecraft.world.item.ItemStack;

public interface SourceInventory {


    boolean isRemote();
    ItemStack takeItem(ItemStack itemStack);

    ItemStack takeItem(ItemStack itemStack, int count);

    ItemStack addItem(ItemStack itemStack);

    void setChanged();

    int getItemSize();

    ItemStack getItem(int i);


}
