package com.kwwsyk.endinv.client.gui;

import com.kwwsyk.endinv.client.config.ClientConfig;
import com.kwwsyk.endinv.client.gui.bg.FromResource;
import com.kwwsyk.endinv.client.gui.bg.ScreenLayoutMode;
import com.kwwsyk.endinv.client.gui.bg.ScreenRectangleWidgetParam;
import com.kwwsyk.endinv.client.gui.bg.ScreenTextureMode;
import com.kwwsyk.endinv.client.gui.widget.SortTypeSwitchBox;
import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.menu.page.DisplayPage;
import com.kwwsyk.endinv.menu.page.ItemDisplay;
import com.kwwsyk.endinv.network.payloads.PageClickPayload;
import com.kwwsyk.endinv.options.SortType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EndlessInventoryScreen extends AbstractContainerScreen<EndlessInventoryMenu> {

    private int containerRows;
    private float scrollOffs = 0;
    private final ClientConfig clientConfig;
    public Button configButton;
    public EditBox searchBox;
    private ScreenLayoutMode screenLayoutMode;
    private ScreenRectangleWidgetParam configButtonParam;
    private ScreenRectangleWidgetParam pageSwitchTabParam;
    private ScreenRectangleWidgetParam searchBoxParam;
    private ScreenRectangleWidgetParam sortTypeSwitchBoxParam;
    private ScreenTextureMode screenTextureMode;
    private int pageX;
    private int pageY;
    private int pageXSize;
    private int pageYSize;
    private double lastCLickedX;
    private double lastClickedY;
    private long lastClickedTime;
    private int lastClickedButton;
    private boolean doubleClick;
    private boolean skipNextRelease;
    private boolean ignoreTextInput;
    private boolean isHoveringOnPage;
    public boolean isHoveringOnSortBox;
    public boolean isHoveringOnPageSwitchTab;
    private int roughMouseX;
    private int roughMouseY;
    private SortTypeSwitchBox sortTypeSwitchBox;

    public EndlessInventoryScreen(EndlessInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        this.clientConfig = ClientConfig.CONFIG;

        this.refresh();
    }

    protected void setScreenTextureAndLayoutMode(){
        if(this.clientConfig.LAYOUT.getAsInt()>0){
            this.leftPos=0;
            this.topPos=0;
            this.screenLayoutMode = new ScreenLayoutMode(0,0,true,
                    (this.width - this.imageWidth) / 2,(this.height - this.imageHeight) / 2);
            this.configButtonParam = new ScreenRectangleWidgetParam(0,0,18,18);
            this.pageSwitchTabParam = new ScreenRectangleWidgetParam(0,18,18,18);
            this.searchBoxParam = new ScreenRectangleWidgetParam(this.leftPos + 116, this.topPos + 6, 80, 9);
            //this.screenTextureMode =
        }else {
            this.leftPos = (this.width - this.imageWidth) / 2;
            this.topPos = (this.height - this.imageHeight) / 2;
            this.configButtonParam = new ScreenRectangleWidgetParam(this.leftPos-18,this.topPos-18,18,18);
            this.searchBoxParam = new ScreenRectangleWidgetParam(this.leftPos + 89, this.topPos + 5, 80, 12);
            this.sortTypeSwitchBoxParam = new ScreenRectangleWidgetParam(this.leftPos+8,topPos+5,60,12);
            this.screenTextureMode = FromResource.createDefaultMode(this);
        }
    }

    public void init(){
        setScreenTextureAndLayoutMode();
        this.screenTextureMode.init();
        pageX = screenTextureMode.screenLayoutMode().menuXPos()+8;
        pageY = screenTextureMode.screenLayoutMode().menuYPos()+17;
        pageXSize = 9*18;
        pageYSize = containerRows*18;

        this.configButton = Button.builder(Component.literal("⚙"),
                        btn -> {
                            // 打开设置界面
                            if (this.minecraft != null) {
                                //this.minecraft.setScreen(new EndInvSettingsScreen(this)); // 你可自定义这个 Screen
                            }
                        })
                .pos(this.configButtonParam.XPos(),this.configButtonParam.YPos())
                .size(this.configButtonParam.XSize(),this.configButtonParam.YSize())
                .build();
        this.searchBox = new EditBox(this.font,
                this.searchBoxParam.XPos(),this.searchBoxParam.YPos(),this.searchBoxParam.XSize(),this.searchBoxParam.YSize()
                , Component.translatable("itemGroup.search"));
        this.sortTypeSwitchBox = new SortTypeSwitchBox(this, sortTypeSwitchBoxParam);


        addRenderableWidget(configButton);
        addRenderableWidget(searchBox);
        addRenderableWidget(sortTypeSwitchBox);
    }

    public void refresh(){
        this.containerRows = menu.getRowCount();
        this.imageHeight = 114 + this.containerRows*18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick){
        this.isHoveringOnSortBox = false;

        super.render(gui,mouseX,mouseY,partialTick);

        //check
        this.isHoveringOnPage = hasClickedOnPage(mouseX,mouseY) && !isHoveringOnSortBox;
        this.roughMouseX = mouseX;
        this.roughMouseY = mouseY;

        this.menu.getDisplayingPage().renderPage(gui, pageX, pageY);
        if(!isHoveringOnSortBox) this.menu.getDisplayingPage().renderHovering(gui,mouseX,mouseY,partialTick);
        //模仿
        this.renderTooltip(gui,mouseX,mouseY);
    }
    public boolean mouseClicked(double mouseX, double mouseY, int keyCode){
        for(GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener.mouseClicked(mouseX, mouseY, keyCode)) {
                this.setFocused(guieventlistener);
                if (keyCode == 0) {
                    this.setDragging(true);
                }

                return true;
            }
        }
        int pageIndex = hasClickedOnPageSwitchBar(mouseX,mouseY);
        if(pageIndex>=0){
            pageSwitched(pageIndex);
            return true;
        }
        DisplayPage displayingPage = menu.getDisplayingPage();
        if(hasClickedOnPage(mouseX,mouseY)){
            double XOffset = mouseX-pageX;
            double YOffset = mouseY-pageY;
            InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(keyCode);
            assert this.minecraft != null;
            boolean isKeyPicking = this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey);//is mouse middle button and enabled for pickup or clone
            long clickTime = Util.getMillis();
            this.doubleClick = keyCode==lastClickedButton && displayingPage.doubleClicked(XOffset,YOffset,lastCLickedX,lastClickedY,clickTime-lastClickedTime);
            this.skipNextRelease = false;
            if(keyCode!=0&&keyCode!=1&&!isKeyPicking){
                checkHotBarClicked:
                if (this.hoveredSlot != null && this.menu.getCarried().isEmpty()) {
                    if (this.minecraft.options.keySwapOffhand.matchesMouse(keyCode)) {
                        pageClicked(XOffset,YOffset,40,ClickType.SWAP);
                        break checkHotBarClicked;
                    }

                    for (int i = 0; i < 9; i++) {
                        if (this.minecraft.options.keyHotbarSlots[i].matchesMouse(keyCode)) {
                            pageClicked(XOffset,YOffset, i, ClickType.SWAP);
                        }
                    }
                }
                
            }else if(!this.isQuickCrafting){
                if(menu.getCarried().isEmpty()){
                    if (this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                        pageClicked(XOffset, YOffset, keyCode, ClickType.CLONE);
                    } else {
                        ClickType clicktype = ClickType.PICKUP;
                        if (hasShiftDown()) {
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
            return true;
        }
        return super.mouseClicked(mouseX,mouseY,keyCode);
    }
    protected int hasClickedOnPageSwitchBar(double mouseX, double mouseY){
        double XOffset=mouseX-screenTextureMode.pageSwitchBarParam().XPos();
        double YOffset=mouseY-screenTextureMode.pageSwitchBarParam().YPos();
        if(XOffset<0 || XOffset>screenTextureMode.pageSwitchBarParam().XSize()) return -1;
        int index = (int)YOffset/screenTextureMode.pageSwitchBarParam().YSize();
        if(index<0||index>=menu.pages.size()) return -1;
        return index;
    }
    protected boolean hasClickedOnPage(double mouseX,double mouseY){
        return mouseX>=(double) pageX && mouseX<=(double)pageX+pageXSize && mouseY>=(double) pageY && mouseY<=(double) pageY+pageYSize;
    }
    public boolean mouseReleased(double mouseX, double mouseY, int keyCode){
        DisplayPage displayingPage = menu.getDisplayingPage();
        if(hasClickedOnPage(mouseX,mouseY) && !isQuickCrafting){
            double XOffset = mouseX - pageX;
            double YOffset = mouseY - pageY;
            InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(keyCode);
            if (this.doubleClick) {
                this.pageClicked(XOffset,YOffset,keyCode,ClickType.PICKUP_ALL);
                this.doubleClick = false;
                this.lastClickedTime = 0L;
            }else {
                //ignore quick craft
                if (this.skipNextRelease) {
                    this.skipNextRelease = false;
                    return true;
                }
                if(!menu.getCarried().isEmpty()){
                    assert this.minecraft != null;
                    if (this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                        this.pageClicked(XOffset,YOffset,keyCode,ClickType.CLONE);
                    }
                }
            }
        }
        return super.mouseReleased(mouseX,mouseY,keyCode);
    }
    private boolean canScroll(){

        return this.menu.getDisplayingPage().canScroll();
    }

    public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY){
        if(super.mouseScrolled(mouseX,mouseY,scrollX,scrollY)){
            return true;
        }else if(!this.canScroll()){
            return false;
        }else{
            this.scrollOffs = this.menu.subtractInputFromScroll(this.scrollOffs,scrollY);
            this.menu.scrollTo(scrollOffs);
            //int startIndex = menu.getRowIndexForScroll(scrollOffs) * 9;
            //PacketDistributor.sendToServer(new EndInvRequestContentPayload(startIndex,9*this.containerRows, SortType.DEFAULT));
            return true;
        }
    }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        this.ignoreTextInput = false;
        if(menu.getDisplayingPage().hasSearchBar()){
            boolean isNumericKey = InputConstants.getKey(keyCode, scanCode).getNumericKeyValue().isPresent();
            boolean flag = false;
            if(isNumericKey && isHoveringOnPage){
                checkHotBarKeyPressed:
                if (this.menu.getCarried().isEmpty()) {
                    assert this.minecraft != null;
                    if (this.minecraft.options.keySwapOffhand.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                        pageClicked(roughMouseX-pageX, roughMouseY-pageY, 40, ClickType.SWAP);
                        flag = true;
                        break checkHotBarKeyPressed;
                    }

                    for(int i = 0; i < 9; ++i) {
                        if (this.minecraft.options.keyHotbarSlots[i].isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                            pageClicked(roughMouseX-pageX, roughMouseY-pageY, i, ClickType.SWAP);
                            flag = true;
                            break checkHotBarKeyPressed;
                        }
                    }
                }
            }
            if(flag){
                this.ignoreTextInput = true;
                return true;
            }else {
                String s = this.searchBox.getValue();
                if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                    if (!Objects.equals(s, this.searchBox.getValue())) {
                        this.refreshSearchResults();
                    }
                    return true;
                } else {
                    return this.searchBox.isFocused() && this.searchBox.isVisible() && keyCode != 256 || super.keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }
        return super.keyPressed(keyCode,scanCode,modifiers);
    }
    public boolean charTyped(char codePoint, int modifiers){
        if (this.ignoreTextInput) {
            return false;
        } else if (!menu.getDisplayingPage().hasSearchBar()) {
            return false;
        } else {
            String s = this.searchBox.getValue();
            if (this.searchBox.charTyped(codePoint, modifiers)) {
                if (!Objects.equals(s, this.searchBox.getValue())) {
                    this.refreshSearchResults();
                }

                return true;
            } else {
                return false;
            }
        }

    }
    private void refreshSearchResults(){
        this.menu.searching = searchBox.getValue();
        this.menu.syncContent();
    }
    protected void pageSwitched(int pageIndex){
        menu.switchPage(pageIndex);
        this.searchBox.visible = menu.getDisplayingPage().hasSearchBar();
    }
    protected void pageClicked(double mouseX, double mouseY, int keyCode, ClickType clickType){
        menu.syncContent();
        menu.getDisplayingPage().pageClicked(mouseX,mouseY,keyCode,clickType);
        PacketDistributor.sendToServer(
                new PageClickPayload(
                        menu.containerId,
                        menu.getDisplayingPageId(),
                        mouseX,
                        mouseY,
                        keyCode,
                        clickType
                        ));
    }
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, @NotNull ClickType type) {
        super.slotClicked(slot,slotId,mouseButton,type);
        if(this.menu.getDisplayingPage() instanceof ItemDisplay itemDisplay)
            itemDisplay.tryRequestContents(itemDisplay.getStartIndex(),itemDisplay.getContainerSize());
        this.menu.broadcastChanges();

    }
    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        this.screenTextureMode.renderBg(guiGraphics,partialTick,mouseX,mouseY);
    }


    public void switchSortTypeTo(SortType type) {
        menu.sortType = type;
        menu.getDisplayingPage().syncContentToServer();
    }
}
