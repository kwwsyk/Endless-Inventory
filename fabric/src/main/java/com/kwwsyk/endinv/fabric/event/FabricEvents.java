package com.kwwsyk.endinv.fabric.event;

public final class FabricEvents {

    private FabricEvents() {
    }

    public static void init() {
        Commands.register();
        LevelEvents.register();
        PlayerEvents.register();
        BlockBreakRedirect.register();
    }
}
