package com.kwwsyk.endinv.common.client.option;

import com.kwwsyk.endinv.common.options.IConfigValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Set;

public interface IClientConfig {

    IConfigValue<Boolean> attaching();

    IConfigValue<Integer> rows();

    IConfigValue<Integer> columns();

    IConfigValue<Boolean> autoSuitColumn();

    IConfigValue<TextureMode> textureMode();

    IConfigValue<Boolean> screenDebugging();

    IConfigValue<Integer> maxPageBarCount();

    Set<String> hidingPageIds();

    void setPageHiding(String id, boolean hiding);

    default void save(){}

    default boolean isPageHidden(String id){
        return hidingPageIds().contains(id);
    }

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
