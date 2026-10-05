package com.kwwsyk.endinv.common.client.gui;

import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.client.gui.bg.IRectangleParam;
import com.kwwsyk.endinv.common.client.gui.bg.SFBgRenderer;
import com.kwwsyk.endinv.common.client.gui.page.DisplayPage;
import com.kwwsyk.endinv.common.client.gui.page.manager.PageManager;
import com.kwwsyk.endinv.common.client.gui.widget.PageSwitchBar;
import com.kwwsyk.endinv.common.client.gui.widget.SortTypeSwitchBox;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
//unused, test for future frame
public  class EndinvScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements PageManager {

    public static final String CONFIG_ICON = "⚙";
    public static final String REVERSE_SORT_ICON = "⇅";
    @Nullable
    private static ScreenFramework INSTANCE;

    private IRectangleParam searchBoxParam,sortBoxParam,reverseSortButtonParam,configButtonParam,pageBarScrollUpButtonParam, pageBarScrollDownButtonParam;
    private PageSwitchBar pageSwitchBar;
    public SFBgRenderer SFBgRenderer;
    public int pageBarCount;

    public static int firstPageIndex = 0;
    public static String searching = "";
    public static SortType sortType = SortType.DEFAULT;
    public static boolean reverseSort = false;
    public static PageType displayingPageType = PageType.ALL_ITEMS;

    //Always pageBarCount + firstPageIndex <= meta.getPages.size()
    public int leftPos, topPos;
    public int imageWidth, imageHeight;
    private int pageX;
    private int pageY;

    private int pageXSize;
    private int pageYSize;
    private int roughMouseX;
    private int roughMouseY;
    public EditBox searchBox;
    public SortTypeSwitchBox sortTypeSwitchBox;
    private Button reverseSortButton;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    //page meta data fields
    private int rows;
    private int columns;

    private DisplayPage displayingPage;
    public final List<DisplayPage> pages = new ArrayList<>();

    public EndinvScreen(
            T menu,
            Inventory inventory,
            Component title,
            int imageWidth,
            int imageHeight
    ) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    /**
     * the return value of {@link #getPages()} shall be from this.
     *
     * @param displayingPages
     */
    @Override
    public List<DisplayPage> buildPages(List<PageType> displayingPages) {
        return PageManager.super.buildPages(displayingPages);
    }

    @Override
    public void extractCarriedItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractCarriedItem(graphics, mouseX, mouseY);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        super.extractSlot(graphics, slot, mouseX, mouseY);
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected boolean isHovering(int left, int top, int w, int h, double xm, double ym) {
        return super.isHovering(left, top, w, h, xm, ym);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public List<DisplayPage> getPages() {
        return List.of();
    }

    @Override
    public DisplayPage getDisplayingPage() {
        return null;
    }

    @Override
    public SourceInventory getSourceInventory() {
        return null;
    }

    @Override
    public Player getPlayer() {
        return null;
    }

    @Override
    public void switchPageWithIndex(int index) {

    }

    @Override
    public int rows() {
        return 0;
    }

    @Override
    public int columns() {
        return 0;
    }

    @Override
    public SortType sortType() {
        return null;
    }

    @Override
    public void setSortType(SortType sortType) {

    }

    @Override
    public boolean isSortReversed() {
        return false;
    }

    @Override
    public void setSortReversed(boolean reversed) {

    }

    @Override
    public String searching() {
        return "";
    }

    @Override
    public void setSearching(String searching) {

    }
}
