package com.kwwsyk.endinv.neoforge.client.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.option.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = ModInfo.MOD_ID,value = Dist.CLIENT)
public class ScreenDebug {

    public static int phase = 0;
    private static boolean hideMenu = false;

    @SubscribeEvent
    public static void hideScreen(ScreenEvent.Render.Pre event){
        if(hideMenu){
            GuiGraphicsExtractor graphics = event.getGuiGraphics();
            Screen screen = event.getScreen();
            Minecraft mc = screen.getMinecraft();
            graphics.text(mc.font,Component.literal("[F4]Menu screen rendering stopped!"),0,0,0xFFFFAA00);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderScreen(ScreenEvent.Render.Post event){
        if(!ClientConfigs.SCREEN_DEBUG.get()) return;
        Screen screen = event.getScreen();
        com.kwwsyk.endinv.common.client.ScreenDebug.debugInfo(screen,event.getGuiGraphics(),event.getMouseX(),event.getMouseY());
    }

    @SubscribeEvent
    public static void click(ScreenEvent.KeyPressed.Post event){
        com.kwwsyk.endinv.common.client.ScreenDebug.click(event.getKey(),event.getScreen());
    }
}
