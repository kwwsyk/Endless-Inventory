package com.kwwsyk.endinv.events;

import com.kwwsyk.endinv.ModInitializer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import static com.kwwsyk.endinv.ModInitializer.OPEN_ENDINV_KEY;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD,value = Dist.CLIENT,modid = ModInitializer.MOD_ID)
public class KeyMappingReg {

    @SubscribeEvent
    public static void registerKey(RegisterKeyMappingsEvent event){

        event.register(OPEN_ENDINV_KEY);
    }
}
