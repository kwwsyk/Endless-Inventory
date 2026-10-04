package com.kwwsyk.endinv.fabric.mixin;

import com.kwwsyk.endinv.common.autopick.AutoPickHelper;
import com.kwwsyk.endinv.fabric.event.MobDeathRedirect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDropMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void endlessinv$captureKiller(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if (source.getEntity() instanceof ServerPlayer player
                && AutoPickHelper.isPlayerEnabledAutoPick(player)) {
            MobDeathRedirect.set(player);
        } else {
            MobDeathRedirect.clear();
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void endlessinv$clearKiller(ServerLevel level, DamageSource source, CallbackInfo ci) {
        MobDeathRedirect.clear();
    }
}
