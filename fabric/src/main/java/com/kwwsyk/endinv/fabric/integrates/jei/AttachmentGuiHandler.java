package com.kwwsyk.endinv.fabric.integrates.jei;

import com.kwwsyk.endinv.common.client.gui.AttachingScreen;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.client.gui.page.DisplayPage;
import com.kwwsyk.endinv.fabric.client.events.ScreenAttachment;
import com.mojang.logging.LogUtils;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AttachmentGuiHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {

    static final Logger LOGGER = LogUtils.getLogger();

    public AttachmentGuiHandler(){}

    @Override
    public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> containerScreen) {
        AttachingScreen<?> attachingScreen = ScreenAttachment.attachment;
        if(attachingScreen !=null){
            return attachingScreen.getArea();
        }
        return IGuiContainerHandler.super.getGuiExtraAreas(containerScreen);
    }

    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
        if(ScreenFramework.getInstance() != null){
            try {
                return Optional.of(new ItemClickEventWrapper(ScreenFramework.getInstance().getDisplayingPage(), mouseX, mouseY));
            } catch (Exception e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }


    public static class ItemClickEventWrapper implements IClickableIngredient<ItemStack>{

        DisplayPage page;
        ItemStack hovered;
        Rect2i hoveredSlotArea;

        public ItemClickEventWrapper(DisplayPage page, double mouseX, double mouseY) {
            this.page = page;
            this.hovered = page.getHoveredOrClickedItem(mouseX,mouseY);
            this.hoveredSlotArea = Objects.requireNonNull(page.getOneInteractableArea(mouseX - page.getPageLeft(), mouseY - page.getPageTop()));
            try {
                checkStatus(mouseX,mouseY);
            } catch (Exception e) {
                LOGGER.error("Check click area failed: ", e);
            }
        }

        /**
         * Get the typed ingredient that can be looked up by JEI for recipes.
         *
         * @since 11.5.0
         */
        @Override
        public ITypedIngredient<ItemStack> getTypedIngredient() {
            return new ITypedIngredient<>() {
                @Override
                public IIngredientType<ItemStack> getType() {
                    return VanillaTypes.ITEM_STACK;
                }

                @Override
                public ItemStack getIngredient() {
                    return hovered;
                }
            };
        }

        /**
         * Get the area that this clickable ingredient is drawn in, in absolute screen coordinates.
         * This is used for click handling, to ensure the mouse-down and mouse-up are on the same ingredient.
         *
         * @since 11.5.0
         */
        @Override
        public Rect2i getArea() {
            return hoveredSlotArea;
        }

        @TestOnly
        private void checkStatus(double mouseX, double mouseY){
            Rect2i area = getArea();
            if(!area.contains((int) mouseX, (int) mouseY))
                throw new IllegalStateException(String.format(
                        "The area does not contain clicked pos: clicked: [%.2f,%.2f], area: [from[%d,%d],to[%d,%d]]",
                        mouseX,mouseY,
                        area.getX(),area.getY(),
                        area.getX()+area.getWidth(),area.getY()+area.getHeight()
                ));
        }
    }
}
