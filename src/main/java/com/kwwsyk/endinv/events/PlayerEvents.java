package com.kwwsyk.endinv.events;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.network.payloads.EndInvSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.kwwsyk.endinv.ModInitializer.ENDINV_SETTINGS;

@EventBusSubscriber(modid = ModInitializer.MOD_ID)
public class PlayerEvents {

    public static boolean tickRefresh =true;
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer serverPlayer){
            if(tickRefresh) {
                PacketDistributor.sendToPlayer(serverPlayer, new EndInvSettings(serverPlayer.getData(ENDINV_SETTINGS).rows()));
                tickRefresh = false ;
            }
        }
    }

    @SubscribeEvent
    public static void onRespawnClone(PlayerEvent.Clone event){
        tickRefresh=true;

    }
}
