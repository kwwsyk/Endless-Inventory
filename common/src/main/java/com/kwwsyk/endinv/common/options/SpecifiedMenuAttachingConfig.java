package com.kwwsyk.endinv.common.options;

import com.kwwsyk.endinv.common.options.config.ComplexConfigEntryImpl;
import com.kwwsyk.endinv.common.options.config.ConfigEntryImpl;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

/**A menu attachability config of specified menu types with better priority
 *
 */
public class SpecifiedMenuAttachingConfig{

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<Map<MenuType<?>,Boolean>> MAP_CODEC = Codec.unboundedMap(
            BuiltInRegistries.MENU.byNameCodec(), Codec.BOOL
    );
    public static final SpecifiedMenuAttachingConfig DEFAULT = new SpecifiedMenuAttachingConfig();
    public static final Codec<SpecifiedMenuAttachingConfig> CODEC = RecordCodecBuilder.create(instance->instance.group(
            Codec.BOOL.fieldOf("inventory").forGetter(SpecifiedMenuAttachingConfig::isInventoryAttachable),
            MAP_CODEC.fieldOf("menu_configs").forGetter(SpecifiedMenuAttachingConfig::getConfigs)
    ).apply(instance, SpecifiedMenuAttachingConfig::new));

    final Map<MenuType<?>, Boolean> configs = new HashMap<>();
    private boolean inventoryAttachable;

    public SpecifiedMenuAttachingConfig(boolean inventoryAttachable, Map<MenuType<?>, Boolean> configs){
        this.configs.putAll(configs);
        this.inventoryAttachable = inventoryAttachable;
    }

    public SpecifiedMenuAttachingConfig(){
        this.inventoryAttachable = true;
    }

    public Map<MenuType<?>, Boolean> getConfigs(){
        return configs;
    }

    public boolean isInventoryAttachable(){
        return inventoryAttachable;
    }

    /**Check if a menu can be attached by {@link com.kwwsyk.endinv.common.client.gui.AttachingScreen}<br>
     * It checks {@link #getMenuConfig} also checks {@link ServerConfigs#DEFAULT_ATTACH}<br>
     * <p>
     *     Whether a menu can be attached depends on:
     *      <table>
     *          <thead>
     *              <tr>
     *                  <th>{@link ServerConfigs#DEFAULT_ATTACH}</th>
     *                  <th>Value: True</th>
     *                  <th>Value: null</th>
     *                  <th>value: false</th>
     *              </tr>
     *          </thead>
     *          <tbody>
     *              <tr>
     *                  <th>True</th>
     *                  <th>true</th>
     *                  <th>true</th>
     *                  <th>false</th>
     *              </tr>
     *               <tr>
     *                  <th>False</th>
     *                  <th>true</th>
     *                  <th>false</th>
     *                  <th>false</th>
     *              </tr>
     *          </tbody>
     *      </table>
     * </p>
     * @param type the {@link MenuType} the menu holds, for {@link net.minecraft.world.inventory.InventoryMenu} it's null
     * @return whether exceeded inventory screen will attach this menu in game.
     */
    public boolean isMenuAttachable(@Nullable MenuType<?> type){
        if(type==null) return isInventoryAttachable();
        Boolean menuConfig = getMenuConfig(type);
        return menuConfig!=null ? menuConfig : com.kwwsyk.endinv.common.options.ServerConfigs.DEFAULT_ATTACH.get();
    }

    public boolean isMenuAttachable(AbstractContainerMenu menu){
        // On dedicated server, creative inventory picker is represented as InventoryMenu,
        // so the client-only CreativeModeInventoryScreen.ItemPickerMenu class must not be referenced here.
        if(menu instanceof InventoryMenu) return isInventoryAttachable();
        return isMenuAttachable(menu.getType());
    }

    public boolean attachable(Player player){
        return isMenuAttachable(player.containerMenu);
    }

    /**
     * Get the config value of menu type, note that the return value doesn't mean attachable.
     */
    @Nullable
    public Boolean getMenuConfig(@Nullable MenuType<?> type){
        if(type==null) return isInventoryAttachable();
        return configs.get(type);
    }

    public void putConfig(@Nullable MenuType<?> menu, boolean attachable){
        if(menu==null) setInventoryAttachable(attachable);
        configs.put(menu,attachable);
    }

    public void setInventoryAttachable(boolean attachable){
        this.inventoryAttachable = attachable;
    }

    public void clearConfigs(){
        this.configs.clear();
    }

    /**
     * Fill configs with all registered menu types.<br>
     * It also involves inventory menu.
     * @param defaultValue the default boolean value of filled menu type key
     * @param overrideOriginal if true, overwrite existing; otherwise keep
     */
    public void fill(boolean defaultValue, boolean overrideOriginal){//helped with gpt
        if (overrideOriginal) {
            // 覆盖已有键
            configs.replaceAll((k, v) -> defaultValue);
        }
        // 确保所有注册类型都有值
        for (MenuType<?> type : BuiltInRegistries.MENU) {//NOTE: Registry<T> implements Iterable (by impl IdMap)
            configs.putIfAbsent(type, defaultValue);
        }
        if (overrideOriginal) {
            setInventoryAttachable(defaultValue);
        }
    }

