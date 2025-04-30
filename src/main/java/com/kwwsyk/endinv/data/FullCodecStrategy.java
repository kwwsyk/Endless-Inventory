package com.kwwsyk.endinv.data;

import com.kwwsyk.endinv.EndlessInventory;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.DataComponentUtil;

import java.util.Map;
import java.util.Optional;

public class FullCodecStrategy implements EndInvCodecStrategy{

    public boolean canHandle(CompoundTag tag){
        return ((CompoundTag)tag.getList("Items", Tag.TAG_COMPOUND).getFirst()).contains("modState");
    }

    public EndlessInventory tagToEndInv(CompoundTag invTag, HolderLookup.Provider lookupProvider){
        //invTag:{uuid,Items:[],size}
        //handle uuid
        EndlessInventory endlessInventory = new EndlessInventory(invTag.getUUID("uuid"));
        //handle Items,size
        deserializeItems(endlessInventory,lookupProvider,invTag);

        decodeAffinities(endlessInventory,lookupProvider, (CompoundTag) invTag.get("Affinities"));
        return  endlessInventory;
    }

    static void deserializeItems(EndlessInventory endlessInventory, HolderLookup.Provider provider, CompoundTag nbt) {
        Map<ItemStack,EndlessInventory.ItemState> itemMap = endlessInventory.getItemMap();
        ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            Optional<ItemStack> stackPre = EndInvCodecStrategy.parse(provider, itemTag).filter(it->!it.isEmpty());
            long modState = itemTag.getLong("modState");
            stackPre.ifPresent(itemStack ->
                    itemMap.put(itemStack.copyWithCount(1),new EndlessInventory.ItemState(itemStack.getCount(),modState)));
        }
    }

    public CompoundTag serializeItems(EndlessInventory endlessInventory, HolderLookup.Provider provider) {
        ListTag nbtTagList = new ListTag();
        Map<ItemStack,EndlessInventory.ItemState> itemMap = endlessInventory.getItemMap();
        for (Map.Entry<ItemStack,EndlessInventory.ItemState> entry : itemMap.entrySet()) {
            ItemStack itemStack = entry.getKey().copyWithCount(entry.getValue().count());
            if (!itemStack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putLong("modState",entry.getValue().lastModified());
                nbtTagList.add(saveItem(itemStack,provider, itemTag));
            }
        }
        CompoundTag affTag = encodeAffinities(endlessInventory.affinities,provider);
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", itemMap.size());
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
