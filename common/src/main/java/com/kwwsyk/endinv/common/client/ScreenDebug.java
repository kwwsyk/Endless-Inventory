package com.kwwsyk.endinv.common.client;

import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.common.menu.page.ItemPage;
import com.kwwsyk.endinv.common.network.payloads.PageData;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

import static com.kwwsyk.endinv.common.client.ClientModInfo.containerScreenHelper;
import static com.kwwsyk.endinv.common.client.ClientModInfo.getClientConfig;

public class ScreenDebug {

    protected static final String HidingText = "[F4]Menu screen rendering stopped!";

    public static int phase = 0;
    protected static boolean hideMenu = false;
    protected static Minecraft mc;

    public static void debugInfo(Screen screen, GuiGraphics graphics, double mouseX, double mouseY){
        if(!ClientModInfo.getClientConfig().screenDebugging().get()) return;
        int width = screen.width;
        int height = screen.height;
        int imageWidth = 0;
        int imageHeight = 0;
        int guiLeft = 0;
        int guiTop = 0;
        mc = Optional.ofNullable(mc).orElseGet(Minecraft::getInstance);
        if(screen instanceof AbstractContainerScreen<?> ACS){
            imageWidth = containerScreenHelper.getGuiXSize(ACS);
            imageHeight = containerScreenHelper.getGuiYSize(ACS);
            guiLeft = containerScreenHelper.getGuiLeft(ACS);
            guiTop = containerScreenHelper.getGuiTop(ACS);
        }
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0,0,1000.0);
        {
            int color = 0xFFFFFFFF; //Nontransparent white
            if (phase == 1) {//16
                for (int x = 0; x < width; x += 16) {
                    color = x % 512 == 0 ? 0xFF000088 : x % 256 == 0 ? 0xFF00AAFF : x % 64 == 0 ? 0xCC00FFFF : 0x2200FFFF;
                    //graphics.fill(x, 0, x + 1, height, color);
                    graphics.vLine(x, 0, height, color);
                }
                for (int y = 0; y < height; y += 16) {
                    color = y % 512 == 0 ? 0xFF000088 : y % 256 == 0 ? 0xFF00AAFF : y % 64 == 0 ? 0xCC00FFFF : 0x2200FFFF;
                    graphics.hLine(0, width, y, color);
                }
            } else if (phase == 2) {
                for (int x = 0; x < width; x += 10) {
                    color = x % 200 == 0 ? 0xFF004400 : x % 100 == 0 ? 0xCC008855 : x % 50 == 0 ? 0xCC00FF00 : 0x2200FF00;
                    graphics.vLine(x, 0, height, color);
                }
                for (int y = 0; y < height; y += 10) {
                    color = y % 200 == 0 ? 0xFF004400 : y % 100 == 0 ? 0xCC008855 : y % 50 == 0 ? 0xCC00FF00 : 0x2200FF00;
                    graphics.hLine(0, width, y, color);
                }
            }
            if (phase > 0) {
                graphics.hLine(0, width, (int) mouseY, 0xff888822);
                graphics.vLine((int) mouseX, 0, height, 0xff888822);

                graphics.drawString(mc.font, Component.literal("[F3]Screen Debug enabled"),0,0,0xFFFFAA00);

                graphics.drawString(mc.font, Component.literal("Width: " + width), width - 128, 10, 0xFFFFFF00);
                graphics.drawString(mc.font, Component.literal("Height: " + height), width - 128, 20, 0xFFFFFF00);
                graphics.drawString(mc.font, Component.literal("ImageWidth/XSize: " + imageWidth), width - 128, 30, 0xFFFFFF00);
                graphics.drawString(mc.font, Component.literal("ImageHeight/YSize: " + imageHeight), width - 128, 40, 0xFFFFFF00);
                graphics.drawString(mc.font, Component.literal("GUILeft: " + guiLeft), width - 128, 50, 0xFFFFFF00);
                graphics.drawString(mc.font, Component.literal("GUITop: " + guiTop), width - 128, 60, 0xFFFFFF00);
                graphics.drawString(mc.font, Component.literal("mouseX: " + mouseX), width - 128, 70, 0xFFFFFF00);
                graphics.drawString(mc.font, Component.literal("mouseY: " + mouseY), width - 128, 80, 0xFFFFFF00);

                if(mc.player!=null) {
                    SyncedConfig syncedConfig = ModRegistries.NbtAttachments.getSyncedConfig().computeIfAbsent(mc.player);
                    PageData pageData = syncedConfig.pageData();
                    graphics.drawString(mc.font, Component.literal("Page data: "), 0, 10, 0xFFFFFF00);
                    graphics.drawString(mc.font, Component.literal("Page: "+pageData.pageRegKey()), 0, 20, 0xFFFFFF00);
                    graphics.drawString(mc.font, Component.literal("Rows: "+pageData.rows()), 10, 30, 0xFFFFFF00);
                    graphics.drawString(mc.font, Component.literal("Columns: "+pageData.columns()), 10, 40, 0xFFFFFF00);
                    graphics.drawString(mc.font, Component.literal("Sort type: "+pageData.sortType()), 10, 50, 0xFFFFFF00);
                    graphics.drawString(mc.font, Component.literal("Sort reversed: "+pageData.reverseSort()), 10, 60, 0xFFFFFF00);
                    graphics.drawString(mc.font, Component.literal("Searching: "+pageData.search()), 10, 70, 0xFFFFFF00);
                    graphics.drawString(mc.font, Component.literal("Configurations: "), 0, 80, 0xFFFFFF00);
                    var config = getClientConfig();
                    graphics.drawString(mc.font, Component.literal("Auto suit in columns: "+ config.autoSuitColumn().get()), 10, 90, 0xFFFFFF00);
                    graphics.drawString(mc.font, Component.literal("Texture mode: "+ config.textureMode().get()), 10, 100, 0xFFFFFF00);
                }
            }
        }
        pose.popPose();
    }

    public static void click(int button, Screen screen){
        if(!ClientModInfo.getClientConfig().screenDebugging().get()) return;

        if(button == InputConstants.KEY_F3){
            phase++;
            if(phase>2) phase=0;
        }
        if(button==InputConstants.KEY_F4){
            hideMenu=!hideMenu;
        }
        if(screen instanceof EndlessInventoryScreen EIS && EIS.getMenu().getDisplayingPage() instanceof ItemPage itemPage){
            if(button == InputConstants.KEY_R){
                itemPage.tryRequestContents();
            }
            if(button == InputConstants.KEY_T){
                itemPage.setChanged();
            }
        }
    }
}
