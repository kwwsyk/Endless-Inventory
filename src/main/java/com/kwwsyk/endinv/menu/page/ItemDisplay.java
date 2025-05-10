package com.kwwsyk.endinv.menu.page;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ItemDisplay extends ItemPage{

    public ItemDisplay(PageType pageType, PageMetaDataManager metaDataManager) {
        super(pageType,metaDataManager);
    }



    public void init(int startIndex, int length) {
        this.startIndex = startIndex;
        this.length = length;
        this.items = NonNullList.withSize(length,ItemStack.EMPTY); // 预填充
        release();
        if(srcInv.isRemote()) {
            requestContents();
        }else {
            refreshItems();
        }
    }
    //often use on server
    public void refreshItems(){
        if(srcInv.isRemote()){
            requestContents();
            return;
        }
        EndlessInventory endInv = (EndlessInventory) metadata.getSourceInventory();
        List<ItemStack> view = endInv.getSortedAndFilteredItemView(startIndex,length,
                metadata.sortType(),metadata.isSortReversed(),
                getClassify(), metadata.searching());
        initializeContents(view);
    }

    public void requestContents(){
        syncContentToServer();
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
        for(int i=0; i< items.size(); ++i){
            ItemStack stack = items.get(i);
            if(ItemStack.isSameItemSameComponents(stack,itemStack)){
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
                }
                ItemStack result = srcInv.takeItem(itemStack,count);
                if(srcInv instanceof EndlessInventory) ret=result;
                return ret;
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
                if (ItemStack.isSameItemSameComponents(itemStack1, itemStack)) {
                    if(!isFull(itemStack1)) {
                        int additional = itemStack1.getCount();
                        int max = metadata.getMaxStackSize();
                        itemStack1.setCount(Math.min(count+additional,max));
                        ret = itemStack.copyWithCount(Math.max(0,count+additional-max));
                    }
                    if(isInfinite(itemStack1)) ret = ItemStack.EMPTY;
                    break l;
                }
                if (itemStack1.isEmpty()){
                    itemStack.limitSize(metadata.getMaxStackSize());
                    this.items.set(i,itemStack);
                    ret = itemStack.copyWithCount(Math.max(0,count- metadata.getMaxStackSize()));
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
