package com.kwwsyk.endinv.neoforge.integrates.jei;

import com.kwwsyk.endinv.common.client.gui.AttachingScreen;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.neoforge.client.events.ScreenAttachment;
import com.mojang.logging.LogUtils;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public class AttachmentGuiHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {

    static final Logger LOGGER = LogUtils.getLogger();

    public AttachmentGuiHandler(){}

    @Override
    public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> containerScreen) {
        AttachingScreen<?> attachedScreen = ScreenAttachment.ATTACHMENT_MANAGER;
        if(attachedScreen!=null){
            return attachedScreen.getArea();
        }
        return IGuiContainerHandler.super.getGuiExtraAreas(containerScreen);
    }

    @Override
    public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory builder, AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
        var sf = ScreenFramework.getInstance();
        if (sf == null) return Optional.empty();

        var page = sf.getDisplayingPage();

        ItemStack hovered = page.getHoveredOrClickedItem(mouseX, mouseY);
        if (hovered == null || hovered.isEmpty()) return Optional.empty();

        int localX = (int) Math.floor(mouseX - page.getPageLeft());
        int localY = (int) Math.floor(mouseY - page.getPageTop());
        Rect2i area = page.getOneInteractableArea(localX, localY);
        if (area == null) return Optional.empty();

//        int absX = (int) Math.floor(mouseX);
//        int absY = (int) Math.floor(mouseY);
//        if (!area.contains(absX, absY)) return Optional.empty();

        return builder.createBuilder(hovered).buildWithArea(area);
    }
}
