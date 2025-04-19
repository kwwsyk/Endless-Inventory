package com.kwwsyk.endinv.network;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.menu.page.ItemDisplay;
import com.kwwsyk.endinv.network.payloads.*;
import com.kwwsyk.endinv.options.ItemClassify;
import com.kwwsyk.endinv.options.SortType;
import com.mojang.logging.LogUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.util.List;

import static com.kwwsyk.endinv.ModInitializer.SYNCED_CONFIG;


public abstract class ServerPayloadHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void handleEndInvSettings(SyncedConfig syncedConfig, IPayloadContext iPayloadContext){
        Player player = iPayloadContext.player();
        player.setData(SYNCED_CONFIG, syncedConfig);
    }

    public static void handleEndInvRequests(PageChangePayload payload, IPayloadContext iPayloadContext){
        ServerPlayer serverPlayer = (ServerPlayer) iPayloadContext.player();
        if(serverPlayer.containerMenu instanceof EndlessInventoryMenu menu){
            EndlessInventory endInv = (EndlessInventory) menu.getSourceInventory();
            int startIndex = payload.startIndex();
            int length = payload.length();
            SortType sortType = payload.sortType();
            ItemClassify classify = payload.classify().value();
            String search = payload.search();

            menu.sortType = sortType;
            menu.searching = search;

            List<ItemStack> view = endInv.getSortedAndFilteredItemView(startIndex,length,sortType,classify,search);

            NonNullList<ItemStack> stacks = NonNullList.withSize(length, ItemStack.EMPTY);
            for(int i=0;i< view.size();++i){
                stacks.set(i,view.get(i));//do not need copy as will be sent
            }
            menu.getDisplayingPage().setChanged();
            menu.getDisplayingPage().setDisplay(startIndex,length);
            PacketDistributor.sendToPlayer(serverPlayer,new SetItemDisplayContentPayload(stacks));
        }
    }
    public static void handlePageClick(PageClickPayload payload, IPayloadContext context){
        ServerPlayer player = (ServerPlayer) context.player();
        if(player.containerMenu.containerId != payload.containerId() ||
                !(player.containerMenu instanceof EndlessInventoryMenu EIM)) return;
        if(EIM.getDisplayingPageId()!=payload.pageId()){
            LOGGER.warn("Different pages are displaying across server and client EIM");
            EIM.switchPageWithId(payload.pageId());
        }
        if(player.isSpectator()){
            EIM.sendAllDataToRemote();
            return;
        }else if(!EIM.stillValid(player)){
            LOGGER.debug("Player {} interacted with invalid menu {}", player, EIM);
            return;
        }
        EIM.suppressRemoteUpdates();
        EIM.getDisplayingPage().pageClicked(payload.XOffset(),payload.YOffset(),payload.keyCode(),payload.clickType());
        if(EIM.getDisplayingPage() instanceof ItemDisplay itemDisplay) itemDisplay.refreshItems();
        EIM.resumeRemoteUpdates();

        EIM.getDisplayingPage().syncContentToClient(player);

        EIM.broadcastChanges();
    }
    public static void handlePageStates(PageStatePayload payload, IPayloadContext context){
        Player player = context.player();
        boolean holdOn = payload.holdOn();
        if(player.containerMenu instanceof EndlessInventoryMenu EIM){
            if(holdOn){
                EIM.getDisplayingPage().setHoldOn();
            }else {
                EIM.getDisplayingPage().release();
            }
        }
    }
}
