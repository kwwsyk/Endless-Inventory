package com.kwwsyk.endinv.forge.integrates.jei;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.forge.network.payloads.JeiTransferRecipePayload;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** {@link EndlessInventoryMenu}'s recipe transfer handler.
 *
 */
public class EIMRecipeTranHandler implements IRecipeTransferHandler<EndlessInventoryMenu, CraftingRecipe> {

    private static final Class<EndlessInventoryMenu> CONTAINER_CLASS = EndlessInventoryMenu.class;
    //is that dangerous?
    private static final MenuType<EndlessInventoryMenu> CONTAINER_TYPE = ModRegistries.Menus.getEndInvMenuType();


    private final IJeiHelpers jeiHelper;
    private final IRecipeTransferHandlerHelper transferHelper;
    private final IRecipeTransferInfo<EndlessInventoryMenu,CraftingRecipe> playerInvInfo;

    public EIMRecipeTranHandler(IJeiHelpers jeiHelper,IRecipeTransferHandlerHelper transferHelper){
        this.jeiHelper = jeiHelper;
        this.transferHelper = transferHelper;
        playerInvInfo = createPlayerInvInfo();
    }

    private IRecipeTransferInfo<EndlessInventoryMenu,CraftingRecipe> createPlayerInvInfo(){
        return this.transferHelper.createBasicRecipeTransferInfo(CONTAINER_CLASS,CONTAINER_TYPE,RecipeTypes.CRAFTING,0,9,10,36);
    }

    /**
     * The container that this recipe transfer handler can use.
     */
    @Override
    public Class<EndlessInventoryMenu> getContainerClass() {
        return CONTAINER_CLASS;
    }

    /**
     * Return the optional menu type that this recipe transfer helper supports.
     * This is used to optionally narrow down the type of container handled by this recipe transfer handler.
     */
    @Override
    public Optional<MenuType<EndlessInventoryMenu>> getMenuType() {
        return Optional.of(CONTAINER_TYPE);
    }

    /**
     * The recipe that this recipe transfer handler can use.
     */
    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    /**
     * @param container   the container to act on
     * @param recipe      the raw recipe instance
     * @param recipeSlots the view of the recipe slots, with information about the ingredients
     * @param player      the player, to do the slot manipulation
     * @param maxTransfer if true, transfer as many items as possible. if false, transfer one set
     * @param doTransfer  if true, do the transfer. if false, check for errors but do not actually transfer the items
     * @return a recipe transfer error if the recipe can't be transferred. Return null on success.
     * @since 9.3.0
     */
    @Override
    public @Nullable IRecipeTransferError transferRecipe(EndlessInventoryMenu container,
                                                         CraftingRecipe recipe,
                                                         IRecipeSlotsView recipeSlots,
                                                         Player player, boolean maxTransfer, boolean doTransfer) {
        try {
            if (!new EIMRecipeTranInfo().canHandle(container, recipe)) {
                return transferHelper.createUserErrorWithTooltip(Component.literal("Unsupported container or recipe"));
            }

            // Context permission check (server rule)
            if (!container.isCrafterEnabled()) {
                return transferHelper.createUserErrorWithTooltip(Component.literal("Crafter is disabled by server rules"));
            }

            TransferPlan plan = createTransferPlan(container, recipe, maxTransfer);
            boolean missing = plan.isMissing();

            if (!doTransfer) {
                // Client-side status: return error to color the button if missing
                return missing ? transferHelper.createUserErrorWithTooltip(Component.literal("Missing ingredient(s)")) : null;
            }

            if (player.level().isClientSide) {
                container.setCraftingVisible(true);
                ModInfo.getPacketDistributor().sendToServer(new JeiTransferRecipePayload(container.containerId, recipe.getId(), maxTransfer));
            } else {
                performTransfer(container, recipe, plan, player);
            }

            // When missing, still allow partial transfer, but communicate status on client
            return missing ? transferHelper.createUserErrorWithTooltip(Component.literal("Missing ingredient(s)")) : null;
        } catch (Exception e) {
            return transferHelper.createInternalError();
        }
    }

    public static void performServerTransfer(EndlessInventoryMenu container, CraftingRecipe recipe, Player player, boolean maxTransfer) {
        TransferPlan plan = createTransferPlan(container, recipe, maxTransfer);
        performTransfer(container, recipe, plan, player);
    }

