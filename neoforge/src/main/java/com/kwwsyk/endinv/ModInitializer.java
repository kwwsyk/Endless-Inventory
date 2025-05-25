package com.kwwsyk.endinv;

import com.kwwsyk.endinv.client.config.ClientConfig;
import com.kwwsyk.endinv.item.ScreenDebugger;
import com.kwwsyk.endinv.item.TestEndInv;
import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.menu.page.ItemDisplay;
import com.kwwsyk.endinv.menu.page.ItemEntryDisplay;
import com.kwwsyk.endinv.menu.page.PageType;
import com.kwwsyk.endinv.menu.page.StarredItemPage;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.options.ItemClassify;
import com.kwwsyk.endinv.options.ServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.registries.*;

import java.util.UUID;
import java.util.function.Supplier;

import static com.kwwsyk.endinv.options.ItemClassify.*;

@Mod(value = ModInitializer.MOD_ID)
public class ModInitializer {
    public static final String MOD_ID = "endless_inventory";
    public static final UUID DEFAULT_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<EndlessInventoryMenu>> ENDLESS_INVENTORY_MENU_TYPE = MENUS.register("endless_inventory",
            ()->new MenuType<>(EndlessInventoryMenu::createClient, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredItem<Item> testEndInv = ITEMS.registerItem("test_endinv", TestEndInv::new);

    public static final DeferredItem<Item> SCREEN_DEBUGGER = ITEMS.registerItem("screen_debugger", ScreenDebugger::new);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,
            MOD_ID);

    public static final Supplier<AttachmentType<UUID>> ENDINV_UUID = ATTACHMENT_TYPES.register(
            "endinv_uuid",
            ()->AttachmentType
                    .builder(()->DEFAULT_UUID)
                    .serialize(UUIDUtil.CODEC)
                    .copyOnDeath()
                    .build()
    );

    public static final ResourceKey<Registry<ItemClassify>> CLASSIFY_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID,"item_classify"));

    public static final ResourceKey<Registry<PageType>> PAGE_REG_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID,"display_page"));

    public static final Registry<ItemClassify> CLASSIFY_REGISTRY =
            new RegistryBuilder<>(CLASSIFY_REGISTRY_KEY)
                    .sync(true)
                    .defaultKey(ResourceLocation.fromNamespaceAndPath(MOD_ID,"all"))
                    .create();

    public static final Registry<PageType> PAGE_REGISTRY =
            new RegistryBuilder<>(PAGE_REG_KEY)
                    .sync(true)
                    .defaultKey(ResourceLocation.fromNamespaceAndPath(MOD_ID,"all_items"))
                    .create();

   public static final DeferredRegister<ItemClassify> CLASSIFIES = DeferredRegister.create(CLASSIFY_REGISTRY,MOD_ID);

   public static final DeferredRegister<PageType> PAGES = DeferredRegister.create(PAGE_REGISTRY,MOD_ID);

    public static final Holder<PageType> ALL_ITEMS = registerPageTypes("all_items", ItemDisplay::new, ALL, "chest");
    public static final Holder<PageType> BLOCK_ITEMS = registerPageTypes("block_items", ItemDisplay::new, BLOCKS, "stone");
    public static final Holder<PageType> WEAPONS = registerPageTypes("weapons", ItemDisplay::new, ItemClassify.WEAPONS, "iron_sword");
    public static final Holder<PageType> TOOLS = registerPageTypes("tools", ItemDisplay::new, ItemClassify.TOOLS, "iron_pickaxe");
    public static final Holder<PageType> EQUIPMENTS = registerPageTypes("equipments", ItemDisplay::new, ItemClassify.EQUIPMENTS, "iron_chestplate");
    public static final Holder<PageType> CONSUMABLE = registerPageTypes("consumable", ItemDisplay::new, FOOD_POTION, "bread");
    public static final Holder<PageType> ENCHANTED_BOOKS = registerPageTypes("enchanted_books", ItemEntryDisplay::new, ENCHANTMENT_BOOKS, "enchanted_book");
    public static final Holder<PageType> VANISHING = registerPageTypes("vanishing_enchantable", ItemDisplay::new, VANISHING_ENCHANTABLE, "diamond_helmet");
    public static final Holder<PageType> BOOKMARK = registerPageTypes("bookmark", StarredItemPage::new, null, "book");

   public static final Supplier<AttachmentType<SyncedConfig>> SYNCED_CONFIG = ATTACHMENT_TYPES.register("endinv_settings",
            ()-> AttachmentType
                    .builder(()-> SyncedConfig.DEFAULT)
                    .serialize(SyncedConfig.CODEC)
                    .copyOnDeath()
                    .build()
    );


    public ModInitializer(IEventBus modEventBus, ModContainer container){
        MENUS.register(modEventBus);
        ITEMS.register(modEventBus);
        CLASSIFIES.register(modEventBus);
        PAGES.register(modEventBus);
        //should be after page reg
        ATTACHMENT_TYPES.register(modEventBus);

        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.CONFIG_SPEC);

        if(FMLEnvironment.dist.isClient())  container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }


    private static Holder<PageType> registerPageTypes(String id, PageType.PageConstructor constructor, @org.jetbrains.annotations.Nullable Holder<ItemClassify> classify, @org.jetbrains.annotations.Nullable String iconLocation){
        var type = PAGES.register(id, ()->new PageType(constructor,classify,iconLocation!=null? ResourceLocation.withDefaultNamespace(iconLocation):null,id));
        PageMetaDataManager.defaultPages.add(type);
        return type;
    }
}
