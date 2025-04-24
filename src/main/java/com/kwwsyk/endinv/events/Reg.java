package com.kwwsyk.endinv.events;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.network.ClientPayloadHandler;
import com.kwwsyk.endinv.network.ServerPayloadHandler;
import com.kwwsyk.endinv.network.payloads.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.NewRegistryEvent;

@EventBusSubscriber(modid = ModInitializer.MOD_ID,bus = EventBusSubscriber.Bus.MOD)
public class Reg {
    @SubscribeEvent
    public static void registerPayload(final RegisterPayloadHandlersEvent event){
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playBidirectional(
                SyncedConfig.TYPE,
                SyncedConfig.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler::handleEndInvSettings,
                        ServerPayloadHandler::handleEndInvSettings
                )
        );
        registrar.playToServer(
                PageMetadata.TYPE,
                PageMetadata.STREAM_CODEC,
                ServerPayloadHandler::handleMenuPage
        );
        registrar.playToClient(
                SetItemDisplayContentPayload.TYPE,
                SetItemDisplayContentPayload.STREAM_CODEC,
                ClientPayloadHandler::handleItemDisplay
        );
        registrar.playToServer(
                PageClickPayload.TYPE,
                PageClickPayload.STREAM_CODEC,
                ServerPayloadHandler::handlePageClick
        );
        registrar.playToServer(
                PageStatePayload.TYPE,
                PageStatePayload.STREAM_CODEC,
                ServerPayloadHandler::handlePageStates
        );
        registrar.playToClient(
                EndInvMetadata.TYPE,
                EndInvMetadata.STREAM_CODEC,
                ClientPayloadHandler::handleEndInvMetaData
        );
        registrar.playToServer(
                OpenEndInvPayload.TYPE,
                OpenEndInvPayload.STREAM_CODEC,
                ServerPayloadHandler::handleEndInvOpening
        );
        registrar.playToServer(
                ItemDisplayItemModPayload.TYPE,
                ItemDisplayItemModPayload.STREAM_CODEC,
                ServerPayloadHandler::handleItemDisplayItemMod
        );
    }
    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event){
        event.register(ModInitializer.CLASSIFY_REGISTRY);
    }
}
