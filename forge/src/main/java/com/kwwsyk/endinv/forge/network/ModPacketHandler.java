package com.kwwsyk.endinv.forge.network;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.network.payloads.toClient.*;
import com.kwwsyk.endinv.common.network.payloads.toServer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ModPacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ModInfo.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static <MSG> BiConsumer<MSG, Supplier<NetworkEvent.Context>> convert(BiConsumer<MSG, ModPacketContext> handler){
        return (msg,sup)-> handler.accept(msg, () -> sup.get().getSender());
    }

    private static void register(){
        int i=0;
        INSTANCE.registerMessage(i++, EndInvContent.class,EndInvContent::encode,EndInvContent::decode,convert(EndInvContent::handle));
        INSTANCE.registerMessage(i++, EndInvMetadata.class,EndInvMetadata::encode,EndInvMetadata::decode,convert(EndInvMetadata::handle));
        INSTANCE.registerMessage(i++, ItemPickedUpPayload.class,ItemPickedUpPayload::encode,ItemPickedUpPayload::decode,convert(ItemPickedUpPayload::handle));
        INSTANCE.registerMessage(i++, SetItemDisplayContentPayload.class,SetItemDisplayContentPayload::encode,SetItemDisplayContentPayload::decode,convert(SetItemDisplayContentPayload::handle));
        INSTANCE.registerMessage(i++, SetStarredPagePayload.class,SetStarredPagePayload::encode,SetStarredPagePayload::decode,convert(SetStarredPagePayload::handle));

        INSTANCE.registerMessage(i++, ItemClickPayload.class,ItemClickPayload::encode,ItemClickPayload::decode,convert(ItemClickPayload::handle));
        INSTANCE.registerMessage(i++, ItemDisplayItemModPayload.class,ItemDisplayItemModPayload::encode,ItemDisplayItemModPayload::decode,convert(ItemDisplayItemModPayload::handle));
        INSTANCE.registerMessage(i++, ItemPageContext.class,ItemPageContext::encode,ItemPageContext::decode,convert(ItemPageContext::handle));
        INSTANCE.registerMessage(i++, OpenEndInvPayload.class,OpenEndInvPayload::encode,OpenEndInvPayload::decode,convert(OpenEndInvPayload::handle));
        INSTANCE.registerMessage(i++, QuickMoveToPagePayload.class,QuickMoveToPagePayload::encode,QuickMoveToPagePayload::decode,convert(QuickMoveToPagePayload::handle));
        INSTANCE.registerMessage(i++, StarItemPayload.class,StarItemPayload::encode,StarItemPayload::decode,convert(StarItemPayload::handle));

        INSTANCE.registerMessage(i, SyncedConfig.class,SyncedConfig::encode,SyncedConfig::decode,convert(SyncedConfig::handle));
    }

    static {
        register();
    }
}
