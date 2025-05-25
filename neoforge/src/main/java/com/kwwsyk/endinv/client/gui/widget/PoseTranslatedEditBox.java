package com.kwwsyk.endinv.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class PoseTranslatedEditBox extends EditBox {
    private final float tranZ;
    public PoseTranslatedEditBox(Font font, int x, int y, int width, int height, Component message,float translateZ) {
        super(font, x, y, width, height, message);
        tranZ = translateZ;
    }

    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick){
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0,0,tranZ);
        super.renderWidget(guiGraphics,mouseX,mouseY,partialTick);
        guiGraphics.pose().popPose();
    }
}
