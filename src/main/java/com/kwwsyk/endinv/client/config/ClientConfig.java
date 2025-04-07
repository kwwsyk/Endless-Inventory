package com.kwwsyk.endinv;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ClientConfig {

    public static final ClientConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public final ModConfigSpec.IntValue ROWS;
    public final ModConfigSpec.EnumValue<SortType> SORT_TYPE;

    private ClientConfig(ModConfigSpec.Builder builder){

        ROWS = builder.translation("config.endinv.comment.row1")
                .translation("config.endinv.comment.row2")
                .defineInRange("rows",0,0,Integer.MAX_VALUE);
        SORT_TYPE = builder.translation("config.endinv.comment.sort_type")
                
                .defineEnum("sort_type", SortType.DEFAULT);
    }

    static {
        Pair<ClientConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

}
