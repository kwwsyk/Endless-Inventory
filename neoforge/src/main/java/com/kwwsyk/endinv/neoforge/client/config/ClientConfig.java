package com.kwwsyk.endinv.neoforge.client.config;

import com.kwwsyk.endinv.common.client.option.IClientConfig;
import com.kwwsyk.endinv.common.client.option.TextureMode;
import com.kwwsyk.endinv.common.menu.page.PageTypeRegistry;
import com.kwwsyk.endinv.common.options.IConfigValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ClientConfig {

    public static final ClientConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public final ModConfigSpec.IntValue ROWS;
    public final ModConfigSpec.IntValue COLUMNS;
    public final ModConfigSpec.BooleanValue AUTO_SUIT_COLUMN;
    public final ModConfigSpec.EnumValue<TextureMode> TEXTURE;
    public final Map<String,ModConfigSpec.BooleanValue> PAGE2HIDING = new LinkedHashMap<>();
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

        for (String id : PageTypeRegistry.getIdList()) {
            ModConfigSpec.BooleanValue pageEntry = builder
                    .comment("Hide page: " + id)
                    .define("hide_pages." + id, false);
            PAGE2HIDING.put(id,pageEntry);
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

    public final IClientConfig INSTANCE = new IClientConfig() {

        private static IConfigValue<Boolean> convert(ModConfigSpec.BooleanValue value){
            return IConfigValue.of(value::getAsBoolean,value::set);
        }

        private static IConfigValue<Integer> convert(ModConfigSpec.IntValue value){
            return IConfigValue.of(value::getAsInt,value::set);
        }

        @Override
        public IConfigValue<Boolean> attaching() {
            return convert(ATTACHING);
        }

        @Override
        public IConfigValue<Integer> rows() {
            return convert(ROWS);
        }

        @Override
        public IConfigValue<Integer> columns() {
            return convert(COLUMNS);
        }

        @Override
        public IConfigValue<Boolean> autoSuitColumn() {
            return convert(AUTO_SUIT_COLUMN);
        }

        @Override
        public IConfigValue<TextureMode> textureMode() {
            return IConfigValue.of(TEXTURE,TEXTURE::set);
        }

        @Override
        public IConfigValue<Boolean> screenDebugging() {
            return convert(ENABLE_DEBUG);
        }

        @Override
        public IConfigValue<Integer> maxPageBarCount(){
            return convert(MAX_PAGE_BARS);
        }

        @Override
        public Set<String> hidingPageIds() {
            return PAGE2HIDING.entrySet().stream()
                    .filter(entry->entry.getValue().getAsBoolean())
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
        }

        @Override
        public void setPageHiding(String id, boolean hiding) {
            Optional.ofNullable(PAGE2HIDING.get(id)).ifPresent(v->v.set(hiding));
            CONFIG_SPEC.save();
        }

        @Override
        public void save() {
            CONFIG_SPEC.save();
        }
    };
}
