package com.kwwsyk.endinv.common.data;

import com.kwwsyk.endinv.common.EndInvAffinities;
import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.util.Accessibility;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.*;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;


public interface EndInvCodecStrategy {

    Logger LOGGER = LogUtils.getLogger();
    Codec<ItemStack> ITEM_CODEC = Codec.lazyInitialized(
            () -> RecordCodecBuilder.create(
                    p_381569_ -> p_381569_.group(
                                    ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
                                    ExtraCodecs.intRange(1, 2147483647).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
                                    DataComponentPatch.CODEC
                                            .optionalFieldOf("components", DataComponentPatch.EMPTY)
                                            .forGetter(p_330103_ -> ((PatchedDataComponentMap)p_330103_.getComponents()).asPatch())
                            )
                            .apply(p_381569_, ItemStack::new)
            )
    );
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


    default EndlessInventory deserializeEndInv(CompoundTag invTag, HolderLookup.Provider lookupProvider){
        //handle uuid and itemMap/items.
        EndlessInventory endlessInventory = new EndlessInventory(invTag.getUUID(UUID_KEY));
        deserializeItems(endlessInventory,lookupProvider,invTag);
        //handle affinities
        decodeAffinities(endlessInventory,lookupProvider, (CompoundTag) invTag.get(AFFINITY_KEY));
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

    default CompoundTag serializeEndInv(EndlessInventory endInv,HolderLookup.Provider provider){
        CompoundTag nbt = new CompoundTag();

        CompoundTag itemData = serializeItems(endInv, provider);
        nbt.merge(itemData);

        CompoundTag affTag = encodeAffinities(endInv.affinities,provider);
        nbt.put(AFFINITY_KEY,affTag);

        if (endInv.getUuid() == null) {
            endInv.giveNewUuid();
        }
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

    void deserializeItems(EndlessInventory endlessInventory, HolderLookup.Provider provider, CompoundTag nbt);

    CompoundTag serializeItems(EndlessInventory endlessInventory,HolderLookup.Provider provider);

    boolean canHandle(CompoundTag tag);

    default CompoundTag encodeAffinities(EndInvAffinities affinities,HolderLookup.Provider provider){
        CompoundTag ret = new CompoundTag();
        if(affinities==null) return ret;
        ListTag nbtTagList = new ListTag();
        List<ItemStack> items = affinities.starredItems;
        for (ItemStack itemStack : items) {
            if (!itemStack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();

                nbtTagList.add(saveItem(itemStack.copyWithCount(1), provider, itemTag));
            }
        }
        ret.put(BOOKMARK_LIST_KEY,nbtTagList);

        return  ret;
    }

    default void decodeAffinities(EndlessInventory endlessInventory, HolderLookup.Provider provider,@Nullable CompoundTag nbt){
        EndInvAffinities aff = endlessInventory.affinities;
        if(nbt==null) return;
        ListTag tagList = nbt.getList(BOOKMARK_LIST_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            parse(provider, itemTag).filter(it -> !it.isEmpty()).ifPresent(aff::addStarredItem);
        }
    }

    static Optional<ItemStack> parse(HolderLookup.Provider lookupProvider, Tag tag) {
        return ITEM_CODEC.parse(lookupProvider
                .createSerializationContext(NbtOps.INSTANCE), tag)
                .resultOrPartial((p_330102_) -> LOGGER.error("Tried to load invalid item: '{}'", p_330102_));
    }

    static Tag saveItem(ItemStack itemStack,HolderLookup.Provider levelRegistryAccess, Tag outputTag) {
        if (itemStack.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty ItemStack");
        } else {
            return wrapEncodingExceptions(itemStack, EndInvCodecStrategy.ITEM_CODEC, levelRegistryAccess, outputTag);
        }
    }

    static <T extends DataComponentHolder> Tag wrapEncodingExceptions(T componentHolder, Codec<T> codec, HolderLookup.Provider provider, Tag tag) {
        try {
            return codec.encode(componentHolder, provider.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
        } catch (Exception exception) {
            logDataComponentSaveError(componentHolder, exception, tag);
            throw exception;
        }
    }

    static void logDataComponentSaveError(DataComponentHolder componentHolder, Exception original, @Nullable Tag tag) {
        StringBuilder cause = new StringBuilder("Error saving [" + componentHolder + "]. Original cause: " + original);

        cause.append("\nWith components:\n{");
        componentHolder.getComponents().forEach((component) -> {
            cause.append("\n\t").append(component);
        });
        cause.append("\n}");
        if (tag != null) {
            cause.append("\nWith tag: ").append(tag);
        }
        Util.logAndPauseIfInIde(cause.toString());
    }
}
