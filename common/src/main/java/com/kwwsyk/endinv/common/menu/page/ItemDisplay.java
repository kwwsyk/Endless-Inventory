package com.kwwsyk.endinv.common.menu.page;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ItemDisplay extends ItemPage{

    public ItemDisplay(PageType pageType, PageMetaDataManager metaDataManager) {
        super(pageType,metaDataManager);
    }

    public void refreshItems(){
        if(!suppressRefresh) requestContents();

        List<ItemStack> view = CachedSrcInv.INSTANCE.getSortedAndFilteredItemView(startIndex,length,
                meta.sortType(), meta.isSortReversed(),
                getClassify(), meta.searching());
        initializeContents(view);
    }

    public void requestContents(){
        sendChangesToServer();
    }

    @Override
    public boolean hasSearchBar() {
        return true;
    }

    @Override
    public boolean hasSortTypeSwitchBar() {
        return true;
    }

    public ItemStack takeItem(ItemStack itemStack,int count){
        for(int i=0; i< items.size(); ++i){//in the loop is the animation
            ItemStack stack = items.get(i);
            if(ItemStack.isSameItemSameTags(stack,itemStack)){
                ItemStack ret = itemStack.copyWithCount(count);
                if(!isInfinite(stack)) {
                    if (count < stack.getCount()) {
                        stack.split(count);
                    } else {
                        ret.setCount(stack.getCount());
                        if(srcInv.isRemote()){
                            stack.setCount(0);
                        }else {
                            stack=ItemStack.EMPTY;
                        }
                        items.set(i,stack);
                    }
                    setChanged();
                }
            }
        }
        return this.srcInv.takeItem(itemStack,count);
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
        ItemStack result = srcInv.takeItem(itemStack,count);
        if(srcInv instanceof EndlessInventory) ret=result;
        return ret;
    }

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
            if (ItemStack.isSameItemSameTags(itemStack1, itemStack)) {
                    if(!isFull(itemStack1)) {
                        int additional = itemStack1.getCount();
                        int max = meta.getMaxStackSize();
                        itemStack1.setCount(Math.min(count+additional,max));
                        ret = itemStack.copyWithCount(Math.max(0,count+additional-max));
                    }
                    if(isInfinite(itemStack1)) ret = ItemStack.EMPTY;
                    break l;
                }
                if (itemStack1.isEmpty()){
                    itemStack.setCount(Math.min(itemStack.getCount(), meta.getMaxStackSize()));
                    this.items.set(i,itemStack);
                    ret = itemStack.copyWithCount(Math.max(0,count- meta.getMaxStackSize()));
                    break l;
                }
            }
        }
        // Important: use `copy()` to avoid duplicate actions due to shared ItemStack references.
        ItemStack remain = this.srcInv.addItem(itemStack.copy());
        if(!remain.isEmpty()) ret = remain;
        return ret;
    }

}
