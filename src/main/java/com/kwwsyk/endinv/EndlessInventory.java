package com.kwwsyk.endinv;


import com.kwwsyk.endinv.options.ItemClassify;
import com.kwwsyk.endinv.options.ServerConfig;
import com.kwwsyk.endinv.util.*;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class EndlessInventory implements SourceInventory{

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final StreamCodec<RegistryFriendlyByteBuf,Map<ItemKey, ItemState>> ITEM_MAP_STREAM_CODEC = ByteBufCodecs.map(
            Object2ObjectLinkedOpenHashMap::new,
            ItemKey.STREAM_CODEC,
            ItemState.STREAM_CODEC,
            Integer.MAX_VALUE
    );

    private UUID uuid;

    private List<ItemStack> items;

    private final Map<ItemKey, ItemState> itemMap;

    @SuppressWarnings("unchecked")
    private final List<ItemStack>[] sortedViews = new List[SortType.values().length];

    private final long[] lastSortedTimes = new long[SortType.values().length];

    private long lastModTime = Util.getMillis();

    private int maxStackSize;

    private boolean infinityMode;

    public final EndInvAffinities affinities;

    public List<ServerPlayer> viewers = new ArrayList<>();

    @Nullable
    private UUID owner;

    public final List<UUID> white_list = new ArrayList<>();

    private Accessibility accessibility;

    public EndlessInventory(){
        this(UUID.randomUUID());
    }

    public EndlessInventory(UUID uuid){
        this.items = new ArrayList<>();
        this.itemMap = new Object2ObjectLinkedOpenHashMap<>();
        this.uuid = uuid;
        this.maxStackSize = ServerConfig.CONFIG.MAX_STACK_SIZE.getAsInt();
        this.infinityMode = ServerConfig.CONFIG.ENABLE_INFINITE.getAsBoolean();
        this.accessibility = ServerConfig.CONFIG.DEFAULT_ACCESSIBILITY.get();
        this.affinities = new EndInvAffinities(this);
    }


    private List<ItemStack> getSortedView(SortType type, boolean reverse) {
        int idx = type.ordinal();
        if (lastSortedTimes[idx] != lastModTime || sortedViews[idx] == null) {
            List<ItemStack> view = itemMap.entrySet().stream()
                    .map(e -> e.getKey().toStack(e.getValue().count()))
                    .collect(Collectors.toList());

            switch (type) {
                case COUNT -> view.sort(Comparator.comparingInt(ItemStack::getCount));
                case SPACE_AND_NAME -> view.sort(byId);
                case ID -> view.sort(REGISTRY_ORDER_COMPARATOR);
                case LAST_MODIFIED -> view.sort(Comparator.comparingLong(s -> itemMap.get(ItemKey.asKey(s)).lastModTime()));
                default -> {}
            }
            sortedViews[idx] = view;
            lastSortedTimes[idx] = lastModTime;
        }
        var ret = sortedViews[idx];
        if(reverse) ret = ret.reversed();
        return ret;
    }

    Comparator<ItemStack> byId = Comparator.comparing(
            s -> Optional.ofNullable(s.getItemHolder().getKey())
                    .map(ResourceKey::location)
                    .map(Object::toString)
                    .orElse("~") // 如果未注册，排在最前
    );
    Comparator<ItemStack> REGISTRY_ORDER_COMPARATOR = Comparator.comparingInt(
            stack -> BuiltInRegistries.ITEM.getId(stack.getItem())
    );

    public List<ItemStack> getSortedAndFilteredItemView(int startIndex, int length, SortType sortType,boolean reverse, ItemClassify classify, String search){
        Stream<ItemStack> base = getSortedView(sortType,reverse).stream();
        List<ItemStack> filtered = base
                .filter(classify::matches)
                .filter(stack -> SearchUtil.matchesSearch(stack,search))
                .toList();
        if(startIndex>= filtered.size()) return new ArrayList<>();
        return filtered.subList(startIndex,Math.min(startIndex+length,filtered.size()));
    }

    public List<ItemStackLike> getStarredItems(@Nonnegative int startIndex, @Nonnegative int length){
        var items = affinities.getStarredItems(startIndex,length);
        return items.stream().map(this::getStackWithZeroCount).toList();
    }

    public ItemStackLike getStackWithZeroCount(ItemStack stack){
        var state = itemMap.get(ItemKey.asKey(stack));
        if(state==null) return ItemStackLike.asKey(stack);
        return ItemStackLike.asKey(stack,state.count());
    }

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
        //invalidateCaches();
    }

    public Map<ItemKey,ItemState> getItemMap(){
        return itemMap;
    }

    public List<ItemStack> getItemsAsList(){
        syncItemsFromMap();
        return this.items;
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

    @Nullable
    public Optional<ServerPlayer> getOwner(ServerLevel level) {
        return level.getPlayers(pl->Objects.equals(pl.getUUID(),owner)).stream().findAny();
    }

    public boolean isOwner(Player player){
        return Objects.equals(player.getUUID(),owner);
    }

    public UUID getOwnerUUID(){
        return owner;
    }


    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
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
     * @return Size of Endless Inventory.
     */
    public int getItemSize() {
        return this.items.size();
    }


    public ItemStack getItem(int i) {
        return i>=0 && i<this.items.size() ? this.items.get(i) : ItemStack.EMPTY;
    }


    public void setChanged() {
        ServerLevelEndInv.levelEndInvData.setDirty();
    }

    public long updateLastModTime(){
        lastModTime=Util.getMillis();
        return lastModTime;
    }

    /**
     * Set endinv modState to new greater state.
     * @param newState should be greater than its original state
     * @return endinv's modState that has been updated
     */
    public long updateModState(long newState){
        this.lastModTime = Math.max(lastModTime,newState);
        return lastModTime;
    }

    public void broadcastChanges(){
        this.viewers.forEach(player -> ServerLevelEndInv
                .checkAndGetManagerForPlayer(player)
                .ifPresent(manager -> manager.getDisplayingPage().syncContentToClient(player)));
    }

    public boolean accessible(Player player) {
        return accessibility==Accessibility.PUBLIC || (owner!=null && owner.equals(player.getUUID())) || white_list.contains(player.getUUID()) && accessibility==Accessibility.RESTRICTED;
    }


    public void clearContent() {
        this.itemMap.clear();
        this.setChanged();
    }

}
