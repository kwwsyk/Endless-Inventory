package com.kwwsyk.endinv.fabric.mixin;

import com.kwwsyk.endinv.common.autopick.AutoPickHelper;
import com.kwwsyk.endinv.fabric.event.LootEvent;
import com.kwwsyk.endinv.fabric.event.MobDeathRedirect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntitySpawnAtLocationMixin {

    @Inject(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN"))
    private void endlessinv$handleLivingDrop(ServerLevel level, ItemStack stack, Vec3 offset,
                                              CallbackInfoReturnable<ItemEntity> cir) {
        ServerPlayer player = MobDeathRedirect.get();
        ItemEntity entity = cir.getReturnValue();
        if (player == null || entity == null || !((Object) this instanceof LivingEntity)
                || !AutoPickHelper.isPlayerEnabledAutoPick(player)) {
            return;
        }
        LootEvent.handleItemDrop(entity, player);
    }
}
