package com.kwwsyk.endinv.data;

import com.kwwsyk.endinv.EndlessInventory;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.DataComponentUtil;

import java.util.List;

public class SortedSaveStrategy implements EndInvCodecStrategy{

    public boolean canHandle(CompoundTag tag){
        return true;
    }

    public EndlessInventory tagToEndInv(CompoundTag invTag, HolderLookup.Provider lookupProvider){
        //invTag:{uuid,Items:[],size}
        //handle uuid
        EndlessInventory endlessInventory = new EndlessInventory(invTag.getUUID("uuid"));
        //handle Items,size
        deserializeItems(endlessInventory,lookupProvider,invTag);
        endlessInventory.syncMapFromItems();

        decodeAffinities(endlessInventory,lookupProvider, (CompoundTag) invTag.get("Affinities"));
        return  endlessInventory;
    }

    static void deserializeItems(EndlessInventory endlessInventory, HolderLookup.Provider provider, CompoundTag nbt) {

        List<ItemStack> items = endlessInventory.getItems();
        ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            EndInvCodecStrategy.parse(provider, itemTag).filter(it->!it.isEmpty()).ifPresent(items::add);
        }
    }

    public CompoundTag serializeItems(EndlessInventory endlessInventory, HolderLookup.Provider provider) {
        ListTag nbtTagList = new ListTag();
        endlessInventory.syncItemsFromMap();
        List<ItemStack> items = endlessInventory.getItems();
        for (ItemStack itemStack : items) {
            if (!itemStack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();

                nbtTagList.add(saveItem(itemStack,provider, itemTag));
            }
        }
        CompoundTag affTag = encodeAffinities(endlessInventory.affinities,provider);
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", items.size());
        nbt.put("Affinities",affTag);
        return nbt;
    }

    static Tag saveItem(ItemStack itemStack,HolderLookup.Provider levelRegistryAccess, Tag outputTag) {
        if (itemStack.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty ItemStack");
        } else {
            return DataComponentUtil.wrapEncodingExceptions(itemStack, EndInvCodecStrategy.ITEM_CODEC, levelRegistryAccess, outputTag);
        }
    }
}
