package com.kwwsyk.endinv.item;

import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class TestEndInv extends Item {

    public TestEndInv(Properties properties){
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if(!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(EndlessInventoryMenu::createServer,
                    Component.literal("test")));
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
