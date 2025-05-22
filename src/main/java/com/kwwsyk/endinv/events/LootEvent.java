package com.kwwsyk.endinv.events;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.ServerLevelEndInv;
import com.kwwsyk.endinv.network.payloads.toClient.ItemPickedUpPayload;
import com.kwwsyk.endinv.options.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

@EventBusSubscriber(modid = ModInitializer.MOD_ID,bus = EventBusSubscriber.Bus.GAME)
public class LootEvent {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if(!isPlayerEnabledAutoPick(player)) return;
        if (ServerConfig.CONFIG.ENABLE_AUTO_PICK.getAsBoolean()) {
            EndlessInventory endInv = ServerLevelEndInv.getEndInvForPlayer(player).orElse(null);
            if(endInv==null) return;
            boolean flag = true;
            for (ItemEntity drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                ItemStack remain = endInv.addItem(stack);
                stack.split(remain.getCount());
                if(!stack.isEmpty()) PacketDistributor.sendToPlayer(player,new ItemPickedUpPayload(stack));
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
            EndlessInventory endInv = ServerLevelEndInv.getEndInvForPlayer(player).orElse(null);
            if(endInv==null) return;
            boolean flag = true;

            for (ItemEntity drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                ItemStack remain = endInv.addItem(stack);
                stack.split(remain.getCount());
                if(!stack.isEmpty()) PacketDistributor.sendToPlayer(player,new ItemPickedUpPayload(stack));
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
            int newValue = repairPlayerItems(player,exp);
            player.giveExperiencePoints(newValue);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPickupItem(ItemEntityPickupEvent.Pre event){
        Player player = event.getPlayer();
        if(!(player instanceof ServerPlayer) || !ServerConfig.CONFIG.ENABLE_AUTO_PICK.getAsBoolean() || !isPlayerEnabledAutoPick(player)){
            return;
        }
        ItemEntity entity = event.getItemEntity();
        ItemStack stack = entity.getItem();
        if(shouldMoveTo(player,stack)){
            ServerLevelEndInv.getEndInvForPlayer(player).ifPresent(endInv->{
                ItemStack remain = endInv.addItem(stack.copy());

                if(!stack.isEmpty()) PacketDistributor.sendToPlayer((ServerPlayer) player,new ItemPickedUpPayload(stack.copy()));
                if(remain.isEmpty()){
                    stack.setCount(0);
                }else {
                    stack.split(remain.getCount());
                }
            });
        }
    }

    private static boolean shouldMoveTo(Player player, ItemStack stack){
        if(stack.isEmpty()) return false;
        Item item = stack.getItem();
        switch (item){
            case SwordItem swordItem -> {
                return hasSuch(player,swordItem);
            }
            case AxeItem axeItem -> {
                return hasSuch(player,axeItem);
            }
            case PickaxeItem such -> {
                return hasSuch(player,such);
            }
            case ShovelItem such -> {
                return hasSuch(player,such);
            }
            case HoeItem such -> {
                return hasSuch(player,such);
            }
            case TridentItem such -> {
                return hasSuch(player,such);
            }
            case ShieldItem such -> {
                return hasSuch(player,such);
            }
            case ShearsItem such -> {
                return hasSuch(player,such);
            }
            case FlintAndSteelItem such -> {
                return hasSuch(player,such);
            }
            case ElytraItem such -> {
                return hasSuch(player,such);
            }
            case BowItem such -> {
                return hasSuch(player,such);
            }
            case CrossbowItem such -> {
                return hasSuch(player,such);
            }
            case MaceItem such -> {
                return hasSuch(player,such);
            }
            case ArmorItem armorItem -> {
                return hasOrSwearing(player,armorItem);
            }
            default -> {
                return true;
            }
        }
    }

    private static boolean hasSuch(Player player, Item item){
        return player.inventoryMenu.slots.stream().anyMatch(slot->slot.getItem().getItem().getClass()==item.getClass());
    }

    private static boolean hasOrSwearing(Player player,ArmorItem armor){
        EquipmentSlot slot = armor.getEquipmentSlot();
        ItemStack equipped = player.getItemBySlot(slot);
        if(equipped.isEmpty()){
            return player.inventoryMenu.slots.stream().anyMatch(slt-> slt.getItem().getItem() instanceof ArmorItem a1 && a1.getEquipmentSlot() == slot);
        }
        return true;
    }


    //copied from ExperienceOrb.java
    private static int repairPlayerItems(ServerPlayer player, int value) {
        Optional<EnchantedItemInUse> optional = EnchantmentHelper.getRandomItemWith(EnchantmentEffectComponents.REPAIR_WITH_XP, player, ItemStack::isDamaged);
        if (optional.isPresent()) {
            ItemStack itemstack = optional.get().itemStack();
            int i = EnchantmentHelper.modifyDurabilityToRepairFromXp(player.serverLevel(), itemstack, (int) (value * itemstack.getXpRepairRatio()));
            int j = Math.min(i, itemstack.getDamageValue());
            itemstack.setDamageValue(itemstack.getDamageValue() - j);
            if (j > 0) {
                int k = value - j * value / i;
                if (k > 0) {
                    return repairPlayerItems(player, k);
                }
            }

            return 0;
        } else {
            return value;
        }
    }

    private static boolean isPlayerEnabledAutoPick(Player player){
        return player.getData(ModInitializer.SYNCED_CONFIG).autoPicking();
    }
}
