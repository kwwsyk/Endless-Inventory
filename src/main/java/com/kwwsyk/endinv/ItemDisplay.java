package com.kwwsyk.endinv;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemDisplay implements Container {
    private final NonNullList<ItemStack> items;
    private final SourceInventory sourceInventory;
    private int startIndex;
    private int length;
    private final EndlessInventoryMenu menu;
    private final SourceInventory REMOTE = new SourceInventory() {
        public ItemStack getItem(int i){
            return ItemStack.EMPTY;
        }

        public int getItemSize(){
            return ItemDisplay.this.menu.getItemSize();
        }

        @Override
        public boolean isRemote() {
            return true;
        }

        @Override
        public ItemStack takeItem(ItemStack itemStack) {
            setChanged();
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack takeItem(ItemStack itemStack, int count) {
            setChanged();
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack takeItem(int index, int count) {
            setChanged();
            return ItemStack.EMPTY;
        }

        @Override
        public void addItem(ItemStack itemStack) {
            setChanged();
        }

        @Override
        public void setChanged() {
            ItemDisplay.this.setChanged();

        }

        @Override
        public ItemStack removeItem(int index) {
            setChanged();
            return ItemStack.EMPTY;
        }
    };
    private boolean supressRefresh = false;

    public ItemDisplay(EndlessInventoryMenu menu, @Nullable SourceInventory sourceInventory, int length) {
        this(menu,sourceInventory, 0, length);
    }

    public ItemDisplay(EndlessInventoryMenu menu, @Nullable SourceInventory sourceInventory, int startIndex, int length) {
        this.menu = menu;
        if(sourceInventory!=null) {
            this.sourceInventory = sourceInventory;
        }else {
            this.sourceInventory = REMOTE;
        }
        this.items = NonNullList.withSize(length,ItemStack.EMPTY); // 预填充
        this.startIndex = startIndex;
        this.length = length;
    }

    /**Change displayed items of EndInv
     * @param startIndex the index of the item first displayed in EndInv
     * @param length the count of the item should be displayed, should equal to 9*rows
     */
    public void setDisplay(int startIndex, int length) {
        this.startIndex = startIndex;
        this.length = length;
        if(this.sourceInventory==REMOTE) {
            requestContents(startIndex,length);
        }else {
            refreshItems();
        }
    }
    //often use on server
    public void refreshItems(){
        if(!supressRefresh) {
            for (int i = 0; i < length; ++i) {
                if(startIndex+i < this.sourceInventory.getItemSize()) {
                    this.items.set(i, this.sourceInventory.getItem(startIndex + i).copy());
                }else{
                    this.items.set(i,ItemStack.EMPTY);
                }
            }
            this.supressRefresh = true;
        }
    }
    //client
    private void requestContents(int startIndex,int length){
        this.menu.requestContent(startIndex, length);
    }

    public void tryRequestContents(int startIndex,int length){
        if(!supressRefresh) this.requestContents(startIndex,length);
    }

    public void initializeContents(List<ItemStack> stacks){
        for(int i=0; i<this.length; ++i){
            if(i<stacks.size() && stacks.get(i) != null) {
                this.items.set(i, stacks.get(i));
            }else {
                this.items.set(i,ItemStack.EMPTY);
            }
        }
    }



    public SourceInventory getSourceInventory(){
        return this.sourceInventory;
    }
    //May shift
    public ItemStack takeItem(ItemStack itemStack){
        return this.sourceInventory.takeItem(itemStack);
    }
    public ItemStack takeItem(ItemStack itemStack,int count){
        return this.sourceInventory.takeItem(itemStack,count);
    }
    public ItemStack takeItem(int index, int count){
        //Will take Client display item firstly
        ItemStack itemStack = this.items.get(index);
        ItemStack ret = itemStack.copy();
        if(count< itemStack.getCount()) {
            itemStack.split(count);
            ret.setCount(count);
        }else {
            itemStack=ItemStack.EMPTY;
        }
        this.items.set(index,itemStack);
        //then affect server
        takeItem(ret,count);
        return ret;
    }
    public ItemStack takeItem(int index){
        return takeItem(index,Math.min(this.items.get(index).getCount(),this.items.get(index).getMaxStackSize()));
    }
    //May shift

    public void addItem(ItemStack itemStack){
        l:
        {
            for (int i = 0; i < this.length; ++i) {
                ItemStack itemStack1 = this.items.get(i);
                if (ItemStack.isSameItemSameComponents(itemStack1, itemStack)) {
                    itemStack1.grow(itemStack.getCount());
                    break l;
                }
                if (itemStack1.isEmpty()){
                    this.items.set(i,itemStack);
                    break l;
                }
            }
        }
        // Important: use `copy()` to avoid duplicate actions due to shared ItemStack references.
        this.sourceInventory.addItem(itemStack.copy());
    }

    public int getStartIndex(){
        return this.startIndex;
    }

    @Override
    public int getContainerSize() {
        return this.length;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(stack -> stack.isEmpty());
    }

    @Override
    public ItemStack getItem(int index) {
        return (index >= 0 && index < items.size()) ? items.get(index) : ItemStack.EMPTY;
    }

    @Override
    /**
     * This method is only supposed to run in client;
     *   and should not modify source endInv.
     *  To modify EndInv, use {@link #addItem(ItemStack)} {@link #takeItem(ItemStack, int)}
     *  {@link #removeItem(ItemLike)} etc.
     *
     */
    public void setItem(int index, ItemStack itemStack) {
        if (index >= 0 && index < items.size()) {
            this.items.set(index,itemStack);
        }
    }

    @Override
    public void setChanged() {
        this.supressRefresh = false;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index >= 0 && index < items.size() && !items.get(index).isEmpty()) {
            return sourceInventory.takeItem(index, count);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index >= 0 && index < items.size() && !items.get(index).isEmpty()) {
            int count = items.get(index).getCount();
            return sourceInventory.takeItem(index, count);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void clearContent() {
        items.replaceAll(ignored -> ItemStack.EMPTY);

    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
