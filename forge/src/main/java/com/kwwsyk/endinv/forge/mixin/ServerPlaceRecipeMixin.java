package com.kwwsyk.endinv.forge.mixin;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.util.recipeTransferHelper.RecipeItemProvider;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(ServerPlaceRecipe.class)
public class ServerPlaceRecipeMixin<C extends Container>{

    @Final
    @Shadow
    protected StackedContents stackedContents;
    @Unique
    @Nullable
    private EndlessInventory endInv;
    @Unique private int ei$lastIndex = Integer.MIN_VALUE; // 记录 moveItemToGrid 中的 i


    @Inject(method = "recipeClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/RecipeBookMenu;fillCraftSlotsStackedContents(Lnet/minecraft/world/entity/player/StackedContents;)V"))
    private void fillEndInvStackedContents(ServerPlayer player, RecipeHolder<? extends Recipe<C>> holder, boolean placeAll, CallbackInfo ci){
        endInv = ServerLevelEndInv.getEndInvForPlayer(player).orElse(null);
        if(endInv==null) return;
        RecipeItemProvider.fillStackedContents(endInv.getItemsAsList(), this.stackedContents);
    }

    @Inject(method = "recipeClicked", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/recipebook/ServerPlaceRecipe;handleRecipeClicked(Lnet/minecraft/world/item/crafting/RecipeHolder;Z)V"))
    private void finishHandleClick(ServerPlayer player, RecipeHolder<? extends Recipe<C>> holder, boolean placeAll, CallbackInfo ci){
        if(endInv!=null){
            endInv.broadcastChanges();
        }
    }

    @ModifyVariable(method = "moveItemToGrid",
            at = @At(value = "STORE"), // i 被赋值处
            ordinal = 0)
    private int ei$captureIndex(int i) {
        this.ei$lastIndex = i;
        return i; // 不篡改
    }

    // C. 在 RETURN 使用记录的 i，避免本地变量再捕获
    @Inject(method = "moveItemToGrid", at = @At("RETURN"))
    private void ei$afterMove(Slot slotToFill, ItemStack ingredient, CallbackInfo ci) {
        if (this.ei$lastIndex != -1) return;
        if (endInv == null) return;

        ItemStack taken = endInv.takeItem(ingredient, 1);
        if (taken.isEmpty()) return;

        ItemStack cur = slotToFill.getItem();
        if (cur.isEmpty()) slotToFill.set(taken.copy());
        else cur.grow(taken.getCount());

        // 可按需：endInv.broadcastChanges(); 频率自己把控
    }
}
