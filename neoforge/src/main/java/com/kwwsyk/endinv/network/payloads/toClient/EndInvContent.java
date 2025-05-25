package com.kwwsyk.endinv.network.payloads.toClient;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.util.ItemKey;
import com.kwwsyk.endinv.util.ItemState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record EndInvContent(Map<ItemKey, ItemState> itemMap) implements CustomPacketPayload {

    public static final Type<EndInvContent> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"endinv_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EndInvContent> STREAM_CODEC = StreamCodec.composite(
            EndlessInventory.ITEM_MAP_STREAM_CODEC,EndInvContent::itemMap,
            EndInvContent::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
