package com.kwwsyk.endinv.menu.page;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.SourceInventory;
import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.network.payloads.PageChangePayload;
import com.kwwsyk.endinv.network.payloads.PageStatePayload;
import com.kwwsyk.endinv.network.payloads.SetItemDisplayContentPayload;
import com.kwwsyk.endinv.options.ItemClassify;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemDisplay extends DisplayPage{
    private final NonNullList<ItemStack> items;
    private final SourceInventory sourceInventory;
    private int startIndex;
    private int length;
    private int leftPos;
    private int topPos;

    private boolean suppressRefresh = false;

    public ItemDisplay(EndlessInventoryMenu menu, @Nullable SourceInventory sourceInventory,
                       int startIndex, int length,
                       Holder<ItemClassify> classify,int pageIndex) {
        super(menu,classify,pageIndex);
        if(sourceInventory!=null) {
            this.sourceInventory = sourceInventory;
        }else {
            this.sourceInventory = this.menu.getSourceInventory();
        }
        this.items = NonNullList.withSize(length,ItemStack.EMPTY); // 预填充
        this.startIndex = startIndex;
        this.length = length;
    }

    public ItemDisplay(EndlessInventoryMenu menu, Holder<ItemClassify> classify,int pageIndex){
        this(menu,menu.getSourceInventory(),0, menu.getRowCount()*9,classify,pageIndex);
    }
    public boolean canScroll(){
        return startIndex>0 || startIndex+length >= menu.getItemSize();
    }

    public void scrollTo(float pos){
        int startIndex = getRowIndexForScroll(pos)*9;
        this.setDisplay(startIndex,this.length);
    }

    @Override
    public int calculateRowCount() {
        return Mth.positiveCeilDiv(length,9);
    }

    /**Change displayed items of EndInv
     * @param startIndex the index of the item first displayed in EndInv
     * @param length the count of the item should be displayed, should equal to 9*rows
     */
    public void setDisplay(int startIndex, int length) {
        this.startIndex = startIndex;
        this.length = length;
        release();
        if(this.sourceInventory==this.menu.REMOTE) {
            requestContents(startIndex,length);
        }else {
            refreshItems();
        }
    }
    //often use on server
    public void refreshItems(){
        if(!suppressRefresh) {
            EndlessInventory endInv = (EndlessInventory) menu.getSourceInventory();
            List<ItemStack> view = endInv.getSortedAndFilteredItemView(startIndex,length,menu.sortType, getItemClassify().value(),menu.searching);

            initializeContents(view);
            this.suppressRefresh = true;
        }
    }
    //client
    private void requestContents(int startIndex,int length){
        this.menu.requestContent(startIndex, length);
    }

    public void tryRequestContents(int startIndex,int length){
        if(!suppressRefresh) this.requestContents(startIndex,length);
    }
    private List<ItemStack> inQueueStacks = null;
    public void initializeContents(List<ItemStack> stacks){
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

    public boolean isFull(ItemStack itemStack){
        return itemStack.getCount() >= menu.getMaxStackSize();
    }

    public boolean isInfinite(ItemStack itemStack){
        return  isFull(itemStack) && menu.enableInfinity();
    }

    public SourceInventory getSourceInventory(){
        return this.sourceInventory;
    }
    //May shift
    public ItemStack takeItem(ItemStack itemStack){
        return takeItem(itemStack,itemStack.getMaxStackSize());
    }
    public ItemStack takeItem(ItemStack itemStack,int count){
        for(int i=0; i< items.size(); ++i){
            ItemStack stack = items.get(i);
            if(ItemStack.isSameItemSameComponents(stack,itemStack)){
                ItemStack ret = itemStack.copyWithCount(count);
                if(!isInfinite(stack)) {
                    if (count < stack.getCount()) {
                        stack.split(count);
                    } else {
                        ret.setCount(stack.getCount());
                        stack = ItemStack.EMPTY;
                        items.set(i,stack);
                    }
                }
                ItemStack result = sourceInventory.takeItem(itemStack,count);
                if(sourceInventory instanceof EndlessInventory) ret=result;
                return ret;
            }
        }
        return this.sourceInventory.takeItem(itemStack,count);
    }
    public ItemStack takeItem(int index, int count){
        //Will take Client display item firstly
        ItemStack itemStack = this.items.get(index);
        ItemStack ret = itemStack.copy();
        if(!isInfinite(itemStack)) {
            if (count < itemStack.getCount()) {
                itemStack.split(count);
                ret.setCount(count);
            } else {
                itemStack = ItemStack.EMPTY;
            }
            this.items.set(index, itemStack);
        }
        //then affect server
        ItemStack result = sourceInventory.takeItem(itemStack,count);
        if(sourceInventory instanceof EndlessInventory) ret=result;
        return ret;
    }
    public ItemStack takeItem(int index){
        return takeItem(index,Math.min(this.items.get(index).getCount(),this.items.get(index).getMaxStackSize()));
    }
    //May shift

    /**
     * Add item into ItemDisplay and EndInv.
     * Return Empty if successfully inserted all or client fake insert.
     * @param itemStack to add
     * @return remain item that not inserted. Client may not sync to server.
     */
    public ItemStack addItem(ItemStack itemStack){
        ItemStack ret = ItemStack.EMPTY;
        int count = itemStack.getCount();
        l:
        {
            for (int i = 0; i < this.length; ++i) {
                ItemStack itemStack1 = this.items.get(i);
                if (ItemStack.isSameItemSameComponents(itemStack1, itemStack)) {
                    if(!isFull(itemStack1)) {
                        int additional = itemStack1.getCount();
                        int max = menu.getMaxStackSize();
                        itemStack1.setCount(Math.min(count+additional,max));
                        ret = itemStack.copyWithCount(Math.max(0,count+additional-max));
                    }
                    if(isInfinite(itemStack1)) ret = ItemStack.EMPTY;
                    break l;
                }
                if (itemStack1.isEmpty()){
                    itemStack.limitSize(menu.getMaxStackSize());
                    this.items.set(i,itemStack);
                    ret = itemStack.copyWithCount(Math.max(0,count-menu.getMaxStackSize()));
                    break l;
                }
            }
        }
        // Important: use `copy()` to avoid duplicate actions due to shared ItemStack references.
        ItemStack remain = this.sourceInventory.addItem(itemStack.copy());
        if(!remain.isEmpty()) ret = remain;
        return ret;
    }

    public int getStartIndex(){
        return this.startIndex;
    }

    public int getContainerSize() {
        return this.length;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public void setChanged() {
        this.suppressRefresh = false;
    }

    public boolean stillValid(@NotNull Player player) {
        return true;
    }
    public void release(){
        if(holdOn){
            if(menu.getSourceInventory()== menu.REMOTE)
                PacketDistributor.sendToServer(new PageStatePayload(false));
            holdOn = false;
            if(inQueueStacks==null) return;
            initializeContents(inQueueStacks);
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
            guiGraphics.renderItem(stack,x+columnIndex*18,y+rowIndex*18+1,columnIndex+rowIndex*180);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack,x+columnIndex*18,y+rowIndex*18+1);
            columnIndex++;
            if(columnIndex>=9){
                columnIndex=0;
                rowIndex++;
            }
        }
        guiGraphics.pose().popPose();
    }
    public void renderHovering(GuiGraphics graphics, int mouseX, int mouseY, float partialTick){
        renderSlotHighlight(graphics, mouseX, mouseY, partialTick);
        int hoveringSlot = getSlotForMouseOffset(mouseX-leftPos,mouseY-topPos);
        if(hoveringSlot>=0&&hoveringSlot<items.size()){
            ItemStack hovering = items.get(hoveringSlot);
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
        for(int u=0;u<9;++u){
            for(int v=0;v<calculateRowCount();++v){
                int x1 = leftPos+18*u-1;
                int x2 = leftPos+18*u+16;
                int y1 = topPos+18*v+1;
                int y2 = topPos+18*v+18;
                if(mouseX>x1 && mouseX<x2 && mouseY>y1 && mouseY<y2){
                    graphics.fillGradient(RenderType.guiOverlay(),x1,y1,x2,y2,0x80ffffff,0x80ffffff,0);
                }
            }
        }
    }
    @Override
    public boolean hasSearchBar() {
        return true;
    }

    @Override
    public boolean hasSortTypeSwitchBar() {
        return true;
    }

    @Override
    public boolean doubleClicked(double XOffset, double YOffset, double lastX, double lastY, long clickInterval) {
        return clickedInOneSlot(XOffset, YOffset,lastX,lastY) && clickInterval<=250;
    }
    protected boolean clickedInOneSlot(double XOffset,double YOffset,double lastX,double lastY){
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
    public int getSlotForMouseOffset(double XOffset,double YOffset){
        if(XOffset<0||YOffset<0||XOffset>9*18||YOffset>18*calculateRowCount()) return -1;
        return (int)XOffset/18 + 9*((int)YOffset/18);
    }
    @Override
    public ItemStack tryQuickMoveStackTo(ItemStack stack) {
        return addItem(stack);
    }
    @Override
    public ItemStack tryExtractItem(ItemStack stack,int count){
        return takeItem(stack,count);
    }

    @Override
    public void syncContentToServer() {
        if(menu.getSourceInventory()== menu.REMOTE){
            PacketDistributor.sendToServer(new PageChangePayload(startIndex,length,menu.sortType,getItemClassify(),menu.searching));
        }
    }
    public void syncContentToClient(ServerPlayer player){
        EndlessInventory endInv = (EndlessInventory) menu.getSourceInventory();
        List<ItemStack> view = endInv.getSortedAndFilteredItemView(startIndex,length,menu.sortType, getItemClassify().value(),menu.searching);

        NonNullList<ItemStack> stacks = NonNullList.withSize(length, ItemStack.EMPTY);
        for(int i=0;i< view.size();++i){
            stacks.set(i,view.get(i));
        }
        PacketDistributor.sendToPlayer(player,new SetItemDisplayContentPayload(stacks));
    }
    protected void handleQuickMove(ItemStack clicked){
        ItemStack taken = takeItem(clicked);
        ItemStack remain = menu.quickMoveFromPage(taken);
        addItem(remain);
        setChanged();
    }
    protected void handlePickup(ItemStack clicked, int keyCode){
        ItemStack carried = menu.getCarried();
        if(!carried.isEmpty()){
            ItemStack remain = addItem(carried.copy());
            menu.setCarried(remain);
            setChanged();
        }else{
            int count = Math.min(clicked.getCount(),clicked.getMaxStackSize());
            int takenCount = keyCode==0 ? count : (count + 1) / 2;
            menu.setCarried(takeItem(clicked,takenCount));
            if(!menu.getCarried().isEmpty()) setChanged();
        }
    }
    protected void handleSwap(ItemStack clicked, int keyCode){
        Player player = menu.player;
        Inventory inventory = player.getInventory();
        ItemStack inventoryItem = inventory.getItem(keyCode);
        boolean a = !inventoryItem.isEmpty();
        boolean b = !clicked.isEmpty();
        if( a && !b ){
            ItemStack remain = addItem(inventoryItem);
            inventory.setItem(keyCode, remain);
        }
        if( !a && b ){
            ItemStack swapping = takeItem(clicked); //take most
            inventory.setItem(keyCode,swapping);
        }
        if( a && b ){
            ItemStack remain = addItem(inventoryItem);
            if(remain.isEmpty()) {
                ItemStack swapping = takeItem(clicked); //take most
                inventory.setItem(keyCode, swapping);
            }else {
                inventory.setItem(keyCode,remain);
            }
        }
        setChanged();
    }
    protected void handleThrow(ItemStack clicked){
        Player player = menu.player;
        ItemStack thrown = takeItem(clicked);
        player.drop(thrown,true);
        setChanged();
    }
    protected void handlePickupAll(ItemStack clicked){
        Player player = menu.player;
        ItemStack carried = menu.getCarried();
        int startIndex = menu.slots.size() - 1; //changed: reversed button==0 condition
        for(int index = startIndex; index>=0 ; --index){
            Slot scanning = menu.slots.get(index);
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
        Player player = menu.player;
        if(player.hasInfiniteMaterials() && menu.getCarried().isEmpty()){
            menu.setCarried(clicked.copyWithCount(clicked.getMaxStackSize()));
        }
    }

}
