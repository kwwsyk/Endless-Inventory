package com.kwwsyk.endinv.common.client.gui;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.ClientModInfo;
import com.kwwsyk.endinv.common.client.KeyMappings;
import com.kwwsyk.endinv.common.client.gui.bg.*;
import com.kwwsyk.endinv.common.client.gui.page.DisplayPage;
import com.kwwsyk.endinv.common.client.gui.page.ItemPage;
import com.kwwsyk.endinv.common.client.gui.page.manager.PageManager;
import com.kwwsyk.endinv.common.client.gui.widget.PageSwitchBar;
import com.kwwsyk.endinv.common.client.gui.widget.SortTypeSwitchBox;
import com.kwwsyk.endinv.common.client.option.ClientConfigs;
import com.kwwsyk.endinv.common.client.option.PageSwitchBarConfig;
import com.kwwsyk.endinv.common.client.option.SFParamProvider;
import com.kwwsyk.endinv.common.client.option.TextureMode;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.network.payloads.toServer.CreativeItemModPayload;
import com.kwwsyk.endinv.common.network.payloads.toServer.QuickMoveToPagePayload;
import com.kwwsyk.endinv.common.network.payloads.toServer.StarItemPayload;
import com.kwwsyk.endinv.common.util.SortType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.kwwsyk.endinv.common.client.ClientModInfo.containerScreenHelper;
import static com.kwwsyk.endinv.common.client.ClientModInfo.inputHandler;

public class ScreenFramework implements PageManager, ContainerEventHandler, Renderable {
    @Nullable
    private static ScreenFramework INSTANCE;

    private final Minecraft mc;
    public final AbstractContainerScreen<?> screen;
    public final AbstractContainerMenu menu;

    private final IRectangleParam searchBoxParam,sortBoxParam,reverseSortButtonParam,configButtonParam,pageBarScrollUpButtonParam, pageBarScrollDownButtonParam;
    private final PageSwitchBar pageSwitchBar;
    public SFBgRenderer SFBgRenderer;
    public final int pageBarCount;

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

    private final int pageXSize;
    private int pageYSize;
    private int roughMouseX;
    private int roughMouseY;
    public EditBox searchBox;
    public SortTypeSwitchBox sortTypeSwitchBox;
    private Button reverseSortButton;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private boolean widgetsManagedByScreen;
    private boolean consumeNextRelease;
    //page meta data fields
    private int rows;
    private final int columns;

    private DisplayPage displayingPage;
    public final List<DisplayPage> pages;

    public ScreenFramework(EndlessInventoryScreen screen) {//when opening EIS
        this(screen, ClientConfigs.EIM_CONFIG.get().adjust(screen.getMenu().getBaseRows()));
    }

    public ScreenFramework(AttachingScreen<?> attachingScreen) {
        this(attachingScreen.screen, ClientConfigs.ATTACHED_MENU_CONFIG.get().adjust(attachingScreen.screen));
    }

    public ScreenFramework(
            AbstractContainerScreen<?> screen,
            SFParamProvider param
    ){
        this.screen = screen;
        this.mc = Minecraft.getInstance();
        this.menu = screen.getMenu();

        this.rows = param.rows();//rows and columns affect the structure
        this.columns = param.columns();
        this.leftPos = param.leftPos();
        this.topPos = param.topPos();
        TextureMode textureMode = param.textureMode();

        // Transparent mode uses the provided page width directly.
        this.imageWidth = textureMode==TextureMode.TRANSPARENT ? param.pageParamAdjusted().width() : 7 + columns * 18 + 7;
        this.imageHeight = screen.height;//todo precise
        //renderer may need structure and widget data --here?
        this.SFBgRenderer = screen instanceof EndlessInventoryScreen ?
                new FromResource.MenuMode(this) :
                textureMode != TextureMode.TRANSPARENT ?
                    new FromResource.LeftLayout(this) :
                    new Transparent(this);

        //---WIDGET DATA---
        //------PAGES-----------
        //------prepare page data---------
        IRectangleParam pageParam = param.pageParamAdjusted();
        IRectangleParam configButtonParam = param.configButtonA();
        var pageTabParam = param.pageTabParamAdjusted();
        this.pageX = pageParam.x();
        this.pageY = pageParam.y();
        this.pageXSize = pageParam.width();
        this.pageYSize = pageParam.height();
        this.pages = buildPages(param.pages());//is page a widget? but init page bar count indeed needs it.
        //page switch bar
        int pageTabMaxLength = getPageTabMaxLength(pageTabParam, pageParam, configButtonParam);
        this.pageBarCount = Math.min(pageTabParam.getMaxTabCount(getPages().size(), pageTabMaxLength), getPages().size());
        firstPageIndex = Math.max(0, Math.min(firstPageIndex, Math.max(0, getPages().size() - pageBarCount)));
        this.pageSwitchBar = new PageSwitchBar(this, pageTabParam, pageBarCount, param.textureMode());
        this.pageBarScrollUpButtonParam = param.pageTabDecA();
        this.pageBarScrollDownButtonParam = param.pageTabIncA();
        //other
        this.searchBoxParam = param.searchBoxA();
        this.configButtonParam = configButtonParam;
        this.sortBoxParam = param.sortBoxA();
        this.reverseSortButtonParam = param.reverseSortButtonA();


        //--base info should be all initialized--

        //---construct and switch displaying pages---
        switchPageWithId(displayingPageType.registerName);
        normalizeSortTypeForPage();

        //add widgets when base info are all prepared including displayingPage
        addWidgets();

        INSTANCE = this;
    }

