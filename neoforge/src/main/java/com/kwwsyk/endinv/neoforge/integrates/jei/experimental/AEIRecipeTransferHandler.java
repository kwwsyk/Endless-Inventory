package com.kwwsyk.endinv.neoforge.integrates.jei.experimental;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.options.ContentTransferMode;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.neoforge.client.events.ScreenAttachment;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.util.thread.EffectiveSide;

import javax.annotation.Nullable;
import java.util.*;

public final class AEIRecipeTransferHandler {

    private AEIRecipeTransferHandler() {}

    public record TransferContext(
            int containerId,
            Identifier recipeId,
            boolean maxTransfer,
            boolean requireCompleteSets,
            List<Integer> craftingSlotIndexes,
            List<Integer> inventorySlotIndexes
    ) {}

    public static <C extends AbstractContainerMenu, R> Optional<TransferContext> prepareClientContext(
            C container,
            R recipe,
            IRecipeSlotsView recipeSlotsView,
            Player player,
            boolean maxTransfer,
            IRecipeTransferInfo<C, R> transferInfo,
            List<Slot> recipeSlots,
            List<Slot> inventorySlots
    ) {
        SourceInventory endInv = getClientAttachedInventory(container);
        if (endInv == null && com.kwwsyk.endinv.common.options.ServerConfigs.ENDINV_BEHAVIOR.TransferMode.get() == ContentTransferMode.ALL) {
            endInv = CachedSrcInv.INSTANCE;
        }
        Ingredient[] layout = buildLayoutFromRecipeSlots(recipeSlotsView, recipeSlots.size());
        TransferPlan plan = null;
        if (endInv != null) {
            if (allEmpty(layout) && recipe instanceof Recipe<?> mcRecipe) {
                plan = createTransferPlan(mcRecipe, recipeSlots, inventorySlots, endInv, maxTransfer);
            } else {
                plan = createTransferPlan(layout, recipeSlots, inventorySlots, endInv, maxTransfer);
            }
            if (plan.isMissing() || plan.craftsWanted() <= 0) {
                return Optional.empty();
            }
        }
        Identifier recipeId = tryResolveRecipeId(recipe);
        if (recipeId == null) {
            recipeId = Identifier.fromNamespaceAndPath("endless_inventory", "jei/unknown/" + container.containerId);
        }
        boolean requireCompleteSets = transferInfo.requireCompleteSets(container, recipe);
        List<Integer> craftingIndexes = recipeSlots.stream().map(slot -> slot.index).toList();
        List<Integer> inventoryIndexes = inventorySlots.stream().map(slot -> slot.index).toList();
        return Optional.of(new TransferContext(
                container.containerId,
                recipeId,
                maxTransfer,
                requireCompleteSets,
                craftingIndexes,
                inventoryIndexes
        ));
    }

    @Nullable
    private static SourceInventory getClientAttachedInventory(AbstractContainerMenu container) {
        if (EffectiveSide.get() == LogicalSide.SERVER) {
            return null;
        }
        return ClientAccess.getAttachedInventory(container);
    }

    public static void performServerTransfer(
            AbstractContainerMenu container,
            Recipe<?> recipe,
            List<Slot> recipeSlots,
            List<Slot> inventorySlots,
            ServerPlayer player,
            boolean maxTransfer,
            boolean requireCompleteSets
    ) {
        EndlessInventory endInv = ServerLevelEndInv.getEndInvForPlayer(player).orElse(null);
        if (endInv == null) {
            return;
        }
        TransferPlan plan = createTransferPlan(recipe, recipeSlots, inventorySlots, endInv, maxTransfer);
        if (plan.isMissing() || plan.craftsWanted() <= 0) {
            return;
        }
        performTransfer(container, plan, player, recipeSlots, inventorySlots, endInv);
        switch (com.kwwsyk.endinv.common.options.ServerConfigs.ENDINV_BEHAVIOR.TransferMode.get()) {
            case ALL -> ModInfo.getPacketDistributor()
                    .sendToPlayer(player, new com.kwwsyk.endinv.common.network.payloads.toClient.EndInvContent(endInv.getItemMap()));
            case PART -> ModInfo.getPacketDistributor()
                    .sendToPlayer(player, com.kwwsyk.endinv.common.network.payloads.toClient.EndInvMetadata.getWith(endInv));
        }
    }

