package com.kwwsyk.endinv.neoforge.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.network.payloads.toClient.*;
import com.kwwsyk.endinv.common.network.payloads.toServer.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static com.kwwsyk.endinv.common.AbstractModInitializer.withModLocation;

@EventBusSubscriber(modid = ModInfo.MOD_ID,bus = EventBusSubscriber.Bus.MOD)
public class PayloadReg {



    @SubscribeEvent
    public static void registerPayload(final RegisterPayloadHandlersEvent event){
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playBidirectional(
                new CustomPacketPayload.Type<>(withModLocation("endinv_settings")),
                SyncedConfig.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        (pl,cxt)->pl.handleClient(cxt::player),
                        (pl,cxt)->pl.handleServer(cxt::player)
                )
        );
        registrar.playToServer(
                new CustomPacketPayload.Type<>(withModLocation("page_context")),
                PageContext.STREAM_CODEC,
                (pl,cxt)->pl.handle(cxt::player)
        );
        registrar.playToClient(
                new CustomPacketPayload.Type<>(withModLocation("itemdisplay_content")),
                SetItemDisplayContentPayload.STREAM_CODEC,
                (pl,cxt)->pl.handle(cxt::player)
        );
        registrar.playToServer(
                new CustomPacketPayload.Type<>(withModLocation("page_click")),
                PageClickPayload.STREAM_CODEC,
                (pl,cxt)->pl.handle(cxt::player)
        );
        registrar.playToClient(
                new CustomPacketPayload.Type<>(withModLocation("endinv_meta")),
                EndInvMetadata.STREAM_CODEC,
                (pl,cxt)->pl.handle(cxt::player)
        );
        registrar.playToServer(
                new CustomPacketPayload.Type<>(withModLocation("open_endinv")),
                OpenEndInvPayload.STREAM_CODEC,
                (pl,cxt)->pl.handle(cxt::player)
        );
        registrar.playToServer(
                new CustomPacketPayload.Type<>(withModLocation("item_modify")),
                ItemDisplayItemModPayload.STREAM_CODEC,
                (pl,cxt)->pl.handle(cxt::player)
        );
        registrar.playToClient(
                new CustomPacketPayload.Type<>(withModLocation("auto_picked")),
                ItemPickedUpPayload.STREAM_CODEC,
                (pl,cxt)->pl.handle(cxt::player)
        );
        registrar.playToServer(
                new CustomPacketPayload.Type<>(withModLocation("star_item")),
                StarItemPayload.STREAM_CODEC,
                (pl,cxt)->pl.handle(cxt::player)
        );
        registrar.playToServer(
                new CustomPacketPayload.Type<>(withModLocation("quick_move_page")),
                QuickMoveToPagePayload.STREAM_CODEC,
                (pl,cxt)->pl.handle(cxt::player)
        );
        registrar.playToClient(
                new CustomPacketPayload.Type<>(withModLocation("starred_item")),
                SetStarredPagePayload.STREAM_CODEC,
                (pl,cxt)->pl.handle(cxt::player)
        );
        registrar.playToClient(
                new CustomPacketPayload.Type<>(withModLocation("endinv_content")),
                EndInvContent.STREAM_CODEC,
                (pl,cxt)->pl.handle(cxt::player)
        );
    }
}
