package com.kwwsyk.endinv.common.network.payloads;

import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public interface ModPacketContext {

    @Nullable
    Player player();
}
