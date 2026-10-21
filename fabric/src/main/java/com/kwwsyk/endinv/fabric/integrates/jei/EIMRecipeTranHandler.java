package com.kwwsyk.endinv.fabric.integrates.jei;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.fabric.network.payloads.JeiTransferRecipePayload;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** {@link EndlessInventoryMenu}'s recipe transfer handler.
 *
 */
@SuppressWarnings({"rawtypes"})
public class EIMRecipeTranHandler implements IRecipeTransferHandler<EndlessInventoryMenu, RecipeHolder<CraftingRecipe>> {

    private static final Class<EndlessInventoryMenu> CONTAINER_CLASS = EndlessInventoryMenu.class;
    //is that dangerous?
    private static final MenuType<EndlessInventoryMenu> CONTAINER_TYPE = ModRegistries.Menus.getEndInvMenuType();


    private final IJeiHelpers jeiHelper;
    private final IRecipeTransferHandlerHelper transferHelper;
    private final IRecipeTransferInfo playerInvInfo;

    public EIMRecipeTranHandler(IJeiHelpers jeiHelper,IRecipeTransferHandlerHelper transferHelper){
        this.jeiHelper = jeiHelper;
        this.transferHelper = transferHelper;
        playerInvInfo = createPlayerInvInfo();
    }

    private IRecipeTransferInfo createPlayerInvInfo(){
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
    public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
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
                                                         RecipeHolder<CraftingRecipe> recipe,
                                                         IRecipeSlotsView recipeSlots,
                                                         Player player, boolean maxTransfer, boolean doTransfer) {
        try {
            if (!new EIMRecipeTranInfo().canHandle(container, recipe)) {
                return transferHelper.createUserErrorWithTooltip(Component.literal("Unsupported container or recipe"));
            }

            if (!container.isCrafterEnabled()) {
                return transferHelper.createUserErrorWithTooltip(Component.literal("Crafter is disabled by server rules"));
            }

            TransferPlan plan = createTransferPlan(container, recipe, maxTransfer);
            boolean missing = plan.isMissing();

            if (!doTransfer) {
                return missing ? transferHelper.createUserErrorWithTooltip(
                        Component.literal("Missing required ingredients")) : null;
            }

            if (missing) {
                return null;
            }

            net.minecraft.resources.Identifier id = tryResolveRecipeId(recipe);
            if (id == null) {
                return transferHelper.createInternalError();
            }

            ModInfo.getPacketDistributor().sendToServer(new JeiTransferRecipePayload(container.containerId, id, maxTransfer));
            return null;
        } catch (Throwable t) {
            return transferHelper.createInternalError();
        }
    }

    public static void performServerTransfer(EndlessInventoryMenu container, RecipeHolder<CraftingRecipe> recipe, Player player, boolean maxTransfer) {
        TransferPlan plan = createTransferPlan(container, recipe, maxTransfer);
        performTransfer(container, recipe, plan, player);
    }

    private static boolean sameType(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        return ItemStack.isSameItemSameComponents(a, b);
    }

