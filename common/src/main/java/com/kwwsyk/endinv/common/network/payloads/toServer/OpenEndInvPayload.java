package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.menu.page.pageManager.AttachingManager;
import com.kwwsyk.endinv.neoforge.ModInitializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**Send to server,
 * to open {@link EndlessInventoryMenu} when player is not opening a menu,
 * or to mention server to attach an {@link AttachingManager} if player is opening a menu.
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
