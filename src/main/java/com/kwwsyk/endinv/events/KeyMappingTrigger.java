package com.kwwsyk.endinv.events;

import com.kwwsyk.endinv.ModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.kwwsyk.endinv.ModInitializer.OPEN_ENDINV_KEY;
import static com.kwwsyk.endinv.ModInitializer.SYNCED_CONFIG;

@EventBusSubscriber(value = Dist.CLIENT,bus = EventBusSubscriber.Bus.GAME,modid = ModInitializer.MOD_ID)
public class KeyMappingTrigger {

    @SubscribeEvent
    public static void keyPressed(ClientTickEvent.Post event){
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        while (OPEN_ENDINV_KEY.consumeClick()) {
            PacketDistributor.sendToServer(player.getData(SYNCED_CONFIG));

        }
    }
}
