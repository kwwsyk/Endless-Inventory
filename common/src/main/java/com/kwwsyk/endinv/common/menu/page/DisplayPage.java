package com.kwwsyk.endinv.common.menu.page;


import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.KeyMappings;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.client.gui.bg.ScreenBgRenderer;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

import static com.kwwsyk.endinv.common.client.ClientModInfo.inputHandler;
import static net.minecraft.client.gui.screens.Screen.hasShiftDown;

/**
 *
 */
public abstract class DisplayPage{


    protected final PageType pageType;
    //the registry name of this page type.
    public final String id;

    @Nullable
    protected final Predicate<ItemStack> itemClassify;

    public ResourceLocation icon = null;
    //displayed when hovering on Page switch bar.
    public Component name;

    public PageMetaDataManager meta;

    public final SourceInventory srcInv;

    protected final AbstractContainerMenu menu;

    protected final Minecraft mc;

    public ScreenFramework framework;
    //if holding on the page view shall not change temporarily.
    protected boolean holdOn = false;

    //constructor and initialization methods
    /**
     * Pages are constructed when {@link PageMetaDataManager#buildPages()} is invoked.
     * @param pageType defines type that is synced, including the item-classify.
     */
    public DisplayPage(PageType pageType,PageMetaDataManager metaDataManager){
        this.mc = Minecraft.getInstance();
        this.meta = metaDataManager;
        this.menu = metaDataManager.getMenu();
        this.srcInv = CachedSrcInv.INSTANCE;
        this.pageType = pageType;
        this.id = pageType.registerName;
        this.itemClassify = pageType.itemClassify;
        this.name = Component.translatable("page.endinv."+pageType.registerName);
    }

    public void init(ScreenFramework frameWork){
        this.framework = frameWork;
    }

    //abstract methods
    /**Page
     *  and content is built here
     * @param startIndex decide
     * @param length
     */
    public abstract void refreshContents(int startIndex, int length);

    /**
     * Controls page's scroll behavior.
     * @param pos influent new row index: {@link #getRowIndexForScroll(float)},{@link #getScrollForRowIndex(int)}
     */
    public abstract void scrollTo(float pos);

    /**Controls whether page will change view when mouse scrolled up and down
     * @return true if scroll can be performed
     */
    public abstract boolean canScroll();

    /**Invoked when some inner page fields changes.
     */
    public abstract void sendChangesToServer();

    public abstract boolean hasSearchBar();

    public abstract boolean hasSortTypeSwitchBar();

    /**Render page icon with page's {@link #icon}
     * icon can be an item location or sprite location with 18*18 size.
     */
    public ResourceLocation getIcon(){
        return icon;
    }

    @Nullable
    public Predicate<ItemStack> getClassify(){
        return itemClassify;
    }

    public int getRowIndexForScroll(float scrollOffs) {
        return Math.max((int)((double)(scrollOffs * (float)calculateRowCount()) + 0.5), 0);
    }

    public float getScrollForRowIndex(int rowIndex) {
        return Mth.clamp((float)rowIndex / (float)calculateRowCount(), 0.0F, 1.0F);
    }

    public int calculateRowCount(){
        return Math.max(meta.getItemSize()/ meta.getColumnCount(), CachedSrcInv.INSTANCE.getItemSize()/ meta.getColumnCount());
    }

    protected float subtractInputFromScroll(float scrollOffs, double input) {
        return Mth.clamp(scrollOffs - (float)(input / (double)meta.getRowCount()), 0.0F, 1.0F);
    }

