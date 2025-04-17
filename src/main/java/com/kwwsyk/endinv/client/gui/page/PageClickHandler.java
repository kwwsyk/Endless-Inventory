package com.kwwsyk.endinv.client.gui.page;

import net.minecraft.world.inventory.ClickType;

public interface PageClickHandler {

    boolean doubleClicked(double XOffset,double YOffset,double lastX,double lastY,long clickInterval);
    void pageClicked(double XOffset, double YOffset, int keyCode, ClickType clickType);
}
