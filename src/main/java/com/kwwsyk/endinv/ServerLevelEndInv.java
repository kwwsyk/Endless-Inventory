package com.kwwsyk.endinv;

import com.kwwsyk.endinv.data.EndlessInventoryData;
import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.menu.page.pageManager.AttachingManager;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.kwwsyk.endinv.ModInitializer.DEFAULT_UUID;
import static com.kwwsyk.endinv.ModInitializer.ENDINV_UUID;

public final class ServerLevelEndInv {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Map<ServerPlayer, PageMetaDataManager> PAGE_META_DATA_MANAGER = new HashMap<>();

    public static EndlessInventoryData levelEndInvData;

    private ServerLevelEndInv(){}

    public static Optional<PageMetaDataManager> checkAndGetManagerForPlayer(ServerPlayer player){
        if(player.containerMenu instanceof EndlessInventoryMenu menu) return Optional.of(menu);
        if(PAGE_META_DATA_MANAGER.get(player) instanceof AttachingManager manager){
            if(manager.getMenu()!=player.containerMenu) return Optional.empty();
            return Optional.of(manager);
        }else return Optional.empty();
    }


    /**
     * Get player's EndInv, if player has not or has invalid, create new.
     * @param player the player
     * @return Player's EndInv, original or created
     */
    public static EndlessInventory getEndInvForPlayer(Player player){
        EndlessInventory endlessInventory;
        if(hasEndInvUuid(player)){
            endlessInventory = getPlayerDefaultEndInv(player);
            if(endlessInventory==null) endlessInventory = createForPlayer(player,player.getData(ENDINV_UUID));
        }else{
            endlessInventory = createForPlayer(player);

        }
        endlessInventory.viewers.add((ServerPlayer) player);
        return endlessInventory;
    }

    private static EndlessInventory createForPlayer(Player player){
        EndlessInventory endlessInventory = new EndlessInventory();
        levelEndInvData.addEndInvToLevel(endlessInventory);
        player.setData(ENDINV_UUID,endlessInventory.getUuid());
        return endlessInventory;
    }

    private static EndlessInventory createForPlayer(Player player, UUID uuid){
        EndlessInventory endlessInventory = new EndlessInventory(uuid);
        levelEndInvData.addEndInvToLevel(endlessInventory);
        return endlessInventory;
    }

    /**
     * Check whether player has a valid uuid, which is not {@link ModInitializer#DEFAULT_UUID}
     * @param player player to check endInv uuid
     * @return true if player has uuid and the uuid is valid.
     */
    public static boolean hasEndInvUuid(Player player){
        if(player.hasData(ENDINV_UUID)){
            if(player.getData(ENDINV_UUID)==DEFAULT_UUID){
                LOGGER.warn("Player {} has default endless inventory UUID.", player.getName().getString());
                return false;
            }
            return true;
        }else return false;
    }

    private static EndlessInventory getPlayerDefaultEndInv(Player player){
        return levelEndInvData.fromUUID(player.getData(ENDINV_UUID));
    }
}
