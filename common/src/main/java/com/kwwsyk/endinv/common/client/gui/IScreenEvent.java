package com.kwwsyk.endinv.common.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

public interface IScreenEvent {

    default void addListener(AbstractWidget widget){}

    default double getMouseX(){
        return 0;
    }

    default double getMouseY(){
        return 0;
    }

    default GuiGraphics getGuiGraphics(){
        return null;
    }

    default float getPartialTick(){
        return 0;
    }

    default int getButton(){
        return -1;
    }

    default int getMouseButton(){
        return -1;
    }

    default void setCanceled(boolean canceled){}

    default double getDragX(){
        return 0;
    }

    default double getDragY(){
        return 0;
    }


    default double getScrollDeltaY(){
        return 0;
    }

    default double getScrollDeltaX(){
        return 0;
    }

    default int getKeyCode(){
        return -1;
    }

    default int getScanCode() {
        return -1;
    }

    default int getModifiers(){
        return 0;
    }

    default char getCodePoint(){
        return 0;
    }
}
