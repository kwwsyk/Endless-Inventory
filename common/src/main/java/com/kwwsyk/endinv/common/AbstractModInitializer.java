package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.item.ScreenDebugger;
import com.kwwsyk.endinv.common.item.TestEndInv;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.menu.page.ItemDisplay;
import com.kwwsyk.endinv.common.menu.page.ItemEntryDisplay;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.menu.page.StarredItemPage;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.options.ItemClassify;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;

import java.util.UUID;
import java.util.function.Supplier;

import static com.kwwsyk.endinv.common.options.ItemClassify.*;

public abstract class AbstractModInitializer {

    public static final String MOD_ID = "endless_inventory";

    public static final UUID DEFAULT_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

    @FunctionalInterface
    public interface RegistryCallback {
        <T> Supplier<T> register(String id, Supplier<T> supplier);
    }

    public static ResourceLocation withModLocation(String id){
        return ResourceLocation.fromNamespaceAndPath(MOD_ID,id);
    }

    protected AbstractModInitializer(){}

    protected void registerItems(RegistryCallback method){
        ModRegistries.Items.testEndInv = method.register("endinv_accessor",()->new TestEndInv(new Item.Properties()));
        ModRegistries.Items.screenDebugger = method.register("screen_debugger",()->new ScreenDebugger(new Item.Properties()));
    }

    protected void registerMenuType(RegistryCallback method){
        ModRegistries.Menus.endinvMenuType = method.register("endinv_menu",createEndInvMenuType());
    }

    protected abstract Supplier<MenuType<EndlessInventoryMenu>> createEndInvMenuType();


    public static final Supplier<AttachmentType<UUID>> ENDINV_UUID = ATTACHMENT_TYPES.register(
            "endinv_uuid",
            ()->AttachmentType
                    .builder(()->DEFAULT_UUID)
                    .serialize(UUIDUtil.CODEC)
                    .copyOnDeath()
                    .build()
    );

    public static final ResourceKey<Registry<ItemClassify>> CLASSIFY_REGISTRY_KEY =
            ResourceKey.createRegistryKey(withModLocation("item_classify"));

    public static final ResourceKey<Registry<PageType>> PAGE_REG_KEY =
            ResourceKey.createRegistryKey(withModLocation("display_page"));

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

    private static Holder<PageType> registerPageTypes(String id, PageType.PageConstructor constructor, @org.jetbrains.annotations.Nullable Holder<ItemClassify> classify, @org.jetbrains.annotations.Nullable String iconLocation){
        var type = PAGES.register(id, ()->new PageType(constructor,classify,iconLocation!=null? ResourceLocation.withDefaultNamespace(iconLocation):null,id));
        PageMetaDataManager.defaultPages.add(type);
        return type;
    }
}
