package com.kwwsyk.endinv.client.gui;

import com.kwwsyk.endinv.SourceInventory;
import com.kwwsyk.endinv.client.config.ClientConfig;
import com.kwwsyk.endinv.client.gui.bg.FromResource;
import com.kwwsyk.endinv.client.gui.bg.ScreenRectangleWidgetParam;
import com.kwwsyk.endinv.client.gui.widget.SortTypeSwitchBox;
import com.kwwsyk.endinv.menu.page.DefaultPages;
import com.kwwsyk.endinv.menu.page.DisplayPage;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.menu.page.pageManager.PageQuickMoveHandler;
import com.kwwsyk.endinv.network.payloads.EndInvMetadata;
import com.kwwsyk.endinv.network.payloads.PageClickPayload;
import com.kwwsyk.endinv.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.options.ItemClassify;
import com.kwwsyk.endinv.options.SortType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.kwwsyk.endinv.ModInitializer.SYNCED_CONFIG;
import static net.minecraft.client.gui.screens.Screen.hasShiftDown;

public class AttachedScreen<T extends AbstractContainerMenu> implements SortTypeSwitcher{

    public final AbstractContainerScreen<T> screen;
    private final PageMetaDataManager pageMetadata = new PageMetaDataManager() {
        @Override
        public AbstractContainerMenu getMenu() {
            return screen.getMenu();
        }

        @Override
        public SourceInventory getSourceInventory() {
            return REMOTE;
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
            displayingPage = pages.get(index);
            SyncedConfig.updateClientConfigAndSync(player.getData(SYNCED_CONFIG).pageIdChanged(displayingPage.pageId));
            displayingPage.init(0,9*getRowCount());
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
            return endInvMetadata.itemSize();
        }

        @Override
        public int getMaxStackSize() {
            return endInvMetadata.maxStackSize();
        }

        @Override
        public boolean enableInfinity() {
            return endInvMetadata.infinityMode();
        }

        @Override
        public ItemStack quickMoveFromPage(ItemStack stack) {//todo
            return quickMoveHandler.quickMoveFromPage(stack);
        }

        @Override
        public SortType sortType() {
            return sortType;
        }

        @Override
        public void setSortType(SortType sortType) {
            AttachedScreen.this.sortType = sortType;
        }

        @Override
        public String searching() {
            return searching;
        }

        @Override
        public void setSearching(String searching) {
            AttachedScreen.this.searching = searching;
        }

        @Override
        public void sendEndInvMetadataToRemote() {//do nop as in client
        }

        @Override
        public EndInvMetadata getEndInvMetadata() {
            return endInvMetadata;
        }
    };
    private final PageQuickMoveHandler quickMoveHandler;
    private int leftPos;
    private int topPos;
    private ScreenRectangleWidgetParam configButtonParam;
    private int imageWidth;
    private ScreenRectangleWidgetParam searchBoxParam;
    private ScreenRectangleWidgetParam sortTypeSwitchBoxParam;
    private FromResource screenRenderer;
    private Button configButton;

    private int pageX;
    private int pageY;
    private int pageXSize;
    private int pageYSize;
    private boolean isHoveringOnPage;
    private boolean isHoveringOnSortBox;
    private EditBox searchBox;
    private double lastCLickedX;
    private double lastClickedY;
    private long lastClickedTime;
    private int lastClickedButton;
    private boolean doubleClick;
    private boolean skipNextRelease;
    private boolean ignoreTextInput;
    private float scrollOffs;
    private int roughMouseX;
    private int roughMouseY;
    private DisplayPage displayingPage;
    public final List<DisplayPage> pages;
    private int rows;
    public SortType sortType;
    public String searching;
    private Player player;
    private EndInvMetadata endInvMetadata;

