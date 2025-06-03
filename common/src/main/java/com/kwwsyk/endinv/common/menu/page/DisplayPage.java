package com.kwwsyk.endinv.common.menu.page;


import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.page.PageClickHandler;
import com.kwwsyk.endinv.common.client.gui.page.PageRenderer;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.network.payloads.toServer.PageStatePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public abstract class DisplayPage{


    private final PageType pageType;

    public final String id;

    public PageMetaDataManager metadata;

    public final SourceInventory srcInv;

    @Nullable
    private final Predicate<ItemStack> itemClassify;

    public ResourceLocation icon = null;

    public Component name;

    protected boolean holdOn = false;//if holding on the page view shall not change temporarily.

    public DisplayPage(PageType pageType,PageMetaDataManager metaDataManager){
        this.metadata = metaDataManager;
        this.srcInv = metaDataManager.getSourceInventory().isRemote() ? CachedSrcInv.INSTANCE : metaDataManager.getSourceInventory();
        this.pageType = pageType;
        this.id = pageType.registerName;
        this.itemClassify = pageType.itemClassify;
        this.name = Component.translatable("page.endinv."+pageType.registerName);
    }

    /**Render page icon with page's {@link #icon}
     * icon can be an item location or sprite location with 18*18 size
     */

    public ResourceLocation getIcon(){
        return icon;
    }

    @Nullable
    public Predicate<ItemStack> getClassify(){
        return itemClassify;
    }

    public abstract void scrollTo(float pos);

    public int getRowIndexForScroll(float scrollOffs) {
        return Math.max((int)((double)(scrollOffs * (float)calculateRowCount()) + 0.5), 0);
    }

    public float getScrollForRowIndex(int rowIndex) {
        return Mth.clamp((float)rowIndex / (float)calculateRowCount(), 0.0F, 1.0F);
    }

    public int calculateRowCount(){
        return Math.max(metadata.getItemSize()/ metadata.getColumnCount(), CachedSrcInv.INSTANCE.getItemSize()/metadata.getColumnCount());
    }

    public abstract void init(int startIndex, int length);

    public void setChanged() {
    }

    public abstract void syncContentToServer();

    public abstract void syncContentToClient(ServerPlayer player);

    public ItemStack tryQuickMoveStackTo(ItemStack stack){
        if(!srcInv.isRemote()){
            return srcInv.addItem(stack);
        }
        return stack.copy();
    }

    public ItemStack tryExtractItem(ItemStack item, int count){
        return ItemStack.EMPTY;
    }

    public void setHoldOn(){
        if(!holdOn){
            if(srcInv.isRemote())
                ModInfo.getPacketDistributor().sendToServer(new PageStatePayload(true));
            holdOn = true;
        }
    }

    public void release(){
        if(holdOn){
            if(srcInv.isRemote())
                ModInfo.getPacketDistributor().sendToServer(new PageStatePayload(false));
            holdOn = false;
        }
    }

    public abstract void handleStarItem(double XOffset, double YOffset);

    public abstract boolean canScroll();

    public PageType getPageType() {
        return pageType;
    }

    public abstract PageRenderer getPageRenderer();

    public abstract PageClickHandler getPageClickHandler();

    public abstract boolean hasSearchBar();

    public abstract boolean hasSortTypeSwitchBar();

    public abstract void pageClicked(double v, double v1, int i, ClickType clickType);
}
