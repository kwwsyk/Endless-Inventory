package com.kwwsyk.endinv.client.gui.bg;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface ScreenBgRenderer {

    void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY);

    ScreenRectangleWidgetParam pageSwitchBarParam();

    default Optional<BgRenderer> getDefaultPageBgRenderer(){
        return Optional.empty();
    }

    interface PageRenderer{

        default void renderBg(ScreenBgRenderer screenBgRenderer, GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY){
            screenBgRenderer.getDefaultPageBgRenderer().ifPresent(bgRenderer -> bgRenderer.renderBg(guiGraphics, partialTicks, mouseX, mouseY));
        }
    }

    @FunctionalInterface
    interface BgRenderer{
        void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY);
    }
}
