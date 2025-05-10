package com.kwwsyk.endinv.data;

import com.kwwsyk.endinv.EndInvAffinities;
import com.kwwsyk.endinv.EndlessInventory;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

import static com.kwwsyk.endinv.data.SortedSaveStrategy.saveItem;

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


    EndlessInventory tagToEndInv(CompoundTag invTag, HolderLookup.Provider lookupProvider);

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
}
