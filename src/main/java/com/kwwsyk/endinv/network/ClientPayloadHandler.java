package com.kwwsyk.endinv.network;

import com.kwwsyk.endinv.client.config.ClientConfig;
import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.menu.page.ItemDisplay;
import com.kwwsyk.endinv.network.payloads.SetItemDisplayContentPayload;
import com.kwwsyk.endinv.network.payloads.SyncedConfig;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.kwwsyk.endinv.ModInitializer.SYNCED_CONFIG;

public abstract class ClientPayloadHandler {



    public static void handleEndInvSettings(SyncedConfig syncedConfig, IPayloadContext iPayloadContext){
        Player player = iPayloadContext.player();
        player.setData(SYNCED_CONFIG,syncedConfig);
        ClientConfig.CONFIG.ROWS.set(syncedConfig.rows());
    }

    public static void handleItemDisplay(SetItemDisplayContentPayload setItemDisplayContentPayload, IPayloadContext iPayloadContext) {
        Player player = iPayloadContext.player();
        if(player.containerMenu instanceof EndlessInventoryMenu menu && menu.getDisplayingPage() instanceof ItemDisplay itemDisplay){
            itemDisplay.initializeContents(setItemDisplayContentPayload.stacks());
        }
    }
}
