package com.kwwsyk.endinv.common;

import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public interface NbtAttachment<T> {

    @Nullable
    T getWith(Player player);

    void setTo(Player player, T t);

    T computeIfAbsent(Player player);

}
