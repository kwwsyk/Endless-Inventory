package com.kwwsyk.endinv.forge;

import com.kwwsyk.endinv.common.options.ContentTransferMode;
import com.kwwsyk.endinv.common.options.IConfigValue;
import com.kwwsyk.endinv.common.options.IServerConfig;
import com.kwwsyk.endinv.common.options.MissingEndInvPolicy;
import com.kwwsyk.endinv.common.util.Accessibility;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ServerConfig {

    public static final ServerConfig CONFIG;
    public static final ForgeConfigSpec CONFIG_SPEC;
    public final ForgeConfigSpec.IntValue MAX_STACK_SIZE;
    public final ForgeConfigSpec.BooleanValue ENABLE_INFINITE;
    public final ForgeConfigSpec.BooleanValue ENABLE_AUTO_PICK;
    public final ForgeConfigSpec.EnumValue<ContentTransferMode> TRANSFER_MODE;
    public final ForgeConfigSpec.EnumValue<Accessibility> DEFAULT_ACCESSIBILITY;
    public final ForgeConfigSpec.EnumValue<MissingEndInvPolicy> CREATION_MODE;

    private ServerConfig(ForgeConfigSpec.Builder builder){
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
        Pair<ServerConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    public final IServerConfig INSTANCE = new IServerConfig() {

        private static IConfigValue<Integer> convert(ForgeConfigSpec.IntValue value){
            return IConfigValue.of(value,value::set);
        }

        private static IConfigValue<Boolean> convert(ForgeConfigSpec.BooleanValue value){
            return IConfigValue.of(value,value::set);
        }

        @Override
        public IConfigValue<Integer> getMaxAllowedStackSize() {
            return convert(MAX_STACK_SIZE);
        }

        @Override
        public IConfigValue<Boolean> allowInfinityMode() {
            return convert(ENABLE_INFINITE);
        }

        @Override
        public IConfigValue<Boolean> enableAutoPick() {
            return convert(ENABLE_AUTO_PICK);
        }

        @Override
        public IConfigValue<ContentTransferMode> transferMode() {
            return IConfigValue.of(TRANSFER_MODE,TRANSFER_MODE::set);
        }

        @Override
        public IConfigValue<Accessibility> defaultAccessibility() {
            return IConfigValue.of(DEFAULT_ACCESSIBILITY,DEFAULT_ACCESSIBILITY::set);
        }

        @Override
        public IConfigValue<MissingEndInvPolicy> policyHandlingMissing() {
            return IConfigValue.of(CREATION_MODE,CREATION_MODE::set);
        }
    };
}
