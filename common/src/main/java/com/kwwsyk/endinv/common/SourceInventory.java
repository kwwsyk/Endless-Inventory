package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.util.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SourceInventory {

    //item container
    protected Map<ItemKey, ItemState> itemMap;
    protected List<ItemStack> items;

    //meta
    protected UUID uuid;
    protected long lastModTime = Util.getMillis();
    protected int maxStackSize;
    protected boolean infinityMode;
    //accessibility attributes
    @Nullable
    protected UUID owner;
    public List<UUID> white_list = new ArrayList<>();
    protected Accessibility accessibility;

    public SourceInventory(UUID uuid){
        this.items = new ArrayList<>();
        this.itemMap = new Object2ObjectLinkedOpenHashMap<>();
        this.uuid = uuid;
        this.maxStackSize = ModInfo.getServerConfig().getMaxAllowedStackSize().get();
        this.infinityMode = ModInfo.getServerConfig().allowInfinityMode().get();
        this.accessibility = ModInfo.getServerConfig().defaultAccessibility().get();
    }


    //abstract methods
    public abstract boolean isRemote();

    public abstract void setChanged();

    //field accessor
    public UUID getUuid(){
        return this.uuid;
    }

    public UUID giveNewUuid(){
        uuid=UUID.randomUUID();
        return uuid;
    }

    public Map<ItemKey,ItemState> getItemMap(){
        return itemMap;
    }

    public List<ItemStack> getItemsAsList(){
        syncItemsFromMap();
        return this.items;
    }

    /**
     * @return Size of Endless Inventory.
     */
    public int getItemSize() {
        syncItemsFromMap();
        return this.items.size();
    }

    public int getMaxItemStackSize() {
        return maxStackSize;
    }

    public void setMaxItemStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    public boolean isInfinityMode() {
        return infinityMode;
    }

    public void setInfinityMode(boolean infinityMode) {
        this.infinityMode = infinityMode;
    }

    public Accessibility getAccessibility() {
        return accessibility;
    }

    public void setAccessibility(Accessibility accessibility) {
        this.accessibility = accessibility;
    }

    public boolean accessible(Player player) {
        return accessibility==Accessibility.PUBLIC || (owner!=null && owner.equals(player.getUUID())) || white_list.contains(player.getUUID()) && accessibility==Accessibility.RESTRICTED;
    }

    public boolean isOwner(Player player){
        return Objects.equals(player.getUUID(),owner);
    }

    @Nullable
    public UUID getOwnerUUID(){
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }


    //item handler methods: item modification
    /**
     * Take at most its max stack size count item
     * @param itemStack will take itemStack with same id and component
     * @return items taken
     */
    public ItemStack takeItem(ItemStack itemStack) {
        return takeItem(itemStack,itemStack.getMaxStackSize());
    }

    /**
     * Take away those itemStack having same id&component with given itemStack in EndInv
     *
     * @param stack given item, with id and component | 物品堆叠，id和组件的提供器
     * @param count the max count of items taken away | 拿走的最大数目
     * @return taken items | 被拿走的物品
     */
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

    /**
     * Add item to EndInv and return remain item copy.
     * If adding is performed, update EndInv State by invoking {@link #setChanged()},
     * which is when some item inserted, including infinite mode inserting.
     * @param itemStack to add
     * @return Remain item copied, or {@link ItemStack#EMPTY} if all inserted.
     */
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

    public void clearContent() {
        this.itemMap.clear();
        this.setChanged();
    }

    //item handler: item view methods

    protected List<ItemStack> getSortedView(SortType type, boolean reverse) {
        var ret = itemMap.entrySet().stream()
                .map(e -> e.getKey().toStack(e.getValue().count()))
                .sorted(ModInfo.sortHelper.getComparator(type, this))
                .collect(Collectors.toList());
        if(reverse) ret = ret.reversed();
        return ret;
    }

    public List<ItemStack> getSortedAndFilteredItemView(int startIndex, int length, SortType sortType, boolean reverse, @Nullable Predicate<ItemStack> classify, String search){
        Stream<ItemStack> base = getSortedView(sortType,reverse).stream();
        List<ItemStack> filtered = base
                .filter(classify!=null?classify:is->true)
                .filter(stack -> SearchUtil.matchesSearch(stack,search))
                .toList();
        if(startIndex>= filtered.size()) return new ArrayList<>();
        return filtered.subList(startIndex,Math.min(startIndex+length,filtered.size()));
    }

    //item handler: map-list sync methods

    public void syncItemsFromMap() {
        this.items = itemMap.entrySet().stream()
                .map(e -> e.getKey().toStack(e.getValue().count()))
                .collect(Collectors.toList());
    }

    public void syncMapFromItems() {
        this.itemMap.clear();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            long now = updateLastModTime();
            var key = ItemKey.asKey(stack);
            this.itemMap.put(key, new ItemState(stack.getCount(), now));
        }
        //invalidateCaches(); ?
    }

    //modification version mangers
    public long updateLastModTime(){
        lastModTime=Util.getMillis();
        return lastModTime;
    }
}
