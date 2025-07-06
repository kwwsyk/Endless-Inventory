package com.kwwsyk.endinv.common.menu.page;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.network.payloads.toServer.ItemClickPayload;
import com.kwwsyk.endinv.common.network.payloads.toServer.ItemDisplayItemModPayload;
import com.kwwsyk.endinv.common.network.payloads.toServer.ItemPageContext;
import com.kwwsyk.endinv.common.network.payloads.toServer.StarItemPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.kwwsyk.endinv.common.ModInfo.getPacketDistributor;
import static com.kwwsyk.endinv.common.ModRegistries.NbtAttachments.getSyncedConfig;

/**
 * DisplayPage that has a list of ItemStack, and items are linked to EndInv.
 */
public abstract class ItemPage extends DisplayPage {

    protected NonNullList<ItemStack> items;

    protected int startIndex = 0;

    protected int length;

    protected boolean suppressRefresh = false;

    protected List<ItemStack> inQueueStacks = null;

    public ItemPage(PageType pageType, PageMetaDataManager metaDataManager) {
        super(pageType,metaDataManager);
        this.length = meta.getRowCount()* meta.getColumnCount();
    }

    @Override
    public void scrollTo(float pos) {
        int startIndex = getRowIndexForScroll(pos)* meta.getColumnCount();
        this.refreshContents(startIndex,this.length);
    }

    @Override
    public void refreshContents() {
        refreshContents(0,meta.getRowCount()*meta.getColumnCount());
    }

    /**Change displayed items of EndInv
     * @param startIndex the index of the item first displayed in EndInv
     * @param length the count of the item should be displayed, should equal to rows*columns
     */
    public void refreshContents(int startIndex, int length) {
        this.startIndex = startIndex;
        this.length = Math.min(length, meta.getRowCount()* meta.getColumnCount());
        if(items==null || length!=this.items.size()){
            this.items = NonNullList.withSize(length,ItemStack.EMPTY);
        }
        release();
        refreshItems();
    }

    public abstract void refreshItems();

    public void tryRequestContents(){
        if(!suppressRefresh) this.requestContents();
    }

    public abstract void requestContents();

    public void initializeContents(@NotNull List<ItemStack> stacks){
        if(holdOn){
            inQueueStacks = stacks;
            return;
        }
        for(int i=0; i<this.length; ++i){
            if(i<stacks.size() && stacks.get(i) != null) {
                this.items.set(i, stacks.get(i)).copy();
            }else {
                this.items.set(i,ItemStack.EMPTY);
            }
        }
    }

    public void initializeContents(CachedSrcInv srcInv){
        var view = srcInv.getSortedAndFilteredItemView(startIndex,length, meta.sortType(), meta.isSortReversed(), getClassify(), meta.searching());
        initializeContents(view);
    }

    public void sendChangesToServer() {
        getPacketDistributor().sendToServer(new ItemPageContext(startIndex,length, getSyncedConfig().computeIfAbsent(meta.getPlayer()).pageData()));
    }

    public int getStartIndex(){
        return this.startIndex;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public void setChanged() {
        this.suppressRefresh = false;
    }

    @Override
    public boolean canScroll() {
        return startIndex>0
                ||( srcInv instanceof CachedSrcInv cache ? startIndex+length <= cache.getSortedAndFilteredItemView(0,Integer.MAX_VALUE,  meta.sortType(), meta.isSortReversed(), getClassify(), meta.searching()).size()
                : startIndex+length< meta.getItemSize());
    }

    public int getSlotForMouseOffset(double XOffset,double YOffset){
        if(XOffset<0||YOffset<0||XOffset>18* meta.getColumnCount()||YOffset>18* meta.getRowCount()) return -1;
        return (int)XOffset/18 + meta.getColumnCount()*((int)YOffset/18);
    }

    public void release(){
        if(holdOn){
            holdOn = false;
            if(inQueueStacks==null) return;
            initializeContents(inQueueStacks);
        }
    }

    @Override
    public void handleStarItem(double XOffset, double YOffset) {
        int slot = getSlotForMouseOffset(XOffset,YOffset);
        if(slot>=0&&slot<items.size()) {
            ItemStack clicked = items.get(slot);
            if(clicked.isEmpty()) return;
            getPacketDistributor().sendToServer(new StarItemPayload(clicked,true));
        }
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
            if(columnIndex>= meta.getColumnCount()){
                columnIndex=0;
                rowIndex++;
            }
        }
        guiGraphics.pose().popPose();
    }

    protected void renderEmpty(GuiGraphics guiGraphics,int x,int y,ItemStack itemStack){
        ItemStack toRender = new ItemStack(itemStack.getItemHolder(),1,itemStack.getComponentsPatch());
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
        int hoveringSlot = getSlotForMouseOffset(mouseX-leftPos,mouseY-topPos);
        if(hoveringSlot>=0&&hoveringSlot<items.size()){
            ItemStack hovering = items.get(hoveringSlot);
            if(hovering.isEmpty()) return;
            graphics.pose().pushPose();
            graphics.pose().translate(0,0,550.0F);
            graphics.renderTooltip(Minecraft.getInstance().font,
                    AbstractContainerScreen.getTooltipFromItem(Minecraft.getInstance(),hovering),
                    hovering.getTooltipImage(),
                    mouseX, mouseY);
            graphics.pose().popPose();
        }
    }
    protected void renderSlotHighlight(GuiGraphics graphics, int mouseX, int mouseY, float partialTick){
        for(int u = 0; u< meta.getColumnCount(); ++u){
            for(int v = 0; v< meta.getRowCount(); ++v){
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
        int slot = getSlotForMouseOffset(XOffset,YOffset);
        if(slot>=0&&slot<items.size()) {
            ItemStack clicked = items.get(slot);
            var copy = clicked.copy();
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
            ModInfo.getPacketDistributor().sendToServer(new ItemClickPayload(copy,button,clickType));
            refreshItems();
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
    public ItemStack tryQuickMoveStackTo(ItemStack stack) {
        var remain = addItem(stack);
        refreshContents();
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
                getPacketDistributor().sendToServer(new ItemDisplayItemModPayload(carried.copy(),true));
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
            if(ItemStack.isSameItemSameComponents(carried,scanningItem)){
                ItemStack taken = scanning.safeTake(scanningItem.getCount(), scanningItem.getCount(), player);
                ItemStack remain = addItem(taken);
                if(!remain.isEmpty()) scanning.set(remain);
                setChanged();
            }
        }
    }
    protected void handleClone(ItemStack clicked){
        Player player = meta.getPlayer();
        if(player.hasInfiniteMaterials() && meta.getMenu().getCarried().isEmpty()){
            meta.getMenu().setCarried(clicked.copyWithCount(clicked.getMaxStackSize()));
        }
    }
}
