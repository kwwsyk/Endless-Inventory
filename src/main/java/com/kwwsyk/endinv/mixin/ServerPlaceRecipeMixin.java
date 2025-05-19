package com.kwwsyk.endinv.mixin;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.ServerLevelEndInv;
import com.kwwsyk.endinv.util.recipeTransferHelper.RecipeItemProvider;
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

@Mixin(ServerPlaceRecipe.class)
public class ServerPlaceRecipeMixin<I extends RecipeInput, R extends Recipe<I>>{

    @Final
    @Shadow
    protected StackedContents stackedContents;
    @Unique
    private EndlessInventory endInv;


    @Inject(method = "recipeClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/RecipeBookMenu;fillCraftSlotsStackedContents(Lnet/minecraft/world/entity/player/StackedContents;)V"))
    private void fillEndInvStackedContents(ServerPlayer player, RecipeHolder<R> recipe, boolean placeAll, CallbackInfo ci){
        endInv = ServerLevelEndInv.getEndInvForPlayer(player);
        //if(endInv==null) return;
        RecipeItemProvider.fillStackedContents(endInv.getItemsAsList(), this.stackedContents);
    }

    @Inject(method = "moveItemToGrid",at = @At("RETURN"),locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void getAttachedItems(Slot slot, ItemStack stack, int maxAmount, CallbackInfoReturnable<Integer> cir, int i){
        if(i!=-1) return;
        ItemStack itemStack = endInv.takeItem(stack,maxAmount);
        endInv.broadcastChanges();
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
