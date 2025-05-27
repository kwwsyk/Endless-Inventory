package com.kwwsyk.endinv.common.client.option;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;

public interface IClientConfig {

    boolean attaching();

    int rows();

    int columns();

    boolean autoSuitColumn();

    TextureMode textureMode();

    boolean screenDebugging();

    List<String> hidingPageIdList();

    default int calculateDefaultRowCount(boolean ofMenu){
        Minecraft mc = Minecraft.getInstance();
        int height = mc.getWindow().getGuiScaledHeight();
        return Math.max(Math.floorDiv(height-60,18)-(ofMenu?4:0),0);
    }

    default int calculateSuitInColumnCount(AbstractContainerScreen<?> screen){
        int leftPos = (screen.width - 176)/2;
        int width = leftPos - 20 - 6 -6;
        return Math.max(0,Math.floorDiv(width,18));
    }
}
