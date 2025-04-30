package com.kwwsyk.endinv.network.payloads.toClient;

import com.kwwsyk.endinv.ModInitializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record SetItemDisplayContentPayload(List<ItemStack> stacks) implements CustomPacketPayload {

    public static final Type<SetItemDisplayContentPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"endinv_content"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetItemDisplayContentPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC,SetItemDisplayContentPayload::stacks,
            SetItemDisplayContentPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
}
