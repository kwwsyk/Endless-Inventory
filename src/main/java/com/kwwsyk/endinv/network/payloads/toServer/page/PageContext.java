package com.kwwsyk.endinv.network.payloads.toServer.page;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.network.payloads.PageData;
import com.kwwsyk.endinv.util.SortType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**In page context used on Page operations.
 * @param startIndex
 * @param length
 * @param pageData
 */
public record PageContext(int startIndex, int length, PageData pageData) implements CustomPacketPayload {
    
    public static final Type<PageContext> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"page_metadata"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PageContext> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PageContext::startIndex,
            ByteBufCodecs.INT, PageContext::length,
            PageData.STREAM_CODEC, PageContext::pageData,
            PageContext::new
    );


    public SortType sortType() {
        return pageData.sortType();
    }

    public String search() {
        return pageData.search();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
