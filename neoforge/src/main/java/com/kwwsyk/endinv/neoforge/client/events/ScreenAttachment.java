package com.kwwsyk.endinv.neoforge.client.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.gui.AttachingScreen;
import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.common.client.gui.IScreenEvent;
import com.kwwsyk.endinv.common.client.gui.bg.IRectangleParam;
import com.kwwsyk.endinv.common.client.option.ClientConfigs;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import javax.annotation.Nullable;

@EventBusSubscriber(value = Dist.CLIENT,modid = ModInfo.MOD_ID)
public class ScreenAttachment {
    @Nullable
    public static AttachingScreen<?> ATTACHMENT_MANAGER;
    @Nullable
    private static Button configButton;

    @Nullable
    private static AttachingScreen<?> checkAndGetAttached(ScreenEvent event){
        if(
                !(
                        event.getScreen() instanceof AbstractContainerScreen<?> screen
                                && !(screen instanceof EndlessInventoryScreen)
                                && AttachingScreen.isActive(ATTACHMENT_MANAGER, screen)
                )
        ){
            ATTACHMENT_MANAGER = null;
        }
        return ATTACHMENT_MANAGER;
    }

//    @SubscribeEvent
//    public static void opening(ScreenEvent.Opening event){
//        if(event.getScreen() instanceof AbstractContainerScreen<?>){
//            // no-op: kept for compatibility
//        }
//    }

    @SubscribeEvent
    public static void closing(ScreenEvent.Closing event){
        if(ATTACHMENT_MANAGER!=null){
            ATTACHMENT_MANAGER.closed(new IScreenEvent(){});
            ATTACHMENT_MANAGER = null;
        }
    }

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event){
        if(!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || screen instanceof EndlessInventoryScreen) return;
        IRectangleParam btnParam = ClientConfigs.ATTACHED_MENU_CONFIG.get().adjust(screen).configButtonA();
        configButton = AttachingScreen.configButton(
                event.getScreen(), btnParam,
                () -> {
                    if(ATTACHMENT_MANAGER==null){
                        ATTACHMENT_MANAGER = AttachingScreen.attach(screen, event::addListener);
                    }
                },
                () -> {
                    if(ATTACHMENT_MANAGER!=null){
                        ATTACHMENT_MANAGER.closed(new IScreenEvent(){});
                        ATTACHMENT_MANAGER = null;
                    }
                }
        );
        event.addListener(configButton);
        if(AttachingScreen.isAttachable(screen)){
            Player player = screen.getMinecraft().player;
            if(player==null) return;

            if(ATTACHMENT_MANAGER==null){
                ATTACHMENT_MANAGER = AttachingScreen.attach(screen, event::addListener);
            }
        }
    }

    @SubscribeEvent
    public static void renderPre(ScreenEvent.Render.Background event){
        if(ATTACHMENT_MANAGER!=null){
            ATTACHMENT_MANAGER.renderPre(new IScreenEvent() {
                @Override
                public double getMouseX() {
                    return event.getMouseX();
                }

                @Override
                public double getMouseY() {
                    return event.getMouseY();
                }

                @Override
                public float getPartialTick() {
                    return event.getPartialTick();
                }

                @Override
                public GuiGraphicsExtractor getGuiGraphics() {
                    return event.getGuiGraphics();
                }
            });
        }
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event){
        if(ATTACHMENT_MANAGER!=null){
            ATTACHMENT_MANAGER.render(new IScreenEvent() {
                @Override
                public double getMouseX() {
                    return event.getMouseX();
                }

                @Override
                public double getMouseY() {
                    return event.getMouseY();
                }

                @Override
                public float getPartialTick() {
                    return event.getPartialTick();
                }

                @Override
                public GuiGraphicsExtractor getGuiGraphics() {
                    return event.getGuiGraphics();
                }
            });
        }
    }

    @SubscribeEvent
    public static void mouseClicked(ScreenEvent.MouseButtonPressed.Pre event){
        if(configButton != null && configButton.isMouseOver(event.getMouseX(), event.getMouseY())) return;
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.mouseClicked(new IScreenEvent() {
                @Override
                public double getMouseX() {
                    return event.getMouseX();
                }

                @Override
                public double getMouseY() {
                    return event.getMouseY();
                }

                @Override
                public void setCanceled(boolean canceled) {
                    event.setCanceled(canceled);
                }

                @Override
                public int getButton() {
                    return event.getButton();
                }

                @Override
                public MouseButtonEvent getMouseButtonEvent(){
                    return event.getMouseButtonEvent();
                }
            });
        }
    }

    @SubscribeEvent
    public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.mouseReleased(new IScreenEvent() {
                @Override
                public double getMouseX() {
                    return event.getMouseX();
                }

                @Override
                public double getMouseY() {
                    return event.getMouseY();
                }

                @Override
                public void setCanceled(boolean canceled) {
                    event.setCanceled(canceled);
                }

                @Override
                public int getButton() {
                    return event.getButton();
                }
                @Override
                public MouseButtonEvent getMouseButtonEvent(){
                    return event.getMouseButtonEvent();
                }
            });
        }
    }

    @SubscribeEvent
    public static void mouseDragged(ScreenEvent.MouseDragged.Pre event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.mouseDragged(new IScreenEvent() {
                @Override
                public double getMouseX() {
                    return event.getMouseX();
                }

                @Override
                public double getMouseY() {
                    return event.getMouseY();
                }

                @Override
                public void setCanceled(boolean canceled) {
                    event.setCanceled(canceled);
                }

                @Override
                public double getDragX() {
                    return event.getDragX();
                }

                @Override
                public double getDragY() {
                    return event.getDragY();
                }

                @Override
                public int getMouseButton() {
                    return event.getMouseButton();
                }

                @Override
                public MouseButtonEvent getMouseButtonEvent(){
                    return event.getMouseButtonEvent();
                }
            });
        }
    }

    @SubscribeEvent
    public static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.mouseScrolled(new IScreenEvent() {
                @Override
                public double getScrollDeltaX() {
                    return event.getScrollDeltaX();
                }

                @Override
                public double getScrollDeltaY() {
                    return event.getScrollDeltaY();
                }

                @Override
                public double getMouseY() {
                    return event.getMouseY();
                }

                @Override
                public double getMouseX() {
                    return event.getMouseX();
                }

                @Override
                public void setCanceled(boolean canceled) {
                    event.setCanceled(canceled);
                }
            });
        }
    }

    @SubscribeEvent
    public static void keyPressed(ScreenEvent.KeyPressed.Pre event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.keyPressed(new IScreenEvent() {
                @Override
                public int getKeyCode() {
                    return event.getKeyCode();
                }

                @Override
                public int getModifiers() {
                    return event.getKeyEvent().modifiers();
                }

                @Override
                public int getScanCode() {
                    return event.getScanCode();
                }

                @Override
                public void setCanceled(boolean canceled) {
                    event.setCanceled(canceled);
                }

                public KeyEvent getKeyEvent(){
                    return event.getKeyEvent();
                }
            });
        }
    }

    @SubscribeEvent
    public static void charTyped(ScreenEvent.CharacterTyped.Pre event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.charTyped(new IScreenEvent() {
                @Override
                public char getCodePoint() {
                    return (char) event.getCodePoint();
                }

                @Override
                public int getModifiers() {
                    return 0;
                }

                @Override
                public void setCanceled(boolean canceled) {
                    event.setCanceled(canceled);
                }

                @Override
                public CharacterEvent getCharEvent() {
                    return event.getCharacterEvent();
                }
            });
        }

    }
}
