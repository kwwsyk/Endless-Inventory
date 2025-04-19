package com.kwwsyk.endinv.client.config;

import com.kwwsyk.endinv.options.ItemClassify;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {

    public static final ClientConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public final ModConfigSpec.IntValue ROWS;
    public final ModConfigSpec.IntValue LAYOUT;
    public final List<ModConfigSpec.BooleanValue> PAGES = new ArrayList<>();

    private ClientConfig(ModConfigSpec.Builder builder){

        ROWS = builder.translation("config.endinv.comment.row1")
                .translation("config.endinv.comment.row2")
                .defineInRange("rows",0,0,Integer.MAX_VALUE);

        LAYOUT = builder.defineInRange("layout",0,0,Integer.MAX_VALUE);

        int index = 0;
        for (Holder<ItemClassify> classify : ItemClassify.DEFAULT_CLASSIFIES) {
            String name = classify.getRegisteredName();
            boolean hidden = ItemClassify.INDEX2HIDING.get(index)>0;
            ModConfigSpec.BooleanValue builderEntry = builder.comment("Hide "+name+" page, true for hidden")
                    .translation("config.endinv.comment.hidepages")
                    .define("hide_pages."+name, hidden);
            PAGES.add(builderEntry);
            index++;
        }
    }

    public int calculateDefaultRowCount(){
        Minecraft mc = Minecraft.getInstance();
        int height = mc.getWindow().getGuiScaledHeight();
        if(LAYOUT.getAsInt()==0)
            return Math.floorDiv(height-115,18);
        return Math.floorDiv(height-25,18);
    }

    static {
        Pair<ClientConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

}
