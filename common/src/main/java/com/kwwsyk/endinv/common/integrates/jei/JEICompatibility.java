package com.kwwsyk.endinv.common.integrates.jei;

import com.kwwsyk.endinv.neoforge.ModInitializer;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModInfoRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class JEICompatibility implements IModPlugin{

    public JEICompatibility(){}

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"compatibility");
    }

    @Override
    public void registerModInfo(IModInfoRegistration modAliasRegistration){
        modAliasRegistration.addModAliases(ModInitializer.MOD_ID,"endinv","end_inv");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration guiHandlerRegistration){
        guiHandlerRegistration.addGenericGuiContainerHandler(AbstractContainerScreen.class,new AttachmentGuiHandler());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration){
        IRecipeTransferHandlerHelper helper = registration.getTransferHelper();

    }
}