    public AttachedScreen(AbstractContainerScreen<T> screen){
        this.screen = screen;
        this.pages = buildPages();
        if (Minecraft.getInstance().player != null) {
            this.player = Minecraft.getInstance().player;
            SyncedConfig config = player.getData(SYNCED_CONFIG);
            this.rows = config.rows();
            this.sortType = config.sortType();
            this.searching=config.search();
            pageMetadata.switchPageWithId(config.pageId());
        }
        this.endInvMetadata = new EndInvMetadata(0,Integer.MAX_VALUE,false);
        this.quickMoveHandler = new PageQuickMoveHandler(this.pageMetadata);
    }
    private List<DisplayPage> buildPages(){
        List<DisplayPage> ret = new ArrayList<>();
        for(int i = 0; i< ItemClassify.DEFAULT_CLASSIFIES.size(); ++i){
            Holder<ItemClassify> classify = ItemClassify.DEFAULT_CLASSIFIES.get(i);

            boolean hidden = ClientConfig.CONFIG.PAGES.get(i).getAsBoolean();
            if (!hidden) {
                DisplayPage page = DefaultPages.CLASSIFY2PAGE.get(classify).create(pageMetadata, classify, i);
                page.icon = DefaultPages.CLASSIFY2RSRC.get(classify);
                ret.add(page);
            }

        }
        return ret;
    }
    public void setEndInvMetadata(EndInvMetadata metadata){
        this.endInvMetadata = metadata;
    }

    public void init(ScreenEvent.Init.Post event){
        this.leftPos=32;
        this.topPos=0;
        this.configButtonParam = new ScreenRectangleWidgetParam(this.leftPos, this.topPos,18,18);
        this.searchBoxParam = new ScreenRectangleWidgetParam(this.leftPos + 116, this.topPos + 6, 80, 9);
        this.sortTypeSwitchBoxParam = new ScreenRectangleWidgetParam(this.leftPos+8,topPos+5,60,12);
        this.screenRenderer = FromResource.createLeftMode(screen,pageMetadata);
        this.screenRenderer.init();
        this.pageX = screenRenderer.screenLayoutMode().menuXPos() + 8;
        this.pageY = screenRenderer.screenLayoutMode().menuYPos() + 17;
        this.pageXSize = 9 * 18;
        this.pageYSize = rows * 18;

        this.configButton = Button.builder(Component.literal("⚙"),
                        btn -> {
                            // 打开设置界面
                            if (screen.getMinecraft() != null) {
                                //this.minecraft.setScreen(new EndInvSettingsScreen(this)); // 你可自定义这个 Screen
                            }
                        })
                .pos(this.configButtonParam.XPos(),this.configButtonParam.YPos())
                .size(this.configButtonParam.XSize(),this.configButtonParam.YSize())
                .build();
        this.searchBox = new EditBox(screen.getMinecraft().font,
                this.searchBoxParam.XPos(),this.searchBoxParam.YPos(),this.searchBoxParam.XSize(),this.searchBoxParam.YSize()
                , Component.translatable("itemGroup.search"));
        var sortTypeSwitchBox = new SortTypeSwitchBox(this, sortTypeSwitchBoxParam);

        searchBox.setValue(pageMetadata.searching());

        event.addListener(configButton);
        event.addListener(searchBox);
        event.addListener(sortTypeSwitchBox);
    }

    public void renderPre(ScreenEvent.Render.Pre event) {
        this.isHoveringOnSortBox = false;
    }

    public void render(ScreenEvent.Render.Post event) {
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        float partialTick = event.getPartialTick();
        this.isHoveringOnPage = hasClickedOnPage(mouseX,mouseY)  ;
        this.roughMouseX = mouseX;
        this.roughMouseY = mouseY;

        this.screenRenderer.renderBg(guiGraphics,partialTick,mouseX,mouseY);

        this.pageMetadata.getDisplayingPage().renderPage(guiGraphics, pageX, pageY);
        if(!isHoveringOnSortBox) this.pageMetadata.getDisplayingPage().renderHovering(guiGraphics,mouseX,mouseY,partialTick);
        //模仿
        //this.renderTooltip(guiGraphics,mouseX,mouseY);

    }

    protected boolean hasClickedOnPage(double mouseX,double mouseY){
        return mouseX>=(double) pageX && mouseX<=(double)pageX+pageXSize && mouseY>=(double) pageY && mouseY<=(double) pageY+pageYSize && !isHoveringOnSortBox;
    }

