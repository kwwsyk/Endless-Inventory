package com.kwwsyk.endinv;

import net.minecraft.world.item.ItemStack;

public interface SourceInventory {


    boolean isRemote();

    ItemStack takeItem(ItemStack itemStack);

    ItemStack takeItem(ItemStack itemStack, int count);

    ItemStack takeItem(int index, int count);

    ItemStack addItem(ItemStack itemStack);

    void setChanged();

    ItemStack removeItem(int index);

    int getItemSize();


    ItemStack getItem(int i);
}
