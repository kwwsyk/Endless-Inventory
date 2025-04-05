package com.kwwsyk.endinv.events;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.network.ClientPayloadHandler;
import com.kwwsyk.endinv.network.ServerPayloadHandler;
import com.kwwsyk.endinv.network.payloads.EndInvRequestContentPayload;
import com.kwwsyk.endinv.network.payloads.EndInvSettings;
import com.kwwsyk.endinv.network.payloads.SetItemDisplayContentPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ModInitializer.MOD_ID,bus = EventBusSubscriber.Bus.MOD)
public class Reg {
    @SubscribeEvent
    public static void registerPayload(final RegisterPayloadHandlersEvent event){
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playBidirectional(
                EndInvSettings.TYPE,
                EndInvSettings.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler::handleEndInvSettings,
                        ServerPayloadHandler::handleEndInvSettings
                )
        );
        registrar.playToServer(
                EndInvRequestContentPayload.TYPE,
                EndInvRequestContentPayload.STREAM_CODEC,
                ServerPayloadHandler::handleEndInvRequests
        );
        registrar.playToClient(
                SetItemDisplayContentPayload.TYPE,
                SetItemDisplayContentPayload.STREAM_CODEC,
                ClientPayloadHandler::handleItemDisplay
        );
    }
}
