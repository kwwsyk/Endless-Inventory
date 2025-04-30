package com.kwwsyk.endinv.network;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.menu.page.ItemPage;
import com.kwwsyk.endinv.menu.page.pageManager.AttachingManager;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.network.payloads.toServer.OpenEndInvPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.PageContext;
import com.kwwsyk.endinv.network.payloads.toServer.page.StarItemPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.op.ItemDisplayItemModPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.op.PageClickPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.op.PageStatePayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.op.QuickMoveToPagePayload;
import com.kwwsyk.endinv.util.SortType;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
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

    public static void handleMenuPage(PageContext payload, IPayloadContext iPayloadContext){
        ServerPlayer serverPlayer = (ServerPlayer) iPayloadContext.player();
        PageMetaDataManager menu = checkAndGetManagerForPlayer(serverPlayer);
        if(menu!=null){
            syncPageContext(menu,payload,serverPlayer);
        }
    }

    /**Synchronize server page context from context in payloads, if {@code serverPlayer} is not null,
     *  will send content back to client.
     * @param meta to change object who is holding page context.
     * @param context page context, as independent payload or included in other payloads
     * @param serverPlayer if not null, will send context to set client page contents.
     */
    private static void syncPageContext(PageMetaDataManager meta, PageContext context, @Nullable ServerPlayer serverPlayer){
        int startIndex = context.startIndex();
        int length = context.length();

        SortType sortType = context.sortType();
        boolean reverseSort = context.pageData().reverseSort();
        String search = context.search();

        meta.switchPageWithId(context.pageData().pageId());
        meta.setSortType(sortType);
        meta.setSortReversed(reverseSort);
        meta.setSearching(search);
        meta.getDisplayingPage().setChanged();
        meta.getDisplayingPage().init(startIndex,length);

        if(serverPlayer!=null){
            meta.getDisplayingPage().syncContentToClient(serverPlayer);
        }
    }
    public static void handlePageClick(PageClickPayload payload, IPayloadContext context){
        ServerPlayer player = (ServerPlayer) context.player();
        PageMetaDataManager manager = checkAndGetManagerForPlayer(player);
        if(player.containerMenu.containerId != payload.containerId() || manager==null) return;

        syncPageContext(manager,payload.context(),null);

        if(player.isSpectator()){
            manager.getMenu().sendAllDataToRemote();
            return;
        }else if(!manager.getMenu().stillValid(player)){
            LOGGER.debug("Player {} interacted with invalid menu {}", player, manager.getMenu());
            return;
        }
        manager.getMenu().suppressRemoteUpdates();
        manager.getDisplayingPage().pageClicked(payload.XOffset(),payload.YOffset(),payload.keyCode(),payload.clickType());
        if(manager.getDisplayingPage() instanceof ItemPage itemPage) itemPage.refreshItems();
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
            if(manager.getDisplayingPage() instanceof ItemPage itemPage){
                if(payload.isAdding()){
                    itemPage.addItem(payload.stack());
                }else {
                    ItemStack taken = itemPage.takeItem(payload.stack());
                    if(taken.isEmpty()) return;
                    player.containerMenu.setCarried(taken);
                }
            }
        }
    }

    public static void handleItemStarred(StarItemPayload starItemPayload, IPayloadContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        if(starItemPayload.isAdding()) {
            EndlessInventory.getEndInvForPlayer(player).affinities.addStarredItem(starItemPayload.stack());
        }else {
            EndlessInventory.getEndInvForPlayer(player).affinities.removeStarredItem(starItemPayload.stack());
        }

    }

    public static void handleQuickMovePage(QuickMoveToPagePayload payload, IPayloadContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        PageMetaDataManager manager = checkAndGetManagerForPlayer(player);
        if(manager==null) return;
        Slot slot = manager.getMenu().getSlot(payload.slotId());
        manager.slotQuickMoved(slot);
    }
}
