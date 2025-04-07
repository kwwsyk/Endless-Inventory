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

public record EndInvSettings(int rows) implements CustomPacketPayload {

    public static final EndInvSettings DEFAULT = new EndInvSettings(15);

    public static final Codec<EndInvSettings> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.optionalFieldOf("rows", 15).forGetter(EndInvSettings::rows)
            ).apply(instance, EndInvSettings::new)
    );

    public static final Type<EndInvSettings> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"endinv_settings"));

    public static final StreamCodec<ByteBuf,EndInvSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,EndInvSettings::rows,
            EndInvSettings::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