    private static Ingredient[] buildRecipeLayout(CraftingRecipe recipe) {
        Ingredient[] layout = new Ingredient[9];
        if (recipe instanceof ShapedRecipe shaped) {
            int width = Math.min(3, shaped.getWidth());
            int height = Math.min(3, shaped.getHeight());
            List<Ingredient> ingredients = getRecipeIngredients(shaped);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int srcIndex = y * shaped.getWidth() + x;
                    if (srcIndex < ingredients.size()) {
                        layout[y * 3 + x] = ingredients.get(srcIndex);
                    }
                }
            }
        } else {
            List<Ingredient> ingredients = getRecipeIngredients(recipe);
            for (int i = 0; i < Math.min(ingredients.size(), layout.length); i++) {
                layout[i] = ingredients.get(i);
            }
        }
        return layout;
    }

    @SuppressWarnings("unchecked")
    private static List<Ingredient> getRecipeIngredients(Object recipeObj) {
        try {
            var m = recipeObj.getClass().getMethod("getIngredients");
            Object v = m.invoke(recipeObj);
            if (v instanceof List<?> list) return (List<Ingredient>) list;
        } catch (Throwable ignored) {}
        try {
            var m = recipeObj.getClass().getMethod("ingredients");
            Object v = m.invoke(recipeObj);
            if (v instanceof List<?> list) return (List<Ingredient>) list;
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    @Nullable
    private static ItemCounts countAvailableOfType(ItemStack type, ItemAvailability availability) {
        if (type.isEmpty()) return null;
        return availability.lookup(ItemKey.asKey(type));
    }

    private static Selection chooseBestCandidate(Ingredient ing, ItemAvailability availability, Reservation reservation) {
        Candidate best = Candidate.NONE;
        Set<ItemKey> seen = new HashSet<>();

        for (ItemCounts counts : availability.all()) {
            if (!ing.test(counts.representative())) continue;
            Candidate candidate = Candidate.fromCounts(counts, reservation);
            if (!candidate.isValid()) continue;assert candidate.selection!=null;
            if (!seen.add(candidate.selection().key())) continue;
            best = Candidate.pickBetter(best, candidate);
        }
        assert best.selection!=null;
        return best.selection();
    }

    private static TransferPlan createTransferPlan(EndlessInventoryMenu container, RecipeHolder<CraftingRecipe> recipe, boolean maxTransfer) {
        Ingredient[] layout = buildRecipeLayout(recipe.value());
        List<ItemStack> playerInvItems = new java.util.ArrayList<>();
        var inv = container.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            playerInvItems.add(inv.getItem(i));
        }
        List<ItemStack> pageItems = container.getSourceInventory().getItemsAsList();

        ItemAvailability availability = ItemAvailability.buildFromItems(playerInvItems, pageItems);
        Reservation reservation = new Reservation();
        ItemStack[] chosenPerSlot = new ItemStack[layout.length];
        Arrays.fill(chosenPerSlot, ItemStack.EMPTY);

        boolean missing = false;
        int perSlotStackLimit = Integer.MAX_VALUE;

        for (int i = 0; i < layout.length; i++) {
            Ingredient ing = layout[i];
            if (ing == null || ing.isEmpty()) continue;
            Selection selection = chooseBestCandidate(ing, availability, reservation);
            if (selection.isEmpty()) {
                missing = true;
                continue;
            }
            reservation.reserve(selection);
            chosenPerSlot[i] = selection.stack();
            perSlotStackLimit = Math.min(perSlotStackLimit, selection.stack().getMaxStackSize());
        }

        int craftsPossible;
        if (missing || reservation.isEmpty()) {
            craftsPossible = 0;
        } else {
            craftsPossible = Integer.MAX_VALUE;
            for (Map.Entry<ItemKey, Integer> entry : reservation.totalDemand().entrySet()) {
                ItemCounts counts = availability.lookup(entry.getKey());
                if (counts == null) {
                    craftsPossible = 0;
                    missing = true;
                    break;
                }
                int possible = counts.total() / entry.getValue();
                craftsPossible = Math.min(craftsPossible, possible);
            }
            if (craftsPossible == Integer.MAX_VALUE) craftsPossible = 0;
        }

        if (perSlotStackLimit == Integer.MAX_VALUE) perSlotStackLimit = 64;

        int craftsWanted = maxTransfer ? Math.min(craftsPossible, perSlotStackLimit) : (craftsPossible > 0 ? 1 : 0);
        boolean finalMissing = missing || craftsWanted <= 0;
        return new TransferPlan(layout, chosenPerSlot, craftsWanted, finalMissing);
    }

    private static void performTransfer(EndlessInventoryMenu container, Object recipe, TransferPlan plan, Player player) {
        container.setCraftingVisible(true);

        List<Slot> craftingSlots = container.getCraftingSlots();
        for (Slot cSlot : craftingSlots) {
            if (!cSlot.hasItem()) continue;
            int count = cSlot.getItem().getCount();
            if (count <= 0) continue;
            ItemStack removed = cSlot.safeTake(count, count, player);
            if (!removed.isEmpty()) player.getInventory().placeItemBackInInventory(removed, net.minecraft.util.Prediction.PREDICTED);
        }

        List<Slot> playerInvSlots = container.getPlayerInvSlots();
        Ingredient[] layout = plan.layout();

        for (int i = 0; i < Math.min(craftingSlots.size(), layout.length); i++) {
            Ingredient ing = layout[i];
            if (ing == null || ing.isEmpty()) continue;
            Slot target = craftingSlots.get(i);

            int toTake = plan.craftsWanted();
            if (toTake <= 0) continue;

            ItemStack chosen = plan.chosenPerSlot()[i];
            ItemStack placedStack = ItemStack.EMPTY;

            for (Slot invSlot : playerInvSlots) {
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
                        player.getInventory().placeItemBackInInventory(taken, net.minecraft.util.Prediction.PREDICTED);
                        break;
                    }
                    toTake -= taken.getCount();
                }
            }

            if (toTake > 0) {
                ItemStack extracted;
                if (!chosen.isEmpty()) {
                    extracted = container.tryExtractFromPage(chosen, toTake);
                } else {
                    extracted = container.getSourceInventory().takeFirstPredictedItem(ing, toTake);
                }
                if (!extracted.isEmpty()) {
                    if (placedStack.isEmpty()) {
                        placedStack = extracted;
                    } else if (sameType(placedStack, extracted)) {
                        placedStack.grow(extracted.getCount());
                    } else {
                        player.getInventory().placeItemBackInInventory(extracted, net.minecraft.util.Prediction.PREDICTED);
                    }
                }
            }

            if (!placedStack.isEmpty()) {
                int cap = Math.min(placedStack.getMaxStackSize(), target.getMaxStackSize());
                if (placedStack.getCount() > cap) {
                    ItemStack overflow = placedStack.copyWithCount(placedStack.getCount() - cap);
                    placedStack.setCount(cap);
                    player.getInventory().placeItemBackInInventory(overflow, net.minecraft.util.Prediction.PREDICTED);
                }
                target.set(placedStack);
            }
        }
    }

    private record TransferPlan(Ingredient[] layout, ItemStack[] chosenPerSlot, int craftsWanted, boolean missing) {
        boolean isMissing() { return missing; }
    }

    private static final class ItemAvailability {
        private final Map<ItemKey, ItemCounts> counts = new HashMap<>();

        static ItemAvailability build(List<Slot> invSlots, List<ItemStack> pageItems) {
            ItemAvailability availability = new ItemAvailability();
            for (Slot slot : invSlots) if (slot.hasItem()) availability.add(slot.getItem(), Source.INVENTORY);
            for (ItemStack stack : pageItems) if (!stack.isEmpty()) availability.add(stack, Source.PAGE);
            return availability;
        }

        static ItemAvailability buildFromItems(List<ItemStack> invItems, List<ItemStack> pageItems) {
            ItemAvailability availability = new ItemAvailability();
            for (ItemStack stack : invItems) if (!stack.isEmpty()) availability.add(stack, Source.INVENTORY);
            for (ItemStack stack : pageItems) if (!stack.isEmpty()) availability.add(stack, Source.PAGE);
            return availability;
        }

        void add(ItemStack stack, Source source) {
            ItemKey key = ItemKey.asKey(stack);
            ItemCounts c = counts.computeIfAbsent(key, k -> new ItemCounts(k));
            c.add(stack.getCount(), source);
        }

        ItemCounts lookup(ItemKey key) { return counts.get(key); }
        Collection<ItemCounts> all() { return counts.values(); }

        enum Source { INVENTORY, PAGE }
    }

    private static final class ItemCounts {
        private final ItemKey key;
        private int fromInventory;
        private int fromPage;

        ItemCounts(ItemKey key) { this.key = key; }

        void add(int count, ItemAvailability.Source source) {
            if (source == ItemAvailability.Source.INVENTORY) fromInventory += count;
            else fromPage += count;
        }

        int total() { return fromInventory + fromPage; }
        int totalRemaining(Reservation reservation) { return total() - reservation.totalReserved(key); }
        int inventoryRemaining(Reservation reservation) { return fromInventory - reservation.inventoryReserved(key); }
        ItemKey key() { return key; }
        ItemStack representative() { return key.toStack(1); }
        boolean isPlain() { return key.components() != null && !key.components().isEmpty(); }
    }

    private static final class Reservation {
        private final Map<ItemKey, Integer> total = new HashMap<>();
        private final Map<ItemKey, Integer> inventory = new HashMap<>();

        void reserve(Selection selection) {
            total.merge(selection.key(), 1, Integer::sum);
            if (selection.useInventory()) inventory.merge(selection.key(), 1, Integer::sum);
        }

        int totalReserved(ItemKey key) { return total.getOrDefault(key, 0); }
        int inventoryReserved(ItemKey key) { return inventory.getOrDefault(key, 0); }
        Map<ItemKey, Integer> totalDemand() { return total; }
        boolean isEmpty() { return total.isEmpty(); }
    }

    private record Selection(ItemStack stack, ItemKey key, boolean useInventory) {
        static final Selection EMPTY = new Selection(ItemStack.EMPTY, null, false);
        boolean isEmpty() { return stack.isEmpty(); }
    }

    private record Candidate(Selection selection, int priority, int totalRemaining, int inventoryRemaining) {
        private static final Candidate NONE = new Candidate(Selection.EMPTY, -1, 0, 0);

        boolean isValid() { return selection != null && !selection.isEmpty(); }

        static Candidate fromCounts(@Nullable ItemCounts counts, Reservation reservation) {
            if (counts == null) return NONE;
            int totalRemaining = counts.totalRemaining(reservation);
            if (totalRemaining <= 0) return NONE;
            int inventoryRemaining = counts.inventoryRemaining(reservation);
            boolean hasInventory = inventoryRemaining > 0;
            boolean hasPlainInventory = hasInventory && counts.isPlain();
            int priority = hasPlainInventory ? 3 : (hasInventory ? 2 : 1);
            Selection selection = new Selection(counts.representative(), counts.key(), hasInventory);
            return new Candidate(selection, priority, totalRemaining, inventoryRemaining);
        }

        static Candidate pickBetter(Candidate current, Candidate challenger) {
            if (!challenger.isValid()) return current;
            if (!current.isValid()) return challenger;
            if (challenger.priority != current.priority) return challenger.priority > current.priority ? challenger : current;
            if (challenger.totalRemaining != current.totalRemaining) return challenger.totalRemaining > current.totalRemaining ? challenger : current;
            if (challenger.inventoryRemaining != current.inventoryRemaining) return challenger.inventoryRemaining > current.inventoryRemaining ? challenger : current;
            return current;
        }
    }

    public class EIMRecipeTranInfo implements IRecipeTransferInfo{
        @Override
        public Class<? extends EndlessInventoryMenu> getContainerClass() { return CONTAINER_CLASS; }

        @Override
        public Optional<MenuType<EndlessInventoryMenu>> getMenuType() { return Optional.of(CONTAINER_TYPE); }

        @Override
        public IRecipeType getRecipeType() { return RecipeTypes.CRAFTING; }

        @Override
        public boolean canHandle(net.minecraft.world.inventory.AbstractContainerMenu container, Object recipe) {
            return container instanceof EndlessInventoryMenu;
        }

        @Override
        public List<Slot> getRecipeSlots(net.minecraft.world.inventory.AbstractContainerMenu container, Object recipe) {
            return ((EndlessInventoryMenu)container).getCraftingSlots();
        }

        @Override
        public List<Slot> getInventorySlots(net.minecraft.world.inventory.AbstractContainerMenu container, Object recipe) {
            return ((EndlessInventoryMenu)container).getPlayerInvSlots();
        }
    }

    @org.jetbrains.annotations.Nullable
    private static net.minecraft.resources.Identifier tryResolveRecipeId(Object recipeObj) {
        try {
            var m = recipeObj.getClass().getMethod("getId");
            Object v = m.invoke(recipeObj);
            if (v instanceof net.minecraft.resources.Identifier rl) return rl;
        } catch (Throwable ignored) {}
        try {
            var m = recipeObj.getClass().getMethod("id");
            Object v = m.invoke(recipeObj);
            if (v instanceof net.minecraft.resources.Identifier rl) return rl;
        } catch (Throwable ignored) {}
        try {
            var m = recipeObj.getClass().getMethod("value");
            Object v = m.invoke(recipeObj);
            if (v != null) return tryResolveRecipeId(v);
        } catch (Throwable ignored) {}
        return null;
    }
}
