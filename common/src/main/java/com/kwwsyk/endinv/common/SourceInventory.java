package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.util.*;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kwwsyk.endinv.common.ModInfo.getServerConfig;

/**Holder of core and shared ({@link EndlessInventory}&{@link com.kwwsyk.endinv.common.client.CachedSrcInv} endless INVENTORY logic.
 * TODO improve the logic to be THREAD-SAFE and EFFECTIVE
 */
public abstract class SourceInventory {

    private static final Logger LOGGER = LogUtils.getLogger();

    //item container
    protected final Map<ItemKey, ItemState> itemMap;
    protected List<ItemStack> items;

    //meta
    protected UUID uuid;
    protected volatile long lastModTime = Util.getMillis();
    protected int maxStackSize;
    protected boolean infinityMode;
    //accessibility attributes
    @Nullable
    protected UUID owner;
    public List<UUID> white_list = new ArrayList<>();
    protected Accessibility accessibility;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private volatile boolean itemsDirty = true;

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
        return Collections.unmodifiableList(snapshotItems());
    }

    /**
     * @return Size of Endless Inventory.
     */
    public int getItemSize() {
        readLock.lock();
        try {
            if (!itemsDirty) {
                return this.items.size();
            }
        } finally {
            readLock.unlock();
        }
        writeLock.lock();
        try {
            if (itemsDirty) {
                refreshItemsFromMap();
            }
            return this.items.size();
        } finally {
            writeLock.unlock();
        }
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
        writeLock.lock();
        try {
            ItemKey key = ItemKey.asKey(stack);
            LOGGER.debug("EI:takeItem:stack->itemKay, stack={},itemKay = {}",stack,key);
            ItemState state = itemMap.get(key);
            if (state == null) {
                LOGGER.warn("EI:takeItem: no state for {}", key);
                LOGGER.debug("EI:Current Source Inventory: Class:{},UUID:{},Items:{}",this.getClass(),uuid,buildSnapshotLocked());
                if(getServerConfig().doConvertEmptyTag().get() && stack.getTag()!=null){
                    key = new ItemKey(key.item(),null);
                    if((state=itemMap.get(key))!=null){
                        LOGGER.info("EI:takeItem: converted ItemKey with empty tag {} to null tag",ItemKey.asKey(stack));
                    }else return ItemStack.EMPTY;
                }else return ItemStack.EMPTY;
            }
            LOGGER.info("EI:takeItem: key={} availableCount={} requestedCount={}", key, state.count(), count);
            if(state.count() >= maxStackSize && infinityMode){
                LOGGER.info("EI:takeItem: infinity mode for {} returning {}", key, count);
                setChanged();
                return stack.copyWithCount(count);
            }

            int taken = Math.min(count, state.count());
            ItemStack result = stack.copyWithCount(taken);
            if (taken == state.count()) {
                itemMap.remove(key);
                updateLastModTime();
                LOGGER.info("EI:takeItem: removed all of {} (taken={})", key, taken);
            } else {
                itemMap.put(key, new ItemState(state.count() - taken, updateLastModTime()));
                LOGGER.info("EI:takeItem: taken={} remaining={} for {}", taken, state.count()-taken, key);
            }
            markCacheDirty();
            setChanged();
            return result;
        } finally {
            writeLock.unlock();
        }
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
        writeLock.lock();
        try {
            ItemKey key = ItemKey.asKey(itemStack);
            if(getServerConfig().doConvertEmptyTag().get() && itemStack.getTag()!=null && Objects.equals(itemStack.getTag(),new CompoundTag())){
                key = new ItemKey(itemStack.getItem(),null);
            }
            ItemState state = itemMap.get(key);
            int count = itemStack.getCount();
            int original = 0;

            if (state != null) {
                original = state.count();
            }
            int increased;
            ItemStack remain;
            if(original < maxStackSize){
                increased = original+count;
                if(increased <= maxStackSize){
                    itemMap.put(key, new ItemState(increased, updateLastModTime()));
                    remain = ItemStack.EMPTY;
                }else {
                    itemMap.put(key, new ItemState(maxStackSize, updateLastModTime()));
                    remain = itemStack.copyWithCount(increased-maxStackSize);
                }
            }else if(infinityMode){
                itemMap.put(key, new ItemState(original, updateLastModTime()));
                remain = ItemStack.EMPTY;
            }else {
                return itemStack.copy();
            }
            markCacheDirty();
            setChanged();
            return remain;
        } finally {
            writeLock.unlock();
        }
    }

    public void clearContent() {
        writeLock.lock();
        try {
            this.itemMap.clear();
            markCacheDirty();
            updateLastModTime();
            this.setChanged();
        } finally {
            writeLock.unlock();
        }
    }

    //item handler: item view methods

    protected List<ItemStack> getSortedView(SortType type, boolean reverse) {
        List<ItemStack> ret = snapshotItems();
        ret.sort(ModInfo.sortHelper.getComparator(type, this));
        if(reverse) Collections.reverse(ret);
        return ret;
    }

    public List<ItemStack> getSortedAndFilteredItemView(int startIndex, int length, SortType sortType, boolean reverse, @Nullable Predicate<ItemStack> classify, String search){
        Stream<ItemStack> base = getSortedView(sortType,reverse).stream();
        return base
                .filter(classify!=null?classify:is->true)
                .filter(stack -> SearchUtil.matchesSearch(stack,search))
                .skip(startIndex)
                .limit(Math.max(length, 0))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    //item handler: map-list sync methods

    public void syncItemsFromMap() {
        writeLock.lock();
        try {
            refreshItemsFromMap();
        } finally {
            writeLock.unlock();
        }
    }

    public void syncMapFromItems() {
        writeLock.lock();
        try {
            this.itemMap.clear();
            for (ItemStack stack : items) {
                if (stack.isEmpty()) continue;
                long now = updateLastModTime();
                var key = ItemKey.asKey(stack);
                this.itemMap.put(key, new ItemState(stack.getCount(), now));
            }
            markCacheDirty();
        } finally {
            writeLock.unlock();
        }
    }

    //modification version mangers
    public long updateLastModTime(){
        lastModTime=Util.getMillis();
        return lastModTime;
    }

    protected List<ItemStack> snapshotItems() {
        readLock.lock();
        try {
            if (!itemsDirty) {
                return new ArrayList<>(items);
            }
        } finally {
            readLock.unlock();
        }
        writeLock.lock();
        try {
            if (itemsDirty) {
                refreshItemsFromMap();
            }
            return new ArrayList<>(items);
        } finally {
            writeLock.unlock();
        }
    }

    private void refreshItemsFromMap() {
        this.items = itemMap.entrySet().stream()
                .map(e -> e.getKey().toStack(e.getValue().count()))
                .collect(Collectors.toList());
        itemsDirty = false;
    }

    private List<ItemStack> buildSnapshotLocked() {
        return itemMap.entrySet().stream()
                .map(e -> e.getKey().toStack(e.getValue().count()))
                .collect(Collectors.toList());
    }

    private void markCacheDirty() {
        itemsDirty = true;
    }

    protected void overwriteItems(Map<ItemKey, ItemState> newItems) {
        writeLock.lock();
        try {
            this.itemMap.clear();
            this.itemMap.putAll(newItems);
            markCacheDirty();
            updateLastModTime();
        } finally {
            writeLock.unlock();
        }
    }
}
