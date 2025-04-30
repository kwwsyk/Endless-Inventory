package com.kwwsyk.endinv.menu.page.pageManager;

import com.kwwsyk.endinv.SourceInventory;
import com.kwwsyk.endinv.menu.page.DisplayPage;
import com.kwwsyk.endinv.menu.page.ItemPage;
import com.kwwsyk.endinv.network.payloads.PageData;
import com.kwwsyk.endinv.network.payloads.toClient.EndInvMetadata;
import com.kwwsyk.endinv.network.payloads.toServer.page.PageContext;
import com.kwwsyk.endinv.util.SortType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface PageMetaDataManager {

    AbstractContainerMenu getMenu();
    SourceInventory getSourceInventory();
    List<DisplayPage> getPages();
    DisplayPage getDisplayingPage();
    void switchPageWithIndex(int index);
    int getRowCount();
    int getColumnCount();
    Player getPlayer();
    int getItemSize();
    int getMaxStackSize();
    boolean enableInfinity();
    ItemStack quickMoveFromPage(ItemStack stack);
    SortType sortType();
    void setSortType(SortType sortType);
    boolean isSortReversed();
    void switchSortReversed();
    void setSortReversed(boolean reversed);
    String searching();
    void setSearching(String searching);
    void sendEndInvMetadataToRemote();
    EndInvMetadata getEndInvMetadata();
    default void switchPageWithId(int id){
        for(int i=0; i<getPages().size(); ++i){
            if(getPages().get(i).pageId == id){
                switchPageWithIndex(i);
            }
        }
    }
    default void scrollTo(float pos){
        getDisplayingPage().scrollTo(pos);
    }
    default float subtractInputFromScroll(float scrollOffs, double input) {
        return Mth.clamp(scrollOffs - (float)(input / (double)getRowCount()), 0.0F, 1.0F);
    }
    default int getDisplayingPageId(){
        return getDisplayingPage().pageId;
    }
    default int getDisplayingPageIndex(){
        for(int i=0; i<getPages().size(); ++i){
            if(getPages().get(i)==getDisplayingPage()){
                return i;
            }
        }
        return -1;
    }
    default void slotQuickMoved(Slot clicked) {
        ItemStack itemStack = clicked.getItem();
        ItemStack remain = getDisplayingPage().tryQuickMoveStackTo(itemStack);
        clicked.set(remain);
    }
    default PageContext getInPageContext(){
        DisplayPage page = getDisplayingPage();
        return new PageContext(
                page instanceof ItemPage itemPage ? itemPage.getStartIndex() : 0,
                getRowCount()*getColumnCount(),
                getPageData()
        );
    }
    default PageData getPageData(){
        return new PageData(getRowCount(),getColumnCount(),getDisplayingPageId(),sortType(),isSortReversed(),searching());
    }
    SourceInventory REMOTE = new SourceInventory() {
        public ItemStack getItem(int i) {
            return ItemStack.EMPTY;
        }

        public int getItemSize() {
            return 0;
        }

        @Override
        public boolean isRemote() {
            return true;
        }

        @Override
        public ItemStack takeItem(ItemStack itemStack) {
            setChanged();
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack takeItem(ItemStack itemStack, int count) {
            setChanged();
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack addItem(ItemStack itemStack) {
            setChanged();
            return ItemStack.EMPTY;
        }

        @Override
        public void setChanged() {

        }
    };


}
