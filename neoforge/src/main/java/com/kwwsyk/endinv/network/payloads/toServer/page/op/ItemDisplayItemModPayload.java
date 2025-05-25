package com.kwwsyk.endinv.network.payloads.toServer.page.op;

import com.kwwsyk.endinv.ModInitializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Used when client item modified in ItemDisplay with {@link net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu}
 * @param isAdding true for add item and false for take item.
 */
public record ItemDisplayItemModPayload(ItemStack stack, boolean isAdding) implements CustomPacketPayload {

    public static final Type<ItemDisplayItemModPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"itemdisplay_modify"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ItemDisplayItemModPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,ItemDisplayItemModPayload::stack,
            ByteBufCodecs.BOOL,ItemDisplayItemModPayload::isAdding,
            ItemDisplayItemModPayload::new
    );

    @Override
    public @NotNull Type<ItemDisplayItemModPayload> type() {
        return TYPE;
    }
}
