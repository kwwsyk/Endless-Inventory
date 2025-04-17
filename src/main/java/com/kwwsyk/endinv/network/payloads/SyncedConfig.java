package com.kwwsyk.endinv.network.payloads;

import com.kwwsyk.endinv.ModInitializer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**Synced endless inventory config data across server player (attached) data and
 *  client ClientConfig of player.
 * @param rows
 */
public record SyncedConfig(int rows) implements CustomPacketPayload {

    public static final SyncedConfig DEFAULT = new SyncedConfig(15);

    public static final Codec<SyncedConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.optionalFieldOf("rows", 15).forGetter(SyncedConfig::rows)
            ).apply(instance, SyncedConfig::new)
    );

    public static final Type<SyncedConfig> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"endinv_settings"));

    public static final StreamCodec<ByteBuf, SyncedConfig> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncedConfig::rows,
            SyncedConfig::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
