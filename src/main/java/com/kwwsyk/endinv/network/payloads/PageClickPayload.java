package com.kwwsyk.endinv.network.payloads;

import com.kwwsyk.endinv.ModInitializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

public record PageClickPayload(int containerId, int pageId, double XOffset, double YOffset, int keyCode, ClickType clickType)
implements CustomPacketPayload{

    public static final StreamCodec<RegistryFriendlyByteBuf,PageClickPayload> STREAM_CODEC =
            StreamCodec.of(PageClickPayload::write,PageClickPayload::createPageClickPayload);
    public static final Type<PageClickPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"page_click"));
    private static PageClickPayload createPageClickPayload(RegistryFriendlyByteBuf buffer){
        return new PageClickPayload(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readEnum(ClickType.class));
    }
    private static void write(RegistryFriendlyByteBuf buffer,PageClickPayload payload){
        buffer.writeInt(payload.containerId);
        buffer.writeInt(payload.pageId);
        buffer.writeDouble(payload.XOffset);
        buffer.writeDouble(payload.YOffset);
        buffer.writeInt(payload.keyCode);
        buffer.writeEnum(payload.clickType);
    }


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }//ignore stateId currently

}
