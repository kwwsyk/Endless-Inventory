package com.kwwsyk.endinv;

import com.kwwsyk.endinv.client.config.ClientConfig;
import com.kwwsyk.endinv.item.ScreenDebugger;
import com.kwwsyk.endinv.item.TestEndInv;
import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.options.ItemClassify;
import com.kwwsyk.endinv.options.ServerConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
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
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.UUID;
import java.util.function.Supplier;

@Mod(value = ModInitializer.MOD_ID)
public class ModInitializer {
    public static final String MOD_ID = "endless_inventory";
    public static final UUID DEFAULT_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final Supplier<MenuType<EndlessInventoryMenu>> ENDLESS_INVENTORY_MENU_TYPE = MENUS.register("endless_inventory",
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
    public static final Supplier<AttachmentType<SyncedConfig>> SYNCED_CONFIG = ATTACHMENT_TYPES.register("endinv_settings",
            ()-> AttachmentType
                    .builder(()-> SyncedConfig.DEFAULT)
                    .serialize(SyncedConfig.CODEC)
                    .copyOnDeath()
                    .build()
    );
    public static final KeyMapping OPEN_ENDINV_KEY = new KeyMapping(
            "key.endless_inventory.open",         // 绑定的键名（语言文件中提供翻译）
            InputConstants.KEY_I,                 // 默认键位，这里是 "O"
            "key.categories.inventory"            // 键位分类
    );
    public static final ResourceKey<Registry<ItemClassify>> CLASSIFY_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID,"item_classify"));
    public static final Registry<ItemClassify> CLASSIFY_REGISTRY =
            new RegistryBuilder<>(CLASSIFY_REGISTRY_KEY)
                    .sync(true)
                    .defaultKey(ResourceLocation.fromNamespaceAndPath(MOD_ID,"all"))
                    .create();
   public static final DeferredRegister<ItemClassify> CLASSIFIES = DeferredRegister.create(CLASSIFY_REGISTRY,MOD_ID);


    public ModInitializer(IEventBus modEventBus, ModContainer container){
        MENUS.register(modEventBus);
        ITEMS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
        CLASSIFIES.register(modEventBus);

        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.CONFIG_SPEC);
    }


}
