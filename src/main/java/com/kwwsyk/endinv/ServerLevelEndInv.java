package com.kwwsyk.endinv;

import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.menu.page.pageManager.AttachingManager;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServerLevelEndInv {
    public static final Map<ServerPlayer, PageMetaDataManager> PAGE_META_DATA_MANAGER = new HashMap<>();

    public static Optional<PageMetaDataManager> checkAndGetManagerForPlayer(ServerPlayer player){
        if(player.containerMenu instanceof EndlessInventoryMenu menu) return Optional.of(menu);
        if(PAGE_META_DATA_MANAGER.get(player) instanceof AttachingManager manager){
            if(manager.getMenu()!=player.containerMenu) return Optional.empty();
            return Optional.of(manager);
        }else return Optional.empty();
    }
}