    private static int getPageTabMaxLength(PageSwitchBarConfig.Param pageTabParam, IRectangleParam pageParam, IRectangleParam configButtonParam) {
        if(pageTabParam.direction_isVertical()) {
            return Math.max(0, configButtonParam.y() - pageParam.y());
        }
        return pageParam.width();
    }

    private void addWidgets() {
        Button configButton = this.configButtonParam.buildButton(Component.literal("⚙"),
                btn -> mc.setScreen(ClientModInfo.createConfigScreen(screen)));
        this.reverseSortButton = this.reverseSortButtonParam.buildButton(Component.literal("⇅"),
                btn -> {
                    reverseSort = !reverseSort;
                    if(getDisplayingPage() instanceof ItemPage page){
                        page.refreshItems();
                    }
                });
        this.searchBox = this.searchBoxParam.buildEditBox(mc.font,
                Component.translatableWithFallback("itemGroup.search", "Search Items"),
                s -> refreshSearchResults());
        this.sortTypeSwitchBox = new SortTypeSwitchBox(this,  sortBoxParam, getDisplayingPage().getSortTypes());

        this.searchBox.setValue(searching());
        syncPageControlsVisibility();

        if (pageBarCount < getPages().size()) {
            Button up = pageBarScrollUpButtonParam.buildButton(Component.literal("^"), btn -> {
                        if (firstPageIndex > 0) firstPageIndex--;
                    });
            Button down = pageBarScrollDownButtonParam.buildButton(Component.literal("v"), btn -> {
                        if (firstPageIndex + pageBarCount < getPages().size())
                            firstPageIndex++;
                    });
            widgets.add(up);
            widgets.add(down);
        }

        widgets.add(pageSwitchBar);
        widgets.add(configButton);
        widgets.add(reverseSortButton);
        widgets.add(searchBox);
        widgets.add(sortTypeSwitchBox);
    }

    public void addWidgetToScreen(Consumer<AbstractWidget> installer) {
        widgets.forEach(installer);
    }

    public void setWidgetsManagedByScreen(boolean widgetsManagedByScreen) {
        this.widgetsManagedByScreen = widgetsManagedByScreen;
    }

    public void consumeNextMouseRelease() {
        this.consumeNextRelease = true;
    }

    public void renderBg(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        SFBgRenderer.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        getDisplayingPage().renderBg(guiGraphics, partialTick, mouseX, mouseY);
        getDisplayingPage().render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private boolean isHoveringOnPage;

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        roughMouseX = mouseX;
        roughMouseY = mouseY;

        isHoveringOnPage = hasClickedOnPage(mouseX, mouseY);

        renderWidgetsNotManagedByScreen(guiGraphics, mouseX, mouseY, partialTick);

        if(!sortTypeSwitchBox.isMouseOver(mouseX, mouseY)) {
            getDisplayingPage().renderTooltip(guiGraphics, mouseX, mouseY);
        }

        if (searchBox.isHovered() && !searchBox.isFocused()) guiGraphics.renderTooltip(mc.font, List.of(
                Component.translatable("search.endinv.prefix.sharp"),
                Component.translatable("search.endinv.prefix.at"),
                Component.translatable("search.endinv.prefix.xor"),
                Component.translatable("search.endinv.prefix.star")
        ), Optional.empty(), mouseX, mouseY);
        if (reverseSortButton.isHovered())
            guiGraphics.renderTooltip(mc.font, Component.translatable("button.endinv.reverse"), mouseX, mouseY);
        sortTypeSwitchBox.renderTooltip(guiGraphics, mouseX, mouseY);

    }

    private void renderWidgetsNotManagedByScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if(widgetsManagedByScreen) {
            return;
        }
        for(AbstractWidget widget : widgets) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    protected boolean hasClickedOnPage(double mouseX, double mouseY) {
        return mouseX >= (double) getPageX() && mouseX <= (double) getPageX() + pageXSize
                && mouseY >= (double) getPageY() && mouseY <= (double) getPageY() + pageYSize
                && !sortTypeSwitchBox.isMouseOver(mouseX, mouseY);
    }

    public void pageSwitched(int index) {
        switchPageWithIndex(index + firstPageIndex);
        normalizeSortTypeForPage();
        this.sortTypeSwitchBox.setSortTypes(getDisplayingPage().getSortTypes());
        //getDisplayingPage().syncContentToServer();
        syncPageControlsVisibility();
    }

    private void syncPageControlsVisibility() {
        this.searchBox.setVisible(getDisplayingPage().hasSearchbox());
        syncSortControlsVisibility();
    }

    private void syncSortControlsVisibility() {
        boolean visible = getDisplayingPage().hasSortTypeSwitchBar();
        this.sortTypeSwitchBox.visible = visible;
        this.reverseSortButton.visible = visible;
    }

    public void switchSortTypeTo(SortType type) {
        if(!getDisplayingPage().getSortTypes().contains(type)) return;
        sortType = type;
        if(getDisplayingPage() instanceof ItemPage page){
            page.refreshItems();
        }
    }

    private void normalizeSortTypeForPage() {
        List<SortType> sortTypes = getDisplayingPage().getSortTypes();
        if(!sortTypes.contains(sortType) && !sortTypes.isEmpty()) {
            sortType = sortTypes.getFirst();
        }
    }

    private boolean isHovering(Slot slot, double mouseX, double mouseY) {
        int i = containerScreenHelper.getGuiLeft(screen);
        int j = containerScreenHelper.getGuiTop(screen);
        mouseX -= i;
        mouseY -= j;
        return mouseX >= (double) (slot.x - 1)
                && mouseX < (double) (slot.x + 16 + 1)
                && mouseY >= (double) (slot.y - 1)
                && mouseY < (double) (slot.y + 16 + 1);
    }

