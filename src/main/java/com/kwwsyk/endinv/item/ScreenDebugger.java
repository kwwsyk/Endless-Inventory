package com.kwwsyk.endinv.item;

import com.kwwsyk.endinv.events.RenderEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static com.kwwsyk.endinv.events.RenderEvents.phase;

public class ScreenDebugger extends Item {
    public ScreenDebugger(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(level.isClientSide) phase++;
        if(phase>2) phase=0;
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }
}
