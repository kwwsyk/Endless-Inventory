package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.item.ScreenDebugger;
import com.kwwsyk.endinv.common.item.TestEndInv;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.options.IServerConfig;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;

import java.util.UUID;
import java.util.function.Supplier;


public abstract class AbstractModInitializer {

    public static final ResourceKey<Registry<PageType>> PAGE_REG_KEY = ResourceKey.createRegistryKey(withModLocation("display_page"));

    @FunctionalInterface
    public interface RegistryCallback<T> {
        <R extends T> Supplier<R> register(String id, Supplier<R> supplier);
    }

    public static ResourceLocation withModLocation(String id){
        return ResourceLocation.fromNamespaceAndPath(ModInfo.MOD_ID,id);
    }

    protected AbstractModInitializer(){}

    protected void init(){
        registerItems(itemReg());
        registerMenuType(menuReg());
        registerNbtAttachment();
        registerRegistries();
        ModInfo.setServerConfig(loadServerConfig());
        ModInfo.sortHelper = loadSortHelper();
    }

    private void registerItems(RegistryCallback<Item> method){
        ModRegistries.Items.testEndInv = method.register("endinv_accessor",()->new TestEndInv(new Item.Properties()));
        ModRegistries.Items.screenDebugger = method.register("screen_debugger",()->new ScreenDebugger(new Item.Properties()));
    }

    private void registerMenuType(RegistryCallback<MenuType<?>> method){
        ModRegistries.Menus.endinvMenuType = method.register("endinv_menu",createEndInvMenuType());
    }

    private void registerNbtAttachment(){
        ModRegistries.NbtAttachments.endInvUUID = createEndInvUUID("endinv_uuid");
        ModRegistries.NbtAttachments.syncedConfig = createSyncedConfig("endinv_settings");
    }

    private void registerRegistries(){
        var reg2 = createPageRegistry(PAGE_REG_KEY,withModLocation(PageType.DEFAULT_KEY));
        ModRegistries.setPageTypeReg(reg2);
    }

    protected abstract SortType.ISortHelper loadSortHelper();

    protected abstract IServerConfig loadServerConfig();

    protected abstract RegistryCallback<Item> itemReg();

    protected abstract RegistryCallback<MenuType<?>> menuReg();

    protected abstract Supplier<MenuType<EndlessInventoryMenu>> createEndInvMenuType();

    protected abstract NbtAttachment<UUID> createEndInvUUID(String name);

    protected abstract NbtAttachment<SyncedConfig> createSyncedConfig(String name);

    protected abstract Registry<PageType> createPageRegistry(ResourceKey<Registry<PageType>> pageRegKey,ResourceLocation defaultKey);

}
