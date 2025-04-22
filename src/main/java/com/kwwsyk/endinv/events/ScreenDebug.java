package com.kwwsyk.endinv.events;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.menu.page.ItemDisplay;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = ModInitializer.MOD_ID,value = Dist.CLIENT)
public class ScreenDebug {

    public static int phase = 0;

    @SubscribeEvent
    public static void renderScreen(ScreenEvent.Render.Post event){
        Screen screen = event.getScreen();
        int width = screen.width;
        int height = screen.height;
        int imageWidth = 0;
        int imageHeight = 0;
        int guiLeft = 0;
        int guiTop = 0;
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        GuiGraphics graphics = event.getGuiGraphics();
        Minecraft mc = screen.getMinecraft();
        if(screen instanceof AbstractContainerScreen<?> ACS){
            imageWidth = ACS.getXSize();
            imageHeight = ACS.getYSize();
            guiLeft = ACS.getGuiLeft();
            guiTop = ACS.getGuiTop();
        }
        int color = 0xFFFFFFFF; //Nontransparent white
        if(phase==1){//16
            for(int x=0; x<width; x+=16){
                color = x%512==0? 0xFF000088 : x%256==0? 0xFF00AAFF : x%64==0? 0xCC00FFFF : 0x2200FFFF;
                //graphics.fill(x, 0, x + 1, height, color);
                graphics.vLine(x,0,height,color);
            }
            for (int y=0; y<height; y+=16){
                color = y%512==0? 0xFF000088 : y%256==0? 0xFF00AAFF : y%64==0? 0xCC00FFFF : 0x2200FFFF;
                graphics.hLine(0,width,y,color);
            }
        } else if (phase==2) {
            for(int x=0; x<width; x+=10){
                color = x%200==0? 0xFF004400 : x%100==0? 0xCC008855 : x%50==0? 0xCC00FF00 : 0x2200FF00;
                graphics.vLine(x,0,height,color);
            }
            for(int y=0; y<height; y+=10){
                color = y%200==0? 0xFF004400 : y%100==0? 0xCC008855 : y%50==0? 0xCC00FF00 : 0x2200FF00;
                graphics.hLine(0,width,y,color);
            }
        }
        if(phase>0){
            graphics.hLine(0,width, (int) mouseY,0xff888822);
            graphics.vLine((int)mouseX,0,height,0xff888822);

            graphics.drawString(mc.font, Component.literal("Width: "+width),width-128,10,0xFFFFFF00);
            graphics.drawString(mc.font, Component.literal("Height: "+height),width-128,20,0xFFFFFF00);
            graphics.drawString(mc.font, Component.literal("ImageWidth/XSize: "+imageWidth),width-128,30,0xFFFFFF00);
            graphics.drawString(mc.font, Component.literal("ImageHeight/YSize: "+imageHeight),width-128,40,0xFFFFFF00);
            graphics.drawString(mc.font, Component.literal("GUILeft: "+guiLeft),width-128,50,0xFFFFFF00);
            graphics.drawString(mc.font, Component.literal("GUITop: "+guiTop),width-128,60,0xFFFFFF00);
            graphics.drawString(mc.font, Component.literal("mouseX: "+mouseX),width-128,70,0xFFFFFF00);
            graphics.drawString(mc.font, Component.literal("mouseY: "+mouseY),width-128,80,0xFFFFFF00);
        }
    }

    @SubscribeEvent
    public static void click(ScreenEvent.KeyPressed.Post event){
        int button = event.getKeyCode();
        if(button == InputConstants.KEY_0){
            phase++;
            if(phase>2) phase=0;
        }
        if(event.getScreen() instanceof EndlessInventoryScreen EIS && EIS.getMenu().getDisplayingPage() instanceof ItemDisplay itemDisplay){
            if(button == InputConstants.KEY_R){
                itemDisplay.tryRequestContents();
            }
            if(button == InputConstants.KEY_T){
                itemDisplay.setChanged();
            }
        }
    }
}
