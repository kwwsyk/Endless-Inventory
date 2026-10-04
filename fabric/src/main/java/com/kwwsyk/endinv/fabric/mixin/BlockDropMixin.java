package com.kwwsyk.endinv.fabric.mixin;

import com.kwwsyk.endinv.common.autopick.AutoPickHelper;
import com.kwwsyk.endinv.common.options.ServerConfigs;
import com.kwwsyk.endinv.fabric.event.BlockBreakRedirect;
import com.kwwsyk.endinv.fabric.event.LootEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Block.class)
public abstract class BlockDropMixin {

    @Inject(method = "playerDestroy", at = @At("HEAD"))
    private void endlessinv$captureBreaker(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        if (player instanceof ServerPlayer sp && AutoPickHelper.isPlayerEnabledAutoPick(sp)) {
            BlockBreakRedirect.pushBreaker(sp);
        } else {
            BlockBreakRedirect.clearBreaker();
        }
    }

    @Inject(method = "playerDestroy", at = @At("TAIL"))
    private void endlessinv$clearBreaker(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        BlockBreakRedirect.clearBreaker();
    }

    @Redirect(method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean endlessinv$handleBlockDrop(Level level, Entity spawned) {
        ServerPlayer breaker = BlockBreakRedirect.currentBreaker();
        if (breaker != null && spawned instanceof ItemEntity itemEntity
                && AutoPickHelper.isPlayerEnabledAutoPick(breaker)) {
            LootEvent.handleItemDrop(itemEntity, breaker);
            if (itemEntity.isRemoved()) return true;
        }
        return level.addFreshEntity(spawned);
    }

    @Inject(method = "popExperience", at = @At("HEAD"), cancellable = true)
    private static void endlessinv$redirectPopExp(ServerLevel level, BlockPos pos, int amount, CallbackInfo ci) {
        ServerPlayer breaker = BlockBreakRedirect.currentBreaker();
        if (breaker == null || amount <= 0
                || !AutoPickHelper.isPlayerEnabledAutoPick(breaker)
                || !ServerConfigs.PICKUP_HELPER.EXP_DROPS.GIVE_DIRECTLY.get()) {
            return;
        }
        breaker.giveExperiencePoints(amount);
        ci.cancel();
    }
}
