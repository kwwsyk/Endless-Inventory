package com.kwwsyk.endinv.neoforge.options;

import com.kwwsyk.endinv.common.options.ContentTransferMode;
import com.kwwsyk.endinv.common.options.IServerConfig;
import com.kwwsyk.endinv.common.options.MissingEndInvPolicy;
import com.kwwsyk.endinv.common.util.Accessibility;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ServerConfig {

    public static final ServerConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public final ModConfigSpec.IntValue MAX_STACK_SIZE;
    public final ModConfigSpec.BooleanValue ENABLE_INFINITE;
    public final ModConfigSpec.BooleanValue ENABLE_AUTO_PICK;
    public final ModConfigSpec.EnumValue<ContentTransferMode> TRANSFER_MODE;
    public final ModConfigSpec.EnumValue<Accessibility> DEFAULT_ACCESSIBILITY;
    public final ModConfigSpec.EnumValue<MissingEndInvPolicy> CREATION_MODE;

    private ServerConfig(ModConfigSpec.Builder builder){
        MAX_STACK_SIZE = builder
                .translation("config.endinv.comment.max_stack_size")
                .defineInRange("ItemCapacity.maxStackSize",Integer.MAX_VALUE,0,Integer.MAX_VALUE);
        ENABLE_INFINITE = builder
                .translation("config.endinv.comment.enable_infinite1")
                .define("ItemCapacity.enableInfinite",false);
        ENABLE_AUTO_PICK = builder
                .comment("Will enable player to auto pick item and exp")
                .define("autoPickUtility",false);
        TRANSFER_MODE = builder
                .defineEnum("TransferMode",ContentTransferMode.ALL);
        DEFAULT_ACCESSIBILITY = builder
                .defineEnum("defaultAccessibility",Accessibility.PUBLIC);
        CREATION_MODE = builder
                .defineEnum("creationMode",MissingEndInvPolicy.CREATE_PER_PLAYER);
    }

    static {
        Pair<ServerConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    public final IServerConfig INSTANCE = new IServerConfig() {
        @Override
        public int getMaxAllowedStackSize() {
            return MAX_STACK_SIZE.getAsInt();
        }

        @Override
        public boolean allowInfinityMode() {
            return ENABLE_INFINITE.getAsBoolean();
        }

        @Override
        public boolean enableAutoPick() {
            return ENABLE_AUTO_PICK.getAsBoolean();
        }

        @Override
        public ContentTransferMode transferMode() {
            return TRANSFER_MODE.get();
        }

        @Override
        public Accessibility defaultAccessibility() {
            return DEFAULT_ACCESSIBILITY.get();
        }

        @Override
        public MissingEndInvPolicy policyHandlingMissing() {
            return CREATION_MODE.get();
        }
    };
}
