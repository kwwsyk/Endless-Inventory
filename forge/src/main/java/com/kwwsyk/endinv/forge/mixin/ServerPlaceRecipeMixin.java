package com.kwwsyk.endinv.forge.mixin;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.util.recipeTransferHelper.RecipeItemProvider;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;

@Mixin(ServerPlaceRecipe.class)
public class ServerPlaceRecipeMixin<I extends RecipeInput, R extends Recipe<I>>{

    @Final
    @Shadow
    protected StackedContents stackedContents;
    @Unique
    @Nullable
    private EndlessInventory endInv;


    @Inject(method = "recipeClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/RecipeBookMenu;fillCraftSlotsStackedContents(Lnet/minecraft/world/entity/player/StackedContents;)V"))
    private void fillEndInvStackedContents(ServerPlayer player, RecipeHolder<R> recipe, boolean placeAll, CallbackInfo ci){
        endInv = ServerLevelEndInv.getEndInvForPlayer(player).orElse(null);
        if(endInv==null) return;
        RecipeItemProvider.fillStackedContents(endInv.getItemsAsList(), this.stackedContents);
    }

    @Inject(method = "recipeClicked", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/recipebook/ServerPlaceRecipe;handleRecipeClicked(Lnet/minecraft/world/item/crafting/RecipeHolder;Z)V"))
    private void finishHandleClick(ServerPlayer player, RecipeHolder<R> recipe, boolean placeAll, CallbackInfo ci){
        if(endInv!=null){
            endInv.broadcastChanges();
        }
    }

    @Inject(method = "moveItemToGrid",at = @At("RETURN"),locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void getAttachedItems(Slot slot, ItemStack stack, int maxAmount, CallbackInfoReturnable<Integer> cir, int i){
        if(i!=-1 || endInv==null) return;
        ItemStack itemStack = endInv.takeItem(stack,maxAmount);
        //endInv.broadcastChanges(); Don't let it be invoked too many times.
        if(itemStack.isEmpty()){
            cir.setReturnValue(-1);
            cir.cancel();
            return;
        }
        if(slot.getItem().isEmpty()){
            slot.set(itemStack.copy());
        }else {
            slot.getItem().grow(itemStack.getCount());
        }
        i=maxAmount-itemStack.getCount();
        cir.setReturnValue(i);
        cir.cancel();
    }
}
