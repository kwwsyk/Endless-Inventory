package com.kwwsyk.endinv.neoforge.client.events;

import com.kwwsyk.endinv.neoforge.ModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayDeque;
import java.util.Deque;

@EventBusSubscriber(value = Dist.CLIENT,modid = ModInitializer.MOD_ID)
public class PickingUpTip {

    private static final int MAX_QUEUE_SIZE = 5;
    private static final int DISPLAY_TICKS = 60;
    private static final int MIN_DELAY_BETWEEN_REMOVALS = 10;

    private static final Deque<PickupDisplayItem> pickupQueue = new ArrayDeque<>();
    private static int removalDelayCounter = 0;

    public static void addItem(ItemStack stack) {

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(SoundEvents.ITEM_PICKUP, 0.5f, 1.0f);
        }
        // 尝试合并已有物品
        for (PickupDisplayItem item : pickupQueue) {
            if (ItemStack.isSameItemSameComponents(item.stack, stack)) {
                item.stack.grow(stack.getCount());
                item.timeLeft = DISPLAY_TICKS;
                pickupQueue.remove(item);
                pickupQueue.addFirst(item);
                return;
            }
        }

        // 新物品加入
        if (pickupQueue.size() >= MAX_QUEUE_SIZE) {
            pickupQueue.pollLast();
        }
        pickupQueue.addFirst(new PickupDisplayItem(stack.copy(), DISPLAY_TICKS));
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (pickupQueue.isEmpty()) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Minecraft mc =Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int index = 0;
        for (PickupDisplayItem item : pickupQueue) {
            int x = screenWidth - 24;
            int y = screenHeight - 20 - (index * 18);

            guiGraphics.renderItem(item.stack,x,y);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, item.stack, x, y);

            index++;
        }

        // 处理时间
        if (removalDelayCounter > 0) {
            removalDelayCounter--;
        } else if (!pickupQueue.isEmpty()) {
            PickupDisplayItem lastItem = pickupQueue.peekLast();
            if (lastItem != null) {
                lastItem.timeLeft--;
                if (lastItem.timeLeft <= 0) {
                    pickupQueue.pollLast();
                    removalDelayCounter = MIN_DELAY_BETWEEN_REMOVALS;
                }
            }
        }
    }

    private static class PickupDisplayItem {
        ItemStack stack;
        int timeLeft;

        public PickupDisplayItem(ItemStack stack, int timeLeft) {
            this.stack = stack;
            this.timeLeft = timeLeft;
        }
    }
}