    /**
     * Remove entries whose value equals the current global {@code enableAttaching} setting.
     * Effect: trims redundant per-menu configs that match the server default.
     * Complexity: O(n). No ConcurrentModificationException.
     */
    public void trimNonsenseValues() {//doc and self helped with gpt
        final Boolean target = com.kwwsyk.endinv.common.options.ServerConfigs.DEFAULT_ATTACH.get();
        configs.entrySet().removeIf(e -> Objects.equals(e.getValue(), target));
    }

    /**Invert all present values
     */
    public void invertAllPresent() {
        configs.replaceAll((k, v) -> v == null ? null : !v);
    }

    /** Helper to parse String presented config entry of menu attaching config
     *
     */
    public static class Parser{

        public static final String[] configComments = new String[]{
                "The list holds menu attachability configs",
                "The format is \"namespace:id\"",
                "Such as \"endless_inventory:endinv_menu:false\" (Meanwhile, notice that endinv_menu will never be attached)",
                "Use \"inventory:true(false)\" for inventory menu."
        };

        private final SpecifiedMenuAttachingConfig config;

        public Parser(){
            this.config = new SpecifiedMenuAttachingConfig();
        }

        public Parser(SpecifiedMenuAttachingConfig config){
            this.config = config;
        }

        public static SpecifiedMenuAttachingConfig readList(List<String> list){
            var parser = new Parser();
            for(String s : list){
                parser.parseString(s);
            }
            return parser.config;
        }

        public List<String> encodeConfig(){
            return fromMap(config.configs);
        }

        public static List<String> fromMap(Map<MenuType<?>, Boolean> map) {
            List<String> out = new ArrayList<>(map.size());
            for (var e : map.entrySet()) {
                Identifier id = BuiltInRegistries.MENU.getKey(e.getKey());
                if (id != null && e.getValue() != null) out.add(id + ":" + e.getValue());
            }
            out.sort(Comparator.naturalOrder());
            return out;
        }

        public boolean parseString(String configEntry){
            var entry = tryParseString(configEntry);
            if(!entry.success) {
                LOGGER.warn("Menu attachability config parser failed to parse string entry {}.", configEntry);
                return false;
            }
            config.putConfig(entry.type,entry.value);
            return true;
        }

        public static ParseStringResult tryParseString(String configEntry){
            try{
                int LCIndex = lastColon(configEntry);

                String boolTxt = configEntry.substring(LCIndex+1).trim();
                String namespaceTxt = configEntry.substring(0,LCIndex).trim();

                boolean value = parseBoolean(boolTxt);
                Identifier rl = Identifier.parse(namespaceTxt);

                if(rl.getPath().equals("inventory") || rl.getPath().equals("inventory_menu")){
                    return new ParseStringResult(true, null, value);
                }

                var type = BuiltInRegistries.MENU.get(rl).map(Holder::value).orElse(null);
                if(type==null) return ParseStringResult.FAILED;

                return new ParseStringResult(true, type, value);
            }catch (Exception e){
                return ParseStringResult.FAILED;
            }
        }

        private static int lastColon(String s) { return s != null ? s.lastIndexOf(':') : -1; }

        private static boolean parseBoolean(String s){// s has been trimmed
            if(s.equals("1")) return true;
            return Boolean.parseBoolean(s);
        }

        public SpecifiedMenuAttachingConfig getConfig() {
            return config;
        }

        public static void testConfig(){
            var a = com.kwwsyk.endinv.common.options.ServerConfigs.SPECIFIED_ATTACHABILITY;
            var b = a.get();
            b.fill(true,false);
            a.set(b);
        }

        /**
         * @param success is the parse process successful
         * @param type menu type, null for inventory menu
         * @param value such menu type attachable
         */
         public record ParseStringResult(boolean success, @Nullable MenuType<?> type, boolean value){

             public static final ParseStringResult FAILED = new ParseStringResult(false,null,false);
        }
    }

    public static class Entry extends ComplexConfigEntryImpl<SpecifiedMenuAttachingConfig> {

        public static final String[] COMMENTS = Parser.configComments;
        public static final Entry INSTANCE = new Entry("specifiedMenuAttachability",new String[]{},SpecifiedMenuAttachingConfig.DEFAULT);

        private final ListEntry<String> menuKey2bools = new ListEntry<>("container2attachable",COMMENTS,new ArrayList<>());

        public Entry(String key, String[] comments, SpecifiedMenuAttachingConfig defaultValue) {
            super(key, comments, defaultValue);
        }

        @Override
        public ConfigEntryImpl<?>[] fields() {
            return new ConfigEntryImpl[]{menuKey2bools};
        }

        @Override
        public SpecifiedMenuAttachingConfig get() {
            return Parser.readList(menuKey2bools.get());
        }

        @Override
        public void set(SpecifiedMenuAttachingConfig config) {
            menuKey2bools.set(new Parser(config).encodeConfig());
            //save();
        }
    }
}
