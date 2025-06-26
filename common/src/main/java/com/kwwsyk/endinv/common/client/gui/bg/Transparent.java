package com.kwwsyk.endinv.common.client.gui.bg;

import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class Transparent extends ScreenBgRendererImpl {

    private static final int PAGE_FRAME_COLOR = 0x80A0A0A0;
    private static final int PAGE_BG_COLOR = 0x30373737;

    public Transparent(ScreenFramework frameWork, ScreenRectangleWidgetParam pageSwitchTabParam) {
        super(frameWork);
        this.pageSwitchTabParam = pageSwitchTabParam;
    }


    public class GridPageRenderer implements ScreenBgRenderer.BgRenderer{

        @Override
        public void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            int startX = pageLeft;
            int startY = pageTop;
            int gridWidth = 18*columns;
            int gridHeight = 18*rows;
            int leftWidth = 7;
            int rightWidth = 7;
            int pageWidth = leftWidth+gridWidth+rightWidth;
            int topHeight = 17;
            int bottomHeight = 12;
            int pageHeight = topHeight+gridHeight+bottomHeight;

            guiGraphics.fill(startX,startY,startX+pageWidth,startY+topHeight,PAGE_FRAME_COLOR);
            guiGraphics.fill(startX,startY+topHeight,startX+leftWidth,startY+pageHeight,PAGE_FRAME_COLOR);
            guiGraphics.fill(startX+leftWidth+gridWidth,startY+topHeight,startX+pageWidth,startY+pageHeight,PAGE_FRAME_COLOR);
            guiGraphics.fill(startX+leftWidth,startY+topHeight+gridHeight,startX+leftWidth+gridWidth,startY+pageHeight,PAGE_FRAME_COLOR);

            guiGraphics.fill(startX+leftWidth,startY+topHeight,startX+leftWidth+gridWidth,startY+topHeight+gridHeight,PAGE_BG_COLOR);
        }
    }

    @Override
    public void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int pageX = pageSwitchTabParam.XPos();
        int pageY = pageSwitchTabParam.YPos();
        int selectedPageIndex = manager.getDisplayingPageIndex();
        for (int i = frameWork.firstPageIndex; i < frameWork.firstPageIndex+ frameWork.pageBarCount; ++i) {
            if (i == selectedPageIndex) {
                guiGraphics.fill(pageX,pageY,pageX+32,pageY+28,PAGE_FRAME_COLOR);
            } else {
                guiGraphics.fill(pageX+4,pageY,pageX+32,pageY+28,PAGE_BG_COLOR);
            }
            pageY+=28;
        }
        renderPageBarContent(guiGraphics, partialTicks, mouseX, mouseY);
    }

    @Override
    public Optional<BgRenderer> getDefaultPageBgRenderer() {
        return Optional.of(new GridPageRenderer());
    }
}
