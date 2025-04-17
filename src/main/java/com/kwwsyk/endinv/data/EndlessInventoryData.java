package com.kwwsyk.endinv.data;

import com.kwwsyk.endinv.EndlessInventory;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class EndlessInventoryData extends SavedData {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static EndlessInventoryData levelEndInvData;
    private final List<EndlessInventory> levelEndInvs;

    public static EndInvCodecStrategy LoadStrategy = new FullCodecStrategy();
    public static EndInvCodecStrategy SaveStrategy = new FullCodecStrategy();

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

        CompoundTag head = listTag.getCompound(0);
        checkStrategy(head);

        //{}EndInv -> EndInvs -> levelEndInvs
        listTag.iterator().forEachRemaining(
                (t)->   data.levelEndInvs.add(LoadStrategy.tagToEndInv((CompoundTag) t,lookupProvider))
        );
        return data;
    }

    static void checkStrategy(final CompoundTag tag){
        boolean flag = LoadStrategy.canHandle(tag);
        if(!flag) {
            LoadStrategy = new SortedSaveStrategy();
            LOGGER.debug("EndInv load strategy changed to default as current strategy cannot handle.");
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        // []:List of {}EndInv
        ListTag nbtTagList = new ListTag();

        for (EndlessInventory endlessInventory : levelEndInvs) {
            //{}EndInv: Items: []{id,count,components},Size:I
            CompoundTag invTag = SaveStrategy.serializeItems(endlessInventory,provider);
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





}
