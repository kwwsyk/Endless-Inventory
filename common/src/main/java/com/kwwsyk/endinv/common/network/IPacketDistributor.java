package com.kwwsyk.endinv.common.network;

import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public interface IPacketDistributor {

    void sendToServer(ModPacketPayload payload);

    void sendToPlayer(ServerPlayer player, ModPacketPayload payload);
}
