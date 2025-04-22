package com.kwwsyk.endinv.client.gui.bg;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

public class Transparent implements ScreenRenderer {
    @Override
    public void renderBg(@NotNull GuiGraphics guiGraphics, float v, int i0, int i1) {

    }

    @Override
    public void init() {

    }

    @Override
    public ScreenLayoutMode screenLayoutMode() {
        return null;
    }

    @Override
    public ScreenRectangleWidgetParam pageSwitchBarParam() {
        return null;
    }
}
