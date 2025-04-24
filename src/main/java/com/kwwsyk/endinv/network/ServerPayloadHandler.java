package com.kwwsyk.endinv.network;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.menu.page.ItemDisplay;
import com.kwwsyk.endinv.menu.page.pageManager.AttachingManager;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.network.payloads.*;
import com.kwwsyk.endinv.options.ItemClassify;
import com.kwwsyk.endinv.util.SortType;
import com.mojang.logging.LogUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kwwsyk.endinv.ModInitializer.SYNCED_CONFIG;


public abstract class ServerPayloadHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Map<ServerPlayer, PageMetaDataManager> PAGE_META_DATA_MANAGER = new HashMap<>();

    private static PageMetaDataManager checkAndGetManagerForPlayer(ServerPlayer player){
        if(player.containerMenu instanceof EndlessInventoryMenu menu) return menu;
        if(PAGE_META_DATA_MANAGER.get(player) instanceof AttachingManager manager){
            if(manager.getMenu()!=player.containerMenu) return null;
            return manager;
        }else return null;
    }

    public static void handleEndInvSettings(SyncedConfig syncedConfig, IPayloadContext iPayloadContext){
        Player player = iPayloadContext.player();
        player.setData(SYNCED_CONFIG, syncedConfig);
    }

    public static void handleMenuPage(PageMetadata payload, IPayloadContext iPayloadContext){
        ServerPlayer serverPlayer = (ServerPlayer) iPayloadContext.player();
        PageMetaDataManager menu = checkAndGetManagerForPlayer(serverPlayer);
        if(menu!=null){
            EndlessInventory endInv = (EndlessInventory) menu.getSourceInventory();
            int startIndex = payload.startIndex();
            int length = payload.length();
            SortType sortType = payload.sortType();
            menu.switchPageWithId(payload.pageData().pageId());
            ItemClassify classify = menu.getDisplayingPage().getItemClassify().value();
            String search = payload.search();

            menu.setSortType(sortType);
            menu.setSortReversed(payload.pageData().reverseSort());
            menu.setSearching(search);

            List<ItemStack> view = endInv.getSortedAndFilteredItemView(startIndex,length,sortType,payload.pageData().reverseSort(),classify,search);

            NonNullList<ItemStack> stacks = NonNullList.withSize(length, ItemStack.EMPTY);
            for(int i=0;i< view.size();++i){
                stacks.set(i,view.get(i));//do not need copy as will be sent
            }
            menu.getDisplayingPage().setChanged();
            menu.getDisplayingPage().init(startIndex,length);
            PacketDistributor.sendToPlayer(serverPlayer,new SetItemDisplayContentPayload(stacks));
        }
    }
    public static void handlePageClick(PageClickPayload payload, IPayloadContext context){
        ServerPlayer player = (ServerPlayer) context.player();
        PageMetaDataManager manager = checkAndGetManagerForPlayer(player);
        if(player.containerMenu.containerId != payload.containerId() || manager==null) return;
        if(manager.getDisplayingPageId()!=payload.pageId()){
            LOGGER.warn("Different pages are displaying across server and client");
            manager.switchPageWithId(payload.pageId());
        }
        if(player.isSpectator()){
            manager.getMenu().sendAllDataToRemote();
            return;
        }else if(!manager.getMenu().stillValid(player)){
            LOGGER.debug("Player {} interacted with invalid menu {}", player, manager.getMenu());
            return;
        }
        manager.getMenu().suppressRemoteUpdates();
        manager.getDisplayingPage().pageClicked(payload.XOffset(),payload.YOffset(),payload.keyCode(),payload.clickType());
        if(manager.getDisplayingPage() instanceof ItemDisplay itemDisplay) itemDisplay.refreshItems();
        manager.getMenu().resumeRemoteUpdates();

        manager.getDisplayingPage().syncContentToClient(player);

        manager.getMenu().broadcastChanges();
    }
    public static void handlePageStates(PageStatePayload payload, IPayloadContext context){
        ServerPlayer player = (ServerPlayer) context.player();
        boolean holdOn = payload.holdOn();
        PageMetaDataManager manager = checkAndGetManagerForPlayer(player);
        if(manager!=null){
            if(holdOn){
                manager.getDisplayingPage().setHoldOn();
            }else {
                manager.getDisplayingPage().release();
            }
        }
    }

    public static void handleEndInvOpening(OpenEndInvPayload openEndInvPayload, IPayloadContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        if(!player.getData(SYNCED_CONFIG).attaching()) return;
        if(player.containerMenu == player.inventoryMenu && openEndInvPayload.openNew()){
            player.openMenu(new SimpleMenuProvider(EndlessInventoryMenu::createServer, Component.empty()));
        }else if(!openEndInvPayload.openNew()){
            AttachingManager manager = new AttachingManager(player.containerMenu, EndlessInventory.getEndInvForPlayer(player),player);
            PAGE_META_DATA_MANAGER.put(player,manager);
            manager.sendEndInvMetadataToRemote();
        }

    }

    public static void handleItemDisplayItemMod(ItemDisplayItemModPayload payload, IPayloadContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        if(player.containerMenu.getCarried().isEmpty() && player.hasInfiniteMaterials()){
            PageMetaDataManager manager = checkAndGetManagerForPlayer(player);
            if(manager==null) return;
            if(manager.getDisplayingPage() instanceof ItemDisplay itemDisplay){
                if(payload.isAdding()){
                    itemDisplay.addItem(payload.stack());
                }else {
                    ItemStack taken = itemDisplay.takeItem(payload.stack());
                    if(taken.isEmpty()) return;
                    player.containerMenu.setCarried(taken);
                }
            }
        }
    }
}
