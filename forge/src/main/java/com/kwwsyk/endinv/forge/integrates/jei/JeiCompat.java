package com.kwwsyk.endinv.forge.integrates.jei;

import com.kwwsyk.endinv.common.ModInfo;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin @SuppressWarnings("removal")
public class JeiCompat implements IModPlugin{

    public JeiCompat(){}

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ModInfo.MOD_ID, "compatibility");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration guiHandlerRegistration){
        guiHandlerRegistration.addGenericGuiContainerHandler(AbstractContainerScreen.class,new AttachmentGuiHandler());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration){
        IJeiHelpers jeiHelper = registration.getJeiHelpers();
        IRecipeTransferHandlerHelper transferHelper = registration.getTransferHelper();

        registration.addRecipeTransferHandler(new EIMRecipeTranHandler(jeiHelper,transferHelper), RecipeTypes.CRAFTING);
    }
}
