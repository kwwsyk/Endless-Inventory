package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.util.Accessibility;
import com.kwwsyk.endinv.neoforge.ModInitializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @param owner {@link ModInitializer#DEFAULT_UUID} to present null uuid.
 */
public record EndInvConfig(Accessibility accessibility, UUID owner, List<UUID> white_list) implements CustomPacketPayload {

    public static final EndInvConfig DEFAULT = new EndInvConfig(Accessibility.PUBLIC,ModInitializer.DEFAULT_UUID,new ArrayList<>());

    public static final Type<EndInvConfig> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"endinv_config"));

    public static final StreamCodec<FriendlyByteBuf,EndInvConfig> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public EndInvConfig decode(FriendlyByteBuf friendlyByteBuf) {
                    return new EndInvConfig(
                            friendlyByteBuf.readEnum(Accessibility.class),
                            friendlyByteBuf.readUUID(),
                            friendlyByteBuf.readList(b -> b.readUUID())
                    );
                }

                @Override
                public void encode(FriendlyByteBuf o, EndInvConfig endInvConfig) {
                    o.writeEnum(endInvConfig.accessibility);
                    o.writeUUID(endInvConfig.owner);
                    o.writeCollection(endInvConfig.white_list, (b, uuid) -> b.writeUUID(uuid));
                }
            };

    public static EndInvConfig getWith(EndlessInventory endInv){
        return new EndInvConfig(
                endInv.getAccessibility(),
                endInv.getOwnerUUID()!=null?endInv.getOwnerUUID():ModInitializer.DEFAULT_UUID,
                endInv.white_list);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
