package com.kwwsyk.endinv.common;


import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemStackLike;
import com.kwwsyk.endinv.common.util.ItemState;
import com.kwwsyk.endinv.common.util.SortType;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;


public class EndlessInventory extends SourceInventory {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final StreamCodec<RegistryFriendlyByteBuf,Map<ItemKey, ItemState>> ITEM_MAP_STREAM_CODEC = ByteBufCodecs.map(
            Object2ObjectLinkedOpenHashMap::new,
            ItemKey.STREAM_CODEC,
            ItemState.STREAM_CODEC,
            Integer.MAX_VALUE
    );

    @SuppressWarnings("unchecked")
    private final List<ItemStack>[] sortedViews = new List[SortType.values().length];

    private final long[] lastSortedTimes = new long[SortType.values().length];

    public final EndInvAffinities affinities;

    public List<ServerPlayer> viewers = new ArrayList<>();

    public EndlessInventory(){
        this(UUID.randomUUID());
    }

    public EndlessInventory(UUID uuid){
        super(uuid);
        this.affinities = new EndInvAffinities(this);
    }

    protected List<ItemStack> getSortedView(SortType type, boolean reverse) {
        int idx = type.ordinal();
        if (lastSortedTimes[idx] != lastModTime || sortedViews[idx] == null) {
            List<ItemStack> view = itemMap.entrySet().stream()
                    .map(e -> e.getKey().toStack(e.getValue().count()))
                    .sorted(ModInfo.sortHelper.getComparator(type, this))
                    .collect(Collectors.toList());

            sortedViews[idx] = view;
            lastSortedTimes[idx] = lastModTime;
        }
        var ret = sortedViews[idx];
        if(reverse) ret = ret.reversed();
        return ret;
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

    @Nullable
    public Optional<ServerPlayer> getOwner(ServerLevel level) {
        return level.getPlayers(pl->Objects.equals(pl.getUUID(),owner)).stream().findAny();
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    public void setChanged() {
        ServerLevelEndInv.levelEndInvData.setDirty();
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
        this.viewers.forEach(player -> ServerLevelEndInv.checkAndGetManagerForPlayer(player)
                .ifPresent(manager -> manager.getDisplayingPage().syncContentToClient(player)));
    }
}
