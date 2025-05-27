package com.kwwsyk.endinv.neoforge.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.neoforge.ModInitializer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;

@EventBusSubscriber(modid = ModInfo.MOD_ID,bus = EventBusSubscriber.Bus.MOD)
public class Reg {

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event){
        event.register(ModInitializer.CLASSIFY_REGISTRY);
        event.register(ModInitializer.PAGE_REGISTRY);
    }
}