    private static boolean sameType(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        if (a.getItem() != b.getItem()) return false;
        var ta = a.getTag();
        var tb = b.getTag();
        return java.util.Objects.equals(ta, tb);
    }

    private static int countAvailableOfType(ItemStack type, List<Slot> invSlots, List<ItemStack> pageItems) {
        if (type.isEmpty()) return 0;
        int total = 0;
        for (Slot s : invSlots) {
            if (!s.hasItem()) continue;
            ItemStack st = s.getItem();
            if (sameType(type, st)) total += st.getCount();
        }
        for (ItemStack st : pageItems) {
            if (sameType(type, st)) total += st.getCount();
        }
        return total;
    }

    private static ItemStack chooseBestCandidate(Ingredient ing, List<Slot> invSlots, List<ItemStack> pageItems) {
        ItemStack best = ItemStack.EMPTY;
        int bestCount = 0;
        // Consider explicit candidates first
        for (ItemStack cand : ing.getItems()) {
            if (cand.isEmpty()) continue;
            int count = countAvailableOfType(cand, invSlots, pageItems);
            if (count > bestCount) {
                best = cand;
                bestCount = count;
            }
        }
        // Also consider any matching stacks present in inventory/page
        for (Slot s : invSlots) {
            if (!s.hasItem()) continue;
            ItemStack st = s.getItem();
            if (!ing.test(st)) continue;
            int count = countAvailableOfType(st, invSlots, pageItems);
            if (count > bestCount) {
                best = st.copyWithCount(1);
                bestCount = count;
            }
        }
        for (ItemStack st : pageItems) {
            if (!ing.test(st)) continue;
            int count = countAvailableOfType(st, invSlots, pageItems);
            if (count > bestCount) {
                best = st.copyWithCount(1);
                bestCount = count;
            }
        }
        return best;
    }

    private static TransferPlan createTransferPlan(EndlessInventoryMenu container, CraftingRecipe recipe, boolean maxTransfer) {
        List<Ingredient> ingredients = recipe.getIngredients();
        List<Slot> playerInvSlots = container.getPlayerInvSlots();
        List<ItemStack> pageItems = container.getSourceInventory().getItemsAsList();

        ItemStack[] chosenPerSlot = new ItemStack[9];
        Arrays.fill(chosenPerSlot, ItemStack.EMPTY);
        int craftsPossible = Integer.MAX_VALUE;
        int perSlotStackLimit = Integer.MAX_VALUE;
        int inputsToCheck = Math.min(9, ingredients.size());
        for (int i = 0; i < inputsToCheck; i++) {
            Ingredient ing = ingredients.get(i);
            if (ing.isEmpty()) {
                continue;
            }
            ItemStack selected = chooseBestCandidate(ing, playerInvSlots, pageItems);
            chosenPerSlot[i] = selected;
            int available = selected.isEmpty() ? 0 : countAvailableOfType(selected, playerInvSlots, pageItems);
            craftsPossible = Math.min(craftsPossible, available);
            if (!selected.isEmpty()) {
                perSlotStackLimit = Math.min(perSlotStackLimit, selected.getMaxStackSize());
            }
        }
        if (craftsPossible == Integer.MAX_VALUE) craftsPossible = 0;
        if (perSlotStackLimit == Integer.MAX_VALUE) perSlotStackLimit = 64;

        int craftsWanted = maxTransfer ? Math.min(craftsPossible, perSlotStackLimit) : (craftsPossible > 0 ? 1 : 0);
        boolean missing = craftsWanted <= 0;
        return new TransferPlan(chosenPerSlot, craftsWanted, missing);
    }

