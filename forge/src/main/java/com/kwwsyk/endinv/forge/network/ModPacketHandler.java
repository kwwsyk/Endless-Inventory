package com.kwwsyk.endinv.forge.network;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.network.payloads.toClient.*;
import com.kwwsyk.endinv.common.network.payloads.toServer.*;
import com.kwwsyk.endinv.forge.network.payloads.JeiAttachedTransferPayload;
import com.kwwsyk.endinv.forge.network.payloads.JeiTransferRecipePayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModPacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    @SuppressWarnings("removal")
    public static final SimpleChannel INSTANCE = ChannelBuilder
            .named(new ResourceLocation(ModInfo.MOD_ID, "main"))
            .networkProtocolVersion(1)
            .acceptedVersions(Channel.VersionTest.exact(1))
            .simpleChannel();

    public static <MSG> BiConsumer<MSG, CustomPayloadEvent.Context> convert(BiConsumer<MSG, ModPacketContext> handler){
        return (msg, sup)-> {
            sup.enqueueWork(() -> {
                // Work that needs to be thread-safe (most work)
                // Do stuff
                handler.accept(msg,()->sup.getSender());
            });
            sup.setPacketHandled(true);
        };
    }

    public static <MSG> BiConsumer<MSG, CustomPayloadEvent.Context> convertClient(BiConsumer<MSG, ModPacketContext> handler){
        return (msg, sup)-> {
            sup.enqueueWork(() -> {
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handler.accept(msg,()->sup.getSender()));

            });
            sup.setPacketHandled(true);
        };
    }

    public static <MSG> BiConsumer<MSG, CustomPayloadEvent.Context> convertBi(BiConsumer<MSG, ModPacketContext> handler){
        return (msg, cxt)-> {
            cxt.enqueueWork(() -> {
                ServerPlayer sender;
                if((sender=cxt.getSender())!=null){
                    handler.accept(msg,()->sender);
                }else {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handler.accept(msg, cxt::getSender));
                }
            });
            cxt.setPacketHandled(true);
        };
    }

    private static void register(){
        int i=0;
        INSTANCE.messageBuilder(EndInvContent.class, i++)
                .encoder(EndInvContent::encode).decoder(EndInvContent::decode)
                .consumerNetworkThread(convertClient(EndInvContent::handle)).add();
        INSTANCE.messageBuilder(EndInvMetadata.class, i++)
                .encoder(EndInvMetadata::encode).decoder(EndInvMetadata::decode)
                .consumerNetworkThread(convertClient(EndInvMetadata::handle)).add();
        INSTANCE.messageBuilder(ItemPickedUpPayload.class, i++)
                .encoder(ItemPickedUpPayload::encode).decoder(ItemPickedUpPayload::decode)
                .consumerNetworkThread(convertClient(ItemPickedUpPayload::handle)).add();
        INSTANCE.messageBuilder(SetItemDisplayContentPayload.class, i++)
                .encoder(SetItemDisplayContentPayload::encode).decoder(SetItemDisplayContentPayload::decode)
                .consumerNetworkThread(convertClient(SetItemDisplayContentPayload::handle)).add();
        INSTANCE.messageBuilder(SetStarredPagePayload.class, i++)
                .encoder(SetStarredPagePayload::encode).decoder(SetStarredPagePayload::decode)
                .consumerNetworkThread(convertClient(SetStarredPagePayload::handle)).add();
        INSTANCE.messageBuilder(MenuAttachabilityPayload.class, i++)
                .encoder(MenuAttachabilityPayload::encode).decoder(MenuAttachabilityPayload::decode)
                .consumerNetworkThread(convertClient(MenuAttachabilityPayload::handle)).add();

        INSTANCE.messageBuilder(ItemClickPayload.class, i++)
                .encoder(ItemClickPayload::encode).decoder(ItemClickPayload::decode)
                .consumerNetworkThread(convert(ItemClickPayload::handle)).add();
        INSTANCE.messageBuilder(BulkQuickMoveFromPagePayload.class, i++)
                .encoder(BulkQuickMoveFromPagePayload::encode).decoder(BulkQuickMoveFromPagePayload::decode)
                .consumerNetworkThread(convert(BulkQuickMoveFromPagePayload::handle)).add();
        INSTANCE.messageBuilder(CreativeItemModPayload.class, i++)
                .encoder(CreativeItemModPayload::encode).decoder(CreativeItemModPayload::decode)
                .consumerNetworkThread(convert(CreativeItemModPayload::handle)).add();
        INSTANCE.messageBuilder(ItemPageContext.class, i++)
                .encoder(ItemPageContext::encode).decoder(ItemPageContext::decode)
                .consumerNetworkThread(convert(ItemPageContext::handle)).add();
        INSTANCE.messageBuilder(OpenEndInvPayload.class, i++)
                .encoder(OpenEndInvPayload::encode).decoder(OpenEndInvPayload::decode)
                .consumerNetworkThread(convert(OpenEndInvPayload::handle)).add();
        INSTANCE.messageBuilder(QuickMoveToPagePayload.class, i++)
                .encoder(QuickMoveToPagePayload::encode).decoder(QuickMoveToPagePayload::decode)
                .consumerNetworkThread(convert(QuickMoveToPagePayload::handle)).add();
        INSTANCE.messageBuilder(StarItemPayload.class, i++)
                .encoder(StarItemPayload::encode).decoder(StarItemPayload::decode)
                .consumerNetworkThread(convert(StarItemPayload::handle)).add();
        INSTANCE.messageBuilder(ToggleCraftingPayload.class, i++)
                .encoder(ToggleCraftingPayload::encode).decoder(ToggleCraftingPayload::decode)
                .consumerNetworkThread(convert(ToggleCraftingPayload::handle)).add();
        if(ModList.get().isLoaded("jei")) {
            INSTANCE.messageBuilder(JeiTransferRecipePayload.class, i++)
                    .encoder(JeiTransferRecipePayload::encode).decoder(JeiTransferRecipePayload::decode)
                    .consumerNetworkThread(convert(JeiTransferRecipePayload::handle)).add();
            INSTANCE.messageBuilder(JeiAttachedTransferPayload.class, i++)
                    .encoder(JeiAttachedTransferPayload::encode).decoder(JeiAttachedTransferPayload::decode)
                    .consumerNetworkThread(convert(JeiAttachedTransferPayload::handle)).add();
        }

        INSTANCE.messageBuilder(SyncedConfig.class, i)
                .encoder(SyncedConfig::encode).decoder(SyncedConfig::decode)
                .consumerNetworkThread(convertBi(SyncedConfig::handle)).add();
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // register channel during common setup
        event.enqueueWork(ModPacketHandler::register);
    }
}