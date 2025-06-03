package com.kwwsyk.endinv.common.client.gui;

import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public interface SortTypeSwitcher {

    void switchSortTypeTo(SortType type);

    void setHoveringOnSortBox(boolean isHovering);

    boolean isHoveringOnSortBox();

    PageMetaDataManager getPageMetadata();

    AbstractContainerScreen<?> getScreen();
}
