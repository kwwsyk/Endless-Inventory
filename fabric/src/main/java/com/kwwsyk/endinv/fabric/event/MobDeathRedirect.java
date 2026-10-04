package com.kwwsyk.endinv.fabric.event;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class MobDeathRedirect {
    private static final ThreadLocal<ServerPlayer> CURRENT_KILLER = new ThreadLocal<>();

    private MobDeathRedirect() {}

    public static void set(ServerPlayer killer) {
        CURRENT_KILLER.set(killer);
    }

    public static @Nullable ServerPlayer get() {
        return CURRENT_KILLER.get();
    }

    public static void clear() {
        CURRENT_KILLER.remove();
    }
}
