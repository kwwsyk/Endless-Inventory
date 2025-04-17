package com.kwwsyk.endinv.client.gui.bg;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

public interface ScreenTextureMode {
    void renderBg(@NotNull GuiGraphics guiGraphics, float v, int i0, int i1);
    void init();
    ScreenLayoutMode screenLayoutMode();
    ScreenRectangleWidgetParam pageSwitchBarParam();
}
