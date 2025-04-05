package com.kwwsyk.endinv;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.util.DataComponentUtil;
import org.slf4j.Logger;

import java.util.*;

public class EndlessInventoryData extends SavedData {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static EndlessInventoryData levelEndInvData;
    public static final Codec<ItemStack> ITEM_CODEC = Codec.lazyInitialized(
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
    private final List<EndlessInventory> levelEndInvs;

    EndlessInventoryData(){
        this.levelEndInvs = new ArrayList<>();
    }

    public static void init(ServerLevel level){
        if (!level.dimension().equals(Level.OVERWORLD)) {
            //LOGGER.warn("Skipped EndlessInventoryData initialization in dimension: {}", level.dimension().location());
            return; // 仅在主世界执行
        }
        Factory<EndlessInventoryData> factory = new Factory<>(EndlessInventoryData::create,EndlessInventoryData::load);
        levelEndInvData = level.getDataStorage().computeIfAbsent(factory,"endless_inventories");

        LOGGER.info("Initialized EndlessInventoryData in {} with {} inventories", level.dimension().location(), levelEndInvData.levelEndInvs.size());
    }

    public static EndlessInventoryData create(){
        return new EndlessInventoryData();
    }

    public void addEndInvToLevel(EndlessInventory endlessInventory){
        levelEndInvs.add(endlessInventory);
        setDirty();
    }

    public EndlessInventory fromUUID(UUID uuid){
        for(EndlessInventory endlessInventory : levelEndInvs){
            if (Objects.equals(endlessInventory.getUuid(),uuid)) return endlessInventory;
        }
        return null;
    }



    public static EndlessInventoryData load(final CompoundTag tag, HolderLookup.Provider lookupProvider){
        EndlessInventoryData data = create();
        //Get EndInv[]
        ListTag listTag = tag.getList("endless_inventories",10) ;
        //{}EndInv -> EndInvs -> levelEndInvs
        listTag.iterator().forEachRemaining(
                (t)->   data.levelEndInvs.add(tagToEndInv((CompoundTag) t,lookupProvider))
        );
        return data;
    }

    private static EndlessInventory tagToEndInv(CompoundTag invTag, HolderLookup.Provider lookupProvider){
        //invTag:{uuid,Items:[],size}
        //handle uuid
        EndlessInventory endlessInventory = new EndlessInventory(invTag.getUUID("uuid"));
        //handle Items,size
        deserializeItems(endlessInventory,lookupProvider,invTag);
        return  endlessInventory;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        // []:List of {}EndInv
        ListTag nbtTagList = new ListTag();

        for (EndlessInventory endlessInventory : levelEndInvs) {
            //{}EndInv: Items: []{id,count,components},Size:I
            CompoundTag invTag = serializeItems(endlessInventory,provider);
            if (endlessInventory.getUuid() == null) {
                endlessInventory.giveNewUuid();
            }
            //{}EndInv: uuid: UUID
            invTag.putUUID("uuid", endlessInventory.getUuid());
            //[]+{}EndInv
            nbtTagList.add(invTag);
        }

        CompoundTag saveTag = new CompoundTag();
        //{HEAD}: endless_inventories: []
        saveTag.put("endless_inventories",nbtTagList);
        return saveTag;
    }


    private static CompoundTag serializeItems(EndlessInventory endlessInventory,HolderLookup.Provider provider) {
        ListTag nbtTagList = new ListTag();
        List<ItemStack> items = endlessInventory.getItems();
        for (ItemStack itemStack : items) {
            if (!itemStack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();

                nbtTagList.add(saveItem(itemStack,provider, itemTag));
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", items.size());
        return nbt;
    }

    private static Tag saveItem(ItemStack itemStack,HolderLookup.Provider levelRegistryAccess, Tag outputTag) {
        if (itemStack.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty ItemStack");
        } else {
            return DataComponentUtil.wrapEncodingExceptions(itemStack, ITEM_CODEC, levelRegistryAccess, outputTag);
        }
    }


    private static void deserializeItems(EndlessInventory endlessInventory,HolderLookup.Provider provider, CompoundTag nbt) {
        List<ItemStack> items = endlessInventory.getItems();
        ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);

            parse(provider, itemTag).filter(it->!it.isEmpty()).ifPresent(items::add);

        }
    }

    public static Optional<ItemStack> parse(HolderLookup.Provider lookupProvider, Tag tag) {
        return ITEM_CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial((p_330102_) -> LOGGER.error("Tried to load invalid item: '{}'", p_330102_));
    }
}
