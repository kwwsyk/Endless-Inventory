package com.kwwsyk.endinv.fabric.client.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.gui.AttachingScreen;
import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.common.client.gui.IScreenEvent;
import com.kwwsyk.endinv.common.client.gui.bg.IRectangleParam;
import com.kwwsyk.endinv.common.client.option.ClientConfigs;
import com.kwwsyk.endinv.fabric.mixin.ScreenAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public final class ScreenAttachment {

    @Nullable
    public static AttachingScreen<?> attachment;

    private static final Identifier SEARCH_INPUT_PHASE = Identifier.fromNamespaceAndPath(ModInfo.MOD_ID, "search_input");

    private ScreenAttachment() {
    }

    public static void register() {
        ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> {
            // sync now handled via server flags; no client-side read required here
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) {
                return;
            }
            // Route characters before the host/JEI widget pipeline, including standalone EI.
            var charEvents = ScreenKeyboardEvents.allowCharType(screen);
            charEvents.addPhaseOrdering(SEARCH_INPUT_PHASE, Event.DEFAULT_PHASE);
            charEvents.register(SEARCH_INPUT_PHASE, ScreenAttachment::allowCharType);
            var keyEvents = ScreenKeyboardEvents.allowKeyPress(screen);
            keyEvents.addPhaseOrdering(SEARCH_INPUT_PHASE, Event.DEFAULT_PHASE);
            keyEvents.register(SEARCH_INPUT_PHASE, ScreenAttachment::allowKeyPress);
            if (screen instanceof EndlessInventoryScreen) {
                return;
            }

            // Add independent config/toggle button (Shift opens settings)
            IRectangleParam btnParam = ClientConfigs.ATTACHED_MENU_CONFIG.get().adjust(container).configButtonA();
            ((ScreenAccessor) screen).endinv$invokeAddRenderableWidget(
                    AttachingScreen.configButton(
                            screen,
                            btnParam,
                            () -> {
                                if (attachment == null) {
                                    attachment = AttachingScreen.attach(container,
                                            widget -> ((ScreenAccessor) screen).endinv$invokeAddRenderableWidget(widget));
                                }
                            },
                            () -> {
                                if (attachment != null) {
                                    attachment.closed(new IScreenEvent() {});
                                    attachment = null;
                                }
                            }
                    )
            );

            Player player = client.player;
            if (player == null) {
                attachment = null;
                return;
            }

            if (AttachingScreen.isAttachable(container)) {
                if (attachment == null) {
                    attachment = AttachingScreen.attach(container,
                            widget -> ((ScreenAccessor) screen).endinv$invokeAddRenderableWidget(widget));
                }
            }

            ScreenEvents.remove(screen).register(s -> {
                if (attachment != null) {
                    attachment.closed(new IScreenEvent() {});
                    attachment = null;
                }
            });

            // Rendering is handled via mixin injection to align phase with NeoForge (after renderBg).
            assert attachment != null;

            var clickEvents = ScreenMouseEvents.allowMouseClick(screen);
            clickEvents.addPhaseOrdering(SEARCH_INPUT_PHASE, Event.DEFAULT_PHASE);
            clickEvents.register(SEARCH_INPUT_PHASE, (s, event) -> allowMouseClick(attachment, event));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> allowMouseRelease(attachment, event));
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontal, vertical) -> allowMouseScroll(attachment, mouseX, mouseY, horizontal, vertical));

        });
    }

    private static boolean allowMouseClick(AttachingScreen<?> expected, MouseButtonEvent buttonEvent) {
        if (attachment != expected || !isAttachmentActive(expected)) {
            return true;
        }
        EditBox search = expected.getFrameWork().searchBox;
        if (search.visible && search.active && search.isMouseOver(buttonEvent.x(), buttonEvent.y())
                && (buttonEvent.button() == InputConstants.MOUSE_BUTTON_LEFT
                || buttonEvent.button() == InputConstants.MOUSE_BUTTON_RIGHT)) {
            // The framework flag alone does not change the host's focused child.
            expected.screen.setFocused(search);
            if (buttonEvent.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                search.mouseClicked(buttonEvent, false);
                expected.screen.setDragging(true);
            } else {
                search.setValue("");
            }
            return false;
        }
        boolean[] canceled = {false};
        expected.mouseClicked(new IScreenEvent() {
            public MouseButtonEvent getMouseButtonEvent(){
                return buttonEvent;
            }

            @Override
            public void setCanceled(boolean flag) {
                canceled[0] = flag;
            }
        });
        return !canceled[0];
    }

    private static boolean allowMouseRelease(AttachingScreen<?> expected, MouseButtonEvent buttonEvent) {
        if (attachment != expected || !isAttachmentActive(expected)) {
            return true;
        }
        if (expected.screen.isDragging() && expected.getFrameWork().searchBox.isFocused()
                && buttonEvent.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            expected.getFrameWork().searchBox.mouseReleased(buttonEvent);
            expected.screen.setDragging(false);
            return false;
        }
        boolean[] canceled = {false};
        expected.mouseReleased(new IScreenEvent() {

            public MouseButtonEvent getMouseButtonEvent(){
                return buttonEvent;
            }

            @Override
            public void setCanceled(boolean flag) {
                canceled[0] = flag;
            }
        });
        return !canceled[0];
    }

    public static void onRenderAfterBackground(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        AttachingScreen<?> current = attachment;
        if (current == null || current.getScreen() != screen || !isAttachmentActive(current)) {
            return;
        }
        current.renderPre(new IScreenEvent() {
            @Override
            public double getMouseX() { return mouseX; }

            @Override
            public double getMouseY() { return mouseY; }

            @Override
            public float getPartialTick() { return partialTick; }

            @Override
            public GuiGraphicsExtractor getGuiGraphics() { return graphics; }
        });
    }

    // Reserved for potential overlay rendering parity (currently no-op in common)
    public static void onRenderPost(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        AttachingScreen<?> current = attachment;
        if (current == null || current.getScreen() != screen || !isAttachmentActive(current)) {
            return;
        }
        current.render(new IScreenEvent() {
            @Override
            public double getMouseX() { return mouseX; }

            @Override
            public double getMouseY() { return mouseY; }

            @Override
            public float getPartialTick() { return partialTick; }

            @Override
            public GuiGraphicsExtractor getGuiGraphics() { return graphics; }
        });
    }

    private static boolean allowMouseScroll(AttachingScreen<?> expected, double mouseX, double mouseY, double horizontal, double vertical) {
        if (attachment != expected || !isAttachmentActive(expected)) {
            return true;
        }
        boolean[] canceled = new boolean[]{false};
        expected.mouseScrolled(new IScreenEvent() {
            @Override
            public double getMouseX() {
                return mouseX;
            }

            @Override
            public double getMouseY() {
                return mouseY;
            }

            @Override
            public double getScrollDeltaY() {
                return vertical;
            }

            @Override
            public double getScrollDeltaX() {
                return horizontal;
            }

            @Override
            public void setCanceled(boolean canceled1){
                canceled[0] = canceled1;
            }
        });
        return !canceled[0];
    }

    private static boolean allowKeyPress(Screen screen, KeyEvent keyEvent) {
        if (screen instanceof EndlessInventoryScreen standalone) {
            EditBox search = standalone.getFrameWork().searchBox;
            if (search.visible && search.active && search.isFocused()) {
                return !standalone.getFrameWork().keyPressed(keyEvent);
            }
            return true;
        }
        AttachingScreen<?> expected = attachment;
        if (expected == null || expected.screen != screen || !isAttachmentActive(expected)) {
            return true;
        }
        boolean[] canceled = {false};
        expected.keyPressed(new IScreenEvent() {
            public KeyEvent getKeyEvent(){
                return keyEvent;
            }

            @Override
            public void setCanceled(boolean flag) {
                canceled[0] = flag;
            }
        });
        return !canceled[0];
    }

    public static boolean handleMouseDrag(AbstractContainerScreen<?> screen, MouseButtonEvent event, double deltaX, double deltaY) {
        AttachingScreen<?> current = attachment;
        if (current == null || current.screen != screen || !isAttachmentActive(current)) {
            return false;
        }
        EditBox search = current.getFrameWork().searchBox;
        if (search.visible && search.active && search.isFocused() && screen.isDragging()
                && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            search.mouseDragged(event, deltaX, deltaY);
            return true;
        }
        boolean[] canceled = {false};
        current.mouseDragged(new IScreenEvent() {

            @Override
            public double getDragX() {
                return deltaX;
            }

            @Override
            public double getDragY() {
                return deltaY;
            }

            @Override
            public void setCanceled(boolean flag) {
                canceled[0] = flag;
            }

            public MouseButtonEvent getMouseButtonEvent(){
                return event;
            }
        });
        return canceled[0];
    }

    private static boolean allowCharType(Screen screen, CharacterEvent event) {
        if (screen instanceof EndlessInventoryScreen standalone) {
            EditBox search = standalone.getFrameWork().searchBox;
            if (search.visible && search.active && search.isFocused()) {
                standalone.charTyped(event);
                return false;
            }
            return true;
        }
        AttachingScreen<?> current = attachment;
        if (current == null || current.screen != screen || !isAttachmentActive(current)) {
            return true;
        }
        EditBox search = current.getFrameWork().searchBox;
        if (!search.visible || !search.active || !search.isFocused()) {
            return true;
        }
        current.charTyped(new IScreenEvent() {
            @Override
            public CharacterEvent getCharEvent() {
                return event;
            }
        });
        // Even rejected characters belong to EI while its search owns focus.
        return false;
    }

    private static boolean isAttachmentActive(@Nullable AttachingScreen<?> expected) {
        if (!AttachingScreen.isActive(expected, Minecraft.getInstance().gui.screen())) {
            attachment = null;
            return false;
        }
        return true;
    }
}


