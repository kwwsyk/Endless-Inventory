package com.kwwsyk.endinv.common.options;

import com.kwwsyk.endinv.common.autopick.options.PickupHelperOptions;
import com.kwwsyk.endinv.common.options.config.ConfigEntryImpl;
import com.kwwsyk.endinv.common.options.config.IConfigValue;

import java.util.ArrayList;
import java.util.List;

public final class ServerConfigs {

    /// Implementations:
    /// @see com.kwwsyk.endinv.neoforge.options.ServerConfig
    /// @see com.kwwsyk.endinv.forge.ServerConfig
    /// @see com.kwwsyk.endinv.fabric.ModInit

    private ServerConfigs(){}

    private static final String[] CMT_CREATION_MODE = {
            "The default behavior of creating an end inventory when a player without endinv opens a container. Values: ",
            "CREATE_PER_PLAYER : create for every individual player",
            "USE_GLOBAL_SHARED : create only once and all players use that endinv",
            "NONE : don't create an endinv. You can customize endinv obtain condition to improve challenge." //such as with commands
    };

    private static final List<ConfigEntryImpl<?>> forgeConfigEntries = new ArrayList<>();

    private static <T extends IConfigValue<?>> T register(T configEntry){
        if(configEntry instanceof ConfigEntryImpl<?> entry){
            forgeConfigEntries.add(entry);
        }
        return configEntry;
    }

    public static List<ConfigEntryImpl<?>> getConfigs(){
        return forgeConfigEntries;
    }

    public static final ConfigEntryImpl<CreationEndinvStrategy> CREATION_MODE = register(new ConfigEntryImpl.EnumEntry<>("EndinvCreationMode", CMT_CREATION_MODE, CreationEndinvStrategy.CREATE_PER_PLAYER));
    public static final ConfigEntryImpl<Boolean> DEFAULT_ATTACH = register(new ConfigEntryImpl.BooleanEntry("DefaultAttach", new String[]{"Whether the endless inventory's display pages view is attached by default"}, true));
    public static final DefaultEndinvBehavior ENDINV_BEHAVIOR = register(DefaultEndinvBehavior.INSTANCE);
    public static final SpecifiedMenuAttachingConfig.Entry SPECIFIED_ATTACHABILITY = register(SpecifiedMenuAttachingConfig.Entry.INSTANCE);
    public static final PickupHelperOptions PICKUP_HELPER = register(new PickupHelperOptions());
}
