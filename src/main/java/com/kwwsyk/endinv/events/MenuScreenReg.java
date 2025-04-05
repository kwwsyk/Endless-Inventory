package com.kwwsyk.endinv.events;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.client.gui.EndlessInventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import static com.kwwsyk.endinv.ModInitializer.ENDLESS_INVENTORY_MENU_TYPE;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD,modid = ModInitializer.MOD_ID,value = Dist.CLIENT)
public class MenuScreenReg {
    @SubscribeEvent
    public static void reg(RegisterMenuScreensEvent event){
        event.register(ENDLESS_INVENTORY_MENU_TYPE.get(), EndlessInventoryScreen::new);
    }
}
