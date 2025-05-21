package com.kwwsyk.endinv.client.gui;

import com.kwwsyk.endinv.client.TextureMode;
import com.kwwsyk.endinv.client.config.ClientConfig;
import com.kwwsyk.endinv.client.events.KeyMappingReg;
import com.kwwsyk.endinv.client.gui.bg.FromResource;
import com.kwwsyk.endinv.client.gui.bg.ScreenBgRenderer;
import com.kwwsyk.endinv.client.gui.bg.ScreenRectangleWidgetParam;
import com.kwwsyk.endinv.client.gui.bg.Transparent;
import com.kwwsyk.endinv.client.gui.widget.SortTypeSwitchBox;
import com.kwwsyk.endinv.menu.page.DisplayPage;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.network.payloads.toServer.page.StarItemPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.op.PageClickPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.op.QuickMoveToPagePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.kwwsyk.endinv.ModInitializer.SYNCED_CONFIG;
import static net.minecraft.client.gui.screens.Screen.hasShiftDown;

public class ScreenFrameWork {

    private static ScreenFrameWork INSTANCE;

    private final Minecraft mc;
    public final PageMetaDataManager meta;
    public final AbstractContainerScreen<?> screen;
    public final AbstractContainerMenu menu;
    private final SortTypeSwitcher sortTypeSwitcher;
    private final ScreenRectangleWidgetParam searchBoxParam;
    private final ScreenRectangleWidgetParam sortBoxParam;
    private final ScreenRectangleWidgetParam configButtonParam;
    private final ScreenRectangleWidgetParam pageBarScrollUpButtonParam,pageBarScrollDownButtonParam;
    public final ScreenBgRenderer screenBgRenderer;
    public final int pageBarCount;
    public int firstPageIndex = 0;
    //Always pageBarCount + firstPageIndex <= meta.getPages.size()
    public final int leftPos,topPos;
    public final int imageWidth,imageHeight;
    public final int pageX;
    public final int pageY;
    private final int rows;
    private final int columns;
    private final int pageXSize;
    private final int pageYSize;
    private int roughMouseX;
    private int roughMouseY;
    private EditBox searchBox;
    public SortTypeSwitchBox sortTypeSwitchBox;
    private Button reverseSortButton;
    private Button configButton;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    
    public ScreenFrameWork(EndlessInventoryScreen screen){
        this.screen = screen;
        this.mc = screen.getMinecraft();
        this.meta = screen.getPageMetadata();
        this.menu = screen.getMenu();
        this.leftPos = screen.getGuiLeft();
        this.topPos = screen.getGuiTop();
        this.imageWidth = screen.getXSize();
        this.imageHeight= screen.getYSize();
        this.rows = meta.getRowCount();
        this.columns = meta.getColumnCount();
        this.sortTypeSwitcher = screen;
        this.pageBarCount = Math.min(ClientConfig.CONFIG.MAX_PAGE_BARS.getAsInt(),meta.getPages().size());
        this.pageBarScrollUpButtonParam = new ScreenRectangleWidgetParam(leftPos-32,topPos-16,30,14);
        this.pageBarScrollDownButtonParam = new ScreenRectangleWidgetParam(leftPos-32,topPos+2+28*pageBarCount,30,14);
        this.configButtonParam = new ScreenRectangleWidgetParam(this.leftPos+this.imageWidth,this.topPos,18,18);
        this.searchBoxParam = new ScreenRectangleWidgetParam(this.leftPos + 89, this.topPos + 5, 80, 12);
        this.sortBoxParam = new ScreenRectangleWidgetParam(this.leftPos+8,topPos+5,60,12);
        this.screenBgRenderer = new FromResource.MenuMode(this,new ScreenRectangleWidgetParam(leftPos-32,topPos+1,32,28));

        pageX = leftPos+8;
        pageY = topPos+17;
        pageXSize = columns*18;
        pageYSize = rows*18;
        addWidgets();
        INSTANCE = this;
    }