    private static void performTransfer(EndlessInventoryMenu container, CraftingRecipe recipe, TransferPlan plan, Player player) {
        container.setCraftingVisible(true);

        List<Slot> craftingSlots = container.getCraftingSlots();
        for (Slot cSlot : craftingSlots) {
            if (!cSlot.hasItem()) continue;
            int count = cSlot.getItem().getCount();
            if (count <= 0) continue;
            ItemStack removed = cSlot.safeTake(count, count, player);
            if (!removed.isEmpty()) {
                player.getInventory().placeItemBackInInventory(removed);
            }
        }

        List<Ingredient> ingredients = recipe.getIngredients();
        List<Slot> playerInvSlots = container.getPlayerInvSlots();

        for (int i = 0; i < 9; i++) {
            Ingredient ing = i < ingredients.size() ? ingredients.get(i) : Ingredient.EMPTY;
            if (ing.isEmpty()) continue;
            Slot target = craftingSlots.get(i);

            int toTake = plan.craftsWanted;
            if (toTake <= 0) continue;

            ItemStack chosen = plan.chosenPerSlot[i];
            ItemStack placedStack = ItemStack.EMPTY;

            for (Slot invSlot : playerInvSlots) {
                if (toTake <= 0) break;
                if (!invSlot.hasItem()) continue;
                ItemStack in = invSlot.getItem();
                if (!(chosen.isEmpty() ? ing.test(in) : sameType(chosen, in))) continue;
                int can = Math.min(toTake, in.getCount());
                if (can <= 0) continue;
                ItemStack taken = invSlot.safeTake(can, can, player);
                if (!taken.isEmpty()) {
                    if (placedStack.isEmpty()) {
                        placedStack = taken;
                    } else if (sameType(placedStack, taken)) {
                        placedStack.grow(taken.getCount());
                    } else {
                        player.getInventory().placeItemBackInInventory(taken);
                        break;
                    }
                    toTake -= taken.getCount();
                }
            }

            if (toTake > 0) {
                ItemStack template = !chosen.isEmpty() ? chosen : (ing.getItems().length > 0 ? ing.getItems()[0] : ItemStack.EMPTY);
                if (!template.isEmpty()) {
                    ItemStack extracted = container.tryExtractFromPage(template, toTake);
                    if (!extracted.isEmpty()) {
                        if (placedStack.isEmpty()) {
                            placedStack = extracted;
                        } else if (sameType(placedStack, extracted)) {
                            placedStack.grow(extracted.getCount());
                        } else {
                            player.getInventory().placeItemBackInInventory(extracted);
                        }
                        toTake -= extracted.getCount();
                    }
                }
            }

            if (!placedStack.isEmpty()) {
                int cap = Math.min(placedStack.getMaxStackSize(), target.getMaxStackSize());
                if (placedStack.getCount() > cap) {
                    ItemStack overflow = placedStack.copyWithCount(placedStack.getCount() - cap);
                    placedStack.setCount(cap);
                    player.getInventory().placeItemBackInInventory(overflow);
                }
                target.set(placedStack);
            }
        }
    }

    private record TransferPlan(ItemStack[] chosenPerSlot, int craftsWanted, boolean missing) {
        boolean isMissing() {
            return missing;
        }
    }

    public class EIMRecipeTranInfo implements IRecipeTransferInfo<EndlessInventoryMenu,CraftingRecipe>{

        /**
         * Return the container class that this recipe transfer helper supports.
         */
        @Override
        public Class<? extends EndlessInventoryMenu> getContainerClass() {
            return CONTAINER_CLASS;
        }

        /**
         * Return the optional menu type that this recipe transfer helper supports.
         * This is used to optionally narrow down the type of container handled by this recipe transfer info.
         */
        @Override
        public Optional<MenuType<EndlessInventoryMenu>> getMenuType() {
            return Optional.of(CONTAINER_TYPE);
        }

        /**
         * Return the recipe type that this container can handle.
         *
         * @since 9.5.0
         */
        @Override
        public RecipeType<CraftingRecipe> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        /**
         * Return true if this recipe transfer info can handle the given container instance and recipe.
         */
        @Override
        public boolean canHandle(EndlessInventoryMenu container, CraftingRecipe recipe) {
            //if(!container.isCrafterEnabled()) return false;
            return EIMRecipeTranHandler.this.playerInvInfo.canHandle(container,recipe);
        }

        /**
         * Return a list of slots for the recipe area.
         */
        @Override
        public List<Slot> getRecipeSlots(EndlessInventoryMenu container, CraftingRecipe recipe) {
            return container.getCraftingSlots();
        }

        /**
         * Return a list of slots that the transfer can use to get items for crafting, or place leftover items.
         */
        @Override
        public List<Slot> getInventorySlots(EndlessInventoryMenu container, CraftingRecipe recipe) {
            return container.getPlayerInvSlots();
        }
    }
}
