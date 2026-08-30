package com.kwwsyk.endinv.forge.integrates.jei;

import com.kwwsyk.endinv.common.client.gui.AttachingScreen;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.client.gui.page.DisplayPage;
import com.kwwsyk.endinv.forge.client.events.ScreenAttachment;
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
import java.util.Optional;

public class AttachmentGuiHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {

    static final Logger LOGGER = LogUtils.getLogger();

    public AttachmentGuiHandler(){}

    @Override
    public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> containerScreen) {
        AttachingScreen<?> attachingScreen = ScreenAttachment.ATTACHMENT_MANAGER;
        if(attachingScreen !=null){
            return attachingScreen.getArea();
        }
        return IGuiContainerHandler.super.getGuiExtraAreas(containerScreen);
    }

    /**
     * Return a clickable ingredient under the mouse that JEI could not normally detect, used for JEI recipe lookups.
     * <p>
     * This is useful for guis that don't have normal slots (which is how JEI normally detects items under the mouse).
     * <p>
     * This can also be used to let JEI look up liquids in tanks directly, by returning a FluidStack.
     * Works with any ingredient type that has been registered with {@code IModIngredientRegistration}.
     *
     * @param mouseX          the current X position of the mouse in screen coordinates.
     * @param mouseY          the current Y position of the mouse in screen coordinates.
     * @since 11.5.0
     */
    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
        if(ScreenFramework.getInstance() != null){
            DisplayPage page = ScreenFramework.getInstance().getDisplayingPage();

            if(page.getOneInteractableArea(mouseX - page.getPageLeft(), mouseY - page.getPageTop()) == null) return Optional.empty();

            return Optional.of(new ItemClickEventWrapper(page, mouseX, mouseY));
        }
        return Optional.empty();
    }


    public static class ItemClickEventWrapper implements IClickableIngredient<ItemStack>{

        DisplayPage page;
        ItemStack hovered;
        Rect2i hoveredSlotArea;

        public ItemClickEventWrapper(DisplayPage page, double mouseX, double mouseY){
            this.page = page;
            this.hovered = page.getHoveredOrClickedItem(mouseX,mouseY);
            this.hoveredSlotArea = page.getOneInteractableArea(mouseX - page.getPageLeft(), mouseY - page.getPageTop());
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
                throw new IllegalStateException("The area does not contain clicked pos");
        }
    }
}