    public boolean overridePageHovering() {
        return sortTypeSwitchBox.isMouseOver(roughMouseX, roughMouseY);
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


    private ItemStack creativeQuickInsertedItem = ItemStack.EMPTY;
    private void slotQuickMoved(Slot clicked) {
        ItemStack itemStack = clicked.getItem().copy();
        if (menu instanceof CreativeModeInventoryScreen.ItemPickerMenu && clicked.index < 45 && menu.slots.size() >= 54) {
            if (ItemStack.isSameItemSameComponents(itemStack, creativeQuickInsertedItem)) {
                return;
            } else creativeQuickInsertedItem = itemStack;
            itemStack.setCount(itemStack.getMaxStackSize());
            getDisplayingPage().tryInsertItem(itemStack);
            ModInfo.getPacketDistributor().sendToServer(new CreativeItemModPayload(itemStack, true));
        } else {
            ItemStack remain = getDisplayingPage().tryInsertItem(itemStack);
            clicked.setByPlayer(remain);
            clicked.onTake(getPlayer(), itemStack);
            int payloadId = menu instanceof CreativeModeInventoryScreen.ItemPickerMenu
                    ? getItemPickerMenuSlotOffset(clicked)
                    : menu.slots.indexOf(clicked);
            if (payloadId >= 0) {
                ModInfo.getPacketDistributor().sendToServer(new QuickMoveToPagePayload(payloadId));
            }
        }// should use slot.getContainerSlot() instead of getSlotIndex()
        if (getDisplayingPage() instanceof ItemPage itemPage) {
            itemPage.requestRemoteContents();//send such payloads will not let server send contents
        }//another aspect is to check whether contents are synced across server and client
    }

    /**<p>Get correspond slot index between client creative menu and server player's inventory menu</p>
     * When client player is in {@link CreativeModeInventoryScreen.ItemPickerMenu} player on server only holds {@link net.minecraft.world.inventory.InventoryMenu}<br>
     * <p>
     * In {@code ItemPickerMenu} there are two situations:<br>
     *     1.When player is picking items in tab, there are 9*5+9 slots, slot in hotbar starts with index 45 ends with 53.<br>
     *     2.When player is in "Survival Inventory", the {@code slot.index} is always 0, only {@link Slot#getContainerSlot()} is valid.<br>
     *     To be noticed, {@code Slot#getSlotIndex()} returns same value {@code Slot.slot} but it only exists in Forge's lib. This means use this in Fabric running will throw {@link NoSuchMethodError}</p>
     * @param clicked slot clicked in Inventory by creative player on client.
     * @return slot index that can locate correspond inventory slot used in {@link QuickMoveToPagePayload}
     *
     * @since 1.20.1
     */
    private int getItemPickerMenuSlotOffset(Slot clicked){
        int originalIndex = clicked.index;
        if(originalIndex==0 && clicked.getContainerSlot() >0) return clicked.getContainerSlot();
        if(originalIndex<45) return originalIndex;
        return originalIndex - 9;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int keyCode) {
        if (!searchBoxParam.hasClickedOn((int) mouseX, (int) mouseY)) {
            searchBox.setFocused(false);
        } else {
            searchBox.setFocused(true);//this is what JEI behaves
            if (keyCode == 1) {
                searchBox.setValue("");
                refreshSearchResults();
                return true;
            }
        }
        if(mouseClickedOnWidgets(mouseX, mouseY, keyCode)) {
            return true;
        }
        //handle menu item quick move
        boolean flg = inputHandler.isActiveAndMatches(KeyMappings.QUICK_MOVE, InputConstants.Type.MOUSE.getOrCreate(keyCode));
        if (flg) {
            Slot clicked = findSlot(mouseX, mouseY);
            if (clicked != null && clicked.hasItem()) {
                slotQuickMoved(clicked);
                return true;
            }
        }
        if (hasClickedOnPage(mouseX, mouseY)) {
            sortTypeSwitchBox.setOpen(false);
            return getDisplayingPage().mouseClicked(mouseX - getPageX(), mouseY - getPageY(), keyCode);
        }
        return false;
    }


    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        ItemStack itemstack = this.menu.getCarried();
        //ignore QUICK_CRAFT and touchscreen
        if (!itemstack.isEmpty() || mc.options.touchscreen().get())
            return false;
        //CTRL-click(default) to quick move items as behavior as Mouse Tweaks
        if (inputHandler.isActiveAndMatches(KeyMappings.QUICK_MOVE, InputConstants.Type.MOUSE.getOrCreate(button))) {
            Slot clicked = findSlot(mouseX, mouseY);
            if (clicked != null && clicked.hasItem()) {
                slotQuickMoved(clicked);
                return true;
            }
        }

        if (hasClickedOnPage(mouseX, mouseY)) {
            return getDisplayingPage().mouseDragged(mouseX - getPageX(), mouseY - getPageY(), button, dragX, dragY);
        }
        return false;
    }

    @Override
    public boolean isDragging() {
        return false;
    }

    /**
     * Sets if the GUI element is dragging or not.
     *
     * @param isDragging the dragging state of the GUI element.
     */
    @Override
    public void setDragging(boolean isDragging) {

    }

