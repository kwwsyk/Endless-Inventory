package com.kwwsyk.endinv.network;

import com.kwwsyk.endinv.client.gui.AttachedScreen;
import com.kwwsyk.endinv.events.ScreenAttachment;
import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.menu.page.ItemDisplay;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.network.payloads.EndInvMetadata;
import com.kwwsyk.endinv.network.payloads.SetItemDisplayContentPayload;
import com.kwwsyk.endinv.network.payloads.SyncedConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.kwwsyk.endinv.ModInitializer.SYNCED_CONFIG;

public abstract class ClientPayloadHandler {

    private static PageMetaDataManager checkAndGetManagerForPlayer(LocalPlayer player){
        if(player.containerMenu instanceof EndlessInventoryMenu menu) return menu;
        if(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen){
            AttachedScreen<?> attachedScreen = ScreenAttachment.ATTACHMENT_MANAGER.get(screen);
            if(attachedScreen==null) return null;
            return attachedScreen.getPageMetadata();
        }
        return null;
    }

    public static void handleEndInvSettings(SyncedConfig syncedConfig, IPayloadContext iPayloadContext){
        Player player = iPayloadContext.player();
        player.setData(SYNCED_CONFIG,syncedConfig);
        //ClientConfig.CONFIG.ROWS.set(syncedConfig.rows());
    }

    public static void handleItemDisplay(SetItemDisplayContentPayload setItemDisplayContentPayload, IPayloadContext iPayloadContext) {
        Player player = iPayloadContext.player();
        PageMetaDataManager menu = checkAndGetManagerForPlayer((LocalPlayer) player);
        if(menu!=null && menu.getDisplayingPage() instanceof ItemDisplay itemDisplay){
            itemDisplay.initializeContents(setItemDisplayContentPayload.stacks());
        }
    }

    public static void handleEndInvMetaData(EndInvMetadata endInvMetadata, IPayloadContext iPayloadContext) {

        Screen screen = Minecraft.getInstance().screen;
        if(!(screen instanceof AbstractContainerScreen<?>)) return;
        AttachedScreen<?> attachedScreen = ScreenAttachment.ATTACHMENT_MANAGER.get(screen);
        if(attachedScreen==null) return;
        attachedScreen.setEndInvMetadata(endInvMetadata);

    }
}
