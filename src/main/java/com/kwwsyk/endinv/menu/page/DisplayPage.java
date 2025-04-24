package com.kwwsyk.endinv.menu.page;


import com.kwwsyk.endinv.SourceInventory;
import com.kwwsyk.endinv.client.gui.page.PageClickHandler;
import com.kwwsyk.endinv.client.gui.page.PageRenderer;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.network.payloads.PageStatePayload;
import com.kwwsyk.endinv.options.ItemClassify;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public abstract class DisplayPage implements PageRenderer, PageClickHandler {


    public PageMetaDataManager metadata;
    public final SourceInventory srcInv;
    public final int pageId;
    private final Holder<ItemClassify> itemClassify;
    public ResourceLocation icon = null;
    protected boolean holdOn = false;
    public DisplayPage(PageMetaDataManager metaDataManager, Holder<ItemClassify> itemClassify, int pageId){
        this.metadata = metaDataManager;
        this.srcInv = metaDataManager.getSourceInventory().isRemote() ? REMOTE : metaDataManager.getSourceInventory();
        this.itemClassify = itemClassify;
        this.pageId = pageId;
    }

    /**Render page icon with page's {@link #icon}
     * icon can be an item location or sprite location with 18*18 size
     */
    @Override
    public ResourceLocation getIcon(){
        return icon;
    }

    public Holder<ItemClassify> getItemClassify(){
        return itemClassify;
    }
    public abstract void scrollTo(float pos);
    public int getRowIndexForScroll(float scrollOffs) {
        return Math.max((int)((double)(scrollOffs * (float)metadata.getRowCount()) + 0.5), 0);
    }
    public float getScrollForRowIndex(int rowIndex) {
        return Mth.clamp((float)rowIndex / (float)metadata.getRowCount(), 0.0F, 1.0F);
    }
    public abstract void init(int startIndex, int length);

    public void setChanged() {
    }
    public void syncContentToServer(){

    }
    public void syncContentToClient(ServerPlayer player){

    }
    public ItemStack tryQuickMoveStackTo(ItemStack stack){
        return stack.copy();
    }
    public ItemStack tryExtractItem(ItemStack item, int count){
        return ItemStack.EMPTY;
    }
    public void setHoldOn(){
        if(!holdOn){
            if(srcInv.isRemote())
                PacketDistributor.sendToServer(new PageStatePayload(true));
            holdOn = true;
        }
    }
    public void release(){
        if(holdOn){
            if(srcInv.isRemote())
                PacketDistributor.sendToServer(new PageStatePayload(false));
            holdOn = false;
        }
    }
    public abstract boolean canScroll();

    @FunctionalInterface
    public interface PageConstructor{
        DisplayPage create(PageMetaDataManager metaDataManager, Holder<ItemClassify> itemClassify, int pageIndex);
    }

    protected final SourceInventory REMOTE = new SourceInventory() {
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
            DisplayPage.this.setChanged();

        }
    };
}