    public ScreenFrameWork(AttachedScreen<?> attachedScreen){
        this.screen = attachedScreen.screen;
        this.mc = screen.getMinecraft();
        this.meta = attachedScreen.getPageMetadata();
        this.menu = meta.getMenu();
        this.leftPos = 20;
        this.topPos=20;
        this.rows = meta.getRowCount();
        this.columns = meta.getColumnCount();
        this.sortTypeSwitcher = attachedScreen;
        this.pageBarCount = Math.min(ClientConfig.CONFIG.MAX_PAGE_BARS.getAsInt(),meta.getPages().size());
        this.imageWidth = 13+18*columns;
        this.imageHeight = screen.height;
        int searchBoxY = this.topPos + 17+18*rows + 12;
        this.searchBoxParam =
                new ScreenRectangleWidgetParam(this.leftPos+1, searchBoxY, Math.min(200,imageWidth), Math.min(20,screen.height-searchBoxY));
        this.configButtonParam = new ScreenRectangleWidgetParam(0, searchBoxY,20,20);
        this.pageBarScrollUpButtonParam = new ScreenRectangleWidgetParam(0,topPos,20,14);
        this.pageBarScrollDownButtonParam = new ScreenRectangleWidgetParam(0,topPos+22+28*pageBarCount,20,14);
        this.sortBoxParam = new ScreenRectangleWidgetParam(this.leftPos + 6,topPos + 5,77,12);
        this.screenBgRenderer = ClientConfig.CONFIG.TEXTURE.get() == TextureMode.FROM_RESOURCE ?
                new FromResource.LeftLayout(this,new ScreenRectangleWidgetParam(leftPos-32,topPos+20,32,28)) :
                new Transparent(this,new ScreenRectangleWidgetParam(leftPos-32,topPos+20,32,28));

        pageX = leftPos+8;
        pageY = topPos+17;
        pageXSize = columns*18;
        pageYSize = rows*18;
        addWidgets();
        INSTANCE = this;
    }
    
    private void addWidgets(){
        this.configButton = Button.builder(Component.literal("⚙"),
                        btn -> {
                            screen.getMinecraft().setScreen(new EndInvSettingScreen(screen));
                        })
                .pos(this.configButtonParam.XPos(),this.configButtonParam.YPos())
                .size(this.configButtonParam.XSize(),this.configButtonParam.YSize())
                .build();
        this.reverseSortButton = Button.builder(Component.literal("⇅"),
                        btn->{
                            meta.switchSortReversed();
                            SyncedConfig.updateSyncedConfig(meta.getPlayer().getData(SYNCED_CONFIG).ofReverseSort());
                            meta.getDisplayingPage().syncContentToServer();
                        }
                )
                .pos(sortBoxParam.XPos()+sortBoxParam.XSize()+2,sortBoxParam.YPos())
                .size(sortBoxParam.YSize(),sortBoxParam.YSize())
                .build();
        this.searchBox = new EditBox(mc.font,
                this.searchBoxParam.XPos(),this.searchBoxParam.YPos(),this.searchBoxParam.XSize(),this.searchBoxParam.YSize()
                , Component.translatable("itemGroup.search"));
        this.sortTypeSwitchBox = new SortTypeSwitchBox(sortTypeSwitcher, sortBoxParam);

        this.searchBox.setValue(meta.searching());

        if(pageBarCount<meta.getPages().size()){
            Button up = Button.builder(Component.literal("^"),btn->{if(firstPageIndex>0)firstPageIndex--;})
                    .pos(pageBarScrollUpButtonParam.XPos(),pageBarScrollUpButtonParam.YPos())
                    .size(pageBarScrollUpButtonParam.XSize(),pageBarScrollUpButtonParam.YSize())
                    .build();
            Button down = Button.builder(Component.literal("v"),btn-> {
                if(firstPageIndex + pageBarCount<meta.getPages().size())
                        firstPageIndex++;
                    })
                    .pos(pageBarScrollDownButtonParam.XPos(),pageBarScrollDownButtonParam.YPos())
                    .size(pageBarScrollDownButtonParam.XSize(),pageBarScrollDownButtonParam.YSize())
                    .build();
            widgets.add(up);
            widgets.add(down);
        }


        widgets.add(configButton);
        widgets.add(reverseSortButton);
        widgets.add(searchBox);
        widgets.add(sortTypeSwitchBox);
    }

    public void addWidgetToScreen(Consumer<AbstractWidget> installer){
        widgets.forEach(installer);
    }

    public void renderPre(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick){
        sortTypeSwitcher.setHoveringOnSortBox(false);
    }

    public void renderBg(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick){
        screenBgRenderer.renderBg(guiGraphics,partialTick,mouseX,mouseY);
        meta.getDisplayingPage().getPageRenderer().renderBg(screenBgRenderer,guiGraphics,partialTick,mouseX,mouseY);
    }

