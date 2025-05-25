package com.kwwsyk.endinv.util.recipeTransferHelper;

import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public interface RecipeItemProvider {

    boolean canExtract(List<Ingredient> items);

    List<ItemStack> doExtract(List<Ingredient> items);

    static void fillStackedContents(List<ItemStack> stackSource, StackedContents stackedContents){
        stackSource.forEach(is->stackedContents.accountStack(is,Integer.MAX_VALUE));
    }


}
