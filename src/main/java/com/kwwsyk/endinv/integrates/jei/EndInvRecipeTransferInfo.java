package com.kwwsyk.endinv.integrates.jei;

import com.kwwsyk.endinv.SourceInventory;
import com.kwwsyk.endinv.client.CachedSrcInv;
import com.kwwsyk.endinv.client.events.ScreenAttachment;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EndInvRecipeTransferInfo<C extends AbstractContainerMenu, R> implements IRecipeTransferInfo<C, R> {
	private final Class<? extends C> containerClass;
	@Nullable
	private final MenuType<C> menuType;
	private final RecipeType<R> recipeType;
	private final int recipeSlotStart;
	private final int recipeSlotCount;
	private final int inventorySlotStart;
	private final int inventorySlotCount;

	public EndInvRecipeTransferInfo(
		Class<? extends C> containerClass,
		@Nullable MenuType<C> menuType,
		RecipeType<R> recipeType,
		int recipeSlotStart,
		int recipeSlotCount,
		int inventorySlotStart,
		int inventorySlotCount
	) {
		this.containerClass = containerClass;
		this.menuType = menuType;
		this.recipeType = recipeType;
		this.recipeSlotStart = recipeSlotStart;
		this.recipeSlotCount = recipeSlotCount;
		this.inventorySlotStart = inventorySlotStart;
		this.inventorySlotCount = inventorySlotCount;
	}

	@Override
	public Class<? extends C> getContainerClass() {
		return containerClass;
	}

	@Override
	public Optional<MenuType<C>> getMenuType() {
		return Optional.ofNullable(menuType);
	}

	@Override
	public RecipeType<R> getRecipeType() {
		return recipeType;
	}

	@Override
	public boolean canHandle(C container, R recipe) {
		return true;
	}

	@Override
	public List<Slot> getRecipeSlots(C container, R recipe) {
		List<Slot> slots = new ArrayList<>();
		for (int i = recipeSlotStart; i < recipeSlotStart + recipeSlotCount; i++) {
			Slot slot = container.getSlot(i);
			slots.add(slot);
		}
		return slots;
	}

	@Override
	public List<Slot> getInventorySlots(C container, R recipe) {

		List<Slot> slots = new ArrayList<>();
		for (int i = inventorySlotStart; i < inventorySlotStart + inventorySlotCount; i++) {
			Slot slot = container.getSlot(i);
			slots.add(slot);
		}

		Optional<SourceInventory> optional = getAttachedSrcInv(container);
		if(optional.isEmpty()){
            return slots;
        }


		return slots;
	}

	private Optional<SourceInventory> getAttachedSrcInv(C container){
		if(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> acs && Objects.equals(acs.getMenu(),container)){
			var srcInv = ScreenAttachment.ATTACHMENT_MANAGER.get(acs).getPageMetadata().getSourceInventory();
			if(!Objects.equals(srcInv, CachedSrcInv.INSTANCE)) return Optional.empty();
			return Optional.of(srcInv);
		}
		return Optional.empty();
	}
}
