package com.kwwsyk.endinv.client.gui.page;

import net.minecraft.client.gui.GuiGraphics;

public interface PageRenderer {
    void renderPage(GuiGraphics graphics,int pageXPos,int pageYPos);
    void renderHovering(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    boolean hasSearchBar();
    boolean hasSortTypeSwitchBar();
    void renderPageIcon(GuiGraphics graphics,int x,int y,float partialTick);
}
