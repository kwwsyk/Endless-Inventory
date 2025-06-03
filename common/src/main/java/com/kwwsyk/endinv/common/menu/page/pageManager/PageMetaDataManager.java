package com.kwwsyk.endinv.common.menu.page.pageManager;

import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.menu.page.DisplayPage;
import com.kwwsyk.endinv.common.menu.page.ItemPage;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.menu.page.PageTypeRegistry;
import com.kwwsyk.endinv.common.network.payloads.PageData;
import com.kwwsyk.endinv.common.network.payloads.toServer.PageContext;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public interface PageMetaDataManager {

    List<Holder<PageType>> defaultPages = new ArrayList<>();

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

    void sendEndInvData();

    default void scrollTo(float pos){
        getDisplayingPage().scrollTo(pos);
    }

    default float subtractInputFromScroll(float scrollOffs, double input) {
        return Mth.clamp(scrollOffs - (float)(input / (double)getRowCount()), 0.0F, 1.0F);
    }

    default int getDisplayingPageIndex(){
        for(int i=0; i<getPages().size(); ++i){
            if(getPages().get(i)==getDisplayingPage()){
                return i;
            }
        }
        return -1;
    }

    default void switchPageWithId(String id){
        for(int i=0; i<getPages().size(); ++i){
            if(Objects.equals(getPages().get(i).id,id)){
                switchPageWithIndex(i);
            }
        }
    }

    /**
     * the return value of {@link #getPages()} shall be from this.
     */
    default List<DisplayPage> buildPages(){
        return PageTypeRegistry.getDisplayPages().stream().map(type -> type.buildPage(this)).toList();
    }

    default void slotQuickMoved(Slot clicked) {
        ItemStack itemStack = clicked.getItem();
        ItemStack remain = getDisplayingPage().tryQuickMoveStackTo(itemStack);
        clicked.setByPlayer(remain);
        clicked.onTake(getPlayer(), itemStack);
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
        return new PageData(getDisplayingPageId(), getRowCount(),getColumnCount(),sortType(),isSortReversed(),searching());
    }

    default String getDisplayingPageId(){
        return getDisplayingPage().id;
    }

    default PageType getDisplayingPageType(){
        return getDisplayingPage().getPageType();
    }

}
