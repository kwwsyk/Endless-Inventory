package com.kwwsyk.endinv.neoforge;

import com.kwwsyk.endinv.common.AbstractModInitializer;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.NbtAttachment;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.options.IServerConfig;
import com.kwwsyk.endinv.common.util.SortType;
import com.kwwsyk.endinv.neoforge.client.config.ClientConfig;
import com.kwwsyk.endinv.neoforge.options.ServerConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
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
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegistryBuilder;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Mod(value = ModInfo.MOD_ID)
public class ModInitializer extends AbstractModInitializer {


    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ModInfo.MOD_ID);

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ModInfo.MOD_ID);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ModInfo.MOD_ID);

    public ModInitializer(IEventBus modEventBus, ModContainer container){
        super.init();

        MENUS.register(modEventBus);
        ITEMS.register(modEventBus);
        //should be after page reg
        ATTACHMENT_TYPES.register(modEventBus);

        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.CONFIG_SPEC);

        if(FMLEnvironment.dist.isClient())  container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @Override
    protected SortType.ISortHelper loadSortHelper() {
        return () -> Comparator.comparing(
                s -> Optional.ofNullable(s.getItemHolder().getKey())
                        .map(ResourceKey::location)
                        .map(Object::toString)
                        .orElse("~"));
    }

    @Override
    protected IServerConfig loadServerConfig() {
        return ServerConfig.CONFIG.INSTANCE;
    }

    @Override
    protected RegistryCallback<Item> itemReg() {
        return new RegistryCallback<>() {
            @Override
            public <R extends Item> Supplier<R> register(String id, Supplier<R> supplier) {
                return ITEMS.register(id, supplier);
            }
        };
    }

    @Override
    protected RegistryCallback<MenuType<?>> menuReg() {
        return new RegistryCallback<>() {
            @Override
            public <R extends MenuType<?>> Supplier<R> register(String id, Supplier<R> supplier) {
                return MENUS.register(id, supplier);
            }
        };
    }

    @Override
    protected Supplier<MenuType<EndlessInventoryMenu>> createEndInvMenuType() {
        return ()-> new MenuType<>(EndlessInventoryMenu::createClient,FeatureFlags.DEFAULT_FLAGS);
    }

    @Override
    protected NbtAttachment<UUID> createEndInvUUID(String name) {
        Supplier<AttachmentType<UUID>> ENDINV_UUID = ATTACHMENT_TYPES.register(
                "endinv_uuid",
                ()->AttachmentType
                        .builder(()-> ModInfo.DEFAULT_UUID)
                        .serialize(UUIDUtil.CODEC)
                        .copyOnDeath()
                        .build()
        );
        return new NbtAttachment<>() {
            @Override
            @Nullable
            public UUID getWith(Player player) {
                if(!player.hasData(ENDINV_UUID)) return null;
                return player.getData(ENDINV_UUID);
            }

            @Override
            public void setTo(Player player, UUID uuid) {
                player.setData(ENDINV_UUID, uuid);
            }
        };
    }

    @Override
    protected NbtAttachment<SyncedConfig> createSyncedConfig(String name) {
        Supplier<AttachmentType<SyncedConfig>> SYNCED_CONFIG = ATTACHMENT_TYPES.register("endinv_settings",
                ()-> AttachmentType
                        .builder(()-> SyncedConfig.DEFAULT)
                        .serialize(SyncedConfig.CODEC)
                        .copyOnDeath()
                        .build()
        );
        return new NbtAttachment<>() {
            @Override
            @Nullable
            public SyncedConfig getWith(Player player) {
                if (!player.hasData(SYNCED_CONFIG)) return null;
                return player.getData(SYNCED_CONFIG);
            }

            @Override
            public void setTo(Player player, SyncedConfig syncedConfig) {
                player.setData(SYNCED_CONFIG, syncedConfig);
            }
        };
    }

    protected Registry<PageType> createPageRegistry(ResourceKey<Registry<PageType>> pageRegKey, ResourceLocation defaultKey) {
        return new RegistryBuilder<>(pageRegKey).defaultKey(defaultKey).sync(true).create();
    }
}