    public void setChanged() {}


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
            holdOn = true;
        }
    }

    public void release(){
        if(holdOn){
            holdOn = false;
        }
    }

    public PageType getPageType() {
        return pageType;
    }

    //click handler

    /**Check if it has double-clicked on one object: item slot or other widgets.
     * @param clickInterval time interval of two clicks
     * @return true if it should be seen as double-clicked
     */
    public abstract boolean doubleClickedOnOne(double XOffset, double YOffset, double lastX, double lastY, long clickInterval);

    public abstract void pageClicked(double XOffset, double YOffset, int keyCode, ClickType clickType);

    public abstract void handleStarItem(double XOffset, double YOffset);

    /**Used to handle mouse clicked/dragged on page and page has slots
     * @param XOffset the relative X coordinate to page left pos
     * @param YOffset the relative Y coordinate to page top pos
     * @return the slot id in page or -1 with no slot.
     */
    public int getSlotForMouseOffset(double XOffset,double YOffset){
        return -1;
    }

    //page renderer
    public void renderBg(ScreenBgRenderer screenBgRenderer, GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        screenBgRenderer.getDefaultPageBgRenderer().ifPresent(bgRenderer -> bgRenderer.renderBg(guiGraphics, partialTick, mouseX, mouseY));
    }

    public abstract void renderPage(GuiGraphics graphics, int pageXPos, int pageYPos, ScreenFramework frameWork);

    public abstract void renderHovering(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    public void renderPageIcon(GuiGraphics graphics, int x, int y, float partialTick) {
        if(getIcon()==null) return;
        Optional<Item> optionalItem = BuiltInRegistries.ITEM.getOptional(getIcon());
        if (optionalItem.isPresent()) {
            ItemStack stack = new ItemStack(optionalItem.get());
            graphics.renderItem(stack,x,y);
            return;
        }
        try {
            graphics.blitSprite(getIcon(),x,y,18,18);
        }catch (Exception ignored){}
    }

    //page click handler

    private boolean doubleClick;
    private int lastClickedButton;
    private double lastCLickedX;
    private double lastClickedY;
    private long lastClickedTime;
    private boolean skipNextRelease;

    public boolean mouseClicked(double XOffset, double YOffset, int keyCode){
        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(keyCode);
        boolean isKeyPicking = inputHandler.isActiveAndMatches(mc.options.keyPickItem,mouseKey);//is mouse middle button and enabled for pickup or clone
        long clickTime = Util.getMillis();
        this.doubleClick = keyCode==lastClickedButton && doubleClickedOnOne(XOffset,YOffset,lastCLickedX,lastClickedY,clickTime-lastClickedTime);
        this.skipNextRelease = false;
        if(keyCode!=0&&keyCode!=1&&!isKeyPicking){
            checkHotBarClicked:
            if (this.menu.getCarried().isEmpty()) {
                if (this.mc.options.keySwapOffhand.matchesMouse(keyCode)) {
                    pageClicked(XOffset,YOffset,40, ClickType.SWAP);
                    break checkHotBarClicked;
                }

                for (int i = 0; i < 9; i++) {
                    if (this.mc.options.keyHotbarSlots[i].matchesMouse(keyCode)) {
                        pageClicked(XOffset,YOffset, i, ClickType.SWAP);
                    }
                }
            }
        }else {
            if(menu.getCarried().isEmpty()){
                if (inputHandler.isActiveAndMatches(mc.options.keyPickItem,mouseKey)) {
                    pageClicked(XOffset, YOffset, keyCode, ClickType.CLONE);
                } else {
                    ClickType clicktype = ClickType.PICKUP;
                    if (hasShiftDown()) {
                        setHoldOn();
                        //this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                        clicktype = ClickType.QUICK_MOVE;
                    }
                    pageClicked(XOffset, YOffset, keyCode, clicktype);
                }
                this.skipNextRelease = true;
            }else {//deference to vanilla
                pageClicked(XOffset, YOffset, keyCode, ClickType.PICKUP);
            }
        }
        this.lastClickedTime = clickTime;
        this.lastClickedButton = keyCode;
        this.lastCLickedX = XOffset;
        this.lastClickedY = YOffset;
        return true;
    }


    private int lastDraggedPageSlot = -1;

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY){
        if(hasShiftDown()){
            int slotId = getSlotForMouseOffset(mouseX,mouseY);
            if(slotId>=0 && lastDraggedPageSlot>=0 && slotId!=lastDraggedPageSlot){
                pageClicked(mouseX,mouseY,button,ClickType.QUICK_MOVE);
            }
            lastDraggedPageSlot = slotId;
            return true;
        }else return false;
    }

    public boolean mouseReleased(double XOffset, double YOffset, int keyCode){
        lastDraggedPageSlot = -1;
        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(keyCode);
        if (this.doubleClick) {
            this.pageClicked(XOffset,YOffset,keyCode,ClickType.PICKUP_ALL);
            this.doubleClick = false;
            this.lastClickedTime = 0L;
            return true;
        }else {
            //ignore quick craft
            if (this.skipNextRelease) {
                this.skipNextRelease = false;
                return true;
            }
            if(!menu.getCarried().isEmpty()){
                if (inputHandler.isActiveAndMatches(mc.options.keyPickItem,mouseKey)) {
                    this.pageClicked(XOffset,YOffset,keyCode,ClickType.CLONE);
                    return true;
                }
            }
        }
        return false;
    }

    protected float scrollOffs;

    public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY){
        if(!canScroll()) return false;
        this.scrollOffs = subtractInputFromScroll(this.scrollOffs,scrollY);
        scrollTo(scrollOffs);
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers, int mouseX, int mouseY){
        boolean isNumericKey = InputConstants.getKey(keyCode, scanCode).getNumericKeyValue().isPresent();

        if (isNumericKey && this.menu.getCarried().isEmpty()) {
            if (inputHandler.isActiveAndMatches(mc.options.keySwapOffhand,InputConstants.getKey(keyCode, scanCode))) {
                pageClicked(mouseX, mouseY, 40, ClickType.SWAP);
                return true;
            }

            for(int i = 0; i < 9; ++i) {
                if (inputHandler.isActiveAndMatches(mc.options.keyHotbarSlots[i],InputConstants.getKey(keyCode, scanCode))) {
                    pageClicked(mouseX, mouseY, i, ClickType.SWAP);
                    return true;
                }
            }
        }

        if(inputHandler.isActiveAndMatches(KeyMappings.STAR_ITEM,InputConstants.getKey(keyCode,scanCode))){
            meta.getDisplayingPage().handleStarItem(mouseX,mouseY);
            return true;
        }
        return false;
    }
}
