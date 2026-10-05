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
import com.kwwsyk.endinv.common.client.option.AttachedMenuScreenLayout;
import com.kwwsyk.endinv.common.client.option.ClientConfigs;
import com.kwwsyk.endinv.common.client.option.EIMConfig;
import com.kwwsyk.endinv.common.client.option.TextureMode;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.mixin.ScreenMixin;
import com.kwwsyk.endinv.common.network.payloads.toServer.CreativeItemModPayload;
import com.kwwsyk.endinv.common.network.payloads.toServer.QuickMoveToPagePayload;
import com.kwwsyk.endinv.common.network.payloads.toServer.StarItemPayload;
import com.kwwsyk.endinv.common.util.SortType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.kwwsyk.endinv.common.client.ClientModInfo.containerScreenHelper;
import static com.kwwsyk.endinv.common.client.ClientModInfo.inputHandler;

public class ScreenFramework implements PageManager, GuiEventListener {
    public static final String CONFIG_ICON = "⚙";
    public static final String REVERSE_SORT_ICON = "⇅";
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
    //page meta data fields
    private int rows;
    private final int columns;

    private DisplayPage displayingPage;
    public final List<DisplayPage> pages;

    public ScreenFramework(EndlessInventoryScreen screen) {//when opening EIS
        //------MOST BASE DATA-------
        this.screen = screen;
        this.mc = Minecraft.getInstance();
        this.menu = screen.getMenu();

        //---STRUCTURE AND RENDER DATA---
        this.leftPos = screen.getGuiLeft();
        this.topPos = screen.getGuiTop();
        this.imageWidth = screen.getXSize();
        this.imageHeight = screen.getYSize();
        //row and columns affects the structure
        EIMConfig.Param param = ClientConfigs.EIM_CONFIG.get().adjust();
        this.rows = param.rows();//row and columns affects the structure
        this.columns = param.columns();
        //renderer may need structure and widget data --here YES: needs row/col/left/top...
        this.SFBgRenderer = new FromResource.MenuMode(this, param.pageSwitchBarConfig().tabParam());

        //------WIDGET DATA-------
        //------PAGES-----------
        //------prepare page data---------
        this.pageX = param.pageParamAdjusted().x();
        this.pageY = param.pageParamAdjusted().y();
        this.pageXSize = param.pageParamAdjusted().width();
        this.pageYSize = param.pageParamAdjusted().height();
        this.pages = buildPages(param.pages());//is page a widget? but init page bar count indeed needs it.
        //page switch bar
        this.pageBarCount = Math.min(param.pageTabCount(), getPages().size());
        this.pageSwitchBar = new PageSwitchBar(this, param.pageSwitchBarConfig().applyOffset(leftPos, topPos), pageBarCount, param.textureMode());
        this.pageBarScrollUpButtonParam = param.pageTabIncA();
        this.pageBarScrollDownButtonParam = param.pageTabDecA();
        //other
        this.searchBoxParam = param.searchBoxA();
        this.configButtonParam = param.configButtonA();
        this.sortBoxParam = param.sortBoxA();
        this.reverseSortButtonParam = param.reverseSortButtonA();


        //--base info should be all initialized--

        //---construct and switch displaying pages---
        switchPageWithId(displayingPageType.registerName);
        //add widgets when base info are all prepared including displayingPage
        addWidgets();

        INSTANCE = this;
    }

    public ScreenFramework(AttachingScreen<?> attachingScreen) {
        //------MOST BASE DATA-------
        this.screen = attachingScreen.screen;
        this.mc = Minecraft.getInstance();
        this.menu = attachingScreen.menu;

        //---STRUCTURE AND RENDER DATA---
        AttachedMenuScreenLayout param = ClientConfigs.ATTACHED_MENU_CONFIG.get().adjust(screen);
        this.rows = param.rows();//rows and columns affect the structure
        this.columns = param.columns();
        this.leftPos = param.leftPos();
        this.topPos = param.topPos();
        TextureMode textureMode = param.textureMode();
        // For resource-based (vanilla-like) backgrounds, the 9-slice widths are 7 (left) + 18*columns + 7 (right)
        // Transparent mode uses the provided page width directly.
        this.imageWidth = textureMode==TextureMode.TRANSPARENT ? param.pageParamAdjusted().width() : 7 + columns * 18 + 7;
        this.imageHeight = screen.height;//todo precise
        //renderer may need structure and widget data --here?
        this.SFBgRenderer = textureMode != TextureMode.TRANSPARENT ?
                new FromResource.LeftLayout(this, param.pageTabParamAdjusted()) :
                new Transparent(this, param.pageTabParamAdjusted());

        //---WIDGET DATA---
        //------PAGES-----------
        //------prepare page data---------
        this.pageX = param.pageParamAdjusted().x();
        this.pageY = param.pageParamAdjusted().y();
        this.pageXSize = param.pageParamAdjusted().width();
        this.pageYSize = param.pageParamAdjusted().height();
        this.pages = buildPages(param.pages());//is page a widget? but init page bar count indeed needs it.
        //page switch bar
        this.pageBarCount = Math.min(param.pageTabCount(), getPages().size());
        this.pageSwitchBar = new PageSwitchBar(this, param.pageSwitchBarParam.applyOffset(leftPos,topPos), pageBarCount, param.textureMode());
        this.pageBarScrollUpButtonParam = param.pageTabIncA();
        this.pageBarScrollDownButtonParam = param.pageTabDecA();
        //other
        this.searchBoxParam = param.searchBoxA();
        this.configButtonParam = param.configButtonA();
        this.sortBoxParam = param.sortBoxA();
        this.reverseSortButtonParam = param.reverseSortButtonA();


        //--base info should be all initialized--

        //---construct and switch displaying pages---
        switchPageWithId(displayingPageType.registerName);

        //add widgets when base info are all prepared including displayingPage
        addWidgets();

        INSTANCE = this;
    }