    public boolean mouseReleased(double mouseX, double mouseY, int keyCode) {
        creativeQuickInsertedItem = ItemStack.EMPTY;

        DisplayPage displayingPage = getDisplayingPage();
        displayingPage.release();
        if(consumeNextRelease) {
            consumeNextRelease = false;
            return true;
        }
        if (hasClickedOnPage(mouseX, mouseY)) {
            return displayingPage.mouseReleased(mouseX - getPageX(), mouseY - getPageY(), keyCode);
        }
        return false;
    }


    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (hasClickedOnPage(mouseX, mouseY)) {
            return getDisplayingPage().mouseScrolled(mouseX - getPageX(), mouseY - getPageY(), scrollY);
        }
        return false;
    }

    private boolean mouseClickedOnWidgets(double mouseX, double mouseY, int keyCode) {
        if(sortTypeSwitchBox.mouseClicked(mouseX, mouseY, keyCode)) {
            consumeNextMouseRelease();
            return true;
        }
        for(AbstractWidget widget : widgets) {
            if(widget != searchBox && widget != sortTypeSwitchBox && widget.mouseClicked(mouseX, mouseY, keyCode)) {
                consumeNextMouseRelease();
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (getDisplayingPage().hasSearchbox() && searchBox.isFocused()) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                return false;
            }
            searchBox.keyPressed(keyCode, scanCode, modifiers);
            // Printable keys arrive in charTyped; keep them out of container shortcuts.
            return true;
        }

        if (inputHandler.isActiveAndMatches(KeyMappings.STAR_ITEM, InputConstants.getKey(keyCode, scanCode))) {
            Slot clicked = findSlot(roughMouseX, roughMouseY);
            if (clicked != null && clicked.hasItem()) {
                ItemStack itemStack = clicked.getItem();
                ModInfo.getPacketDistributor().sendToServer(new StarItemPayload(itemStack, true));
                getDisplayingPage().sendChangesToServer();
                return true;
            }
        }

        if (isHoveringOnPage) {
            getDisplayingPage().keyPressed(keyCode, scanCode, modifiers, roughMouseX - getPageX(), roughMouseY - getPageY());
        }
        for(AbstractWidget widget : widgets) {
            if(widget.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        for (AbstractWidget widget : widgets) {
            if(widget.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @org.jetbrains.annotations.Nullable GuiEventListener getFocused() {
        return null;
    }

    /**
     * Sets the focus state of the GUI element.
     *
     * @param focused the focused GUI element.
     */
    @Override
    public void setFocused(@org.jetbrains.annotations.Nullable GuiEventListener focused) {

    }

    public void onClose() {
        INSTANCE = null;
    }

    public void refreshSearchResults() {
        searching = searchBox.getValue();
        if(getDisplayingPage() instanceof ItemPage page){
            page.refreshItems();
        }
    }

    public static @Nullable ScreenFramework getInstance() {
        return INSTANCE;
    }

    public int getPageX() {
        // Combine the static anchor and the debug offset for consistent hit tests.
        return pageX;
    }

    public int getPageY() {
        // Combine the static anchor and the debug offset for consistent hit tests.
        return pageY;
    }

    public void move(int deltaX, int deltaY) {
        // Support debug nudging without rebuilding the widget tree.
        this.pageX += deltaX;
        this.pageY += deltaY;
        DisplayPage current = getDisplayingPage();
        if (current != null) {
            current.syncPos(deltaX, deltaY);
        }
    }

    public void resizePageRows(int rows) {
        // Mirror menu row changes so the client page layout stays aligned with the server menu.
        this.rows = Math.max(1, rows);
        this.pageYSize = this.rows * 18 + 17 + 12;
        DisplayPage current = getDisplayingPage();
        if (current != null) {
            current.resize(this.rows);
        }
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight);
    }

    /**
     * Checks if the given mouse coordinates are over the GUI element.
     * <p>
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     * @return {@code true} if the mouse is over the GUI element, {@code false} otherwise.
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return new ScreenRectangleWidgetParam(leftPos, topPos, imageWidth, imageHeight).hasClickedOn(mouseX, mouseY);
    }

    /**
     * Sets the focus state of the GUI element.
     *
     * @param focused {@code true} to apply focus, {@code false} to remove focus
     */
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    private boolean focused;

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public AbstractContainerMenu getMenu() {
        return menu;
    }

    @Override
    public SourceInventory getSourceInventory() {
        return CachedSrcInv.INSTANCE;
    }

    @Override
    public Player getPlayer() {
        return mc.player;
    }

    @Override
    public void switchPageWithIndex(int index) {
        this.displayingPage = pages.get(index);
        displayingPage.initializeContents();
    }

    @Override
    public int rows() {
        return rows;
    }

    @Override
    public int columns() {
        return columns;
    }

    @Override
    public SortType sortType() {
        return sortType;
    }

    @Override
    public void setSortType(SortType sortType1) {
        sortType = sortType1;
    }

    @Override
    public boolean isSortReversed() {
        return reverseSort;
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
    public void setSearching(String searching1) {
        searching = searching1;
    }

    @Override
    public List<DisplayPage> getPages() {
        return pages;
    }

    @Override
    public DisplayPage getDisplayingPage() {
        return displayingPage;
    }
}
