package com.kwwsyk.endinv.fabric.mixin;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.network.payloads.toClient.ItemPickedUpPayload;
import com.kwwsyk.endinv.fabric.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void endlessinv$autopick(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!ServerConfig.INSTANCE.enableAutoPick().get()) return;
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
        return ModRegistries.NbtAttachments.getSyncedConfig().getWith(player).autoPicking();
    }

    private static boolean canMerge(Player player, ItemStack stack) {
        return player.inventoryMenu.slots.stream().anyMatch(slot -> ItemStack.isSameItemSameComponents(slot.getItem(), stack));
    }

    private static boolean hasSuch(Player player, Item item) {
        return player.inventoryMenu.slots.stream().anyMatch(slot -> slot.getItem().getItem().getClass() == item.getClass());
    }

    private static boolean hasOrWearing(Player player, ArmorItem armor) {
        EquipmentSlot slot = armor.getEquipmentSlot();
        ItemStack equipped = player.getItemBySlot(slot);
        if (equipped.isEmpty()) {
            return player.inventoryMenu.slots.stream().anyMatch(slt -> slt.getItem().getItem() instanceof ArmorItem a1 && a1.getEquipmentSlot() == slot);
        }
        return true;
    }

    private static boolean shouldMoveTo(Player player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item instanceof SwordItem such) {
            return hasSuch(player, such);
        } else if (item instanceof AxeItem such) {
            return hasSuch(player, such);
        } else if (item instanceof PickaxeItem such) {
            return hasSuch(player, such);
        } else if (item instanceof ShovelItem such) {
            return hasSuch(player, such);
        } else if (item instanceof HoeItem such) {
            return hasSuch(player, such);
        } else if (item instanceof TridentItem such) {
            return hasSuch(player, such);
        } else if (item instanceof ShieldItem such) {
            return hasSuch(player, such);
        } else if (item instanceof ShearsItem such) {
            return hasSuch(player, such);
        } else if (item instanceof BoatItem such) {
            return hasSuch(player, such);
        } else if (item instanceof ElytraItem such) {
            return hasSuch(player, such);
        } else if (item instanceof BowItem such) {
            return hasSuch(player, such);
        } else if (item instanceof CrossbowItem such) {
            return hasSuch(player, such);
        } else if (item instanceof ArmorItem armorItem) {
            return hasOrWearing(player, armorItem);
        } else {
            return !canMerge(player, stack);
        }
    }
}

