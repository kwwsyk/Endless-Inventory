package com.kwwsyk.endinv.common.network.payloads;

import com.kwwsyk.endinv.common.ModInfo;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public interface ModPacketPayload extends CustomPacketPayload {

    default Type<? extends CustomPacketPayload> type(){
        return new Type<>(ResourceLocation.fromNamespaceAndPath(ModInfo.MOD_ID,id()));
    }

    String id();

}
