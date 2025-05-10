package com.kwwsyk.endinv.integrates.jei;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EndInvRecipeTransferHandler<C extends AbstractContainerMenu, R> implements IRecipeTransferHandler<C, R> {
	private static final Logger LOGGER = LogManager.getLogger();

	private final IRecipeTransferHandlerHelper handlerHelper;
	private final IRecipeTransferInfo<C, R> transferInfo;

	public EndInvRecipeTransferHandler(
		IRecipeTransferHandlerHelper handlerHelper,
		IRecipeTransferInfo<C, R> transferInfo
	) {
		this.handlerHelper = handlerHelper;
		this.transferInfo = transferInfo;
	}

	@Override
	public Class<? extends C> getContainerClass() {
		return transferInfo.getContainerClass();
	}

	@Override
	public Optional<MenuType<C>> getMenuType() {
		return transferInfo.getMenuType();
	}

	@Override
	public RecipeType<R> getRecipeType() {
		return transferInfo.getRecipeType();
	}

	@Nullable
	@Override
	public IRecipeTransferError transferRecipe(C container, R recipe, IRecipeSlotsView recipeSlotsView, Player player, boolean maxTransfer, boolean doTransfer) {



		if (!transferInfo.canHandle(container, recipe)) {
			IRecipeTransferError handlingError = transferInfo.getHandlingError(container, recipe);
			if (handlingError != null) {
				return handlingError;
			}
			return handlerHelper.createInternalError();
		}

		List<Slot> craftingSlots = Collections.unmodifiableList(transferInfo.getRecipeSlots(container, recipe));
		List<Slot> inventorySlots = Collections.unmodifiableList(transferInfo.getInventorySlots(container, recipe));
		if (!validateTransferInfo(transferInfo, container, craftingSlots, inventorySlots)) {
			return handlerHelper.createInternalError();
		}

		List<IRecipeSlotView> inputItemSlotViews = recipeSlotsView.getSlotViews(RecipeIngredientRole.INPUT);
		if (!validateRecipeView(transferInfo, container, craftingSlots, inputItemSlotViews)) {
			return handlerHelper.createInternalError();
		}

		InventoryState inventoryState = getInventoryState(craftingSlots, inventorySlots, player, container, transferInfo);
		if (inventoryState == null) {
			return handlerHelper.createInternalError();
		}

		// check if we have enough inventory space to shuffle items around to their final locations
		int inputCount = inputItemSlotViews.size();
		if (!inventoryState.hasRoom(inputCount)) {
			Component message = Component.translatable("jei.tooltip.error.recipe.transfer.inventory.full");
			return handlerHelper.createUserErrorWithTooltip(message);
		}

		return null;
	}

	public static <C extends AbstractContainerMenu, R> boolean validateTransferInfo(
		IRecipeTransferInfo<C, R> transferInfo,
		C container,
		List<Slot> craftingSlots,
		List<Slot> inventorySlots
	) {
		for (Slot slot : craftingSlots) {
			if (slot.isFake()) {
				LOGGER.error("Recipe Transfer helper {} does not work for container {}. " +
						"The Recipe Transfer Helper references crafting slot index [{}] but it is a fake (output) slot, which is not allowed.",
					transferInfo.getClass(), container.getClass(), slot.index
				);
				return false;
			}
		}
		for (Slot slot : inventorySlots) {
			if (slot.isFake()) {
				LOGGER.error("Recipe Transfer helper {} does not work for container {}. " +
						"The Recipe Transfer Helper references inventory slot index [{}] but it is a fake (output) slot, which is not allowed.",
					transferInfo.getClass(), container.getClass(), slot.index
				);
				return false;
			}
		}
		Collection<Integer> craftingSlotIndexes = slotIndexes(craftingSlots);
		Collection<Integer> inventorySlotIndexes = slotIndexes(inventorySlots);
		Collection<Integer> containerSlotIndexes = slotIndexes(container.slots);

		if (!containerSlotIndexes.containsAll(craftingSlotIndexes)) {
			LOGGER.error("Recipe Transfer helper does not work for container . " +
					"The Recipes Transfer Helper references crafting slot indexes that are not found in the inventory container slots"
			);
			return false;
		}

		if (!containerSlotIndexes.containsAll(inventorySlotIndexes)) {
			LOGGER.error("Recipe Transfer helper does not work for container. " +
					"The Recipes Transfer Helper references inventory slot indexes that are not found in the inventory container slots"
			);
			return false;
		}

		return true;
	}

	public static <C extends AbstractContainerMenu, R> boolean validateRecipeView(
		IRecipeTransferInfo<C, R> transferInfo,
		C container,
		List<Slot> craftingSlots,
		List<IRecipeSlotView> inputSlots
	) {
		if (inputSlots.size() > craftingSlots.size()) {
			LOGGER.error("Recipe View {} does not work for container {}. " +
					"The Recipe View has more input slots ({}) than the number of inventory crafting slots ({})",
				transferInfo.getClass(), container.getClass(), inputSlots.size(), craftingSlots.size()
			);
			return false;
		}

		return true;
	}

	public static Set<Integer> slotIndexes(Collection<Slot> slots) {
		Set<Integer> set = new IntOpenHashSet(slots.size());
		for (Slot s : slots) {
			set.add(s.index);
		}
		return set;
	}

	@Nullable
	public static <C extends AbstractContainerMenu, R> InventoryState getInventoryState(
		Collection<Slot> craftingSlots,
		Collection<Slot> inventorySlots,
		Player player,
		C container,
		IRecipeTransferInfo<C, R> transferInfo
	) {
		Map<Slot, ItemStack> availableItemStacks = new HashMap<>();
		int filledCraftSlotCount = 0;
		int emptySlotCount = 0;

		for (Slot slot : craftingSlots) {
			final ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				if (!slot.allowModification(player)) {
					LOGGER.error(
						"Recipe Transfer helper {} does not work for container {}. " +
							"The Player is not able to move items out of Crafting Slot number {}",
						transferInfo.getClass(), container.getClass(), slot.index
					);
					return null;
				}
				filledCraftSlotCount++;
				availableItemStacks.put(slot, stack.copy());
			}
		}

		for (Slot slot : inventorySlots) {
			final ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				if (!slot.allowModification(player)) {
					LOGGER.error(
						"Recipe Transfer helper {} does not work for container {}. " +
							"The Player is not able to move items out of Inventory Slot number {}",
						transferInfo.getClass(), container.getClass(), slot.index
					);
					return null;
				}
				availableItemStacks.put(slot, stack.copy());
			} else {
				emptySlotCount++;
			}
		}

		return new InventoryState(availableItemStacks, filledCraftSlotCount, emptySlotCount);
	}

	public record InventoryState(
		Map<Slot, ItemStack> availableItemStacks,
		int filledCraftSlotCount,
		int emptySlotCount
	) {
		/**
		 * check if we have enough inventory space to shuffle items around to their final locations
		 */
		public boolean hasRoom(int inputCount) {
			return filledCraftSlotCount - inputCount <= emptySlotCount;
		}
	}
}
