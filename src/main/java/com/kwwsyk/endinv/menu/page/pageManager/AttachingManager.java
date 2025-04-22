package com.kwwsyk.endinv.menu.page.pageManager;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.SourceInventory;
import com.kwwsyk.endinv.menu.page.DefaultPages;
import com.kwwsyk.endinv.menu.page.DisplayPage;
import com.kwwsyk.endinv.network.payloads.EndInvMetadata;
import com.kwwsyk.endinv.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.options.ItemClassify;
import com.kwwsyk.endinv.options.SortType;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

import static com.kwwsyk.endinv.ModInitializer.SYNCED_CONFIG;

public class AttachingManager implements PageMetaDataManager{

    private final AbstractContainerMenu menu;
    private final EndlessInventory endinv;
    private final ServerPlayer player;
    private DisplayPage displayingPage;
    //client for page switch
    private int displayingPageIndex;
    public final List<DisplayPage> pages;
    private final PageQuickMoveHandler quickMoveHandler;
    public SortType sortType;
    public String searching;
    private int rows;
    public AttachingManager(AbstractContainerMenu menu, EndlessInventory endinv, ServerPlayer player){
        this.menu = menu;
        this.endinv = endinv;
        this.player = player;
        this.pages = buildPages();
        SyncedConfig config = player.getData(SYNCED_CONFIG);
        init(config);
        this.quickMoveHandler = new PageQuickMoveHandler(this);
    }
    private void init(int rows, SortType sortType, String searching, int pageId){
        this.rows = rows;
        this.sortType = sortType;
        this.searching = searching;
        this.switchPageWithId(pageId);
    }
    private void init(SyncedConfig config){
        init(config.rows(),config.sortType(),config.search(),config.pageId());
    }

    private List<DisplayPage> buildPages(){
        List<DisplayPage> ret = new ArrayList<>();
        for(int i = 0; i< ItemClassify.DEFAULT_CLASSIFIES.size(); ++i){
            Holder<ItemClassify> classify = ItemClassify.DEFAULT_CLASSIFIES.get(i);

            DisplayPage page = DefaultPages.CLASSIFY2PAGE.get(classify).create(this, classify, i);
            ret.add(page);
        }
        return ret;
    }

    @Override
    public AbstractContainerMenu getMenu() {
        return menu;
    }

    @Override
    public SourceInventory getSourceInventory() {
        return endinv;
    }

    @Override
    public List<DisplayPage> getPages() {
        return pages;
    }

    @Override
    public DisplayPage getDisplayingPage() {
        return displayingPage;
    }

    @Override
    public void switchPageWithIndex(int index) {
        this.displayingPageIndex = index;
        this.displayingPage = pages.get(index);
        this.displayingPage.init(0,9*rows);
    }

    @Override
    public int getRowCount() {
        return rows;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public int getItemSize() {
        return endinv.getItemSize();
    }

    @Override
    public int getMaxStackSize() {
        return endinv.getMaxItemStackSize();
    }

    @Override
    public boolean enableInfinity() {
        return endinv.isInfinityMode();
    }

    @Override
    public ItemStack quickMoveFromPage(ItemStack stack) {
        return this.quickMoveHandler.quickMoveFromPage(stack);
    }

    @Override
    public SortType sortType() {
        return sortType;
    }

    @Override
    public void setSortType(SortType sortType) {
        this.sortType = sortType;
    }

    @Override
    public String searching() {
        return searching;
    }

    @Override
    public void setSearching(String searching) {
        this.searching= searching;
    }

    @Override
    public void sendEndInvMetadataToRemote() {
        PacketDistributor.sendToPlayer( player,new EndInvMetadata(endinv.getItemSize(),endinv.getMaxItemStackSize(),endinv.isInfinityMode()));
    }

    @Override
    public EndInvMetadata getEndInvMetadata() {
        return new EndInvMetadata(endinv.getItemSize(),endinv.getMaxItemStackSize(),endinv.isInfinityMode());
    }


    @Override
    public int getDisplayingPageIndex() {
        return displayingPageIndex;
    }
}
