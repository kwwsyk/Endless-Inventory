package com.kwwsyk.endinv.common.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.ArrayDeque;
import java.util.Deque;

public class AutoPickTipper {

    private static final int MAX_QUEUE_SIZE = 8;
    private static final int DISPLAY_TICKS = 100;
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

    public static void onRenderGui(GuiGraphics guiGraphics) {
        if (pickupQueue.isEmpty()) return;

        Minecraft mc =Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int index = 0;
        for (PickupDisplayItem item : pickupQueue) {
            int x = screenWidth - 24;
            int y = screenHeight - 20 - (index * 18);
            int color = rarityColor(item.stack.getRarity());

            guiGraphics.renderItem(item.stack,x,y);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, item.stack, x, y);
            //guiGraphics.pose().pushPose();
            //guiGraphics.pose().mulPose(new Quaternionf(new AxisAngle4d(Math.PI/2, 1.0, 1.0, 1.0)));
            guiGraphics.fillGradient(x - 48, y, x + 16, y + 16, 0x00000000, color);
            //guiGraphics.pose().popPose();

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

    @SuppressWarnings("deprecation")
    private static int rarityColor(Rarity rarity){
        switch (rarity){
            case COMMON -> {
                return 0x88ffffff;
            }
            default -> {
                return 0xff000000 | rarity.color.getColor();
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
