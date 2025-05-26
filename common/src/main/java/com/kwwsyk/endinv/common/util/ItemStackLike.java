package com.kwwsyk.endinv.common.util;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ItemStackLike(Item item, int count, DataComponentPatch components) {

    public static final StreamCodec<RegistryFriendlyByteBuf,ItemStackLike> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM),ItemStackLike::item,
            ByteBufCodecs.INT,ItemStackLike::count,
            DataComponentPatch.STREAM_CODEC,ItemStackLike::components,
            ItemStackLike::new
    );

    public static ItemStackLike asKey(ItemStack stack){
        return new ItemStackLike(stack.getItem(),0,stack.getComponentsPatch());
    }

    public static ItemStackLike asKey(ItemStack stack, int count){
        return new ItemStackLike(stack.getItem(),count,stack.getComponentsPatch());
    }

    public ItemStack toKey(){
        return new ItemStack(BuiltInRegistries.ITEM.wrapAsHolder(item),1,components);
    }
}
