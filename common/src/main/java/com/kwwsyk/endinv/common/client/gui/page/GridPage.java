package com.kwwsyk.endinv.common.client.gui.page;

import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.client.gui.page.slotView.PageViewContainer;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.util.ItemKey;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class GridPage extends DisplayPage {//todo support item and fluid types


    protected int startIndex = 0;
    protected int length;

    public GridPage(PageType pageType, ScreenFramework framework) {
        super(pageType, framework);
        this.length = this.framework.rows()* this.framework.columns();
    }

    protected PageViewContainer buildView(List<ItemKey> items){
        return new PageViewContainer(
                this,
                items,
                framework.rows(),
                framework.columns(),
                getPageLeft() + MARGIN_SIDE_WIDTH,
                getPageTop() + MARGIN_TOP_HEIGHT
        );
    }

    /**Build or reload item page's content by SourceInv {@link CachedSrcInv}
     *  and it will set back to unscrolled status.
     *  <P>
     *  Invoked by {@link ScreenFramework#switchPageWithIndex(int)}, as is when the page prepares to be displayed.
     */
    @Override
    public void initializeContents() {
        setVisibleRange(0, framework.rows()* framework.columns());
    }

    @Override
    public void scrollTo(float pos) {
        int startIndex = getRowIndexForScroll(pos)* framework.columns();
        this.setVisibleRange(startIndex,this.length);
    }

    protected abstract void setVisibleRange(int startIndex, int length);

    public int getStartIndex(){
        return this.startIndex;
    }

    public int getSlotByMouseOffset(double XOffset, double YOffset){
        XOffset = XOffset - MARGIN_SIDE_WIDTH; YOffset = YOffset - MARGIN_TOP_HEIGHT;
        if(XOffset<0||YOffset<0||XOffset>18* framework.columns()||YOffset>18* framework.rows()) return -1;
        return (int)XOffset/18 + framework.columns()*((int)YOffset/18);
    }

    /**
     * Get an area of one independent interactable area, mainly one item slot.
     * Can be used to mark one clickable area with other mods, such as Jei's register {@code getClickableIngredientUnderMouse}
     * @param XOffset mouseX-pageX
     * @param YOffset mouseY-pageY
     * @return one interactable area
     */
    @Override @Nullable
    public Rect2i getOneInteractableArea(double XOffset, double YOffset){
        return getSlotArea(getSlotByMouseOffset(XOffset, YOffset));
    }

    /**
     * Get area of one slot by the index of slot
     * @param slot index of slot/item, often use {@link #getSlotByMouseOffset(double, double)} to get
     * @return one slot area
     */
    @Nullable
    public Rect2i getSlotArea(int slot){
        if(slot<0) return null;
        final int slotSize = 18;
        final int rowAt = slot / framework.columns();
        final int columnAt = slot % framework.columns();
        return new Rect2i(
                leftPos+slotSize*columnAt,
                topPos+slotSize*rowAt,
                slotSize,
                slotSize)
                ;
    }

    @Override
    public boolean doubleClickedOnOne(double XOffset, double YOffset, double lastX, double lastY, long clickInterval) {
        return clickedInOneSlot(XOffset, YOffset,lastX,lastY) && clickInterval<=250;
    }

    protected boolean clickedInOneSlot(double XOffset, double YOffset, double lastX, double lastY) {
        return (int)XOffset/18==(int)lastX/18 && (int)YOffset/18 == (int)lastY/18;
    }

    protected boolean isHiddenBySortBox(int rowIndex, int columnIndex){
        return rowIndex<=2 && columnIndex<=3 && Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
                && (
                screen instanceof EndlessInventoryScreen EIS && EIS.getFrameWork().sortTypeSwitchBox.isOpen() && columnIndex <=2
                        || framework.sortTypeSwitchBox.isOpen()
                );
    }

    public void renderEmpty(GuiGraphicsExtractor GuiGraphicsExtractor, int x, int y, ItemStack itemStack){
        ItemStack toRender = itemStack.copyWithCount(1);
        GuiGraphicsExtractor.item(toRender,x,y,0);
        GuiGraphicsExtractor.itemDecorations(Minecraft.getInstance().font,toRender,x,y, ChatFormatting.RED + "0");
    }

}
