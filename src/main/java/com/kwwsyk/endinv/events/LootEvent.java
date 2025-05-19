package com.kwwsyk.endinv.events;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.ServerLevelEndInv;
import com.kwwsyk.endinv.network.payloads.toClient.ItemPickedUpPayload;
import com.kwwsyk.endinv.options.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ModInitializer.MOD_ID,bus = EventBusSubscriber.Bus.GAME)
public class LootEvent {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if(!isPlayerEnabledAutoPick(player)) return;
        if (ServerConfig.CONFIG.ENABLE_AUTO_PICK.getAsBoolean()) {
            EndlessInventory endInv = ServerLevelEndInv.getEndInvForPlayer(player);
            boolean flag = true;
            for (ItemEntity drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                ItemStack remain = endInv.addItem(stack);
                stack.split(remain.getCount());
                PacketDistributor.sendToPlayer(player,new ItemPickedUpPayload(stack));
                if (remain.isEmpty()) {
                    drop.remove(Entity.RemovalReason.DISCARDED);
                } else {
                    drop.setItem(remain);
                    flag = false;
                }
            }
            if (flag)
                event.setCanceled(true); // 取消原始掉落
        }
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event){
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        if(!isPlayerEnabledAutoPick(player)) return;
        if (ServerConfig.CONFIG.ENABLE_AUTO_PICK.getAsBoolean()) {
            EndlessInventory endInv = ServerLevelEndInv.getEndInvForPlayer(player);
            boolean flag = true;

            for (ItemEntity drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                ItemStack remain = endInv.addItem(stack);
                stack.split(remain.getCount());
                PacketDistributor.sendToPlayer(player,new ItemPickedUpPayload(stack));
                if (remain.isEmpty()) {
                    drop.remove(Entity.RemovalReason.DISCARDED);
                } else {
                    drop.setItem(remain);
                    flag = false;
                }
            }
            if (flag)
                event.setCanceled(true); // 取消原始掉落
        }
    }

    @SubscribeEvent
    public static void onExpDrops(LivingExperienceDropEvent event){
        if(event.getAttackingPlayer() instanceof  ServerPlayer player){
            if(!isPlayerEnabledAutoPick(player) || !ServerConfig.CONFIG.ENABLE_AUTO_PICK.getAsBoolean()) return;
            int exp = event.getDroppedExperience();
            player.giveExperiencePoints(exp);
            event.setCanceled(true);
        }
    }

    private static boolean isPlayerEnabledAutoPick(Player player){
        return player.getData(ModInitializer.SYNCED_CONFIG).autoPicking();
    }
}
