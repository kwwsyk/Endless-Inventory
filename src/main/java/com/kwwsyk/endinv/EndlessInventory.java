package com.kwwsyk.endinv;


import com.kwwsyk.endinv.data.EndlessInventoryData;
import com.kwwsyk.endinv.options.ItemClassify;
import com.kwwsyk.endinv.options.ServerConfig;
import com.kwwsyk.endinv.options.SortType;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.ItemStackMap;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kwwsyk.endinv.ModInitializer.DEFAULT_UUID;
import static com.kwwsyk.endinv.ModInitializer.ENDINV_UUID;


public class EndlessInventory implements SourceInventory{

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final EndlessInventory EMPTY = new EndlessInventory(DEFAULT_UUID);

    private UUID uuid;
    private List<ItemStack> items;
    private final Map<ItemStack,ItemState> itemMap;
    @SuppressWarnings("unchecked")
    private final List<ItemStack>[] sortedViews = new List[SortType.values().length];
    private final long[] sortStates = new long[SortType.values().length];
    private long modState = 0L;
    private int maxStackSize;
    private boolean infinityMode;

    public EndlessInventory(){
        this(UUID.randomUUID());
    }

    public EndlessInventory(UUID uuid){
        this.items = new ArrayList<>();
        this.itemMap = ItemStackMap.createTypeAndTagLinkedMap();
        this.uuid = uuid;
        this.maxStackSize = ServerConfig.CONFIG.MAX_STACK_SIZE.getAsInt();
        this.infinityMode = ServerConfig.CONFIG.ENABLE_INFINITE.getAsBoolean();
    }

    public List<ItemStack> getSortedPage(SortType type, int start, int length) {
        List<ItemStack> base = getSortedView(type);
        return base.subList(start, Math.min(start + length, base.size()));
    }

    private List<ItemStack> getSortedView(SortType type) {
        int idx = type.ordinal();
        if (sortStates[idx] != modState || sortedViews[idx] == null) {
            List<ItemStack> view = itemMap.entrySet().stream()
                    .map(e -> e.getKey().copyWithCount(e.getValue().count()))
                    .collect(Collectors.toList());

            switch (type) {
                case COUNT -> view.sort(Comparator.comparingInt(ItemStack::getCount));
                //case ID -> view.sort(Comparator.comparing(s -> s.getItem().builtInRegistryHolder().key().location().toString()));
                case ID -> view.sort(byId);
                case LAST_MODIFIED -> view.sort(Comparator.comparingLong(s -> itemMap.get(s.copyWithCount(1)).lastModified()));
                default -> {}
            }
            sortedViews[idx] = view;
            sortStates[idx] = modState;
        }
        return sortedViews[idx];
    }

    Comparator<ItemStack> byId = Comparator.comparing(
            s -> Optional.ofNullable(s.getItemHolder().getKey())
                    .map(ResourceKey::location)
                    .map(Object::toString)
                    .orElse("~") // 如果未注册，排在最前
    );

    public List<ItemStack> getSortedAndFilteredItemView(int startIndex, int length, SortType sortType, ItemClassify classify, String search){
        Stream<ItemStack> base = getSortedView(sortType).stream();
        List<ItemStack> filtered = base.filter(classify::matches).toList();
        if(startIndex>= filtered.size()) return new ArrayList<>();
        return filtered.subList(startIndex,Math.min(startIndex+length,filtered.size()));
    }

    public void syncItemsFromMap() {
        this.items = itemMap.entrySet().stream()
                .map(e -> e.getKey().copyWithCount(e.getValue().count()))
                .collect(Collectors.toList());
    }

