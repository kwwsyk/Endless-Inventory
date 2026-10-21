package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.util.Accessibility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @param owner {@link ModInfo#DEFAULT_UUID} to present null uuid.
 */
public record EndInvConfig(Accessibility accessibility, UUID owner, List<UUID> white_list) {

    public static final EndInvConfig DEFAULT = new EndInvConfig(Accessibility.PUBLIC, ModInfo.DEFAULT_UUID,new ArrayList<>());


    public static EndInvConfig decode(FriendlyByteBuf friendlyByteBuf) {
        Accessibility accessibility = friendlyByteBuf.readEnum(Accessibility.class);
        UUID owner = friendlyByteBuf.readUUID();
        int whiteListSize = ByteBufCodecs.readCount(friendlyByteBuf, Integer.MAX_VALUE);
        List<UUID> whiteList = new ArrayList<>(Math.min(whiteListSize, 65536));
        for(int i = 0; i < whiteListSize; i++){
            whiteList.add(friendlyByteBuf.readUUID());
        }
        return new EndInvConfig(
                accessibility,
                owner,
                whiteList
        );
    }

    public static void encode(FriendlyByteBuf o, EndInvConfig endInvConfig) {
        o.writeEnum(endInvConfig.accessibility);
        o.writeUUID(endInvConfig.owner);
        ByteBufCodecs.writeCount(o, endInvConfig.white_list.size(), Integer.MAX_VALUE);
        endInvConfig.white_list.forEach(o::writeUUID);
    }


    public static EndInvConfig getWith(EndlessInventory endInv){
        return new EndInvConfig(
                endInv.getAccessibility(),
                endInv.getOwnerUUID()!=null?endInv.getOwnerUUID():ModInfo.DEFAULT_UUID,
                endInv.white_list);
    }

    public String id() {
        return "endinv_config";
    }
}
