package com.kwwsyk.endinv.client.gui;

import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.options.SortType;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public interface SortTypeSwitcher {
    void switchSortTypeTo(SortType type);
    void setHoveringOnSortBox(boolean isHovering);
    PageMetaDataManager getMenu();
    AbstractContainerScreen<?> getScreen();
}
