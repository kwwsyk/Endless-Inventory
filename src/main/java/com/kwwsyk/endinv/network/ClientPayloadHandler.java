package com.kwwsyk.endinv.network;

import com.kwwsyk.endinv.EndlessInventoryMenu;
import com.kwwsyk.endinv.client.LocalData;
import com.kwwsyk.endinv.network.payloads.EndInvSettings;
import com.kwwsyk.endinv.network.payloads.SetItemDisplayContentPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.kwwsyk.endinv.ModInitializer.ENDINV_SETTINGS;

public abstract class ClientPayloadHandler {



    public static void handleEndInvSettings(EndInvSettings endInvSettings, IPayloadContext iPayloadContext){

        LocalData.settings = endInvSettings;
    }

    public static void handleItemDisplay(SetItemDisplayContentPayload setItemDisplayContentPayload, IPayloadContext iPayloadContext) {
        Player player = iPayloadContext.player();
        if(player.containerMenu instanceof EndlessInventoryMenu menu){
            menu.getContainer().initializeContents(setItemDisplayContentPayload.stacks());
        }
    }
}
