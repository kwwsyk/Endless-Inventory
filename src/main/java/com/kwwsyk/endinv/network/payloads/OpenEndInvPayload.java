package com.kwwsyk.endinv.network.payloads;

import com.kwwsyk.endinv.ModInitializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**Send to server,
 * to open {@link com.kwwsyk.endinv.menu.EndlessInventoryMenu} when player is not opening a menu,
 * or to mention server to attach an {@link com.kwwsyk.endinv.menu.page.pageManager.AttachingManager} if player is opening a menu.
 */
public record OpenEndInvPayload(boolean openNew) implements CustomPacketPayload {

    public static final Type<OpenEndInvPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"open_endinv"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OpenEndInvPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,OpenEndInvPayload::openNew,
            OpenEndInvPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
