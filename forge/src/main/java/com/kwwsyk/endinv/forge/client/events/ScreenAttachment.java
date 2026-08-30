package com.kwwsyk.endinv.forge.client.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.ClientModInfo;
import com.kwwsyk.endinv.common.client.gui.AttachingScreen;
import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.common.client.gui.IScreenEvent;
import com.kwwsyk.endinv.common.client.option.CachedConfig;
import com.kwwsyk.endinv.common.client.option.MenuAttachabilityCache;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.network.payloads.toServer.OpenEndInvPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

import static com.kwwsyk.endinv.common.ModRegistries.NbtAttachments.getSyncedConfig;

@Mod.EventBusSubscriber(value = Dist.CLIENT,modid = ModInfo.MOD_ID)
public class ScreenAttachment {
    public static AttachingScreen<?> ATTACHMENT_MANAGER;

    @Nullable
    private static AttachingScreen<?> checkAndGetAttached(ScreenEvent event){
        if(event.getScreen() instanceof AbstractContainerScreen<?> screen){
            Player player = screen.getMinecraft().player;
            if(player==null) return null;
            if(!ClientModInfo.getClientConfig().attaching().get()) {
                ATTACHMENT_MANAGER = null;
                return null;
            }
            if(!MenuAttachabilityCache.isAttachable(screen)){
                ATTACHMENT_MANAGER = null;
                return null;
            }
            return ATTACHMENT_MANAGER;
        }
        return null;
    }

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event){
        if(event.getScreen() instanceof AbstractContainerScreen<?>)
            CachedConfig.readAndSyncClientConfigToServer(false);
    }

    @SubscribeEvent
    public static void closing(ScreenEvent.Closing event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            ATTACHMENT_MANAGER.closed(new IScreenEvent() {});
            ATTACHMENT_MANAGER = null;
        }
    }

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event){
        if(event.getScreen() instanceof AbstractContainerScreen<?> screen && !(screen instanceof EndlessInventoryScreen)){
            Player player = screen.getMinecraft().player;
            if(player==null) return;

            SyncedConfig syncedConfig = getSyncedConfig().getWith(player);
            if(!syncedConfig.checkForAttaching()) return;
            if(!MenuAttachabilityCache.isAttachable(screen)) return;

            CachedConfig.readAndSyncClientConfigToServer(false);

            if(ATTACHMENT_MANAGER==null){
                ModInfo.getPacketDistributor().sendToServer(new OpenEndInvPayload());
                ATTACHMENT_MANAGER = new AttachingScreen<>(screen);
                ATTACHMENT_MANAGER.init(new IScreenEvent() {
                    public void addListener(AbstractWidget widget){
                        event.addListener(widget);
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void renderPre(ScreenEvent.Render.Pre event){

        if(ATTACHMENT_MANAGER!=null){
            ATTACHMENT_MANAGER.renderPre(new IScreenEvent() {});
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
                    return event.getDeltaY();
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
