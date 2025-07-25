package com.kwwsyk.endinv.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ItemStackLike(Item item, int count, CompoundTag tag) {

    public static void encode(FriendlyByteBuf o,ItemStackLike item){
        o.writeItem(item.toKey());
    }

    public static ItemStackLike decode(FriendlyByteBuf o){
        return asKey(o.readItem());
    }

    public static ItemStackLike asKey(ItemStack stack){
        return new ItemStackLike(stack.getItem(),0,stack.getTag());
    }

    public static ItemStackLike asKey(ItemStack stack, int count){
        return new ItemStackLike(stack.getItem(),count,stack.getTag());
    }

    public ItemStack toKey(){
        var ret = new ItemStack(item,count);
        ret.setTag(tag);
        return ret;
    }
}
