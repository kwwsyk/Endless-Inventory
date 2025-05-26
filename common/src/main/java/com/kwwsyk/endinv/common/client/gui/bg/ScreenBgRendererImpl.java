package com.kwwsyk.endinv.common.client.gui.bg;

import com.kwwsyk.endinv.common.client.gui.ScreenFrameWork;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public abstract class ScreenBgRendererImpl implements ScreenBgRenderer{

    protected final ScreenFrameWork frameWork;

    protected final int menuLeft;
    protected final int menuTop;
    protected final int pageLeft;
    protected final int pageTop;
    protected final AbstractContainerScreen<?> screen;
    public ScreenRectangleWidgetParam pageSwitchTabParam;
    protected final int imageWidth;

    protected final int rows;
    protected final int columns;

    protected final PageMetaDataManager manager;

    public ScreenBgRendererImpl(ScreenFrameWork frameWork){
        this.frameWork = frameWork;
        this.screen = frameWork.screen;
        this.imageWidth = 256;
        this.manager = frameWork.meta;
        this.rows = manager.getRowCount();
        this.columns = manager.getColumnCount();
        this.menuLeft = screen.getGuiLeft();
        this.menuTop = screen.getGuiTop();
        this.pageLeft = frameWork.leftPos;
        this.pageTop = frameWork.topPos;
    }

    protected void renderPageBarContent(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY){
        int pageX = pageSwitchTabParam.XPos();
        int pageY = pageSwitchTabParam.YPos();
        int selectedPageIndex = manager.getDisplayingPageIndex();
        for (int i = frameWork.firstPageIndex; i < frameWork.firstPageIndex+ frameWork.pageBarCount; ++i) {
            manager.getPages().get(i).getPageRenderer().renderPageIcon(guiGraphics, pageX + 15, pageY + 5, partialTick);
            if (mouseX > pageX && mouseX < pageX + 32 && mouseY > pageY && mouseY < pageY + 28) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 550.0f);
                guiGraphics.renderTooltip(screen.getMinecraft().font, manager.getPages().get(i).name, mouseX, mouseY);
                guiGraphics.pose().popPose();
            }
            pageY += 28;
        }
    }

    @Override
    public ScreenRectangleWidgetParam pageSwitchBarParam() {
        return pageSwitchTabParam;
    }

    @Override
    public ScreenFrameWork getScreenFrameWork(){
        return frameWork;
    }
}