    public void mouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int keyCode = event.getButton();
        int pageIndex = hasClickedOnPageSwitchBar(mouseX,mouseY);
        if(pageIndex>=0){
            pageSwitched(pageIndex);
            event.setCanceled(true);
            return;
        }
        DisplayPage displayingPage = pageMetadata.getDisplayingPage();
        if(hasClickedOnPage(mouseX,mouseY)) {
            double XOffset = mouseX - pageX;
            double YOffset = mouseY - pageY;
            InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(keyCode);
            //assert this.minecraft != null;
            boolean isKeyPicking = screen.getMinecraft().options.keyPickItem.isActiveAndMatches(mouseKey);//is mouse middle button and enabled for pickup or clone
            long clickTime = Util.getMillis();
            this.doubleClick = keyCode == lastClickedButton && displayingPage.doubleClicked(XOffset, YOffset, lastCLickedX, lastClickedY, clickTime - lastClickedTime);
            this.skipNextRelease = false;
            if (keyCode != 0 && keyCode != 1 && !isKeyPicking) {
                checkHotBarClicked:
                if (this.pageMetadata.getMenu().getCarried().isEmpty()) {
                    if (screen.getMinecraft().options.keySwapOffhand.matchesMouse(keyCode)) {
                        pageClicked(XOffset, YOffset, 40, ClickType.SWAP);
                        break checkHotBarClicked;
                    }

                    for (int i = 0; i < 9; i++) {
                        if (screen.getMinecraft().options.keyHotbarSlots[i].matchesMouse(keyCode)) {
                            pageClicked(XOffset, YOffset, i, ClickType.SWAP);
                        }
                    }
                }

            } else {
                if (pageMetadata.getMenu().getCarried().isEmpty()) {
                    if (screen.getMinecraft().options.keyPickItem.isActiveAndMatches(mouseKey)) {
                        pageClicked(XOffset, YOffset, keyCode, ClickType.CLONE);
                    } else {
                        ClickType clicktype = ClickType.PICKUP;
                        if (hasShiftDown()) {
                            pageMetadata.getDisplayingPage().setHoldOn();
                            //this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                            clicktype = ClickType.QUICK_MOVE;
                        }

                        pageClicked(XOffset, YOffset, keyCode, clicktype);
                    }
                    this.skipNextRelease = true;
                } else {//deference to vanilla
                    pageClicked(XOffset, YOffset, keyCode, ClickType.PICKUP);
                }
            }
            this.lastClickedTime = clickTime;
            this.lastClickedButton = keyCode;
            this.lastCLickedX = XOffset;
            this.lastClickedY = YOffset;
            event.setCanceled(true);
            return;
        }
    }
    protected int hasClickedOnPageSwitchBar(double mouseX, double mouseY){
        double XOffset=mouseX- screenRenderer.pageSwitchBarParam().XPos();
        double YOffset=mouseY- screenRenderer.pageSwitchBarParam().YPos();
        if(XOffset<0 || XOffset> screenRenderer.pageSwitchBarParam().XSize()) return -1;
        int index = (int)YOffset/ screenRenderer.pageSwitchBarParam().YSize();
        if(index<0||index>= pageMetadata.getPages().size()) return -1;
        return index;
    }
    protected void pageClicked(double mouseX, double mouseY, int keyCode, ClickType clickType){
        //menu.syncContent();
        pageMetadata.getDisplayingPage().pageClicked(mouseX,mouseY,keyCode,clickType);
        PacketDistributor.sendToServer(
                new PageClickPayload(
                        screen.getMenu().containerId,
                        pageMetadata.getDisplayingPageId(),
                        mouseX,
                        mouseY,
                        keyCode,
                        clickType
                ));
    }
    protected void pageSwitched(int pageIndex){
        pageMetadata.switchPageWithIndex(pageIndex);
        this.searchBox.visible = pageMetadata.getDisplayingPage().hasSearchBar();
    }
    private int lastDraggedPageSlot = -1;
    public void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int keyCode = event.getButton();
        DisplayPage displayingPage = pageMetadata.getDisplayingPage();
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
                    event.setCanceled(true);
                    return;
                }
                if(!pageMetadata.getMenu().getCarried().isEmpty()){
                    //assert this.minecraft != null;
                    if (screen.getMinecraft().options.keyPickItem.isActiveAndMatches(mouseKey)) {
                        this.pageClicked(XOffset,YOffset,keyCode,ClickType.CLONE);
                    }
                }
            }
        }
    }

    public void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int button = event.getMouseButton();
        double dragX = event.getDragX();
        double dragY = event.getDragY();

        ItemStack itemstack = this.pageMetadata.getMenu().getCarried();
        if(!itemstack.isEmpty() || screen.getMinecraft().options.touchscreen().get() || !hasClickedOnPage(mouseX, mouseY))
            return;
        //handle page drag: mouse tweak style
        if(hasShiftDown()){
            int slotId = pageMetadata.getDisplayingPage().getSlotForMouseOffset(mouseX-pageX,mouseY-pageY);
            if(slotId>=0 && lastDraggedPageSlot>=0 && slotId!=lastDraggedPageSlot){
                pageClicked(mouseX-pageX,mouseY-pageY,button,ClickType.QUICK_MOVE);
            }
            lastDraggedPageSlot = slotId;
            event.setCanceled(true);
        }
    }

    public void mouseScrolled(ScreenEvent.MouseScrolled event) {
        double scrollY = event.getScrollDeltaY();
        if(pageMetadata.getDisplayingPage().canScroll()){
            this.scrollOffs = this.pageMetadata.subtractInputFromScroll(this.scrollOffs,scrollY);
            this.pageMetadata.scrollTo(scrollOffs);
        }
    }

    public void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        int keyCode = event.getKeyCode();
        int scanCode = event.getScanCode();
        int modifiers = event.getModifiers();
        if(pageMetadata.getDisplayingPage().hasSearchBar()){
            boolean isNumericKey = InputConstants.getKey(keyCode, scanCode).getNumericKeyValue().isPresent();
            boolean flag = false;
            if(isNumericKey && isHoveringOnPage){
                checkHotBarKeyPressed:
                if (this.pageMetadata.getMenu().getCarried().isEmpty()) {
                    //assert this.minecraft != null;
                    if (screen.getMinecraft().options.keySwapOffhand.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                        pageClicked(roughMouseX-pageX, roughMouseY-pageY, 40, ClickType.SWAP);
                        flag = true;
                        break checkHotBarKeyPressed;
                    }

                    for(int i = 0; i < 9; ++i) {
                        if (screen.getMinecraft().options.keyHotbarSlots[i].isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                            pageClicked(roughMouseX-pageX, roughMouseY-pageY, i, ClickType.SWAP);
                            flag = true;
                            break checkHotBarKeyPressed;
                        }
                    }
                }
            }
            if(flag){
                this.ignoreTextInput = true;
                event.setCanceled(true);
            }else {
                String s = this.searchBox.getValue();
                if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                    if (!Objects.equals(s, this.searchBox.getValue())) {
                        this.refreshSearchResults();
                    }
                    event.setCanceled(true);
                } else if(this.searchBox.isFocused() && this.searchBox.isVisible() && keyCode != 256) {
                    event.setCanceled(true);
                }
            }
        }
    }

    public boolean charTyped(char codePoint, int modifiers){
        if (this.ignoreTextInput) {
            return false;
        } else if (!pageMetadata.getDisplayingPage().hasSearchBar()) {
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
        this.searching=searchBox.getValue();
        SyncedConfig.updateClientConfigAndSync(player.getData(SYNCED_CONFIG).searchingChanged(searching));
        pageMetadata.getDisplayingPage().release();
        this.pageMetadata.getDisplayingPage().syncContentToServer();
    }

    @Override
    public void switchSortTypeTo(SortType type) {
        this.sortType = type;
        SyncedConfig.updateClientConfigAndSync(player.getData(SYNCED_CONFIG).sortTypeChanged(type));
        pageMetadata.getDisplayingPage().release();
        pageMetadata.getDisplayingPage().syncContentToServer();
    }

    @Override
    public void setHoveringOnSortBox(boolean isHovering) {
        this.isHoveringOnSortBox  = isHovering;
    }

    @Override
    public PageMetaDataManager getMenu() {
        return pageMetadata;
    }

    @Override
    public AbstractContainerScreen<?> getScreen() {
        return screen;
    }


}
