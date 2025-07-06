package com.kwwsyk.endinv.common;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EndInvAffinities {

    public static final Logger LOGGER = LogUtils.getLogger();

    public final List<ItemStack> starredItems = new ArrayList<>();
    public final EndlessInventory endInv;
    public final UUID endInvUUID;

    public EndInvAffinities(EndlessInventory endInv){
        this.endInv = endInv;
        this.endInvUUID = endInv.getUuid();
    }

    public void addStarredItem(ItemStack stack){
        if(stack.isEmpty()) return;
        for(ItemStack item : starredItems){
            if(ItemStack.isSameItemSameComponents(item,stack)){
                return;
            }
        }
        starredItems.add(stack.copyWithCount(1));
    }

    public void removeStarredItem(ItemStack stack){
        if (stack.isEmpty()) return;
        starredItems.removeIf(item -> ItemStack.isSameItemSameComponents(item, stack));
    }

    /**
     * Get list of starred items of EndInv.
     * @param startIndex the startIndex of sublist
     * @param length the length of sublist, if too big, will return whole or just to last.
     * @return the sublist of starred items without copy.
     */
    public List<ItemStack> getStarredItems(@Nonnegative int startIndex,@Nonnegative int length){
        if(length >= starredItems.size()) return starredItems;
        if(startIndex+length > starredItems.size()) return starredItems.subList(startIndex,-1);
        return starredItems.subList(startIndex,startIndex+length);
    }
}
