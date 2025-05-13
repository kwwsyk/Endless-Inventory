package com.kwwsyk.endinv.client.gui.bg;

import com.kwwsyk.endinv.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.client.gui.ScreenFrameWork;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public abstract class ScreenBgRendererImpl implements ScreenBgRenderer{

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

    public ScreenBgRendererImpl(EndlessInventoryScreen screen){
        this.screen = screen;
        this.imageWidth = screen.getXSize();
        this.manager = screen.getMenu();
        this.rows = manager.getRowCount();
        this.columns = 9;
        this.menuLeft = screen.getGuiLeft();
        this.menuTop = screen.getGuiTop();
        this.pageLeft = menuLeft;
        this.pageTop = menuTop;
    }

    public ScreenBgRendererImpl(ScreenFrameWork frameWork){
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

    @Override
    public ScreenRectangleWidgetParam pageSwitchBarParam() {
        return pageSwitchTabParam;
    }
}
