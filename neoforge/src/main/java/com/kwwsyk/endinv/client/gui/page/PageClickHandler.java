package com.kwwsyk.endinv.client.gui.page;

import net.minecraft.world.inventory.ClickType;

public interface PageClickHandler {

    boolean doubleClicked(double XOffset,double YOffset,double lastX,double lastY,long clickInterval);
    void pageClicked(double XOffset, double YOffset, int keyCode, ClickType clickType);

    /**Used to handle mouse clicked/dragged on page and page has slots
     * @param XOffset the relative X coordinate to page left pos
     * @param YOffset the relative Y coordinate to page top pos
     * @return the slot id in page or -1 with no slot.
     */
    default int getSlotForMouseOffset(double XOffset,double YOffset){
        return -1;
    }
}
