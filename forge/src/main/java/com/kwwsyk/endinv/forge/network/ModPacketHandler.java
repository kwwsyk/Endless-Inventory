package com.kwwsyk.endinv.forge.network;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.network.payloads.toClient.*;
import com.kwwsyk.endinv.common.network.payloads.toServer.*;
import com.kwwsyk.endinv.forge.network.payloads.JeiTransferRecipePayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModPacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    @SuppressWarnings("removal")
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ModInfo.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static <MSG> BiConsumer<MSG, Supplier<NetworkEvent.Context>> convert(BiConsumer<MSG, ModPacketContext> handler){
        return (msg,sup)-> {
            sup.get().enqueueWork(() -> {
                // Work that needs to be thread-safe (most work)
                // Do stuff
                handler.accept(msg,()->sup.get().getSender());
            });
            sup.get().setPacketHandled(true);
        };
    }

    public static <MSG> BiConsumer<MSG, Supplier<NetworkEvent.Context>> convertClient(BiConsumer<MSG, ModPacketContext> handler){
        return (msg,sup)-> {
            sup.get().enqueueWork(() -> {
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handler.accept(msg,()->sup.get().getSender()));

            });
            sup.get().setPacketHandled(true);
        };
    }

    public static <MSG> BiConsumer<MSG, Supplier<NetworkEvent.Context>> convertBi(BiConsumer<MSG, ModPacketContext> handler){
        return (msg,sup)-> {
            sup.get().enqueueWork(() -> {
                var cxt = sup.get();
                ServerPlayer sender;
                if((sender=cxt.getSender())!=null){
                    handler.accept(msg,()->sender);
                }else {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handler.accept(msg,()->sup.get().getSender()));
                }
            });
            sup.get().setPacketHandled(true);
        };
    }

    private static void register(){
        int i=0;
        INSTANCE.registerMessage(i++, EndInvContent.class,EndInvContent::encode,EndInvContent::decode,convertClient(EndInvContent::handle));
        INSTANCE.registerMessage(i++, EndInvMetadata.class,EndInvMetadata::encode,EndInvMetadata::decode,convertClient(EndInvMetadata::handle));
        INSTANCE.registerMessage(i++, ItemPickedUpPayload.class,ItemPickedUpPayload::encode,ItemPickedUpPayload::decode,convertClient(ItemPickedUpPayload::handle));
        INSTANCE.registerMessage(i++, SetItemDisplayContentPayload.class,SetItemDisplayContentPayload::encode,SetItemDisplayContentPayload::decode,convertClient(SetItemDisplayContentPayload::handle));
        INSTANCE.registerMessage(i++, SetStarredPagePayload.class,SetStarredPagePayload::encode,SetStarredPagePayload::decode,convertClient(SetStarredPagePayload::handle));

        INSTANCE.registerMessage(i++, ItemClickPayload.class,ItemClickPayload::encode,ItemClickPayload::decode,convert(ItemClickPayload::handle));
        INSTANCE.registerMessage(i++, CreativeItemModPayload.class, CreativeItemModPayload::encode, CreativeItemModPayload::decode,convert(CreativeItemModPayload::handle));
        INSTANCE.registerMessage(i++, ItemPageContext.class,ItemPageContext::encode,ItemPageContext::decode,convert(ItemPageContext::handle));
        INSTANCE.registerMessage(i++, OpenEndInvPayload.class,OpenEndInvPayload::encode,OpenEndInvPayload::decode,convert(OpenEndInvPayload::handle));
        INSTANCE.registerMessage(i++, QuickMoveToPagePayload.class,QuickMoveToPagePayload::encode,QuickMoveToPagePayload::decode,convert(QuickMoveToPagePayload::handle));
        INSTANCE.registerMessage(i++, StarItemPayload.class,StarItemPayload::encode,StarItemPayload::decode,convert(StarItemPayload::handle));
        INSTANCE.registerMessage(i++, ToggleCraftingPayload.class, ToggleCraftingPayload::encode, ToggleCraftingPayload::decode, convert(ToggleCraftingPayload::handle));
        INSTANCE.registerMessage(i++, JeiTransferRecipePayload.class, JeiTransferRecipePayload::encode, JeiTransferRecipePayload::decode, convert(JeiTransferRecipePayload::handle));

        INSTANCE.registerMessage(i, SyncedConfig.class,SyncedConfig::encode,SyncedConfig::decode,convertBi(SyncedConfig::handle));
    }

    /*static {
        register();
    }*/

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // register channel during common setup
        event.enqueueWork(ModPacketHandler::register);
    }
}

