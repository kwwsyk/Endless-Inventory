package com.kwwsyk.endinv.neoforge.events;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.autopick.AutoPickHelper;
import com.kwwsyk.endinv.common.network.payloads.toClient.ItemPickedUpPayload;
import com.kwwsyk.endinv.common.options.ServerConfigs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import static com.kwwsyk.endinv.common.ModInfo.getPacketDistributor;

@EventBusSubscriber(modid = ModInfo.MOD_ID)
public final class LootEvent {
    public static final Set<ItemEntity> CAPTURED_DROPS = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || !AutoPickHelper.isPlayerEnabledAutoPick(player)) return;
        handleItemDrops(event.getDrops(), player);
        if (ServerConfigs.PICKUP_HELPER.EXP_DROPS.GIVE_DIRECTLY.get()) {
            int experience = event.getDroppedExperience();
            if (experience > 0) {
                player.giveExperiencePoints(experience);
                event.setDroppedExperience(0);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !AutoPickHelper.isPlayerEnabledAutoPick(player)) return;
        handleItemDrops(event.getDrops(), player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExperienceDrops(LivingExperienceDropEvent event) {
        if (!(event.getAttackingPlayer() instanceof ServerPlayer player) || !AutoPickHelper.isPlayerEnabledAutoPick(player)) return;
        if (ServerConfigs.PICKUP_HELPER.EXP_DROPS.GIVE_DIRECTLY.get()) {
            int experience = event.getDroppedExperience();
            if (experience > 0) {
                player.giveExperiencePoints(experience);
                event.setDroppedExperience(0);
            }
        }
    }

    private static void handleItemDrops(Iterable<ItemEntity> drops, ServerPlayer player) {
        var options = ServerConfigs.PICKUP_HELPER.ITEM_DROPS;
        for (ItemEntity entity : drops) {
            if (options.PROTECT_DROPS.get()) entity.invulnerableTime = 6000;
            if (options.DIRECTLY_SEND_TO_ENDINV.get()) {
                ItemStack remain = sendItemToEndinv(player, entity.getItem());
                entity.setItem(remain);
                if (remain.isEmpty()) {
                    entity.discard();
                    continue;
                }
            }
            if (options.SEND_TO_INVENTORY.get()) {
                CAPTURED_DROPS.add(entity);
                entity.setNoPickUpDelay();
                entity.playerTouch(player);
                if (entity.isRemoved()) continue;
            }
            int directed = options.DIRECTED_DISTRIBUTE.get();
            if (directed < 0) {
                entity.setPos(player.position());
            } else if (directed > 0) {
                var direction = player.position().add(0.0, player.getBbHeight() * 0.75, 0.0)
                        .subtract(entity.position());
                if (direction.lengthSqr() > 1.0e-6) {
                    entity.setDeltaMovement(direction.normalize().scale(0.05 * directed));
                }
            }
        }
    }

    public static ItemStack sendItemToEndinv(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer) || stack.isEmpty()) return stack;
        EndlessInventory endInv = ServerLevelEndInv.getEndInvForPlayer(player).orElse(null);
        if (endInv == null) return stack;
        ItemStack remain = endInv.addItem(stack.copy());
        int inserted = stack.getCount() - remain.getCount();
        if (inserted > 0) {
            getPacketDistributor().sendToPlayer(serverPlayer,
                    new ItemPickedUpPayload(stack.copyWithCount(inserted)));
        }
        return remain;
    }
}
