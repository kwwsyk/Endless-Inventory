package com.kwwsyk.endinv.common.network.payloads.toServer.page.op;

import com.kwwsyk.endinv.common.network.payloads.toServer.page.PageContext;
import com.kwwsyk.endinv.neoforge.ModInitializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

public record PageClickPayload(int containerId, PageContext context, double XOffset, double YOffset, int keyCode, ClickType clickType)
implements CustomPacketPayload{

    public static final StreamCodec<RegistryFriendlyByteBuf,PageClickPayload> STREAM_CODEC =
            StreamCodec.of(PageClickPayload::write,PageClickPayload::createPageClickPayload);
    public static final Type<PageClickPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"page_click"));
    private static PageClickPayload createPageClickPayload(RegistryFriendlyByteBuf buffer){
        return new PageClickPayload(
                buffer.readInt(),
                PageContext.STREAM_CODEC.decode(buffer),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readEnum(ClickType.class));
    }
    private static void write(RegistryFriendlyByteBuf buffer,PageClickPayload payload){
        buffer.writeInt(payload.containerId);
        PageContext.STREAM_CODEC.encode(buffer,payload.context);
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
