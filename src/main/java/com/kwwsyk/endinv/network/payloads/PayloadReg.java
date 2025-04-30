package com.kwwsyk.endinv.network.payloads;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.network.ClientPayloadHandler;
import com.kwwsyk.endinv.network.ServerPayloadHandler;
import com.kwwsyk.endinv.network.payloads.toClient.EndInvMetadata;
import com.kwwsyk.endinv.network.payloads.toClient.ItemPickedUpPayload;
import com.kwwsyk.endinv.network.payloads.toClient.SetItemDisplayContentPayload;
import com.kwwsyk.endinv.network.payloads.toClient.SetStarredPagePayload;
import com.kwwsyk.endinv.network.payloads.toServer.OpenEndInvPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.PageContext;
import com.kwwsyk.endinv.network.payloads.toServer.page.StarItemPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.op.ItemDisplayItemModPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.op.PageClickPayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.op.PageStatePayload;
import com.kwwsyk.endinv.network.payloads.toServer.page.op.QuickMoveToPagePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ModInitializer.MOD_ID,bus = EventBusSubscriber.Bus.MOD)
public class PayloadReg {
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
                PageContext.TYPE,
                PageContext.STREAM_CODEC,
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
        registrar.playToClient(
                ItemPickedUpPayload.TYPE,
                ItemPickedUpPayload.STREAM_CODEC,
                ClientPayloadHandler::handleAutoPick
        );
        registrar.playToServer(
                StarItemPayload.TYPE,
                StarItemPayload.STREAM_CODEC,
                ServerPayloadHandler::handleItemStarred
        );
        registrar.playToServer(
                QuickMoveToPagePayload.TYPE,
                QuickMoveToPagePayload.STREAM_CODEC,
                ServerPayloadHandler::handleQuickMovePage
        );
        registrar.playToClient(
                SetStarredPagePayload.TYPE,
                SetStarredPagePayload.STREAM_CODEC,
                ClientPayloadHandler::handleStarredItems
        );
    }
}