    private void addWidgets() {
        Button configButton = Button.builder(Component.literal(CONFIG_ICON),
                        btn -> {
                            mc.gui.setScreen(ClientModInfo.createConfigScreen(screen));
                        })
                .pos(this.configButtonParam.x(), this.configButtonParam.y())
                .size(this.configButtonParam.width(), this.configButtonParam.height())
                .build();
        this.reverseSortButton = Button.builder(Component.literal(REVERSE_SORT_ICON),
                        btn -> {
                            reverseSort = !reverseSort;
                            if(getDisplayingPage() instanceof ItemPage page){
                                page.refreshItems();
                            }
                        }
                )
                .pos(reverseSortButtonParam.x(), reverseSortButtonParam.y())
                .size(reverseSortButtonParam.height(), reverseSortButtonParam.height())
                .build();
        this.searchBox = new EditBox(mc.font,
                this.searchBoxParam.x(), this.searchBoxParam.y(), this.searchBoxParam.width(), this.searchBoxParam.height(),
                Component.translatable("itemGroup.search"));
        this.sortTypeSwitchBox = new SortTypeSwitchBox(this,  sortBoxParam);

        this.searchBox.setValue(searching());

        if (pageBarCount < getPages().size()) {
            Button up = Button.builder(Component.literal("^"), btn -> {
                        if (firstPageIndex > 0) firstPageIndex--;
                    })
                    .pos(pageBarScrollUpButtonParam.x(), pageBarScrollUpButtonParam.y())
                    .size(pageBarScrollUpButtonParam.width(), pageBarScrollUpButtonParam.height())
                    .build();
            Button down = Button.builder(Component.literal("v"), btn -> {
                        if (firstPageIndex + pageBarCount < getPages().size())
                            firstPageIndex++;
                    })
                    .pos(pageBarScrollDownButtonParam.x(), pageBarScrollDownButtonParam.y())
                    .size(pageBarScrollDownButtonParam.width(), pageBarScrollDownButtonParam.height())
                    .build();
            widgets.add(up);
            widgets.add(down);
        }

        widgets.add(pageSwitchBar);
        if(screen instanceof EndlessInventoryScreen) widgets.add(configButton);
        widgets.add(reverseSortButton);
        widgets.add(searchBox);
        //widgets.add(sortTypeSwitchBox);
    }

    public void addWidgetToScreen(Consumer<AbstractWidget> installer) {
        widgets.forEach(installer);
    }

    public void renderBg(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        SFBgRenderer.renderBg(GuiGraphicsExtractor, partialTick, mouseX, mouseY);
        getDisplayingPage().renderBg(GuiGraphicsExtractor, partialTick, mouseX, mouseY);
    }

    private boolean isHoveringOnPage;

    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        roughMouseX = mouseX;
        roughMouseY = mouseY;

        isHoveringOnPage = hasClickedOnPage(mouseX, mouseY);

        getDisplayingPage().render(GuiGraphicsExtractor, mouseX, mouseY, partialTick);

