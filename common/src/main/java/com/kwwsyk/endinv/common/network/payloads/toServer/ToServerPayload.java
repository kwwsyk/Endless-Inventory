package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;

public interface ToServerPayload extends ModPacketPayload {

    void handle(ToServerPacketContext context);

}
