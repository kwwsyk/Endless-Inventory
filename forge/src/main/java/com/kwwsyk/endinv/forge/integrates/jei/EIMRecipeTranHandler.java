package com.kwwsyk.endinv.forge.integrates.jei;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.util.ItemKey;
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
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

    // The transfer logic aligns shaped recipes with the crafting grid and caches item availability so that
    // we only scan the inventory once per transfer. The candidate selection also prefers items from the
    // player's inventory when possible to avoid unnecessary page extractions.
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
                container.setCraftingVisible(true);//is crafter visible on server now? or needn't consider it
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

    private static Ingredient[] buildRecipeLayout(CraftingRecipe recipe) {
        Ingredient[] layout = new Ingredient[9];
        Arrays.fill(layout, Ingredient.EMPTY);
        if (recipe instanceof ShapedRecipe shaped) {
            int width = Math.min(3, shaped.getWidth());
            int height = Math.min(3, shaped.getHeight());
            List<Ingredient> ingredients = shaped.getIngredients();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int srcIndex = y * shaped.getWidth() + x;
                    if (srcIndex < ingredients.size()) {
                        layout[y * 3 + x] = ingredients.get(srcIndex);
                    }
                }
            }
        } else {
            List<Ingredient> ingredients = recipe.getIngredients();
            for (int i = 0; i < Math.min(ingredients.size(), layout.length); i++) {
                layout[i] = ingredients.get(i);
            }
        }
        return layout;
    }

    /**
     * Looks up the cached availability information for the requested stack type.
     * The result exposes both the total amount and how many items remain in the player's inventory,
     * so candidate selection can prefer inventory items over pulling from the page.
     */
    @Nullable
    private static ItemCounts countAvailableOfType(ItemStack type, ItemAvailability availability) {
        if (type.isEmpty()) {
            return null;
        }
        return availability.lookup(ItemKey.asKey(type));
    }

    /**
     * Selects the most appropriate stack template for the given ingredient.
     * The search first considers the ingredient's declared stacks, then falls back to any matching stack
     * seen in the player's inventory or the page. Each option is evaluated against the cached availability:
     *  - prefer plain items from the player's inventory,
     *  - then other inventory items,
     *  - finally fall back to page items.
     * Reservation state prevents us from picking the same stack type more times than it actually exists.
     */
    private static Selection chooseBestCandidate(Ingredient ing, ItemAvailability availability, Reservation reservation) {
        Candidate best = Candidate.NONE;
        Set<ItemKey> seen = new HashSet<>();

        for (ItemStack cand : ing.getItems()) {
            ItemCounts counts = countAvailableOfType(cand, availability);
            Candidate candidate = Candidate.fromCounts(counts, reservation);
            if (!candidate.isValid()) {
                continue;
            }
            if (!seen.add(candidate.selection().key())) {
                continue;
            }
            best = Candidate.pickBetter(best, candidate);
        }

        for (ItemCounts counts : availability.all()) {
            if (!ing.test(counts.representative())) {
                continue;
            }
            Candidate candidate = Candidate.fromCounts(counts, reservation);
            if (!candidate.isValid()) {
                continue;
            }
            if (!seen.add(candidate.selection().key())) {
                continue;
            }
            best = Candidate.pickBetter(best, candidate);
        }

        return best.selection();
    }

    private static TransferPlan createTransferPlan(EndlessInventoryMenu container, CraftingRecipe recipe, boolean maxTransfer) {
        Ingredient[] layout = buildRecipeLayout(recipe);
        List<Slot> playerInvSlots = container.getPlayerInvSlots();
        List<ItemStack> pageItems = container.getSourceInventory().getItemsAsList();

        ItemAvailability availability = ItemAvailability.build(playerInvSlots, pageItems);
        Reservation reservation = new Reservation();
        ItemStack[] chosenPerSlot = new ItemStack[layout.length];
        Arrays.fill(chosenPerSlot, ItemStack.EMPTY);

        boolean missing = false;
        int perSlotStackLimit = Integer.MAX_VALUE;

        for (int i = 0; i < layout.length; i++) {
            Ingredient ing = layout[i];
            if (ing.isEmpty()) {
                continue;
            }
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
            if (craftsPossible == Integer.MAX_VALUE) {
                craftsPossible = 0;
            }
        }

        if (perSlotStackLimit == Integer.MAX_VALUE) {
            perSlotStackLimit = 64;
        }

        int craftsWanted = maxTransfer ? Math.min(craftsPossible, perSlotStackLimit) : (craftsPossible > 0 ? 1 : 0);
        boolean finalMissing = missing || craftsWanted <= 0;
        return new TransferPlan(layout, chosenPerSlot, craftsWanted, finalMissing);
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

        List<Slot> playerInvSlots = container.getPlayerInvSlots();
        Ingredient[] layout = plan.layout();

        for (int i = 0; i < Math.min(craftingSlots.size(), layout.length); i++) {
            Ingredient ing = layout[i];
            if (ing.isEmpty()) continue;
            Slot target = craftingSlots.get(i);

            int toTake = plan.craftsWanted();
            if (toTake <= 0) continue;

            ItemStack chosen = plan.chosenPerSlot()[i];
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

    private record TransferPlan(Ingredient[] layout, ItemStack[] chosenPerSlot, int craftsWanted, boolean missing) {
        boolean isMissing() {
            return missing;
        }
    }

    private static final class ItemAvailability {
        private final Map<ItemKey, ItemCounts> counts = new HashMap<>();

        private ItemAvailability() {
        }

        static ItemAvailability build(List<Slot> invSlots, List<ItemStack> pageItems) {
            ItemAvailability availability = new ItemAvailability();
            for (Slot slot : invSlots) {
                if (!slot.hasItem()) continue;
                availability.add(slot.getItem(), Source.INVENTORY);
            }
            for (ItemStack stack : pageItems) {
                if (stack.isEmpty()) continue;
                availability.add(stack, Source.PAGE);
            }
            return availability;
        }

        private void add(ItemStack stack, Source source) {
            ItemKey key = ItemKey.asKey(stack);
            ItemCounts counts = this.counts.computeIfAbsent(key, k -> new ItemCounts(k, stack.copyWithCount(1)));
            counts.add(stack.getCount(), source);
        }

        @Nullable
        ItemCounts lookup(ItemKey key) {
            return counts.get(key);
        }

        Collection<ItemCounts> all() {
            return counts.values();
        }

        private enum Source {
            INVENTORY,
            PAGE
        }
    }

    private static final class ItemCounts {
        private final ItemKey key;
        private final ItemStack representative;
        private int inventoryCount;
        private int pageCount;

        ItemCounts(ItemKey key, ItemStack representative) {
            this.key = key;
            this.representative = representative;
        }

        void add(int amount, ItemAvailability.Source source) {
            if (source == ItemAvailability.Source.INVENTORY) {
                inventoryCount += amount;
            } else {
                pageCount += amount;
            }
        }

        ItemKey key() {
            return key;
        }

        ItemStack representative() {
            return representative.copy();
        }

        int total() {
            return inventoryCount + pageCount;
        }

        int totalRemaining(Reservation reservation) {
            return total() - reservation.totalReserved(key);
        }

        int inventoryRemaining(Reservation reservation) {
            return inventoryCount - reservation.inventoryReserved(key);
        }

        boolean isPlain() {
            return key.tag() == null;
        }
    }

    private static final class Reservation {
        private final Map<ItemKey, Integer> total = new HashMap<>();
        private final Map<ItemKey, Integer> inventory = new HashMap<>();

        void reserve(Selection selection) {
            if (selection.key() == null) return;
            total.merge(selection.key(), 1, Integer::sum);
            if (selection.useInventory()) {
                inventory.merge(selection.key(), 1, Integer::sum);
            }
        }

        int totalReserved(ItemKey key) {
            return total.getOrDefault(key, 0);
        }

        int inventoryReserved(ItemKey key) {
            return inventory.getOrDefault(key, 0);
        }

        Map<ItemKey, Integer> totalDemand() {
            return total;
        }

        boolean isEmpty() {
            return total.isEmpty();
        }
    }

    private record Selection(ItemStack stack, ItemKey key, boolean useInventory) {
        static final Selection EMPTY = new Selection(ItemStack.EMPTY, null, false);

        boolean isEmpty() {
            return stack.isEmpty();
        }
    }

    private record Candidate(Selection selection, int priority, int totalRemaining, int inventoryRemaining) {
        private static final Candidate NONE = new Candidate(Selection.EMPTY, -1, 0, 0);

        boolean isValid() {
            return selection != null && !selection.isEmpty();
        }

        static Candidate fromCounts(@Nullable ItemCounts counts, Reservation reservation) {
            if (counts == null) {
                return NONE;
            }
            int totalRemaining = counts.totalRemaining(reservation);
            if (totalRemaining <= 0) {
                return NONE;
            }
            int inventoryRemaining = counts.inventoryRemaining(reservation);
            boolean hasInventory = inventoryRemaining > 0;
            boolean hasPlainInventory = hasInventory && counts.isPlain();
            int priority = hasPlainInventory ? 3 : (hasInventory ? 2 : 1);
            Selection selection = new Selection(counts.representative(), counts.key(), hasInventory);
            return new Candidate(selection, priority, totalRemaining, inventoryRemaining);
        }

        static Candidate pickBetter(Candidate current, Candidate challenger) {
            if (!challenger.isValid()) {
                return current;
            }
            if (!current.isValid()) {
                return challenger;
            }
            if (challenger.priority != current.priority) {
                return challenger.priority > current.priority ? challenger : current;
            }
            if (challenger.totalRemaining != current.totalRemaining) {
                return challenger.totalRemaining > current.totalRemaining ? challenger : current;
            }
            if (challenger.inventoryRemaining != current.inventoryRemaining) {
                return challenger.inventoryRemaining > current.inventoryRemaining ? challenger : current;
            }
            return current;
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
