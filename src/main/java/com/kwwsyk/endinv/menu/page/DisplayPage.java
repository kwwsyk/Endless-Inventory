package com.kwwsyk.endinv.menu.page;


import com.kwwsyk.endinv.client.gui.page.PageClickHandler;
import com.kwwsyk.endinv.client.gui.page.PageRenderer;
import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.options.ItemClassify;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public abstract class DisplayPage implements PageRenderer, PageClickHandler {


    public EndlessInventoryMenu menu;
    public final int pageId;
    private final Holder<ItemClassify> itemClassify;
    public ResourceLocation icon = null;
    public int iconX = 18;
    public int iconY = 18;

    public DisplayPage(EndlessInventoryMenu menu, Holder<ItemClassify> itemClassify, int pageId){
        this.menu = menu;
        this.itemClassify = itemClassify;
        this.pageId = pageId;
    }
    /**Render page icon with page's {@link #icon}
     * icon can be an item location or sprite location with 18*18 size
     */
    @Override
    public void renderPageIcon(GuiGraphics graphics, int x, int y, float partialTick) {
        if(icon==null) return;
        Optional<Item> optionalItem = BuiltInRegistries.ITEM.getOptional(icon);
        if (optionalItem.isPresent()) {
            ItemStack stack = new ItemStack(optionalItem.get());
            graphics.renderItem(stack,x,y);
            return;
        }
        try {
            graphics.blitSprite(icon,x,y,iconX,iconY);
        }catch (Exception ignored){}
    }

    public Holder<ItemClassify> getItemClassify(){
        return itemClassify;
    }
    public abstract void scrollTo(float pos);
    public int getRowIndexForScroll(float scrollOffs) {
        return Math.max((int)((double)(scrollOffs * (float)this.calculateRowCount()) + 0.5), 0);
    }
    public float getScrollForRowIndex(int rowIndex) {
        return Mth.clamp((float)rowIndex / (float)this.calculateRowCount(), 0.0F, 1.0F);
    }
    public abstract int calculateRowCount();
    public abstract void setDisplay(int startIndex,int length);

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
    public abstract boolean canScroll();

    @FunctionalInterface
    public interface PageConstructor{
        DisplayPage create(EndlessInventoryMenu menu, Holder<ItemClassify> itemClassify, int pageIndex);
    }
}
