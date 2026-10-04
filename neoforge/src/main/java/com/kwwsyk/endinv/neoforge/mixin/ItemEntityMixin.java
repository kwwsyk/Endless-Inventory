package com.kwwsyk.endinv.neoforge.mixin;

import com.kwwsyk.endinv.common.autopick.AutoPickHelper;
import com.kwwsyk.endinv.common.options.ServerConfigs;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import static com.kwwsyk.endinv.neoforge.events.LootEvent.CAPTURED_DROPS;
import static com.kwwsyk.endinv.neoforge.events.LootEvent.sendItemToEndinv;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Unique
    private final ItemEntity endinv$self = (ItemEntity) (Object) this;

    @WrapOperation(method = "playerTouch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean endinv$pickupIntoEndlessInventory(Inventory inventory, ItemStack stack,
                                                       Operation<Boolean> original, Player player) {
        var options = ServerConfigs.PICKUP_HELPER.ITEM_DROPS;
        boolean sendRemainder = options.PICK_TO_ENDINV.get()
                || options.ENDINV_AFTER_INVENTORY.get() && CAPTURED_DROPS.contains(endinv$self);
        if (player instanceof ServerPlayer && AutoPickHelper.isPlayerEnabledAutoPick(player) && sendRemainder) {
            ItemStack remain = sendItemToEndinv(player, stack);
            stack.setCount(remain.getCount());
            CAPTURED_DROPS.remove(endinv$self);
            if (remain.isEmpty()) return true;
        }
        return original.call(inventory, stack);
    }
}
