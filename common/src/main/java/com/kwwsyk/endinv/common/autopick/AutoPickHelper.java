package com.kwwsyk.endinv.common.autopick;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.autopick.events.IBlockBreakEvent;
import com.kwwsyk.endinv.common.autopick.events.ILivingDropsEvent;
import com.kwwsyk.endinv.common.autopick.events.ILivingExpDropsEvent;
import com.kwwsyk.endinv.common.autopick.events.IPlayerPickupItemEvent;
import com.kwwsyk.endinv.common.network.payloads.toClient.ItemPickedUpPayload;
import com.kwwsyk.endinv.common.options.ServerConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class AutoPickHelper {

    /**
     * Check if a player can auto pick.
     * @param player to check
     * @return true -> proceed auto pick process
     */
    public static boolean isEnabled(Player player){
        return isPlayerEnabledAutoPick(player) && ServerConfigs.PICKUP_HELPER.ITEM_DROPS.DIRECTLY_SEND_TO_ENDINV.get();
    }

    public static boolean isPlayerEnabledAutoPick(Player player){
        return true;
    }

    /**
     * Hook living entity dropping, jmp ItemEntity spawning and directly send drops to EndInv
     * @param event use mod loader's event api to implement it
     */
    public static void onLivingDrops(ILivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (AutoPickHelper.isEnabled(player)) {
            EndlessInventory endInv = ServerLevelEndInv.getEndInvForPlayer(player).orElse(null);
            if(endInv==null) return;
            boolean flag = true;
            for (ItemEntity drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                ItemStack remain = endInv.addItem(stack);
                stack.split(remain.getCount());
                if(!stack.isEmpty()) ModInfo.getPacketDistributor().sendToPlayer(player,new ItemPickedUpPayload(stack));
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

    /**
     * Hook lining entity dropping exp, jmp exp entity spawn and directly send exp to player
     * @param event use mod loader's event api to implement it
     */
    public static void onExpDrops(ILivingExpDropsEvent event){
        if(event.getAttackingPlayer() instanceof  ServerPlayer player){
            if(!AutoPickHelper.isEnabled(player)) return;
            int exp = event.getDroppedExperience();
            int newValue = repairPlayerItems(player,exp);
            player.giveExperiencePoints(newValue);
            event.setCanceled(true);
        }
    }

    /**
     * Hook blocks' break and jmp dropped (ItemEntity) spawning, directly send drops to EndInv.
     * For exp, do the same thing
     * <p>
     * Future to do: split steps and separate item and exp drop. To increase compatibility with other mods.
     * @param event use mod loader's event api to implement it
     */
    public static void onBlockBreak(IBlockBreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!AutoPickHelper.isEnabled(player)) return;

        EndlessInventory endInv = ServerLevelEndInv.getEndInvForPlayer(player).orElse(null);
        if (endInv == null) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);

        List<ItemStack> drops = Block.getDrops(state,level,pos,blockEntity,player,player.getMainHandItem());

        if (event.getExpToDrop() > 0) {
            int exp = event.getExpToDrop();
            int repaired = repairPlayerItems(player, exp);
            player.giveExperiencePoints(repaired);
            event.setExpToDrop(0);
        }

        boolean allPicked = true;
        for (ItemStack drop : drops) {
            ItemStack remain = endInv.addItem(drop.copy());
            drop.shrink(remain.getCount());
            if (!drop.isEmpty()) {
                ModInfo.getPacketDistributor().sendToPlayer(player, new ItemPickedUpPayload(drop));
            }
            if (!remain.isEmpty()) {
                allPicked = false;
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remain));
            }
        }

        if (allPicked) {
            // 不生成掉落物
            event.setCanceled(true);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }



    public static void onPickupItem(IPlayerPickupItemEvent event){
        if(!(event.getPlayer() instanceof ServerPlayer player) || !isEnabled(player)){
            return;
        }
        ItemEntity entity = event.getItem();
        if(entity.hasPickUpDelay()) return;
        ItemStack stack = entity.getItem();
        if(shouldMoveTo(player,stack)){
            ServerLevelEndInv.getEndInvForPlayer(player).ifPresent(endInv->{
                ItemStack remain = endInv.addItem(stack.copy());

                if(!stack.isEmpty()) ModInfo.getPacketDistributor().sendToPlayer(player,new ItemPickedUpPayload(stack.copy()));
                if(remain.isEmpty()){
                    stack.setCount(0);
                }else {//rare
                    stack.split(remain.getCount());
                }
            });
        }
    }

    /**Items satisfied with several conditions will stay in the player inventory.
     * 1. Player has mergeable items in the inventory.
     * 2. Player has unstackable some class items in inventory or worn.
     */
    private static boolean shouldMoveTo(Player player, ItemStack stack){
        if(stack.isEmpty()) return false;
        Item item = stack.getItem();
        if(item.builtInRegistryHolder().is(ItemTags.SWORDS)){
            return hasSuch(player,item);
        }else if(item instanceof AxeItem axeItem){
            return hasSuch(player,axeItem);
        }else if(item.builtInRegistryHolder().is(ItemTags.PICKAXES)){
            return hasSuch(player,item);
        }else if(item instanceof ShovelItem such){
            return hasSuch(player,such);
        }else if(item instanceof HoeItem such){
            return hasSuch(player,such);
        }else if(item instanceof TridentItem such){
            return hasSuch(player,such);
        }else if(item instanceof ShieldItem such){
            return hasSuch(player,such);
        }else if(item instanceof ShearsItem such){
            return hasSuch(player,such);
        }else if(item instanceof BoatItem such){
            return hasSuch(player,such);
        }else if(item == Items.ELYTRA){
            return hasSuch(player,item);
        }else if(item instanceof BowItem such){
            return hasSuch(player,such);
        }else if(item instanceof CrossbowItem such){
            return hasSuch(player,such);
        }else if(item.components().has(DataComponents.EQUIPPABLE)){
            return hasOrSwearing(player,item);
        }else{
            return !canMerge(player,stack);
        }
    }

    private static boolean canMerge(Player player, ItemStack stack){
        return player.inventoryMenu.slots.stream().anyMatch(slot -> ItemStack.isSameItemSameComponents(slot.getItem(), stack));
    }

    private static boolean hasSuch(Player player, Item item){
        return player.inventoryMenu.slots.stream().anyMatch(slot->slot.getItem().getItem().getClass()==item.getClass());
    }

    private static boolean hasOrSwearing(Player player,Item armor){
        EquipmentSlot slot = armorSlot(armor);
        if(slot == null) return true;
        ItemStack equipped = player.getItemBySlot(slot);
        if(equipped.isEmpty()){
            return player.inventoryMenu.slots.stream().anyMatch(sl->armorSlot(sl.getItem().getItem())==slot);
        }
        return true;
    }

    private static EquipmentSlot armorSlot(Item armor){
        var c = armor.components().get(DataComponents.EQUIPPABLE);
        if(c==null) return null;//cantHappen
        return c.slot();
    }

    //copied from ExperienceOrb.java
    private static int repairPlayerItems(Player player, int repairAmount) {
        // Simplified for 1.21.x: defer repairing via Mending to vanilla mechanics
        return repairAmount;
    }
}
