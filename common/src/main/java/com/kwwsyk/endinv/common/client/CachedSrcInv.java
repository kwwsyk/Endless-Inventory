package com.kwwsyk.endinv.common.client;

import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.network.payloads.toClient.EndInvMetadata;
import com.kwwsyk.endinv.neoforge.options.ItemClassify;
import com.kwwsyk.endinv.util.*;
import com.kwwsyk.neoforge.util.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class CachedSrcInv implements SourceInventory {

    public static final CachedSrcInv INSTANCE = new CachedSrcInv();

    Minecraft mc =Minecraft.getInstance();
    @Nullable
    LocalPlayer player = mc.player;
    private List<ItemStack> items;
    private Map<ItemKey, ItemState> itemMap = new Object2ObjectLinkedOpenHashMap<>();

    private int maxStackSize;
    private boolean infinityMode;

    private Accessibility accessibilty;
    @Nullable
    private UUID owner;
    public List<UUID> white_list = new ArrayList<>();

    private CachedSrcInv(){}

    private List<ItemStack> getSortedView(SortType type, boolean reverse) {

        List<ItemStack> view = itemMap.entrySet().stream()
                .map(e -> e.getKey().toStack(e.getValue().count()))
                .collect(Collectors.toList());

        switch (type) {
            case SortType.COUNT -> view.sort(Comparator.comparingInt(ItemStack::getCount));
            case SortType.SPACE_AND_NAME -> view.sort(byId);
            case SortType.ID -> view.sort(REGISTRY_ORDER_COMPARATOR);
            case SortType.LAST_MODIFIED -> view.sort(Comparator.comparingLong(s -> itemMap.get(ItemKey.asKey(s)).lastModTime()));
            default -> {}
        }

        var ret = view;
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

    public List<ItemStack> getSortedAndFilteredItemView(int startIndex, int length, SortType sortType, boolean reverse, ItemClassify classify, String search){
        Stream<ItemStack> base = getSortedView(sortType,reverse).stream();
        List<ItemStack> filtered = base
                .filter(classify::matches)
                .filter(stack -> SearchUtil.matchesSearch(stack,search))
                .toList();
        if(startIndex>= filtered.size()) return new ArrayList<>();
        return filtered.subList(startIndex,Math.min(startIndex+length,filtered.size()));
    }

    public List<ItemStack> getItemsAsList(){
        syncItemsFromMap();
        return items;
    }

    public void syncItemsFromMap() {
        this.items = itemMap.entrySet().stream()
                .map(e -> e.getKey().toStack(e.getValue().count()))
                .collect(Collectors.toList());
    }

    public void initializeContents(Map<ItemKey, ItemState> itemMap){
        this.itemMap = new Object2ObjectLinkedOpenHashMap<>(itemMap);
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public ItemStack takeItem(ItemStack itemStack){
        return takeItem(itemStack,itemStack.getMaxStackSize());
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

    @Override
    public int getItemSize() {
        syncItemsFromMap();
        return items.size();
    }

    @Override
    public ItemStack getItem(int i) {
        return i>=0 && i<this.items.size() ? this.items.get(i) : ItemStack.EMPTY;
    }

    public long updateLastModTime(){
        return Util.getMillis();
    }

    public void syncMetadata(EndInvMetadata endInvMetadata) {
        this.maxStackSize = endInvMetadata.maxStackSize();
        this.infinityMode = endInvMetadata.infinityMode();
        this.accessibilty =endInvMetadata.config().accessibility();
        this.owner = endInvMetadata.config().owner();
        this.white_list = endInvMetadata.config().white_list();
    }

    public Accessibility getAccessibility() {
        return this.accessibilty;
    }

    public void setAccessibility(Accessibility accessibility) {
        this.accessibilty = accessibility;
    }

    public UUID getOwnerUUID(){
        return owner;
    }
}
