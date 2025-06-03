package com.kwwsyk.endinv.neoforge.client.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.gui.AttachedScreen;
import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.common.client.gui.IScreenEvent;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.network.payloads.toServer.OpenEndInvPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static com.kwwsyk.endinv.common.ModRegistries.NbtAttachments.getSyncedConfig;

@EventBusSubscriber(value = Dist.CLIENT,modid = ModInfo.MOD_ID,bus = EventBusSubscriber.Bus.GAME)
public class ScreenAttachment {
    public static final  Map<AbstractContainerScreen<?>, AttachedScreen<?>> ATTACHMENT_MANAGER = new HashMap<>();

    @Nullable
    private static AttachedScreen<?> checkAndGetAttached(ScreenEvent event){
        if(event.getScreen() instanceof AbstractContainerScreen<?> screen){
            Player player = screen.getMinecraft().player;
            if(player==null) return null;
            if(!getSyncedConfig().computeIfAbsent(player).attaching()) {
                ATTACHMENT_MANAGER.remove(screen);
                return null;
            }
            return ATTACHMENT_MANAGER.get(screen);
        }
        return null;
    }

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event){
        SyncedConfig.readAndSyncClientConfigToServer(false);
    }

    @SubscribeEvent
    public static void closing(ScreenEvent.Closing event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            ATTACHMENT_MANAGER.remove((AbstractContainerScreen<?>) event.getScreen());
        }
    }

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event){
        if(event.getScreen() instanceof AbstractContainerScreen<?> screen && !(screen instanceof EndlessInventoryScreen)){
            Player player = screen.getMinecraft().player;
            if(player==null) return;

            SyncedConfig syncedConfig = getSyncedConfig().computeIfAbsent(player);
            if(!syncedConfig.checkForAttaching()) return;

            ATTACHMENT_MANAGER.computeIfAbsent(screen, screen1 -> {
                PacketDistributor.sendToServer(new OpenEndInvPayload(false));
                return new AttachedScreen<>(screen);
            }).init(new IScreenEvent() {
                public void addListener(AbstractWidget widget){
                    event.addListener(widget);
                }
            });
        }
    }

    @SubscribeEvent
    public static void renderPre(ScreenEvent.Render.Pre event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.renderPre(new IScreenEvent() {});
        }
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.render(new IScreenEvent() {
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
                public GuiGraphics getGuiGraphics() {
                    return event.getGuiGraphics();
                }
            });
        }
    }

    @SubscribeEvent
    public static void mouseClicked(ScreenEvent.MouseButtonPressed.Pre event){
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
            });
        }
    }

    @SubscribeEvent
    public static void mouseScrolled(ScreenEvent.MouseScrolled.Post event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.mouseScrolled(new IScreenEvent() {
                @Override
                public double getScrollDeltaY() {
                    return event.getScrollDeltaY();
                }

                @Override
                public double getScrollDeltaX() {
                    return event.getScrollDeltaX();
                }

                @Override
                public double getMouseY() {
                    return event.getMouseY();
                }

                @Override
                public double getMouseX() {
                    return event.getMouseX();
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
                    return event.getModifiers();
                }

                @Override
                public int getScanCode() {
                    return event.getScanCode();
                }

                @Override
                public void setCanceled(boolean canceled) {
                    event.setCanceled(canceled);
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
                    return event.getCodePoint();
                }

                @Override
                public int getModifiers() {
                    return event.getModifiers();
                }

                @Override
                public void setCanceled(boolean canceled) {
                    event.setCanceled(canceled);
                }
            });
        }

    }
}
