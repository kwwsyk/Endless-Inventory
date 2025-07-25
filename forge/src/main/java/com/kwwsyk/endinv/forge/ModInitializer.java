package com.kwwsyk.endinv.forge;

import com.kwwsyk.endinv.common.AbstractModInitializer;
import com.kwwsyk.endinv.common.IPlatform;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.NbtAttachment;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.network.IPacketDistributor;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.options.IServerConfig;
import com.kwwsyk.endinv.forge.nbtAttcachment.AttachingCapabilities;
import com.kwwsyk.endinv.forge.nbtAttcachment.EndInvUuid;
import com.kwwsyk.endinv.forge.network.ModPacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

@Mod(ModInfo.MOD_ID)
public class ModInitializer extends AbstractModInitializer {

    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(ForgeRegistries.ITEMS,ModInfo.MOD_ID);

    public static final DeferredRegister<MenuType<?>> MENU = DeferredRegister.create(ForgeRegistries.MENU_TYPES,ModInfo.MOD_ID);

    public ModInitializer(IEventBus modEventBus, ModContainer container){
        super.init();


    }

    @Override
    protected IPlatform loadOtherPlatformSpecific() {
        return (clcItem,crrItem,slot,action,player,access)-> ForgeHooks.onItemStackedOn(clcItem,clcItem,slot,action,player,access);
    }

    @Override
    protected IPacketDistributor loadPacketDistributor() {
        return new IPacketDistributor() {
            @Override
            public void sendToServer(ModPacketPayload payload) {
                ModPacketHandler.INSTANCE.sendToServer(payload);
            }

            @Override
            public void sendToPlayer(ServerPlayer player, ModPacketPayload payload) {
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(()->player),payload);
            }
        };
    }

    @Override
    protected IServerConfig loadServerConfig() {
        return ServerConfig.CONFIG.INSTANCE;
    }

    @Override
    protected RegistryCallback<Item> itemReg() {
        return new RegistryCallback<Item>() {
            @Override
            public <R extends Item> Supplier<R> register(String id, Supplier<R> supplier) {
                return ITEM.register(id,supplier);
            }
        };
    }

    @Override
    protected RegistryCallback<MenuType<?>> menuReg() {
        return new RegistryCallback<>() {
            @Override
            public <R extends MenuType<?>> Supplier<R> register(String id, Supplier<R> supplier) {
                return MENU.register(id, supplier);
            }
        };
    }

    @Override
    protected Supplier<MenuType<EndlessInventoryMenu>> createEndInvMenuType() {
        return ()-> new MenuType<>(EndlessInventoryMenu::createClient, FeatureFlags.DEFAULT_FLAGS);
    }

    @Override
    protected NbtAttachment<UUID> createEndInvUUID(String name) {
        return new NbtAttachment<UUID>() {
            @Nullable
            @Override
            public UUID getWith(Player player) {
                var opt = player.getCapability(AttachingCapabilities.END_INV_UUID).resolve();
                return opt.map(EndInvUuid::getUuid).orElse(null);
            }

            @Override
            public void setTo(Player player, UUID uuid) {
                var opt = player.getCapability(AttachingCapabilities.END_INV_UUID).resolve();
                opt.ifPresent(endInvUuid -> endInvUuid.setUuid(uuid));
            }

            @Override
            public UUID computeIfAbsent(Player player) {
                return player.getCapability(AttachingCapabilities.END_INV_UUID).resolve().map(EndInvUuid::getUuid).orElse(ModInfo.DEFAULT_UUID);
            }
        };
    }

    @Override
    protected NbtAttachment<SyncedConfig> createSyncedConfig(String name) {
        return null;
    }
}
