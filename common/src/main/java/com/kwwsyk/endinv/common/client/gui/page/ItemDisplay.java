package com.kwwsyk.endinv.common.client.gui.page;

import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.page.manager.PageManager;
import com.kwwsyk.endinv.common.client.option.CachedConfig;
import com.kwwsyk.endinv.common.menu.page.PageType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**Page that displays EndInv's items (directly {@link CachedSrcInv})
 *
 */
public class ItemDisplay extends ItemPage{

    public ItemDisplay(PageType pageType, PageManager metaDataManager) {
        super(pageType,metaDataManager);
    }

    public void refreshItems(){
        requestRemoteContents();
        readCachedItems();
    }

    /**
     * Read items data from {@link CachedSrcInv} when transfer mode is {@code ALL} and rebuild the view.
     */
    public void readCachedItems(){
        List<ItemStack> view = CachedSrcInv.INSTANCE.getSortedAndFilteredItemView(startIndex,length,
                CachedConfig.sortType(), CachedConfig.reverseSort(),
                getClassify(), CachedConfig.searching());
        buildContentsWith(view);
    }

    public void requestRemoteContents(){
        sendChangesToServer();
    }

    /**
     * Build Displayed view with a ItemStack list.
     * @param stacks itemstack list to fill the view
     */
    public void buildContentsWith(@NotNull List<ItemStack> stacks){
        if(holdOn){
            inQueueStacks = stacks;
            return;
        }
        for(int i=0; i<this.length; ++i){
            if(i<stacks.size() && stacks.get(i) != null) {
                this.items.set(i, stacks.get(i).copy());
            }else {
                this.items.set(i,ItemStack.EMPTY);
            }
        }
    }

    @Override
    public boolean hasSearchbox() {
        return true;
    }

    @Override
    public boolean hasSortTypeSwitchBar() {
        return true;
    }

    public ItemStack takeItem(ItemStack itemStack,int count){
        for(int i=0; i< items.size(); ++i){//in the loop is the animation
            ItemStack stack = items.get(i);
            if (ItemStack.isSameItemSameComponents(stack, itemStack)) {
                if(!isInfinite(stack)) {
                    if (count < stack.getCount()) {
                        stack.split(count);
                    } else {
                        stack.setCount(0);
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

    public void release(){
        if(holdOn){
            holdOn = false;
            if(inQueueStacks==null) return;
            buildContentsWith(inQueueStacks);
        }
    }
}
