package com.kwwsyk.endinv.client.config;

import com.kwwsyk.endinv.options.ItemClassify;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {

    public static final ClientConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public final ModConfigSpec.IntValue ROWS;
    public final ModConfigSpec.IntValue COLUMNS;
    public final ModConfigSpec.BooleanValue AUTO_SUIT_COLUMN;
    public final ModConfigSpec.IntValue TEXTURE;
    public final List<ModConfigSpec.BooleanValue> PAGES = new ArrayList<>();
    public final ModConfigSpec.BooleanValue ATTACHING;

    private ClientConfig(ModConfigSpec.Builder builder){
        ATTACHING = builder.comment("show endless inventory view when opening a menu.")
                .define("attachingMenuScreen",true);

        ROWS = builder.comment("Default rows of EndInv view, 0 for auto.")
                .translation("config.endinv.comment.row1")
                .translation("config.endinv.comment.row2")
                .defineInRange("rows",0,0,Integer.MAX_VALUE);
        COLUMNS = builder.comment("Defaut columns of EndInv view, 0 for auto.")
                .defineInRange("columns",9,0,Integer.MAX_VALUE);
        AUTO_SUIT_COLUMN = builder.comment("auto suit in columns if GUI Size is too big.")
                .define("auto_suit_column",true);

        TEXTURE = builder.comment("Texture mode of EndInv view, transparent or vanilla menu style")
                .defineInRange("texture_mode",0,0,Integer.MAX_VALUE);

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
        return Math.floorDiv(height-45,18);
    }
    public int calculateSuitInColumnCount(AbstractContainerScreen<?> screen){
        int leftPos = screen.getGuiLeft();
        int width = leftPos - 20 - 6 -6;
        return Math.floorDiv(width,18);
    }

    static {
        Pair<ClientConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

}
