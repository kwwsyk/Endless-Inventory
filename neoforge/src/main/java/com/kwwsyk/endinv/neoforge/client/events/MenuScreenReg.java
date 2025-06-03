package com.kwwsyk.endinv.neoforge.client.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD,modid = ModInfo.MOD_ID,value = Dist.CLIENT)
public class MenuScreenReg {
    @SubscribeEvent
    public static void reg(RegisterMenuScreensEvent event){
        event.register(ModRegistries.Menus.getEndInvMenuType(), EndlessInventoryScreen::new);
    }
}
