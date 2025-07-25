package com.kwwsyk.endinv.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ItemKey(Item item, CompoundTag tag) {


    public static void encode(FriendlyByteBuf o,ItemKey key){
        o.writeItem(key.toStack(1));
    }

    public static ItemKey decode(FriendlyByteBuf o){
        ItemStack stack = o.readItem();
        return asKey(stack);
    }

    public ItemStack toStack(int count){
        var ret = new ItemStack(item);
        ret.setTag(tag);
        return ret;
    }

    public static ItemKey asKey(ItemStack stack){
        return new ItemKey(stack.getItem(),stack.getTag());
    }
}
