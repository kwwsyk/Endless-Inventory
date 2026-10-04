package com.kwwsyk.endinv.fabric.mixin;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.network.payloads.toClient.ItemPickedUpPayload;
import com.kwwsyk.endinv.common.options.ServerConfigs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void endlessinv$autopick(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!ServerConfigs.PICKUP_HELPER.ITEM_DROPS.PICK_TO_ENDINV.get()) return;
        if (!isPlayerEnabledAutoPick(serverPlayer)) return;

        ItemEntity self = (ItemEntity) (Object) this;
        if (self.hasPickUpDelay()) return;

        ItemStack stack = self.getItem();
        if (stack.isEmpty()) return;

        if (shouldMoveTo(player, stack)) {
            ServerLevelEndInv.getEndInvForPlayer(serverPlayer).ifPresent(endInv -> {
                ItemStack remain = endInv.addItem(stack.copy());
                if (!stack.isEmpty()) {
                    ModInfo.getPacketDistributor().sendToPlayer(serverPlayer, new ItemPickedUpPayload(stack.copy()));
                }
                if (remain.isEmpty()) {
                    stack.setCount(0);
                } else {
                    stack.split(remain.getCount());
                }
                self.setItem(stack);
            });
        }
    }

    private static boolean isPlayerEnabledAutoPick(Player player) {
        return ModRegistries.NbtAttachments.getSyncedConfig().computeIfAbsent(player).autoPicking();
    }

    private static boolean canMerge(Player player, ItemStack stack) {
        return player.inventoryMenu.slots.stream().anyMatch(slot -> ItemStack.isSameItemSameComponents(slot.getItem(), stack));
    }

    private static boolean hasSuch(Player player, Item item) {
        return player.inventoryMenu.slots.stream().anyMatch(slot -> slot.getItem().getItem().getClass() == item.getClass());
    }

    private static boolean hasOrWearing(Player player, Item armor) {
        var equip = armor.components().get(DataComponents.EQUIPPABLE);
        if (equip == null) {
            return false;
        }
        EquipmentSlot slot = equip.slot();
        ItemStack equipped = player.getItemBySlot(slot);
        if (!equipped.isEmpty() && equipped.getItem() == armor) {
            return true;
        }
        return player.inventoryMenu.slots.stream().anyMatch(slt -> slt.getItem().getItem() == armor);
    }

    private static boolean shouldMoveTo(Player player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item.components().has(DataComponents.EQUIPPABLE)) {
            return hasOrWearing(player, item);
        }
        if (stack.getMaxStackSize() == 1) {
            return hasSuch(player, item);
        }
        return !canMerge(player, stack);
    }
}

