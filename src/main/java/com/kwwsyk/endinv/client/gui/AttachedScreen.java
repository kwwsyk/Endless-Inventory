package com.kwwsyk.endinv.client.gui;

import com.kwwsyk.endinv.SourceInventory;
import com.kwwsyk.endinv.client.config.ClientConfig;
import com.kwwsyk.endinv.client.gui.bg.FromResource;
import com.kwwsyk.endinv.client.gui.bg.ScreenLayoutMode;
import com.kwwsyk.endinv.client.gui.bg.ScreenRectangleWidgetParam;
import com.kwwsyk.endinv.client.gui.widget.PoseTranslatedEditBox;
import com.kwwsyk.endinv.client.gui.widget.SortTypeSwitchBox;
import com.kwwsyk.endinv.menu.page.DefaultPages;
import com.kwwsyk.endinv.menu.page.DisplayPage;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.menu.page.pageManager.PageQuickMoveHandler;
import com.kwwsyk.endinv.network.payloads.EndInvMetadata;
import com.kwwsyk.endinv.network.payloads.PageClickPayload;
import com.kwwsyk.endinv.network.payloads.PageData;
import com.kwwsyk.endinv.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.options.ItemClassify;
import com.kwwsyk.endinv.util.SortType;
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
import java.util.Optional;

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
            displayingPage.init(0,rows*columns);
        }

        @Override
        public int getRowCount() {
            return rows;
        }

        @Override
        public int getColumnCount() {
            return columns;
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
        public ItemStack quickMoveFromPage(ItemStack stack) {
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
        public boolean isSortReversed() {
            return reverseSort;
        }

        @Override
        public void switchSortReversed() {
            reverseSort = !reverseSort;
        }

        @Override
        public void setSortReversed(boolean reversed) {
            reverseSort = reversed;
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
    private float scrollYOffs;
    private int roughMouseX;
    private int roughMouseY;
    private DisplayPage displayingPage;
    public final List<DisplayPage> pages;
    private int rows;
    private int columns;

    public SortType sortType;
    public String searching;
    private Player player;
    private EndInvMetadata endInvMetadata;
    public SortTypeSwitchBox sortTypeSwitchBox;
    private boolean reverseSort;
    private Button sorttypeReverseButton;

    public AttachedScreen(AbstractContainerScreen<T> screen){
        this.screen = screen;
        this.pages = buildPages();
        if (Minecraft.getInstance().player != null) {
            this.player = Minecraft.getInstance().player;
            PageData data = player.getData(SYNCED_CONFIG).pageData();
            this.rows = data.rows();
            this.columns = data.columns();
            this.sortType = data.sortType();
            this.searching=data.search();
            pageMetadata.switchPageWithId(data.pageId());
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
        this.leftPos=20;
        this.topPos=20;
        this.configButtonParam = new ScreenRectangleWidgetParam(0, this.topPos,20,20);
        int yPos = this.topPos + 17+18*rows;
        this.searchBoxParam =
                new ScreenRectangleWidgetParam(this.leftPos+1, yPos, Math.min(320,18*columns+13), Math.min(20,screen.height-yPos));
        this.sortTypeSwitchBoxParam = new ScreenRectangleWidgetParam(this.leftPos + 6,topPos + 5,77,12);
        ScreenLayoutMode layoutMode = new ScreenLayoutMode(leftPos,topPos,true,
                -1,-1);
        ScreenRectangleWidgetParam pageSwitchBarParam = new ScreenRectangleWidgetParam(leftPos-32,topPos+20,32,28);
        this.screenRenderer = FromResource.createLeftMode(screen,pageMetadata,layoutMode,pageSwitchBarParam);
        this.screenRenderer.init();
        this.pageX = screenRenderer.screenLayoutMode().menuXPos() + 8;
        this.pageY = screenRenderer.screenLayoutMode().menuYPos() + 17;
        this.pageXSize = columns*18;
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
        this.sorttypeReverseButton = Button.builder(Component.literal("⇅"),
                        btn->{
                            pageMetadata.switchSortReversed();
                            SyncedConfig.updateClientConfigAndSync(pageMetadata.getPlayer().getData(SYNCED_CONFIG).ofReverseSort());
                            pageMetadata.getDisplayingPage().syncContentToServer();
                        }
                )
                .pos(sortTypeSwitchBoxParam.XPos()+sortTypeSwitchBoxParam.XSize()+2,sortTypeSwitchBoxParam.YPos())
                .size(sortTypeSwitchBoxParam.YSize(),sortTypeSwitchBoxParam.YSize())
                .build();
        this.searchBox = new PoseTranslatedEditBox(screen.getMinecraft().font,
                this.searchBoxParam.XPos(),this.searchBoxParam.YPos(),this.searchBoxParam.XSize(),this.searchBoxParam.YSize()
                , Component.translatable("itemGroup.search"), 100.0F);
        this.sortTypeSwitchBox = new SortTypeSwitchBox(this, sortTypeSwitchBoxParam);

        searchBox.setValue(pageMetadata.searching());

        event.addListener(configButton);
        event.addListener(searchBox);
        event.addListener(sortTypeSwitchBox);
        event.addListener(sorttypeReverseButton);
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
        if(searchBox.isHovered() && !searchBox.isFocused()) guiGraphics.renderTooltip(screen.getMinecraft().font, List.of(
                Component.translatable("search.endinv.prefix.sharp"),
                Component.translatable("search.endinv.prefix.at"),
                Component.translatable("search.endinv.prefix.xor"),
                Component.translatable("search.endinv.prefix.star")
        ), Optional.empty(),mouseX,mouseY);
        if(sorttypeReverseButton.isHovered()) guiGraphics.renderTooltip(screen.getMinecraft().font,Component.translatable("button.endinv.reverse"),mouseX,mouseY);
    }

    protected boolean hasClickedOnPage(double mouseX,double mouseY){
        return mouseX>=(double) pageX && mouseX<=(double)pageX+pageXSize
                && mouseY>=(double) pageY && mouseY<=(double) pageY+pageYSize
                && !isHoveringOnSortBox;
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
    protected void pageClicked(double XOffset, double YOffset, int keyCode, ClickType clickType){
        //menu.syncContent();
        pageMetadata.getDisplayingPage().pageClicked(XOffset,YOffset,keyCode,clickType);
        PacketDistributor.sendToServer(
                new PageClickPayload(
                        screen.getMenu().containerId,
                        pageMetadata.getDisplayingPageId(),
                        XOffset,
                        YOffset,
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
            this.scrollYOffs = this.pageMetadata.subtractInputFromScroll(this.scrollYOffs,scrollY);
            this.pageMetadata.scrollTo(scrollYOffs);
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

    public void charTyped(ScreenEvent.CharacterTyped.Pre event){
        char codePoint = event.getCodePoint();
        int modifiers = event.getModifiers();
        if (!this.ignoreTextInput && pageMetadata.getDisplayingPage().hasSearchBar() && this.searchBox.isFocused()) {
            String s = this.searchBox.getValue();
            if (this.searchBox.charTyped(codePoint, modifiers)) {
                if (!Objects.equals(s, this.searchBox.getValue())) {
                    this.refreshSearchResults();
                }
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
    public PageMetaDataManager getPageMetadata() {
        return pageMetadata;
    }

    @Override
    public AbstractContainerScreen<?> getScreen() {
        return screen;
    }


}
