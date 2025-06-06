package com.kwwsyk.endinv.data;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.util.ItemKey;
import com.kwwsyk.endinv.util.ItemState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

public class FullCodecStrategy implements EndInvCodecStrategy{

    public boolean canHandle(CompoundTag tag){
        return ((CompoundTag)tag.getList(ITEM_LIST_KEY, Tag.TAG_COMPOUND).getFirst()).contains(LAST_MOD_TIME_LONG_KEY);
    }

    public void deserializeItems(EndlessInventory endlessInventory, HolderLookup.Provider provider, CompoundTag nbt) {
        Map<ItemKey, ItemState> itemMap = endlessInventory.getItemMap();
        ListTag tagList = nbt.getList(ITEM_LIST_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            Optional<ItemStack> stackPre = EndInvCodecStrategy.parse(provider, itemTag).filter(it->!it.isEmpty());
            final long modState = itemTag.getLong(LAST_MOD_TIME_LONG_KEY);
            endlessInventory.updateModState(modState);
            stackPre.ifPresent(itemStack ->
                    itemMap.put(ItemKey.asKey(itemStack),new ItemState(itemStack.getCount(), modState)));
        }
    }

    public CompoundTag serializeItems(EndlessInventory endlessInventory, HolderLookup.Provider provider) {
        ListTag nbtTagList = new ListTag();
        Map<ItemKey, ItemState> itemMap = endlessInventory.getItemMap();
        for (var entry : itemMap.entrySet()) {
            ItemStack itemStack = entry.getKey().toStack(entry.getValue().count());
            if (!itemStack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putLong(LAST_MOD_TIME_LONG_KEY,entry.getValue().lastModTime());
                nbtTagList.add(EndInvCodecStrategy.saveItem(itemStack,provider, itemTag));
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put(ITEM_LIST_KEY, nbtTagList);
        nbt.putInt(SIZE_INT_KEY, itemMap.size());
        return nbt;
    }


}
