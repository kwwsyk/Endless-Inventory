package com.kwwsyk.endinv.neoforge.client.config;

import com.kwwsyk.endinv.common.client.TextureMode;
import com.kwwsyk.endinv.common.menu.page.PageType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

import static com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager.defaultPages;

public class ClientConfig {

    public static final ClientConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public final ModConfigSpec.IntValue ROWS;
    public final ModConfigSpec.IntValue COLUMNS;
    public final ModConfigSpec.BooleanValue AUTO_SUIT_COLUMN;
    public final ModConfigSpec.EnumValue<TextureMode> TEXTURE;
    public final List<ModConfigSpec.BooleanValue> PAGES = new ArrayList<>();
    public final ModConfigSpec.BooleanValue ATTACHING;
    public final ModConfigSpec.BooleanValue ENABLE_DEBUG;
    public final ModConfigSpec.IntValue MAX_PAGE_BARS;

    private ClientConfig(ModConfigSpec.Builder builder){
        ATTACHING = builder.comment("show endless inventory view when opening a menu.")
                .define("attachingMenuScreen",true);

        ROWS = builder.comment("Default rows of EndInv view, 0 for auto.")
                .translation("config.endinv.comment.row1")
                .defineInRange("rows",0,0,Integer.MAX_VALUE);
        COLUMNS = builder.comment("Default columns of EndInv view, 0 for auto.")
                .defineInRange("columns",9,0,Integer.MAX_VALUE);

        AUTO_SUIT_COLUMN = builder.comment("auto suit in columns if GUI Size is too big.")
                .define("auto_suit_column",true);

        TEXTURE = builder.comment("Texture mode of EndInv view, transparent or vanilla menu style")
                .defineEnum("texture_mode",TextureMode.FROM_RESOURCE);

        ENABLE_DEBUG = builder.comment("Press F3 in screen can show some information of menu screen")
                .define("enable_debug",false);

        MAX_PAGE_BARS = builder
                .defineInRange("max_page_bars",10,1,255);

        for (net.minecraft.core.Holder<PageType> pageHolder : defaultPages) {
            ModConfigSpec.BooleanValue pageEntry = builder
                    .comment("Hide page: " + pageHolder.getRegisteredName())
                    .define("hide_pages." + pageHolder.getRegisteredName(), false);
            PAGES.add(pageEntry);
        }
    }

    public int calculateDefaultRowCount(boolean ofMenu){
        Minecraft mc = Minecraft.getInstance();
        int height = mc.getWindow().getGuiScaledHeight();
        return Math.max(Math.floorDiv(height-60,18)-(ofMenu?4:0),0);
    }
    public int calculateSuitInColumnCount(AbstractContainerScreen<?> screen){
        int leftPos = screen.getGuiLeft();
        int width = leftPos - 20 - 6 -6;
        return Math.max(0,Math.floorDiv(width,18));
    }

    static {
        Pair<ClientConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

}
