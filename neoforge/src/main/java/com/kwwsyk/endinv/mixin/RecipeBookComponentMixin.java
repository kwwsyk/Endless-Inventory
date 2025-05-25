package com.kwwsyk.endinv.mixin;

import com.kwwsyk.endinv.client.CachedSrcInv;
import com.kwwsyk.endinv.util.recipeTransferHelper.RecipeItemProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.entity.player.StackedContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin {

    @Final
    @Shadow
    private StackedContents stackedContents;
    @Shadow
    protected Minecraft minecraft;
    @Unique
    private CachedSrcInv srcInv = CachedSrcInv.INSTANCE;

    @Inject(method = "initVisuals",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/RecipeBookMenu;fillCraftSlotsStackedContents(Lnet/minecraft/world/entity/player/StackedContents;)V"))
    private void fillEndInvStackedContents(CallbackInfo ci){
        RecipeItemProvider.fillStackedContents(srcInv.getItemsAsList(),stackedContents);
    }

    @Inject(method = "updateStackedContents",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/RecipeBookMenu;fillCraftSlotsStackedContents(Lnet/minecraft/world/entity/player/StackedContents;)V"))
    private void updateStackedContentsOfEndInv(CallbackInfo ci){
        RecipeItemProvider.fillStackedContents(srcInv.getItemsAsList(),stackedContents);
    }
}
