package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemState;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public interface SourceInventory {


    boolean isRemote();
    ItemStack takeItem(ItemStack itemStack);

    ItemStack takeItem(ItemStack itemStack, int count);

    ItemStack addItem(ItemStack itemStack);

    void setChanged();

    int getItemSize();

    ItemStack getItem(int i);


    Map<ItemKey, ItemState> getItemMap();
}
