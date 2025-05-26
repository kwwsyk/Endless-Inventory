package com.kwwsyk.endinv.neoforge.client.events;

import com.kwwsyk.endinv.common.client.gui.AttachedScreen;
import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.network.payloads.toServer.OpenEndInvPayload;
import com.kwwsyk.endinv.neoforge.ModInitializer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

import static com.kwwsyk.endinv.neoforge.ModInitializer.SYNCED_CONFIG;

@EventBusSubscriber(value = Dist.CLIENT,modid = ModInitializer.MOD_ID,bus = EventBusSubscriber.Bus.GAME)
public class ScreenAttachment {
    public static final  Map<AbstractContainerScreen<?>, AttachedScreen<?>> ATTACHMENT_MANAGER = new HashMap<>();

    private static AttachedScreen<?> checkAndGetAttached(ScreenEvent event){
        if(event.getScreen() instanceof AbstractContainerScreen<?> screen){
            Player player = screen.getMinecraft().player;
            if(player==null) return null;
            if(!player.getData(SYNCED_CONFIG).attaching()) {
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
            SyncedConfig.readAndSyncClientConfigToServer(false);
            SyncedConfig syncedConfig = player.getData(SYNCED_CONFIG);
            if(!syncedConfig.checkForAttaching()) return;

            ATTACHMENT_MANAGER.computeIfAbsent(screen, screen1 -> {
                PacketDistributor.sendToServer(new OpenEndInvPayload(false));
                return new AttachedScreen<>(screen);
            }).init(event);
        }
    }

    @SubscribeEvent
    public static void renderPre(ScreenEvent.Render.Pre event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.renderPre(event);
        }
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.render(event);
        }
    }

    @SubscribeEvent
    public static void mouseClicked(ScreenEvent.MouseButtonPressed.Pre event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.mouseClicked(event);
        }
    }

    @SubscribeEvent
    public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.mouseReleased(event);
        }
    }

    @SubscribeEvent
    public static void mouseDragged(ScreenEvent.MouseDragged.Pre event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.mouseDragged(event);
        }
    }

    @SubscribeEvent
    public static void mouseScrolled(ScreenEvent.MouseScrolled.Post event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.mouseScrolled(event);
        }
    }

    @SubscribeEvent
    public static void keyPressed(ScreenEvent.KeyPressed.Pre event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.keyPressed(event);
        }
    }

    @SubscribeEvent
    public static void charTyped(ScreenEvent.CharacterTyped.Pre event){
        var attached = checkAndGetAttached(event);
        if(attached!=null){
            attached.charTyped(event);
        }

    }
}
