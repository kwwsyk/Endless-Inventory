package com.kwwsyk.endinv.fabric.event;

import com.kwwsyk.endinv.common.autopick.AutoPickHelper;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerPlayer;

public final class BlockBreakRedirect {

    private static final ThreadLocal<ServerPlayer> CURRENT_BREAKER = new ThreadLocal<>();

    private BlockBreakRedirect() {}

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer sp && AutoPickHelper.isPlayerEnabledAutoPick(sp)) {
                CURRENT_BREAKER.set(sp);
            } else {
                CURRENT_BREAKER.remove();
            }
            return true; // allow vanilla break; we intercept drops via mixins
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            CURRENT_BREAKER.remove();
        });
    }

    public static ServerPlayer currentBreaker() {
        return CURRENT_BREAKER.get();
    }

    public static void pushBreaker(ServerPlayer sp) {
        CURRENT_BREAKER.set(sp);
    }

    public static void clearBreaker() {
        CURRENT_BREAKER.remove();
    }
}
