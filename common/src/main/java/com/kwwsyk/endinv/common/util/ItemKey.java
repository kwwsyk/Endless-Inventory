package com.kwwsyk.endinv.common.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public record ItemKey(Item item, @Nullable CustomData customData) {

    public static void encode(FriendlyByteBuf output, ItemKey key) {
        output.writeItem(key.toStack(1));
    }

    public static ItemKey decode(FriendlyByteBuf input) {
        ItemStack stack = input.readItem();
        return asKey(stack);
    }

    public ItemStack toStack(int count) {
        ItemStack result = new ItemStack(item, count);
        applyCustomData(result, customData);
        return result;
    }

    public static ItemKey asKey(ItemStack stack) {
        return new ItemKey(stack.getItem(), copyCustomData(stack));
    }

    public boolean hasCustomData() {
        return !isEmpty(customData);
    }

    public static @Nullable CustomData copyCustomData(ItemStack stack) {
        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        if (component == null) {
            return null;
        }
        CompoundTag copied = component.copyTag();
        return copied == null ? null : CustomData.of(copied);
    }

    public static void applyCustomData(ItemStack stack, @Nullable CustomData component) {
        if (isEmpty(component)) {
            stack.remove(DataComponents.CUSTOM_DATA);
            return;
        }

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(component.copyTag()));
    }

    public static boolean isEmpty(@Nullable CustomData component) {
        if (component == null) {
            return true;
        }
        CompoundTag copied = component.copyTag();
        return copied == null || copied.isEmpty();
    }
}
