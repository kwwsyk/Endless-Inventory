package com.kwwsyk.endinv.fabric.mixin;

import com.kwwsyk.endinv.common.autopick.AutoPickHelper;
import com.kwwsyk.endinv.common.options.ServerConfigs;
import com.kwwsyk.endinv.fabric.event.BlockBreakRedirect;
import com.kwwsyk.endinv.fabric.event.MobDeathRedirect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbAwardMixin {
    @Unique
    private boolean endinv$protectionApplied;

    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private static void endlessinv$giveDirectly(ServerLevel level, Vec3 pos, int amount, CallbackInfo ci) {
        if (amount <= 0 || !ServerConfigs.PICKUP_HELPER.EXP_DROPS.GIVE_DIRECTLY.get()) return;
        ServerPlayer player = BlockBreakRedirect.currentBreaker();
        if (player == null) player = MobDeathRedirect.get();
        if (player == null || !AutoPickHelper.isPlayerEnabledAutoPick(player)) return;
        player.giveExperiencePoints(amount);
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void endinv$adjustOrbBehavior(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Player nearest = self.level().getNearestPlayer(self, 16.0);
        if (nearest == null || nearest.isDeadOrDying() || nearest.isSpectator()
                || !AutoPickHelper.isPlayerEnabledAutoPick(nearest)) return;
        var options = ServerConfigs.PICKUP_HELPER.EXP_DROPS;
        if (options.PROTECT_DROPS.get() && !endinv$protectionApplied) {
            self.invulnerableTime = 6000;
            endinv$protectionApplied = true;
        }
        if (options.TOUCH_DIRECTLY.get() && !self.level().isClientSide()) {
            self.playerTouch(nearest);
            nearest.takeXpDelay = 0;
            return;
        }
        int directed = options.DIRECTED_DISTRIBUTE.get();
        if (directed < 0) {
            self.setPos(nearest.position());
        } else if (directed > 0) {
            var direction = nearest.position().add(0.0, nearest.getBbHeight() * 0.75, 0.0)
                    .subtract(self.position());
            if (direction.lengthSqr() > 1.0e-6) {
                self.setDeltaMovement(direction.normalize().scale(0.05 * directed));
            }
        }
    }
}
