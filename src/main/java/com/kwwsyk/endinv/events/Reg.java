package com.kwwsyk.endinv.events;

import com.kwwsyk.endinv.ModInitializer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;

@EventBusSubscriber(modid = ModInitializer.MOD_ID,bus = EventBusSubscriber.Bus.MOD)
public class Reg {

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event){
        event.register(ModInitializer.CLASSIFY_REGISTRY);
    }
}
