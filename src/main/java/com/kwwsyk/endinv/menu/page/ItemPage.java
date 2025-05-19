package com.kwwsyk.endinv.menu.page;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.client.CachedSrcInv;
import com.kwwsyk.endinv.client.events.ScreenAttachment;
import com.kwwsyk.endinv.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.network.payloads.toClient.EndInvContent;
import com.kwwsyk.endinv.network.payloads.toClient.SetItemDisplayContentPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.PageContext;
import com.kwwsyk.endinv.network.payloads.toServer.page.StarItemPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.op.ItemDisplayItemModPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.op.PageStatePayload;
import com.kwwsyk.endinv.options.ContentTransferMode;
import com.kwwsyk.endinv.options.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * DisplayPage that has a list of ItemStack, and items are linked to EndInv.
 */
public abstract class ItemPage extends DisplayPage{

    protected NonNullList<ItemStack> items;
    protected int startIndex = 0;
    protected int length;
    //leftPos and topPos are used as Renderer param
    protected int leftPos;
    protected int topPos;
    protected boolean suppressRefresh = false;
    protected List<ItemStack> inQueueStacks = null;

    public ItemPage(PageType pageType, PageMetaDataManager metaDataManager) {
        super(pageType,metaDataManager);
        this.length = metadata.getRowCount()*metadata.getColumnCount();
    }

    @Override
    public void scrollTo(float pos) {
        int startIndex = getRowIndexForScroll(pos)*metadata.getColumnCount();
        this.init(startIndex,this.length);
    }
    /**Change displayed items of EndInv
     * @param startIndex the index of the item first displayed in EndInv
     * @param length the count of the item should be displayed, should equal to rows*columns
     */
    @Override
    public void init(int startIndex, int length) {
        this.startIndex = startIndex;
        this.length = Math.min(length,metadata.getRowCount()*metadata.getColumnCount());
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
        var view = srcInv.getSortedAndFilteredItemView(startIndex,length,metadata.sortType(),metadata.isSortReversed(), getClassify(),metadata.searching());
        initializeContents(view);
    }

    public void syncContentToServer() {
        if(srcInv.isRemote()){
            PacketDistributor.sendToServer(new PageContext(startIndex,length, metadata.getPlayer().getData(ModInitializer.SYNCED_CONFIG).pageData()));
        }
    }

    public void syncContentToClient(ServerPlayer player){
        EndlessInventory endInv = (EndlessInventory) metadata.getSourceInventory();
        if(ServerConfig.CONFIG.TRANSFER_MODE.get()== ContentTransferMode.PART) {
            List<ItemStack> view = endInv.getSortedAndFilteredItemView(startIndex, length, metadata.sortType(), metadata.isSortReversed(), getClassify(), metadata.searching());

            NonNullList<ItemStack> stacks = NonNullList.withSize(length, ItemStack.EMPTY);
            for (int i = 0; i < view.size(); ++i) {
                stacks.set(i, view.get(i));
            }
            PacketDistributor.sendToPlayer(player, new SetItemDisplayContentPayload(stacks));
        } else if (ServerConfig.CONFIG.TRANSFER_MODE.get() == ContentTransferMode.ALL) {
            PacketDistributor.sendToPlayer(player,new EndInvContent(endInv.getItemMap()));
        }
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
                ||( srcInv instanceof CachedSrcInv cache ? startIndex+length <= cache.getSortedAndFilteredItemView(0,Integer.MAX_VALUE,  metadata.sortType(), metadata.isSortReversed(), getClassify(), metadata.searching()).size()
                : startIndex+length< metadata.getItemSize());
    }

    public int getSlotForMouseOffset(double XOffset,double YOffset){
        if(XOffset<0||YOffset<0||XOffset>18*metadata.getColumnCount()||YOffset>18*metadata.getRowCount()) return -1;
        return (int)XOffset/18 + metadata.getColumnCount()*((int)YOffset/18);
    }

    public void release(){
        if(holdOn){
            if(srcInv.isRemote())
                PacketDistributor.sendToServer(new PageStatePayload(false));
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
            PacketDistributor.sendToServer(new StarItemPayload(clicked,true));
        }
    }