    public void syncMapFromItems() {
        this.itemMap.clear();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            long now = increaseModState();
            ItemStack key = stack.copyWithCount(1);
            this.itemMap.put(key, new ItemState(stack.getCount(), now));
        }
        //invalidateCaches();
    }

    public Map<ItemStack,ItemState> getItemMap(){
        return itemMap;
    }

    private static EndlessInventory createForPlayer(Player player){
        EndlessInventory endlessInventory = new EndlessInventory();
        EndlessInventoryData.levelEndInvData.addEndInvToLevel(endlessInventory);
        player.setData(ENDINV_UUID,endlessInventory.uuid);
        return endlessInventory;
    }

    private static EndlessInventory createForPlayer(Player player,UUID uuid){
        EndlessInventory endlessInventory = new EndlessInventory(uuid);
        EndlessInventoryData.levelEndInvData.addEndInvToLevel(endlessInventory);
        return endlessInventory;
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

    public List<ItemStack> getItems(){
        return this.items;
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
        ItemStack key = itemStack.copyWithCount(1);
        ItemState state = itemMap.get(key);
        int count = itemStack.getCount();
        int original = 0;

        if (state != null) {
            original = state.count;
        }
        int increased;
        if(original < maxStackSize){
            increased = original+count;
            if(increased <= maxStackSize){
                itemMap.put(key, new ItemState(increased, increaseModState()));
                setChanged();
                return ItemStack.EMPTY;
            }else {
                itemMap.put(key, new ItemState(maxStackSize, increaseModState()));
                setChanged();
                return itemStack.copyWithCount(increased-maxStackSize);
            }
        }else if(infinityMode){
            itemMap.put(key, new ItemState(original, increaseModState()));
            setChanged();
            return ItemStack.EMPTY;
        }else {
            return itemStack.copy();
        }
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
        ItemStack key = stack.copyWithCount(1);
        ItemState state = itemMap.get(key);
        if (state == null) return ItemStack.EMPTY;
        //if infinity
        if(state.count >= maxStackSize && infinityMode){
            setChanged();
            return stack.copyWithCount(count);
        }

        int taken = Math.min(count, state.count());
        ItemStack result = stack.copyWithCount(taken);
        if (taken == state.count()) {
            itemMap.remove(key);
            increaseModState();
        } else {
            itemMap.put(key, new ItemState(state.count() - taken, increaseModState()));
        }
        setChanged();
        return result;

    }

    @Override
    public boolean isRemote() {
        return false;
    }

    /**
     * Take at most its max stack size count item
     * @param itemStack will take itemStack with same id and component
     * @return items taken
     */
    public ItemStack takeItem(ItemStack itemStack){
        return  takeItem(itemStack,itemStack.getMaxStackSize());
    }

    public UUID getUuid(){
        return  this.uuid;
    }

    public UUID giveNewUuid(){
        uuid=UUID.randomUUID();
        return uuid;
    }

    /**
     * Get player's EndInv, if player has not or has invalid, create new.
     * @param player the player
     * @return Player's EndInv, original or created
     */
    public static EndlessInventory getEndInvForPlayer(Player player){
        EndlessInventory endlessInventory;
        if(hasEndInvUuid(player)){
            endlessInventory = getPlayerDefaultEndInv(player);
            if(endlessInventory==null) endlessInventory = createForPlayer(player,player.getData(ENDINV_UUID));
        }else{
            endlessInventory = createForPlayer(player);

        }
        return endlessInventory;
    }

    /**
     * Check whether player has a valid uuid, which is not {@link ModInitializer#DEFAULT_UUID}
     * @param player player to check endInv uuid
     * @return true if player has uuid and the uuid is valid.
     */
    public static boolean hasEndInvUuid(Player player){
        if(player.hasData(ENDINV_UUID)){
            if(player.getData(ENDINV_UUID)==DEFAULT_UUID){
                LOGGER.warn("Player {} has default endless inventory UUID.", player.getName().getString());
                return false;
            }
            return true;
        }else return false;
    }

    private static EndlessInventory getPlayerDefaultEndInv(Player player){
        return EndlessInventoryData.levelEndInvData.fromUUID(player.getData(ENDINV_UUID));
    }

    /**
     * @return Size of Endless Inventory.
     */
    public int getItemSize() {
        return this.items.size();
    }


    public ItemStack getItem(int i) {
        return i>=0 && i<this.items.size() ? this.items.get(i) : ItemStack.EMPTY;
    }


    public void setChanged() {
        EndlessInventoryData.levelEndInvData.setDirty();
    }
    public long increaseModState(){
        ++modState;
        return modState;
    }

    public boolean stillValid(Player player) {
        //TODO ACCESSIBLE?
        return true;
    }


    public void clearContent() {
        this.itemMap.clear();
        this.setChanged();
    }

    public record ItemState(int count, long lastModified){}
}
