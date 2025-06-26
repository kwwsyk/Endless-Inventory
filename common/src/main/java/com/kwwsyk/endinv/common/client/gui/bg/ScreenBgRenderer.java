package com.kwwsyk.endinv.common.client.gui.bg;

import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface ScreenBgRenderer {

    ScreenFramework getScreenFrameWork();

    void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY);

    ScreenRectangleWidgetParam pageSwitchBarParam();

    default Optional<BgRenderer> getDefaultPageBgRenderer(){
        return Optional.empty();
    }

    @FunctionalInterface
    interface BgRenderer{
        void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY);
    }
}
