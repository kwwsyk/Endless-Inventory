package com.kwwsyk.endinv.common.client.gui;

import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.menu.page.DisplayPage;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageQuickMoveHandler;
import com.kwwsyk.endinv.common.network.payloads.PageData;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.network.payloads.toClient.EndInvConfig;
import com.kwwsyk.endinv.common.network.payloads.toClient.EndInvMetadata;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.kwwsyk.endinv.common.ModRegistries.NbtAttachments.getSyncedConfig;

public class AttachedScreen<T extends AbstractContainerMenu> implements SortTypeSwitcher{

    public final AbstractContainerScreen<T> screen;
    private ScreenFrameWork frameWork;
    private final PageMetaDataManager pageMetadata = new PageMetaDataManager() {
        @Override
        public AbstractContainerMenu getMenu() {
            return screen.getMenu();
        }

        @Override
        public SourceInventory getSourceInventory() {
            return CachedSrcInv.INSTANCE;
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
            SyncedConfig.updateSyncedConfig(getSyncedConfig().computeIfAbsent(player).pageKeyChanged(displayingPage.id));
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
        public void sendEndInvData() {//do nop as in the client
        }

    };
    private final PageQuickMoveHandler quickMoveHandler;
    private boolean isHoveringOnSortBox;
    private DisplayPage displayingPage;
    public final List<DisplayPage> pages;
    private int rows;
    private int columns;

    public SortType sortType;
    public String searching;
    private Player player;
    private EndInvMetadata endInvMetadata;
    private boolean reverseSort;


    public AttachedScreen(AbstractContainerScreen<T> screen){
        this.screen = screen;
        this.pages = pageMetadata.buildPages();
        assert Minecraft.getInstance().player != null;
        this.player = Minecraft.getInstance().player;
        PageData data = getSyncedConfig().computeIfAbsent(player).pageData();
        this.rows = data.rows();
        this.columns = data.columns();
        this.sortType = data.sortType();
        this.searching=data.search();
        this.pageMetadata.switchPageWithId(data.pageRegKey());

        this.endInvMetadata = new EndInvMetadata(0,Integer.MAX_VALUE,false,EndInvConfig.DEFAULT);
        this.quickMoveHandler = new PageQuickMoveHandler(this.pageMetadata);
    }

    public void setEndInvMetadata(EndInvMetadata metadata){
        this.endInvMetadata = metadata;
    }

    public void init(IScreenEvent event){
        this.frameWork = new ScreenFrameWork(this);

        frameWork.addWidgetToScreen(event::addListener);
    }

    public void renderPre(IScreenEvent event) {
        this.isHoveringOnSortBox = false;
    }

    public void render(IScreenEvent event) {
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        float partialTick = event.getPartialTick();

        frameWork.renderBg(guiGraphics,mouseX,mouseY,partialTick);
        frameWork.render(guiGraphics,mouseX,mouseY,partialTick);
    }


    public void mouseClicked(IScreenEvent event) {
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int keyCode = event.getButton();
        boolean isActionOverride = frameWork.mouseClicked(mouseX,mouseY,keyCode);
        event.setCanceled(isActionOverride);
    }


    public void mouseReleased(IScreenEvent event) {
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int keyCode = event.getButton();
        event.setCanceled(frameWork.mouseReleased(mouseX,mouseY,keyCode));
    }

    public void mouseDragged(IScreenEvent event) {
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int button = event.getMouseButton();
        double dragX = event.getDragX();
        double dragY = event.getDragY();

        event.setCanceled(frameWork.mouseDragged(mouseX,mouseY,button,dragX,dragY));
    }

    public void mouseScrolled(IScreenEvent event) {
        double scrollY = event.getScrollDeltaY();
        frameWork.mouseScrolled(event.getMouseX(),event.getMouseY(),event.getScrollDeltaX(),scrollY);
    }

    public void keyPressed(IScreenEvent event) {
        int keyCode = event.getKeyCode();
        int scanCode = event.getScanCode();
        int modifiers = event.getModifiers();
        event.setCanceled(frameWork.keyPressed(keyCode,scanCode,modifiers));
    }

    public void charTyped(IScreenEvent event) {
        char codePoint = event.getCodePoint();
        int modifiers = event.getModifiers();
        event.setCanceled(frameWork.charTyped(codePoint,modifiers));
    }

    public void closed(IScreenEvent event){
        frameWork.onClose();
    }

    @Override
    public void switchSortTypeTo(SortType type) {
        this.sortType = type;
        SyncedConfig.updateSyncedConfig(getSyncedConfig().computeIfAbsent(player).sortTypeChanged(type));
        pageMetadata.getDisplayingPage().release();
        pageMetadata.getDisplayingPage().syncContentToServer();
    }

    @Override
    public void setHoveringOnSortBox(boolean isHovering) {
        this.isHoveringOnSortBox  = isHovering;
    }

    @Override
    public boolean isHoveringOnSortBox() {
        return isHoveringOnSortBox;
    }

    @Override
    public PageMetaDataManager getPageMetadata() {
        return pageMetadata;
    }

    @Override
    public AbstractContainerScreen<?> getScreen() {
        return screen;
    }

    public ScreenFrameWork getFrameWork(){
        return frameWork;
    }

    public List<Rect2i> getArea(){
        return List.of(new Rect2i(frameWork.leftPos, frameWork.topPos, frameWork.imageWidth, frameWork.imageHeight));
    }
}
