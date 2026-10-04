package com.kwwsyk.endinv.neoforge.mixin;

import com.kwwsyk.endinv.common.autopick.AutoPickHelper;
import com.kwwsyk.endinv.common.options.ServerConfigs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public class ExperienceOrbMixin {
    @Unique
    private boolean endinv$protectionApplied;

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
