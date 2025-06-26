package com.kwwsyk.endinv.common.client;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.network.payloads.toClient.EndInvMetadata;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemState;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**Client only {@link SourceInventory}
 * Served as data cache of EndlessInventory.
 */
public class CachedSrcInv extends SourceInventory {

    public static final CachedSrcInv INSTANCE = new CachedSrcInv();

    private CachedSrcInv(){
        super(ModInfo.DEFAULT_UUID);
    }

    public void initializeContents(Map<ItemKey, ItemState> itemMap){
        this.itemMap = new Object2ObjectLinkedOpenHashMap<>(itemMap);
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public ItemStack takeItem(ItemStack stack, int count){
        if(stack.isEmpty()) return ItemStack.EMPTY;
        ItemKey key = ItemKey.asKey(stack);
        ItemState state = itemMap.get(key);
        if (state == null) return ItemStack.EMPTY;
        //if infinity
        if(state.count() >= maxStackSize && infinityMode){
            setChanged();
            return stack.copyWithCount(count);
        }

        int taken = Math.min(count, state.count());
        ItemStack result = stack.copyWithCount(taken);
        if (taken == state.count()) {
            itemMap.remove(key);
            updateLastModTime();
        } else {
            itemMap.put(key, new ItemState(state.count() - taken, updateLastModTime()));
        }
        setChanged();
        return result;

    }

    @Override
    public ItemStack addItem(ItemStack itemStack){
        if(itemStack.isEmpty()) return ItemStack.EMPTY;
        ItemKey key = ItemKey.asKey(itemStack);
        ItemState state = itemMap.get(key);
        int count = itemStack.getCount();
        int original = 0;

        if (state != null) {
            original = state.count();
        }
        int increased;
        if(original < maxStackSize){
            increased = original+count;
            if(increased <= maxStackSize){
                itemMap.put(key, new ItemState(increased, updateLastModTime()));
                setChanged();
                return ItemStack.EMPTY;
            }else {
                itemMap.put(key, new ItemState(maxStackSize, updateLastModTime()));
                setChanged();
                return itemStack.copyWithCount(increased-maxStackSize);
            }
        }else if(infinityMode){
            itemMap.put(key, new ItemState(original, updateLastModTime()));
            setChanged();
            return ItemStack.EMPTY;
        }else {
            return itemStack.copy();
        }
    }

    @Override
    public void setChanged() {

    }

    public long updateLastModTime(){
        return Util.getMillis();
    }

    public void syncMetadata(EndInvMetadata endInvMetadata) {
        this.maxStackSize = endInvMetadata.maxStackSize();
        this.infinityMode = endInvMetadata.infinityMode();
        this.accessibility =endInvMetadata.config().accessibility();
        this.owner = endInvMetadata.config().owner();
        this.white_list = endInvMetadata.config().white_list();
    }
}
