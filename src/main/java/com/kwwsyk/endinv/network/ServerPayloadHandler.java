package com.kwwsyk.endinv.network;

import com.kwwsyk.endinv.ServerLevelEndInv;
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
import java.util.Objects;
import java.util.Optional;

import static com.kwwsyk.endinv.ModInitializer.SYNCED_CONFIG;


public abstract class ServerPayloadHandler {
    private static final Logger LOGGER = LogUtils.getLogger();


    public static void handleEndInvSettings(SyncedConfig syncedConfig, IPayloadContext iPayloadContext){
        Player player = iPayloadContext.player();
        player.setData(SYNCED_CONFIG, syncedConfig);
    }

    public static void handleMenuPage(PageContext payload, IPayloadContext iPayloadContext){
        ServerPlayer serverPlayer = (ServerPlayer) iPayloadContext.player();
        var optional = ServerLevelEndInv.checkAndGetManagerForPlayer(serverPlayer);
        optional.ifPresent(manager -> syncPageContext(manager, payload, serverPlayer));
    }

    /**Synchronize server page context from context in payloads, if {@code serverPlayer} is not null,
     *  will send content back to client.
     * @param meta to change object who is holding page context.
     * @param context page context, as independent payload or included in other payloads
     * @param serverPlayer if not null, will send context to set client page contents.
     */
    private static void syncPageContext(PageMetaDataManager meta, PageContext context, @Nullable ServerPlayer serverPlayer){

        if(!Objects.equals(meta.getInPageContext(),context)) {

            int startIndex = context.startIndex();
            int length = context.length();

            SortType sortType = context.sortType();
            boolean reverseSort = context.pageData().reverseSort();
            String search = context.search();


            meta.setSortType(sortType);
            meta.setSortReversed(reverseSort);
            meta.setSearching(search);
            meta.getDisplayingPage().setChanged();
            if(!Objects.equals(context.pageData().pageType().value(),meta.getDisplayingPageType())) {
                meta.switchPageWithType(context.pageData().pageType().value());
            }else meta.getDisplayingPage().init(startIndex,length);
        }

        if(serverPlayer!=null){
            meta.getDisplayingPage().syncContentToClient(serverPlayer);
        }
    }
    public static void handlePageClick(PageClickPayload payload, IPayloadContext context){
        ServerPlayer player = (ServerPlayer) context.player();
        Optional<PageMetaDataManager> optional = ServerLevelEndInv.checkAndGetManagerForPlayer(player);
        if(player.containerMenu.containerId != payload.containerId() || optional.isEmpty()) return;
        PageMetaDataManager manager = optional.get();

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
        var optional = ServerLevelEndInv.checkAndGetManagerForPlayer(player);
        if(optional.isPresent()){
            PageMetaDataManager manager = optional.get();
            if(holdOn){
                manager.getDisplayingPage().setHoldOn();
            }else {
                manager.getDisplayingPage().release();
            }
        }
    }

    public static void handleEndInvOpening(OpenEndInvPayload openEndInvPayload, IPayloadContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        if(!player.getData(SYNCED_CONFIG).checkForAttaching()) return;
        if(player.containerMenu == player.inventoryMenu && openEndInvPayload.openNew()){
            player.openMenu(new SimpleMenuProvider(EndlessInventoryMenu::createServer, Component.empty()));
        }else if(!openEndInvPayload.openNew()){
            ServerLevelEndInv.getEndInvForPlayer(player).ifPresent(endInv->{
                AttachingManager manager = new AttachingManager(player.containerMenu, endInv ,player);
                ServerLevelEndInv.PAGE_META_DATA_MANAGER.put(player,manager);
                manager.sendEndInvData();
            });

        }

    }

    public static void handleItemDisplayItemMod(ItemDisplayItemModPayload payload, IPayloadContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        if(player.containerMenu.getCarried().isEmpty() && player.hasInfiniteMaterials()){
            var optional = ServerLevelEndInv.checkAndGetManagerForPlayer(player);
            if(optional.isEmpty()) return;
            PageMetaDataManager manager = optional.get();
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
        ServerLevelEndInv.getEndInvForPlayer(player).ifPresent(endInv->{
            if(starItemPayload.isAdding()) {
                endInv.affinities.addStarredItem(starItemPayload.stack());
            }else {
                endInv.affinities.removeStarredItem(starItemPayload.stack());
            }
        });
    }

    public static void handleQuickMovePage(QuickMoveToPagePayload payload, IPayloadContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        var optional = ServerLevelEndInv.checkAndGetManagerForPlayer(player);
        optional.ifPresent(manager -> {
            Slot slot = manager.getMenu().getSlot(payload.slotId());
            manager.slotQuickMoved(slot);
        });

    }
}
