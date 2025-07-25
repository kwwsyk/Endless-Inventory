package com.kwwsyk.endinv.common.data;

import com.kwwsyk.endinv.common.EndInvAffinities;
import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.util.Accessibility;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;


public interface EndInvCodecStrategy {

    Logger LOGGER = LogUtils.getLogger();
    String END_INV_LIST_KEY = "endless_inventories";
    String ITEM_LIST_KEY = "Items";
    String SIZE_INT_KEY = "Size";
    String LAST_MOD_TIME_LONG_KEY = "modState";
    String UUID_KEY = "uuid";
    String MAX_STACK_SIZE_INT_KEY = "maxItemStackSize";
    String INFINITY_BOOL_KEY = "Infinity";
    String AFFINITY_KEY = "Affinities";
    String BOOKMARK_LIST_KEY = "starred_items";
    String OWNER_UUID_KEY = "Owner";
    String WHITE_LIST_KEY = "white_list";
    String ACCESSIBILITY_KEY = "Accessibility";


    default EndlessInventory deserializeEndInv(CompoundTag invTag){
        //handle uuid and itemMap/items.
        EndlessInventory endlessInventory = new EndlessInventory(invTag.getUUID(UUID_KEY));
        deserializeItems(endlessInventory,invTag);
        //handle affinities
        decodeAffinities(endlessInventory, (CompoundTag) invTag.get(AFFINITY_KEY));
        //handle player uuid
        if(invTag.contains(OWNER_UUID_KEY)) endlessInventory.setOwner(invTag.getUUID(OWNER_UUID_KEY));
        if(invTag.contains(WHITE_LIST_KEY)){
            endlessInventory.white_list.addAll(invTag.getList(WHITE_LIST_KEY,Tag.TAG_INT_ARRAY).stream().map(NbtUtils::loadUUID).toList());
        }
        if(invTag.contains(ACCESSIBILITY_KEY)) endlessInventory.setAccessibility(Accessibility.valueOf(invTag.getString(ACCESSIBILITY_KEY)));
        //handle other fields
        if(invTag.contains(MAX_STACK_SIZE_INT_KEY)) endlessInventory.setMaxItemStackSize(invTag.getInt(MAX_STACK_SIZE_INT_KEY));
        if(invTag.contains(INFINITY_BOOL_KEY)) endlessInventory.setInfinityMode(invTag.getBoolean(INFINITY_BOOL_KEY));



        return  endlessInventory;
    }

    default CompoundTag serializeEndInv(EndlessInventory endInv){
        CompoundTag nbt = new CompoundTag();

        CompoundTag itemData = serializeItems(endInv);
        nbt.merge(itemData);

        CompoundTag affTag = encodeAffinities(endInv.affinities);
        nbt.put(AFFINITY_KEY,affTag);

        nbt.putUUID(UUID_KEY,endInv.getUuid());

        if (endInv.getOwnerUUID() != null) {
            nbt.putUUID(OWNER_UUID_KEY,endInv.getOwnerUUID());
        }
        ListTag whiteList = new ListTag();
        for(var uuid: endInv.white_list){
            whiteList.add(NbtUtils.createUUID(uuid));
        }
        nbt.put(WHITE_LIST_KEY,whiteList);

        nbt.putString(ACCESSIBILITY_KEY,endInv.getAccessibility().name());

        nbt.putInt(MAX_STACK_SIZE_INT_KEY,endInv.getMaxItemStackSize());
        nbt.putBoolean(INFINITY_BOOL_KEY,endInv.isInfinityMode());

        return nbt;
    }

    void deserializeItems(EndlessInventory endlessInventory,CompoundTag nbt);

    CompoundTag serializeItems(EndlessInventory endlessInventory);

    boolean canHandle(CompoundTag tag);

    default CompoundTag encodeAffinities(EndInvAffinities affinities){
        CompoundTag ret = new CompoundTag();
        if(affinities==null) return ret;
        ListTag nbtTagList = new ListTag();
        List<ItemStack> items = affinities.starredItems;
        for (ItemStack itemStack : items) {
            if (!itemStack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();

                nbtTagList.add(saveItem(itemStack.copyWithCount(1), itemTag));
            }
        }
        ret.put(BOOKMARK_LIST_KEY,nbtTagList);

        return  ret;
    }

    default void decodeAffinities(EndlessInventory endlessInventory,@Nullable CompoundTag nbt){
        EndInvAffinities aff = endlessInventory.affinities;
        if(nbt==null) return;
        ListTag tagList = nbt.getList(BOOKMARK_LIST_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            parse(itemTag).filter(it -> !it.isEmpty()).ifPresent(aff::addStarredItem);
        }
    }

    static Optional<ItemStack> parse(Tag tag) {
        if (!(tag instanceof CompoundTag compound)) {
            return Optional.empty();
        }
        try {
            return Optional.of(ItemStack.of(compound));
        } catch (Exception e) {
            LOGGER.error("Tried to load invalid item: '{}'", compound, e);
            return Optional.empty();
        }
    }

    static Tag saveItem(ItemStack itemStack, CompoundTag outputTag) {
        if (itemStack.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty ItemStack");
        }
        return itemStack.save(outputTag);
    }
}
