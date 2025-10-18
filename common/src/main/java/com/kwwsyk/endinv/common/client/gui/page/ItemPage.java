package com.kwwsyk.endinv.common.client.gui.page;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.common.client.gui.page.manager.PageManager;
import com.kwwsyk.endinv.common.client.option.CachedConfig;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.network.payloads.toServer.CreativeItemModPayload;
import com.kwwsyk.endinv.common.network.payloads.toServer.ItemClickPayload;
import com.kwwsyk.endinv.common.network.payloads.toServer.ItemPageContext;
import com.kwwsyk.endinv.common.network.payloads.toServer.StarItemPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

import static com.kwwsyk.endinv.common.ModInfo.getPacketDistributor;

/**Page that holds ItemStack list. It's the main type of pages.
 *
 */
public abstract class ItemPage extends DisplayPage {

    private static final Logger LOGGER = LogUtils.getLogger();

    protected NonNullList<ItemStack> items;

    protected int startIndex = 0;

    protected int length;

    protected List<ItemStack> inQueueStacks = null;

    public ItemPage(PageType pageType, PageManager manager) {
        super(pageType, manager);
        this.length = meta.rows()* meta.columns();
    }

    /**
     * Adjust the number of slots maintained by the item page so it matches the latest menu layout.
     */
    @Override
    public void resize(int rows) {
        int targetRows = Math.max(1, rows);
        this.length = targetRows * meta.columns();
        initializeContents(this.startIndex, this.length);
    }

    @Override
    public void scrollTo(float pos) {
        int startIndex = getRowIndexForScroll(pos)* meta.columns();
        this.initializeContents(startIndex,this.length);
    }

    /**Build or reload item page's content by SourceInv {@link CachedSrcInv}
     *  and it will set back to unscrolled status.
     */
    @Override
    public void initializeContents() {
        initializeContents(0,meta.rows()*meta.columns());
    }

    /**The <em>init</em> method of ItemPage, it will change the startIndex and length of page and init a new {@link #items} list.
     * Then it invokes {@link #refreshItems()} to build showing items
     * @param startIndex the index of the item first displayed in EndInv(SrcInv)
     * @param length the count of the item should be displayed, should equal to rows*columns
     */
    protected void initializeContents(int startIndex, int length) {
        this.startIndex = startIndex;
        this.length = Math.min(length, meta.rows()* meta.columns());
        // ensure internal invariants
        if(this.items==null || this.items.size()!=this.length) this.items = NonNullList.withSize(this.length, ItemStack.EMPTY);
        if(length != this.items.size()){
            this.items = NonNullList.withSize(length,ItemStack.EMPTY);
        }
        release();
        this.refreshItems();
    }

    /**The <em>refresh</em> method of ItemPage, this method shall keep the startIndex and length and fill {@link #items}
     * with such and srcInv.<br>
     * This may invoke {@link #requestRemoteContents()} or other send packet methods.
     * So be caution use this method especially on receiving request-remote-callback packets. Like {@link com.kwwsyk.endinv.common.network.payloads.toClient.EndInvContent}
     */
    public abstract void refreshItems();

    public abstract void requestRemoteContents();

    /**The change usually means pageMetaData changes and called by framework in sort,search,... changes.<br>
     * For ItemPage and ItemDisplay: also used to request contents as sending {@link ItemPageContext} has such side effect.
     */
    public void sendChangesToServer() {
        var layout = CachedConfig.currentLayout();
        getPacketDistributor().sendToServer(new ItemPageContext(startIndex, length, layout));//send this packet will receive a content packet as callback
    }

    public int getStartIndex(){
        return this.startIndex;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public boolean canScroll() {
        return startIndex>0 ||(startIndex+length <= srcInv.getItemSize());
    }

    public int getSlotByMouseOffset(double XOffset, double YOffset){
        if(XOffset<0||YOffset<0||XOffset>18* meta.columns()||YOffset>18* meta.rows()) return -1;
        return (int)XOffset/18 + meta.columns()*((int)YOffset/18);
    }

    /**
     * Get an area of one independent interactable area, mainly one item slot.
     * Can be used to mark one clickable area with other mods, such as Jei's register {@code getClickableIngredientUnderMouse}
     * @param XOffset mouseX-pageX
     * @param YOffset mouseY-pageY
     * @return one interactable area
     */
    @Override
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
        final int rowAt = slot / meta.columns();
        final int columnAt = slot % meta.columns();
        return new Rect2i(leftPos+slotSize*columnAt, topPos+slotSize*rowAt, slotSize, slotSize);
    }

