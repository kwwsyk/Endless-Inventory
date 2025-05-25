package com.kwwsyk.endinv.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import static com.kwwsyk.endinv.client.events.ScreenDebug.phase;

public class ScreenDebugger extends Item {
    public ScreenDebugger(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        if(level.isClientSide) phase++;
        if(phase>2) phase=0;
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }
}
