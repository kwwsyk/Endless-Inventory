package com.kwwsyk.endinv.common.integrates.jei;

import com.kwwsyk.endinv.common.client.gui.AttachedScreen;
import com.kwwsyk.endinv.neoforge.client.events.ScreenAttachment;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AttachmentGuiHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {

    public AttachmentGuiHandler(){}

    @Override
    public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull AbstractContainerScreen<?> containerScreen) {
        AttachedScreen<?> attachedScreen = ScreenAttachment.ATTACHMENT_MANAGER.get(containerScreen);
        if(attachedScreen!=null){
            return attachedScreen.getArea();
        }
        return IGuiContainerHandler.super.getGuiExtraAreas(containerScreen);
    }
}