        if (searchBox.isHovered() && !searchBox.isFocused())
            GuiGraphicsExtractor.setTooltipForNextFrame(
                    mc.font,
                    List.of(
                            Component.translatable("search.endinv.prefix.sharp"),
                            Component.translatable("search.endinv.prefix.at"),
                            Component.translatable("search.endinv.prefix.xor"),
                            Component.translatable("search.endinv.prefix.star")
                    ),
                    java.util.Optional.empty(),
                    mouseX,
                    mouseY
            );
        if (reverseSortButton.isHovered())
            GuiGraphicsExtractor.setTooltipForNextFrame(
                    mc.font,
                    List.of(Component.translatable("button.endinv.reverse")),
                    java.util.Optional.empty(),
                    mouseX,
                    mouseY
            );
        this.sortTypeSwitchBox.extractRenderState(GuiGraphicsExtractor, mouseX, mouseY, partialTick);
    }

    protected boolean hasClickedOnPage(double mouseX, double mouseY) {
        return mouseX >= (double) getPageX() && mouseX <= (double) getPageX() + pageXSize
                && mouseY >= (double) getPageY() && mouseY <= (double) getPageY() + pageYSize
                && !sortTypeSwitchBox.isHovered();
    }

    public void pageSwitched(int index) {
        switchPageWithIndex(index + firstPageIndex);
        //getDisplayingPage().syncContentToServer();
        this.searchBox.setVisible(getDisplayingPage().hasSearchbox());
        this.sortTypeSwitchBox.visible = getDisplayingPage().hasSortTypeSwitchBar();
    }

    public void switchSortTypeTo(SortType type) {
        sortType = type;
        if(getDisplayingPage() instanceof ItemPage page){
            page.refreshItems();
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
        return sortTypeSwitchBox.isHovered();
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
     *     To be noticed, {@code __may_deprecated_1.21.4__ Slot#getSlotIndex()} returns same value {@code Slot.slot} but it only exists in Forge's lib. This means use this in Fabric running will throw {@link NoSuchMethodError}</p>
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

    public boolean mouseClicked(MouseButtonEvent clickEvent, boolean pre) {
        double mouseX = clickEvent.x();
        double mouseY = clickEvent.y();
        int keyCode = clickEvent.button();
        if(this.sortTypeSwitchBox.mouseClicked(clickEvent, pre)){
            return true;
        }

        if (!searchBoxParam.hasClickedOn(mouseX, mouseY)) {
            searchBox.setFocused(false);
        } else {
            searchBox.setFocused(true);//this is what JEI behaves
            if (keyCode == InputConstants.MOUSE_BUTTON_RIGHT) {
                searchBox.setValue("");
                refreshSearchResults();
                return true;
            }
        }
        //handle menu item quick move
        boolean flg = inputHandler.isActiveAndMatches(KeyMappings.QUICK_MOVE, clickEvent);
        if (flg) {
            Slot clicked = findSlot(mouseX, mouseY);
            if (clicked != null && clicked.hasItem()) {
                slotQuickMoved(clicked);
                return true;
            }
        }
//        //handle clicked on the page switch bar
        // Use widget onClick to avoid signature mismatch across versions
        pageSwitchBar.onClick(clickEvent, pre);//todo ?
        // onClick reports via framework.pageSwitched(), treat as handled when mouse is over tab area
        if (mouseX >= pageSwitchBar.getX() && mouseX < pageSwitchBar.getX() + pageSwitchBar.getWidth()
                && mouseY >= pageSwitchBar.getY() && mouseY < pageSwitchBar.getY() + pageSwitchBar.getHeight()) {
            return true;
        }
//        int pageIndex = hasClickedOnPageSwitchBar(mouseX, mouseY);
//        if (pageIndex >= 0) {
//            pageSwitched(pageIndex);
//            return true;
//        }
        //
        if (hasClickedOnPage(mouseX, mouseY)) {
            sortTypeSwitchBox.setOpen(false);
            return getDisplayingPage().mouseClicked(new MouseButtonEvent(mouseX - pageX, mouseY - pageY, clickEvent.buttonInfo()), pre);
        }
        return false;
    }


    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        ItemStack itemstack = this.menu.getCarried();
        //ignore QUICK_CRAFT and touchscreen
        if (!itemstack.isEmpty())
            return false;
        //CTRL-click(default) to quick move items as behavior as Mouse Tweaks
        if (inputHandler.isActiveAndMatches(KeyMappings.QUICK_MOVE, event)) {
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

    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        creativeQuickInsertedItem = ItemStack.EMPTY;

        DisplayPage displayingPage = getDisplayingPage();
        displayingPage.release();
        if (hasClickedOnPage(mouseX, mouseY)) {
            return displayingPage.mouseReleased(new MouseButtonEvent(mouseX - getPageX(), mouseY - getPageY(), event.buttonInfo()));
        }
        return false;
    }


    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hasClickedOnPage(mouseX, mouseY)) {
            return getDisplayingPage().mouseScrolled(mouseX - getPageX(), mouseY - getPageY(), scrollY);
        }
        return false;
    }

    private boolean ignoreTextInput;

    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();
        this.ignoreTextInput = false;

        if (inputHandler.isActiveAndMatches(KeyMappings.STAR_ITEM, event)) {
            Slot clicked = findSlot(roughMouseX, roughMouseY);
            if (clicked != null && clicked.hasItem()) {
                ItemStack itemStack = clicked.getItem();
                ModInfo.getPacketDistributor().sendToServer(new StarItemPayload(itemStack, true));
                getDisplayingPage().sendChangesToServer();
                return true;
            }
        }

        boolean flag = false;
        if (isHoveringOnPage) {
            flag = getDisplayingPage().keyPressed(keyCode, scanCode, modifiers, roughMouseX - getPageX(), roughMouseY - getPageY());
        }
        if (flag) {
            this.ignoreTextInput = true;
            return true;
        }

        // Let default input pipeline handle focused widgets in 1.21.11
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        if (this.ignoreTextInput || !getDisplayingPage().hasSearchbox()) {
            return false;
        } else {
            // Defer text input handling to the widget pipeline
            return false;
        }
    }

    public void onClose() {
        INSTANCE = null;
        for(var widget:widgets){
            ((ScreenMixin)screen).endinv$invokeRemoveWidget(widget);
        }
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
        displayingPageType = displayingPage.getPageType();
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

