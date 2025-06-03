package com.kwwsyk.endinv.common.data;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.kwwsyk.endinv.common.data.EndInvCodecStrategy.END_INV_LIST_KEY;

public class EndlessInventoryData extends SavedData {

    private static final Logger LOGGER = LogUtils.getLogger();
    public final List<EndlessInventory> levelEndInvs;

    public static EndInvCodecStrategy LoadStrategy = new FullCodecStrategy();
    public static EndInvCodecStrategy SaveStrategy = new FullCodecStrategy();

    public record BackupResult(boolean success, @Nullable String message) {}

    private EndlessInventoryData(){
        this.levelEndInvs = new ArrayList<>();
    }

    public static void init(ServerLevel level){
        if (!level.dimension().equals(Level.OVERWORLD)) {
            //LOGGER.warn("Skipped EndlessInventoryData initialization in dimension: {}", level.dimension().location());
            return; // 仅在主世界执行
        }
        Factory<EndlessInventoryData> factory = new Factory<>(EndlessInventoryData::create,EndlessInventoryData::load, DataFixTypes.HOTBAR);
        ServerLevelEndInv.levelEndInvData = level.getDataStorage().computeIfAbsent(factory,END_INV_LIST_KEY);

        LOGGER.info("Initialized EndlessInventoryData in {} with {} inventories", level.dimension().location(), ServerLevelEndInv.levelEndInvData.levelEndInvs.size());
    }

    public static BackupResult backup(ServerLevel level) {
        try {
            Path worldDir = level.getServer().getWorldPath(LevelResource.ROOT).normalize();
            Path dataFile = worldDir.resolve("data/endless_inventories.dat");

            if (!Files.exists(dataFile)) {
                throw new FileNotFoundException("Cannot find data file: " + dataFile);
            }

            // 创建备份文件夹
            Path backupDir = worldDir.resolve("endinv_backup");
            Files.createDirectories(backupDir);

            // 添加时间戳到备份文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backupFile = backupDir.resolve("endless_inventories_" + timestamp + ".dat");

            // 复制文件
            Files.copy(dataFile, backupFile, StandardCopyOption.REPLACE_EXISTING);

            return new BackupResult(true, backupFile.toString());
        } catch (IOException e) {
            return new BackupResult(false, e.getMessage());
        } catch (Exception e) {
            return new BackupResult(false, "Unexpected exception");
        }
    }

    public static EndlessInventoryData create(){
        return new EndlessInventoryData();
    }

    public void addEndInvToLevel(EndlessInventory endlessInventory){
        levelEndInvs.add(endlessInventory);
        setDirty();
    }

    public EndlessInventory byIndexRemove(int index){
        if(index<0 || index>= levelEndInvs.size()) return null;
        return levelEndInvs.remove(index);
    }

    public EndlessInventory fromUUID(UUID uuid){
        for(EndlessInventory endlessInventory : levelEndInvs){
            if (Objects.equals(endlessInventory.getUuid(),uuid)) return endlessInventory;
        }
        return null;
    }

    @Nullable
    public EndlessInventory fromIndex(int index){
        if(index<0 || index>= levelEndInvs.size()) return null;
        return levelEndInvs.get(index);
    }

    public int getIndex(EndlessInventory endlessInventory) {
        int index = 0;
        for(EndlessInventory endinv : levelEndInvs){
            if(Objects.equals(endlessInventory,endinv)) return index;
            index++;
        }
        return -1;
    }

    public static EndlessInventoryData load(final CompoundTag tag, HolderLookup.Provider lookupProvider){
        EndlessInventoryData data = create();
        //Get EndInv[]
        ListTag listTag = tag.getList(END_INV_LIST_KEY,10) ;

        CompoundTag head = listTag.getCompound(0);
        checkStrategy(head);

        //{}EndInv -> EndInvs -> levelEndInvs
        listTag.iterator().forEachRemaining(
                (t)->   data.levelEndInvs.add(LoadStrategy.deserializeEndInv((CompoundTag) t,lookupProvider))
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
            CompoundTag invTag = SaveStrategy.serializeEndInv(endlessInventory,provider);
            nbtTagList.add(invTag);
        }

        CompoundTag saveTag = new CompoundTag();
        //{HEAD}: endless_inventories: []
        saveTag.put(END_INV_LIST_KEY,nbtTagList);
        return saveTag;
    }
}