    private static TransferPlan createTransferPlan(
            Recipe<?> recipe,
            List<Slot> recipeSlots,
            List<Slot> inventorySlots,
            SourceInventory endInv,
            boolean maxTransfer
    ) {
        Ingredient[] layout = buildRecipeLayout(recipe, recipeSlots.size());
        return createTransferPlan(layout, recipeSlots, inventorySlots, endInv, maxTransfer);
    }

    private static TransferPlan createTransferPlan(
            Ingredient[] layout,
            List<Slot> recipeSlots,
            List<Slot> inventorySlots,
            SourceInventory endInv,
            boolean maxTransfer
    ) {
        List<ItemStack> pageItems = endInv.getItemsAsList();
        ItemAvailability availability = ItemAvailability.build(Collections.emptyList(), pageItems);
        Reservation reservation = new Reservation();
        ItemStack[] chosenPerSlot = new ItemStack[layout.length];
        Arrays.fill(chosenPerSlot, ItemStack.EMPTY);
        boolean missing = false;
        int perSlotStackLimit = Integer.MAX_VALUE;

        for (int i = 0; i < layout.length; i++) {
            Ingredient ing = layout[i];
            if (ing == null || ing.isEmpty()) continue;
            Selection selection = chooseBestCandidate(ing, availability, reservation);
            if (selection.isEmpty()) { missing = true; continue; }
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
                if (counts == null) { craftsPossible = 0; missing = true; break; }
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

    private static void performTransfer(
            AbstractContainerMenu container,
            TransferPlan plan,
            Player player,
            List<Slot> recipeSlots,
            List<Slot> inventorySlots,
            SourceInventory endInv
    ) {
        for (Slot cSlot : recipeSlots) {
            if (!cSlot.hasItem()) continue;
            int count = cSlot.getItem().getCount();
            if (count <= 0) continue;
            ItemStack removed = cSlot.safeTake(count, count, player);
            if (!removed.isEmpty()) player.getInventory().placeItemBackInInventory(removed, net.minecraft.util.Prediction.PREDICTED);
        }

        Ingredient[] layout = plan.layout();
        for (int i = 0; i < Math.min(recipeSlots.size(), layout.length); i++) {
            Ingredient ing = layout[i];
            if (ing.isEmpty()) continue;
            Slot target = recipeSlots.get(i);
            int toTake = plan.craftsWanted();
            if (toTake <= 0) continue;

            ItemStack chosen = plan.chosenPerSlot()[i];
            ItemStack placedStack = ItemStack.EMPTY;

            ItemStack template = !chosen.isEmpty() ? chosen : ItemStack.EMPTY;
            if (!template.isEmpty()) {
                ItemStack extracted = endInv.takeItem(template, toTake);
                if (!extracted.isEmpty()) {
                    if (placedStack.isEmpty()) {
                        placedStack = extracted;
                    } else if (sameType(placedStack, extracted)) {
                        placedStack.grow(extracted.getCount());
                    } else {
                        player.getInventory().placeItemBackInInventory(extracted, net.minecraft.util.Prediction.PREDICTED);
                    }
                    toTake -= extracted.getCount();
                }
            }

            if (toTake > 0) {
                if (template.isEmpty()) {
                    ItemStack extracted = endInv.takeFirstPredictedItem(stack -> ing.test(stack), toTake);
                    if (!extracted.isEmpty()) {
                        if (placedStack.isEmpty()) {
                            placedStack = extracted;
                        } else if (sameType(placedStack, extracted)) {
                            placedStack.grow(extracted.getCount());
                        } else {
                            player.getInventory().placeItemBackInInventory(extracted, net.minecraft.util.Prediction.PREDICTED);
                        }
                        toTake -= extracted.getCount();
                    }
                }
                for (Slot invSlot : inventorySlots) {
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
                            player.getInventory().placeItemBackInInventory(taken, net.minecraft.util.Prediction.PREDICTED);
                            break;
                        }
                        toTake -= taken.getCount();
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
        container.broadcastChanges();
    }

    private static boolean sameType(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        return ItemStack.isSameItemSameComponents(a, b);
    }

    private static Ingredient[] buildRecipeLayout(Recipe<?> recipe, int targetSlots) {
        Ingredient[] layout = new Ingredient[targetSlots];
        if (recipe instanceof ShapedRecipe shaped) {
            int gridW = (targetSlots == 9) ? 3 : (targetSlots == 4 ? 2 : shaped.getWidth());
            int gridH = gridW > 0 ? (targetSlots / gridW) : 0;
            int width = Math.min(gridW, shaped.getWidth());
            int height = Math.min(gridH, shaped.getHeight());
            List<?> ingredients = getRecipeIngredients(shaped);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int srcIndex = y * shaped.getWidth() + x;
                    int dstIndex = y * gridW + x;
                    if (srcIndex < ingredients.size() && dstIndex < targetSlots) {
                        Object elem = ingredients.get(srcIndex);
                        if (elem instanceof Ingredient ing) {
                            layout[dstIndex] = ing;
                        } else if (elem instanceof java.util.Optional<?> opt && opt.orElse(null) instanceof Ingredient ing2) {
                            layout[dstIndex] = ing2;
                        }
                    }
                }
            }
        } else {
            List<?> ingredients = getRecipeIngredients(recipe);
            for (int i = 0; i < Math.min(ingredients.size(), layout.length); i++) {
                Object elem = ingredients.get(i);
                if (elem instanceof Ingredient ing) {
                    layout[i] = ing;
                } else if (elem instanceof java.util.Optional<?> opt && opt.orElse(null) instanceof Ingredient ing2) {
                    layout[i] = ing2;
                }
            }
        }
        return layout;
    }

    private static Ingredient[] buildLayoutFromRecipeSlots(IRecipeSlotsView slotsView, int targetSlots) {
        Ingredient[] layout = new Ingredient[targetSlots];
        int i = 0;
        List<IRecipeSlotView> lst = slotsView.getSlotViews(RecipeIngredientRole.INPUT);
        for (IRecipeSlotView slotView1 : lst) {
            if (i >= targetSlots) break;
            List<ItemStack> stacks = slotView1.getIngredients(VanillaTypes.ITEM_STACK).toList();
            if (!stacks.isEmpty()) {
                // Build an Ingredient from the distinct items (item identity only)
                layout[i] = Ingredient.of(stacks.stream().map(ItemStack::getItem));
            }
            i++;
        }
        return layout;
    }

    private static boolean allEmpty(Ingredient[] layout) {
        for (Ingredient ing : layout) {
            if (ing != null && !ing.isEmpty()) return false;
        }
        return true;
    }

    @Nullable
    private static Identifier tryResolveRecipeId(Object recipeObj) {
        try {
            var m = recipeObj.getClass().getMethod("getId");
            Object v = m.invoke(recipeObj);
            if (v instanceof Identifier rl) return rl;
        } catch (Throwable ignored) {}
        try {
            var m = recipeObj.getClass().getMethod("id");
            Object v = m.invoke(recipeObj);
            if (v instanceof Identifier rl) return rl;
        } catch (Throwable ignored) {}
        try {
            var m = recipeObj.getClass().getMethod("value");
            Object v = m.invoke(recipeObj);
            if (v != null) return tryResolveRecipeId(v);
        } catch (Throwable ignored) {}
        return null;
    }

    private static Selection chooseBestCandidate(Ingredient ing, ItemAvailability availability, Reservation reservation) {
        Candidate best = Candidate.NONE;
        Set<ItemKey> seen = new HashSet<>();
        for (ItemCounts counts : availability.all()) {
            if (!ing.test(counts.representative())) continue;
            Candidate candidate = Candidate.fromCounts(counts, reservation);
            if (!candidate.isValid()) continue;
            if (!seen.add(candidate.selection().key())) continue;
            best = Candidate.pickBetter(best, candidate);
        }
        return best.selection();
    }

    @SuppressWarnings("unchecked")
    private static List<?> getRecipeIngredients(Object recipeObj) {
        try {
            var m = recipeObj.getClass().getMethod("getIngredients");
            Object v = m.invoke(recipeObj);
            if (v instanceof List<?> list) return list;
        } catch (Throwable ignored) {}
        try {
            var m = recipeObj.getClass().getMethod("ingredients");
            Object v = m.invoke(recipeObj);
            if (v instanceof List<?> list) return list;
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    @Nullable
    private static ItemCounts countAvailableOfType(ItemStack type, ItemAvailability availability) {
        if (type.isEmpty()) return null;
        return availability.lookup(ItemKey.asKey(type));
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
        private void add(ItemStack stack, Source source) {
            ItemKey key = ItemKey.asKey(stack);
            ItemCounts counts = this.counts.computeIfAbsent(key, k -> new ItemCounts(k, stack.copyWithCount(1)));
            counts.add(stack.getCount(), source);
        }
        @Nullable ItemCounts lookup(ItemKey key) { return counts.get(key); }
        Collection<ItemCounts> all() { return counts.values(); }
        private enum Source { INVENTORY, PAGE }
    }

    private static final class ItemCounts {
        private final ItemKey key; private final ItemStack representative;
        private int inventoryCount; private int pageCount;
        ItemCounts(ItemKey key, ItemStack representative) { this.key = key; this.representative = representative; }
        void add(int amount, ItemAvailability.Source source) { if (source == ItemAvailability.Source.INVENTORY) inventoryCount += amount; else pageCount += amount; }
        ItemKey key() { return key; }
        ItemStack representative() { return representative.copy(); }
        int total() { return inventoryCount + pageCount; }
        int totalRemaining(Reservation reservation) { return total() - reservation.totalReserved(key); }
        int inventoryRemaining(Reservation reservation) { return inventoryCount - reservation.inventoryReserved(key); }
        boolean isPlain() { return key.components()!=null && !key.components().isEmpty(); }
    }

    private static final class Reservation {
        private final Map<ItemKey, Integer> total = new HashMap<>();
        private final Map<ItemKey, Integer> inventory = new HashMap<>();
        void reserve(Selection selection) { if (selection.key() == null) return; total.merge(selection.key(), 1, Integer::sum); if (selection.useInventory()) inventory.merge(selection.key(), 1, Integer::sum); }
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
            if (counts == null) return NONE; int totalRemaining = counts.totalRemaining(reservation); if (totalRemaining <= 0) return NONE;
            int inventoryRemaining = counts.inventoryRemaining(reservation); boolean hasInventory = inventoryRemaining > 0; boolean hasPlainInventory = hasInventory && counts.isPlain();
            int priority = hasPlainInventory ? 3 : (hasInventory ? 2 : 1); Selection selection = new Selection(counts.representative(), counts.key(), hasInventory);
            return new Candidate(selection, priority, totalRemaining, inventoryRemaining);
        }
        static Candidate pickBetter(Candidate current, Candidate challenger) {
            if (!challenger.isValid()) return current; if (!current.isValid()) return challenger;
            if (challenger.priority != current.priority) return challenger.priority > current.priority ? challenger : current;
            if (challenger.totalRemaining != current.totalRemaining) return challenger.totalRemaining > current.totalRemaining ? challenger : current;
            if (challenger.inventoryRemaining != current.inventoryRemaining) return challenger.inventoryRemaining > current.inventoryRemaining ? challenger : current;
            return current;
        }
    }

    private static class ClientAccess {
        @Nullable
        private static SourceInventory getAttachedInventory(AbstractContainerMenu container) {
            var attachment = ScreenAttachment.ATTACHMENT_MANAGER;
            if (attachment == null) {
                if (com.kwwsyk.endinv.common.options.ServerConfigs.ENDINV_BEHAVIOR.TransferMode.get() == ContentTransferMode.ALL) {
                    return CachedSrcInv.INSTANCE;
                }
                return null;
            }
            if (Minecraft.getInstance().player == null) {
                return null;
            }
            if (attachment.getScreen().getMenu() != container) {
                return null;
            }
            return attachment.getFrameWork().getSourceInventory();
        }
    }
}