    /**Get mouse hovered or clicked item by mouse offset.
     * @param XOffset mouseX-pageX
     * @param YOffset mouseY-pageY
     * @return hovered or clicked item
     */
    @Override
    public ItemStack getItemByMouseOffset(double XOffset, double YOffset){
        int slot = getSlotByMouseOffset(XOffset,YOffset);
        if(slot>=0 && slot<items.size()) {
            return items.get(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void handleStarItem(double XOffset, double YOffset) {
        ItemStack clicked = getItemByMouseOffset(XOffset, YOffset);
        if(clicked.isEmpty()) return;
        getPacketDistributor().sendToServer(new StarItemPayload(clicked,true));
    }

    public void renderPage(GuiGraphics guiGraphics){
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
        int rowIndex = 0;
        int columnIndex = 0;
        for(ItemStack stack : items){
            if(stack.isEmpty() && !stack.is(Items.AIR)) renderEmpty(guiGraphics,leftPos+columnIndex*18,topPos+rowIndex*18+1,stack);
            guiGraphics.renderItem(stack,leftPos+columnIndex*18,topPos+rowIndex*18+1,columnIndex+rowIndex*180);
            if(!isHiddenBySortBox(rowIndex,columnIndex))
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack,leftPos+columnIndex*18,topPos+rowIndex*18+1, getDisplayAmount(stack));
            columnIndex++;
            if(columnIndex>= meta.columns()){
                columnIndex=0;
                rowIndex++;
            }
        }
        guiGraphics.pose().popPose();
    }

    protected void renderEmpty(GuiGraphics guiGraphics,int x,int y,ItemStack itemStack){
        ItemStack toRender = itemStack.copyWithCount(1);
        guiGraphics.renderItem(toRender,x,y,0);
        guiGraphics.renderItemDecorations(Minecraft.getInstance().font,toRender,x,y,ChatFormatting.RED+"0");
    }

    protected boolean isHiddenBySortBox(int rowIndex, int columnIndex){
        return rowIndex<=2 && columnIndex<=3 && Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
                && (
                screen instanceof EndlessInventoryScreen EIS && EIS.getFrameWork().sortTypeSwitchBox.isOpen() && columnIndex <=2
                        || framework.sortTypeSwitchBox.isOpen()
                );
    }

    protected String getDisplayAmount(ItemStack stack){
        int count = stack.getCount();
        double value;
        String suffix;

        if(count == this.meta.getMaxStackSize() && meta.enableInfinity()){
            return "∞";
        }

        if (count >= 1_000_000_000) {
            value = count / 1_000_000_000.0;
            suffix = "b";
        } else if (count >= 1_000_000) {
            value = count / 1_000_000.0;
            suffix = "m";
        } else if (count >= 1_000) {
            value = count / 1_000.0;
            suffix = "k";
        }else if(count==0){
            return ChatFormatting.RED + "0";
        }else {
            return String.valueOf(count);
        }

        return String.format("%.1f%s", value, suffix);
    }

    public void renderHovering(GuiGraphics graphics, int mouseX, int mouseY, float partialTick){
        renderSlotHighlight(graphics, mouseX, mouseY, partialTick);
        ItemStack hovering = getHoveredOrClickedItem(mouseX, mouseY);
        if(hovering.isEmpty()) return;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 550.0F);
        {
            graphics.renderTooltip(Minecraft.getInstance().font,
                    AbstractContainerScreen.getTooltipFromItem(Minecraft.getInstance(), hovering),
                    hovering.getTooltipImage(),
                    mouseX, mouseY);
        }
        graphics.pose().popPose();
    }
    protected void renderSlotHighlight(GuiGraphics graphics, int mouseX, int mouseY, float partialTick){
        for(int u = 0; u< meta.columns(); ++u){
            for(int v = 0; v< meta.rows(); ++v){
                int x1 = leftPos+18*u-1;
                int x2 = leftPos+18*u+16;
                int y1 = topPos+18*v+1;
                int y2 = topPos+18*v+18;
                if(mouseX>x1 && mouseX<x2 && mouseY>y1 && mouseY<y2){
                    if(!meta.getMenu().getCarried().isEmpty()) return;
                    graphics.fillGradient(RenderType.guiOverlay(),x1,y1,x2,y2,0x80ffffff,0x80ffffff,0);
                }
            }
        }
    }

    @Override
    public boolean doubleClickedOnOne(double XOffset, double YOffset, double lastX, double lastY, long clickInterval) {
        return clickedInOneSlot(XOffset, YOffset,lastX,lastY) && clickInterval<=250;
    }

    protected boolean clickedInOneSlot(double XOffset, double YOffset, double lastX, double lastY) {
        return (int)XOffset/18==(int)lastX/18 && (int)YOffset/18 == (int)lastY/18;
    }

    @Override
    public void pageClicked(double XOffset, double YOffset, int button, ClickType clickType) {
        int slot = getSlotByMouseOffset(XOffset,YOffset);
        if(slot>=0&&slot<items.size()) {
            ItemStack clicked = items.get(slot).copy();
            switch (clickType) {
                case PICKUP -> handlePickup(clicked, button);
                case QUICK_MOVE -> handleQuickMove(clicked);
                case SWAP -> handleSwap(clicked, button);
                case THROW -> handleThrow(clicked);
                case PICKUP_ALL -> handlePickupAll(clicked);
                case CLONE -> handleClone(clicked);
                default -> {
                }
            }
            LOGGER.info("EI:sending:ItemClickPayload: player={} clickType={} button={} stack={}"
                    ,meta.getPlayer(),clickType,button, clicked);
            ModInfo.getPacketDistributor().sendToServer(new ItemClickPayload(
                    clicked.getCount() > 64 ? clicked.copyWithCount(64) : clicked.copy(),
                    button,clickType));
            this.refreshItems();
        }
    }

    public ItemStack takeItem(ItemStack itemStack){
        return takeItem(itemStack,itemStack.getMaxStackSize());
    }

    public ItemStack takeItem(ItemStack itemStack,int count){
        setChanged();
        return this.srcInv.takeItem(itemStack,count);
    }

    public ItemStack takeItem(int index, int count){
        ItemStack itemStack = this.items.get(index);
        setChanged();
        return srcInv.takeItem(itemStack,count);
    }

    public ItemStack addItem(ItemStack itemStack){
        setChanged();
        return srcInv.addItem(itemStack.copy());
    }

    public boolean isFull(ItemStack itemStack){
        return itemStack.getCount() >= meta.getMaxStackSize();
    }

    public boolean isInfinite(ItemStack itemStack){
        return  isFull(itemStack) && meta.enableInfinity();
    }

    protected void handleQuickMove(ItemStack clicked){
        ItemStack taken = takeItem(clicked);
        ItemStack remain = meta.quickMoveFromPage(taken);
        addItem(remain);
        setChanged();
    }
    @Override
    public ItemStack tryInsertItem(ItemStack stack) {
        var remain = addItem(stack);
        initializeContents();
        return remain;
    }
    @Override
    public ItemStack tryExtractItem(ItemStack stack,int count){
        return takeItem(stack,count);
    }

    protected void handlePickup(ItemStack clicked, int keyCode){
        ItemStack carried = meta.getMenu().getCarried();
        if(!carried.isEmpty()){
            ItemStack remain = addItem(carried.copy());
            if(ModInfo.isClientLoaded() && meta.getMenu() instanceof CreativeModeInventoryScreen.ItemPickerMenu){
                getPacketDistributor().sendToServer(new CreativeItemModPayload(carried.copy(),true));
            }
            meta.getMenu().setCarried(remain);
            setChanged();
        }else{
            int count = Math.min(clicked.getCount(),clicked.getMaxStackSize());
            int takenCount = keyCode==0 ? count : (count + 1) / 2;
            meta.getMenu().setCarried(takeItem(clicked,takenCount));
            if(!meta.getMenu().getCarried().isEmpty()) setChanged();
        }
    }

    protected void handleSwap(ItemStack clicked, int inventorySlotId){
        Player player = meta.getPlayer();
        Inventory inventory = player.getInventory();
        ItemStack inventoryItem = inventory.getItem(inventorySlotId);
        boolean a = !inventoryItem.isEmpty();
        boolean b = !clicked.isEmpty();
        if( a && !b ){
            ItemStack remain = addItem(inventoryItem);
            inventory.setItem(inventorySlotId, remain);
        }
        if( !a && b ){
            ItemStack swapping = takeItem(clicked); //take most
            inventory.setItem(inventorySlotId,swapping);
        }
        if( a && b ){
            ItemStack remain = addItem(inventoryItem);
            if(remain.isEmpty()) {
                ItemStack swapping = takeItem(clicked); //take most
                inventory.setItem(inventorySlotId, swapping);
            }else {
                inventory.setItem(inventorySlotId,remain);
            }
        }
        setChanged();
    }
    protected void handleThrow(ItemStack clicked){
        Player player = meta.getPlayer();
        ItemStack thrown = takeItem(clicked);
        player.drop(thrown,true);
        setChanged();
    }
    protected void handlePickupAll(ItemStack clicked){
        Player player = meta.getPlayer();
        ItemStack carried = meta.getMenu().getCarried();
        int startIndex = meta.getMenu().slots.size() - 1; //changed: reversed button==0 condition
        for(int index = startIndex; index>=0 ; --index){
            Slot scanning = meta.getMenu().slots.get(index);
            if(!(scanning.container instanceof Inventory)) break;
            ItemStack scanningItem =scanning.getItem();
            if (ItemStack.isSameItemSameComponents(carried, scanningItem)) {
                ItemStack taken = scanning.safeTake(scanningItem.getCount(), scanningItem.getCount(), player);
                ItemStack remain = addItem(taken);
                if(!remain.isEmpty()) scanning.set(remain);
                setChanged();
            }
        }
    }
    protected void handleClone(ItemStack clicked){
        Player player = meta.getPlayer();
        if(player.getAbilities().instabuild && meta.getMenu().getCarried().isEmpty()){
            meta.getMenu().setCarried(clicked.copyWithCount(clicked.getMaxStackSize()));
        }
    }
}