    private boolean isHoveringOnPage;
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick){
        roughMouseX = mouseX;
        roughMouseY = mouseY;

        isHoveringOnPage = hasClickedOnPage(mouseX,mouseY);


        meta.getDisplayingPage().getPageRenderer().renderPage(guiGraphics,pageX,pageY);
        if(!sortTypeSwitcher.isHoveringOnSortBox()) meta.getDisplayingPage().getPageRenderer().renderHovering(guiGraphics,mouseX,mouseY,partialTick);

        if(searchBox.isHovered() && !searchBox.isFocused()) guiGraphics.renderTooltip(screen.getMinecraft().font, List.of(
                Component.translatable("search.endinv.prefix.sharp"),
                Component.translatable("search.endinv.prefix.at"),
                Component.translatable("search.endinv.prefix.xor"),
                Component.translatable("search.endinv.prefix.star")
        ), Optional.empty(),mouseX,mouseY);
        if(reverseSortButton.isHovered()) guiGraphics.renderTooltip(screen.getMinecraft().font,Component.translatable("button.endinv.reverse"),mouseX,mouseY);

    }

    protected boolean hasClickedOnPage(double mouseX,double mouseY){
        return mouseX>=(double) pageX && mouseX<=(double)pageX+pageXSize
                && mouseY>=(double) pageY && mouseY<=(double) pageY+pageYSize
                && !sortTypeSwitcher.isHoveringOnSortBox();
    }

    protected void pageClicked(double mouseX, double mouseY, int keyCode, ClickType clickType){
        //menu.syncContent();
        meta.getDisplayingPage().getPageClickHandler().pageClicked(mouseX,mouseY,keyCode,clickType);
        PacketDistributor.sendToServer(
                new PageClickPayload(menu.containerId, meta.getInPageContext(), mouseX, mouseY, keyCode, clickType));
    }

    protected int hasClickedOnPageSwitchBar(double mouseX, double mouseY){
        double XOffset=mouseX- screenBgRenderer.pageSwitchBarParam().XPos();
        double YOffset=mouseY- screenBgRenderer.pageSwitchBarParam().YPos();
        if(XOffset<0 || XOffset> screenBgRenderer.pageSwitchBarParam().XSize() || YOffset<0) return -1;
        int index = (int)YOffset/ screenBgRenderer.pageSwitchBarParam().YSize();
        if(index<0||index>=pageBarCount) return -1;
        return index;
    }

    protected void pageSwitched(int index){
        meta.switchPageWithIndex(index + firstPageIndex);
        //meta.getDisplayingPage().syncContentToServer();
        this.searchBox.setVisible(meta.getDisplayingPage().hasSearchBar());
        this.sortTypeSwitchBox.visible = meta.getDisplayingPage().hasSortTypeSwitchBar();
    }

    private boolean isHovering(Slot slot, double mouseX, double mouseY) {
        return this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }

    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        int i = this.screen.getGuiLeft();
        int j = this.screen.getGuiTop();
        mouseX -= i;
        mouseY -= j;
        return mouseX >= (double)(x - 1)
                && mouseX < (double)(x + width + 1)
                && mouseY >= (double)(y - 1)
                && mouseY < (double)(y + height + 1);
    }

    @Nullable
    private Slot findSlot(double mouseX, double mouseY) {
        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            if (this.isHovering(slot, mouseX, mouseY) && slot.isActive()) {
                return slot;
            }
        }

        return null;
    }

    private void slotQuickMoved(Slot clicked){
        ItemStack itemStack = clicked.getItem();
        ItemStack remain = meta.getDisplayingPage().tryQuickMoveStackTo(itemStack);
        clicked.setByPlayer(remain);
        clicked.onTake(meta.getPlayer(), itemStack);
        PacketDistributor.sendToServer(new QuickMoveToPagePayload(clicked.index));
    }

    //handle input seg:
    private boolean doubleClick;
    private int lastClickedButton;
    private double lastCLickedX;
    private double lastClickedY;
    private long lastClickedTime;
    private boolean skipNextRelease;


    public boolean mouseClicked(double mouseX, double mouseY, int keyCode){
        if(!searchBoxParam.hasCLickedOn((int) mouseX, (int) mouseY)){
            searchBox.setFocused(false);
        }
        //handle menu item quick move
        if(KeyMappingReg.QUICK_MOVE_KEY.get().isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(keyCode))){
            Slot clicked = findSlot(mouseX,mouseY);
            if(clicked!=null && clicked.hasItem()){
                slotQuickMoved(clicked);
                return true;
            }
        }
        //handle clicked on the page switch bar
        int pageIndex = hasClickedOnPageSwitchBar(mouseX,mouseY);
        if(pageIndex>=0){
            pageSwitched(pageIndex);
            return true;
        }
        //
        if(!hasClickedOnPage(mouseX,mouseY)) return false;
        double XOffset = mouseX-pageX;
        double YOffset = mouseY-pageY;
        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(keyCode);
        boolean isKeyPicking = this.mc.options.keyPickItem.isActiveAndMatches(mouseKey);//is mouse middle button and enabled for pickup or clone
        long clickTime = Util.getMillis();
        this.doubleClick = keyCode==lastClickedButton && meta.getDisplayingPage().getPageClickHandler().doubleClicked(XOffset,YOffset,lastCLickedX,lastClickedY,clickTime-lastClickedTime);
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
                if (this.mc.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                    pageClicked(XOffset, YOffset, keyCode, ClickType.CLONE);
                } else {
                    ClickType clicktype = ClickType.PICKUP;
                    if (hasShiftDown()) {
                        meta.getDisplayingPage().setHoldOn();
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        ItemStack itemstack = this.menu.getCarried();
        if(!itemstack.isEmpty() || mc.options.touchscreen().get())
            return false;
        if(KeyMappingReg.QUICK_MOVE_KEY.get().isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(button))){
            Slot clicked = findSlot(mouseX,mouseY);
            if(clicked!=null && clicked.hasItem()){
                slotQuickMoved(clicked);
                return true;
            }
        }
        if(!hasClickedOnPage(mouseX, mouseY)) return false;
        //handle page drag: mouse tweak style
        if(hasShiftDown()){
            int slotId = meta.getDisplayingPage().getPageClickHandler().getSlotForMouseOffset(mouseX-pageX,mouseY-pageY);
            if(slotId>=0 && lastDraggedPageSlot>=0 && slotId!=lastDraggedPageSlot){
                pageClicked(mouseX-pageX,mouseY-pageY,button,ClickType.QUICK_MOVE);
            }
            lastDraggedPageSlot = slotId;
            return true;
        }else return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int keyCode){
        DisplayPage displayingPage = meta.getDisplayingPage();
        displayingPage.release();
        if(hasClickedOnPage(mouseX,mouseY)){
            lastDraggedPageSlot = -1;
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
                    if (mc.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                        this.pageClicked(XOffset,YOffset,keyCode,ClickType.CLONE);
                    }
                }
            }
        }
        return false;
    }

    private float scrollOffs;
    public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY){
        if(hasClickedOnPage(mouseX,mouseY)){
            if(!meta.getDisplayingPage().canScroll()) return false;
            this.scrollOffs = this.meta.subtractInputFromScroll(this.scrollOffs,scrollY);
            this.meta.scrollTo(scrollOffs);
            return true;
        }
        return false;
    }


    private boolean ignoreTextInput;

    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        this.ignoreTextInput = false;

        if(KeyMappingReg.STAR_ITEM_KEY.get().isActiveAndMatches(InputConstants.getKey(keyCode,scanCode))){
            Slot clicked = findSlot(roughMouseX,roughMouseY);
            if(clicked!=null && clicked.hasItem()){
                ItemStack itemStack = clicked.getItem();
                PacketDistributor.sendToServer(new StarItemPayload(itemStack,true));
                meta.getDisplayingPage().syncContentToServer();
                return true;
            }
        }

        boolean isNumericKey = InputConstants.getKey(keyCode, scanCode).getNumericKeyValue().isPresent();
        boolean flag = false;
        if(isHoveringOnPage){
            checkHotBarKeyPressed:
            if (isNumericKey && this.menu.getCarried().isEmpty()) {
                if (mc.options.keySwapOffhand.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                    pageClicked(roughMouseX-pageX, roughMouseY-pageY, 40, ClickType.SWAP);
                    flag = true;
                    break checkHotBarKeyPressed;
                }

                for(int i = 0; i < 9; ++i) {
                    if (mc.options.keyHotbarSlots[i].isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                        pageClicked(roughMouseX-pageX, roughMouseY-pageY, i, ClickType.SWAP);
                        flag = true;
                        break checkHotBarKeyPressed;
                    }
                }
            }

            if(KeyMappingReg.STAR_ITEM_KEY.get().isActiveAndMatches(InputConstants.getKey(keyCode,scanCode))){
                meta.getDisplayingPage().handleStarItem(roughMouseX-pageX,roughMouseY-pageY);
                flag = true;
            }
        }
        if(flag){
            this.ignoreTextInput = true;
            return true;
        }
        if(meta.getDisplayingPage().hasSearchBar()){
            String s = this.searchBox.getValue();
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                if (!Objects.equals(s, this.searchBox.getValue())) {
                    this.refreshSearchResults();
                }
                return true;
            } else {
                return this.searchBox.isFocused() && this.searchBox.isVisible() && keyCode != 256;
            }
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers){
        if (this.ignoreTextInput || !meta.getDisplayingPage().hasSearchBar()) {
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

    public void refreshSearchResults(){
        String searching = searchBox.getValue();
        meta.setSearching(searching);
        SyncedConfig.updateSyncedConfig(meta.getPlayer().getData(SYNCED_CONFIG).searchingChanged(searching));
        meta.getDisplayingPage().release();
        meta.getDisplayingPage().syncContentToServer();
    }

    public static @Nullable ScreenFrameWork getInstance(){
        return INSTANCE;
    }
}
