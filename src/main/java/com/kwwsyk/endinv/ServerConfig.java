package com.kwwsyk.endinv;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ServerConfig {

    public static final ServerConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public final ModConfigSpec.IntValue MAX_STACK_SIZE;
    public final ModConfigSpec.BooleanValue ENABLE_INFINITE;

    private ServerConfig(ModConfigSpec.Builder builder){
        MAX_STACK_SIZE = builder
                .translation("config.endinv.comment.max_stack_size")
                .defineInRange("ItemCapacity.maxStackSize",Integer.MAX_VALUE,0,Integer.MAX_VALUE);
        ENABLE_INFINITE = builder
                .translation("config.endinv.comment.enable_infinite1")
                .translation("config.endinv.comment.enable_infinite2")
                .define("ItemCapacity.enableInfinite",false);
    }

    static {
        Pair<ServerConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }
}