    public void renderPage(GuiGraphics guiGraphics, int x, int y){
        this.leftPos=x;
        this.topPos=y;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
        int rowIndex = 0;
        int columnIndex = 0;
        for(ItemStack stack : items){
            if(stack.isEmpty() && !stack.is(Items.AIR)) renderEmpty(guiGraphics,x+columnIndex*18,y+rowIndex*18+1,stack);
            guiGraphics.renderItem(stack,x+columnIndex*18,y+rowIndex*18+1,columnIndex+rowIndex*180);
            if(!isHiddenBySortBox(rowIndex,columnIndex))
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack,x+columnIndex*18,y+rowIndex*18+1, getDisplayAmount(stack));
            columnIndex++;
            if(columnIndex>= metadata.getColumnCount()){
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
        return rowIndex<=2 && columnIndex<=3 && Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && (
                screen instanceof EndlessInventoryScreen EIS && EIS.getFrameWork().sortTypeSwitchBox.isOpen() && columnIndex <=2
                        || ScreenAttachment.ATTACHMENT_MANAGER.get(screen)!=null
                        && ScreenAttachment.ATTACHMENT_MANAGER.get(screen).getFrameWork().sortTypeSwitchBox.isOpen()
        );
    }

    public static String getDisplayAmount(ItemStack stack){
        int count = stack.getCount();
        double value;
        String suffix;

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
                    hovering, mouseX, mouseY);
            graphics.pose().popPose();
        }
    }
    protected void renderSlotHighlight(GuiGraphics graphics, int mouseX, int mouseY, float partialTick){
        for(int u=0;u<metadata.getColumnCount();++u){
            for(int v=0;v<metadata.getRowCount();++v){
                int x1 = leftPos+18*u-1;
                int x2 = leftPos+18*u+16;
                int y1 = topPos+18*v+1;
                int y2 = topPos+18*v+18;
                if(mouseX>x1 && mouseX<x2 && mouseY>y1 && mouseY<y2){
                    if(!metadata.getMenu().getCarried().isEmpty()) return;
                    graphics.fillGradient(RenderType.guiOverlay(),x1,y1,x2,y2,0x80ffffff,0x80ffffff,0);
                }
            }
        }
    }

    @Override
    public boolean doubleClicked(double XOffset, double YOffset, double lastX, double lastY, long clickInterval) {
        return clickedInOneSlot(XOffset, YOffset,lastX,lastY) && clickInterval<=250;
    }

    protected boolean clickedInOneSlot(double XOffset, double YOffset, double lastX, double lastY) {
        return (int)XOffset/18==(int)lastX/18 && (int)YOffset/18 == (int)lastY/18;
    }

    @Override
    public void pageClicked(double XOffset, double YOffset, int keyCode, ClickType clickType) {
        int slot = getSlotForMouseOffset(XOffset,YOffset);
        if(slot>=0&&slot<items.size()) {
            ItemStack clicked = items.get(slot);
            switch (clickType) {
                case PICKUP -> handlePickup(clicked, keyCode);
                case QUICK_MOVE -> handleQuickMove(clicked);
                case SWAP -> handleSwap(clicked, keyCode);
                case THROW -> handleThrow(clicked);
                case PICKUP_ALL -> handlePickupAll(clicked);
                case CLONE -> handleClone(clicked);
                default -> {
                }
            }

        }
    }

    public ItemStack takeItem(ItemStack itemStack){
        return takeItem(itemStack,itemStack.getMaxStackSize());
    }
    public ItemStack takeItem(ItemStack itemStack,int count){
        return this.srcInv.takeItem(itemStack,count);
    }
    public ItemStack takeItem(int index, int count){
        //Will take Client display item firstly
        ItemStack itemStack = this.items.get(index);
        ItemStack ret = itemStack.copy();
        //then affect server
        ItemStack result = srcInv.takeItem(itemStack,count);
        if(srcInv instanceof EndlessInventory) ret=result;
        return ret;
    }

    public ItemStack addItem(ItemStack itemStack){
        ItemStack ret = ItemStack.EMPTY;
        // Important: use `copy()` to avoid duplicate actions due to shared ItemStack references.
        ItemStack remain = this.srcInv.addItem(itemStack.copy());
        if(!remain.isEmpty()) ret = remain;
        return ret;
    }

    public boolean isFull(ItemStack itemStack){
        return itemStack.getCount() >= metadata.getMaxStackSize();
    }

    public boolean isInfinite(ItemStack itemStack){
        return  isFull(itemStack) && metadata.enableInfinity();
    }

    protected void handleQuickMove(ItemStack clicked){
        ItemStack taken = takeItem(clicked);
        ItemStack remain = metadata.quickMoveFromPage(taken);
        addItem(remain);
        setChanged();
    }
    @Override
    public ItemStack tryQuickMoveStackTo(ItemStack stack) {
        return addItem(stack);
    }
    @Override
    public ItemStack tryExtractItem(ItemStack stack,int count){
        return takeItem(stack,count);
    }

    protected void handlePickup(ItemStack clicked, int keyCode){
        ItemStack carried = metadata.getMenu().getCarried();
        if(!carried.isEmpty()){
            ItemStack remain = addItem(carried.copy());
            if(FMLEnvironment.dist.isClient() && metadata.getMenu() instanceof CreativeModeInventoryScreen.ItemPickerMenu){
                PacketDistributor.sendToServer(new ItemDisplayItemModPayload(carried.copy(),true));
            }
            metadata.getMenu().setCarried(remain);
            setChanged();
        }else{
            int count = Math.min(clicked.getCount(),clicked.getMaxStackSize());
            int takenCount = keyCode==0 ? count : (count + 1) / 2;
            metadata.getMenu().setCarried(takeItem(clicked,takenCount));
            if(!metadata.getMenu().getCarried().isEmpty()) setChanged();
        }
    }

    protected void handleSwap(ItemStack clicked, int inventorySlotId){
        Player player = metadata.getPlayer();
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
        Player player = metadata.getPlayer();
        ItemStack thrown = takeItem(clicked);
        player.drop(thrown,true);
        setChanged();
    }
    protected void handlePickupAll(ItemStack clicked){
        Player player = metadata.getPlayer();
        ItemStack carried = metadata.getMenu().getCarried();
        int startIndex = metadata.getMenu().slots.size() - 1; //changed: reversed button==0 condition
        for(int index = startIndex; index>=0 ; --index){
            Slot scanning = metadata.getMenu().slots.get(index);
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
        Player player = metadata.getPlayer();
        if(player.hasInfiniteMaterials() && metadata.getMenu().getCarried().isEmpty()){
            metadata.getMenu().setCarried(clicked.copyWithCount(clicked.getMaxStackSize()));
        }
    }
}
