package com.kwwsyk.endinv.data;

import com.kwwsyk.endinv.EndlessInventory;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SortedSaveStrategy implements EndInvCodecStrategy{

    public boolean canHandle(CompoundTag tag){
        return true;
    }

    public void deserializeItems(EndlessInventory endlessInventory, HolderLookup.Provider provider, CompoundTag nbt) {
        List<ItemStack> items = endlessInventory.getItemsAsList();
        ListTag tagList = nbt.getList(ITEM_LIST_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            EndInvCodecStrategy.parse(provider, itemTag).filter(it->!it.isEmpty()).ifPresent(items::add);
        }
        endlessInventory.syncMapFromItems();
    }

    public CompoundTag serializeItems(EndlessInventory endlessInventory, HolderLookup.Provider provider) {
        ListTag nbtTagList = new ListTag();
        endlessInventory.syncItemsFromMap();
        List<ItemStack> items = endlessInventory.getItemsAsList();
        for (ItemStack itemStack : items) {
            if (!itemStack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();

                nbtTagList.add(EndInvCodecStrategy.saveItem(itemStack,provider, itemTag));
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put(ITEM_LIST_KEY, nbtTagList);
        nbt.putInt(SIZE_INT_KEY, items.size());
        return nbt;
    }
}
