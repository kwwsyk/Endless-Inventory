package com.kwwsyk.endinv.neoforge.client.events;

import com.kwwsyk.endinv.neoforge.ModInitializer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD,value = Dist.CLIENT,modid = ModInitializer.MOD_ID)
public class KeyMappingReg {


    public static final Lazy<KeyMapping> OPEN_ENDINV_KEY = Lazy.of(
            ()->new KeyMapping(
                    "key.endinv.open_endinv_menu",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_I,
                    "key.categories.endinv"
            )
    );
    public static final Lazy<KeyMapping> QUICK_MOVE_KEY = Lazy.of(
            ()->new KeyMapping(
                    "key.endinv.quick_move_item",
                    KeyConflictContext.GUI,
                    KeyModifier.CONTROL,
                    InputConstants.Type.MOUSE,
                    GLFW.GLFW_MOUSE_BUTTON_1,
                    "key.categories.endinv"
            )
    );
    public static final Lazy<KeyMapping> STAR_ITEM_KEY = Lazy.of(
            ()->new KeyMapping(
                    "key.endinv.star_item",
                    KeyConflictContext.GUI,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_A,
                    "key.categories.endinv"
            )
    );

    @SubscribeEvent
    public static void registerKey(RegisterKeyMappingsEvent event){

        event.register(OPEN_ENDINV_KEY.get());
        event.register(QUICK_MOVE_KEY.get());
        if(!ModList.get().isLoaded("jei")){
            event.register(STAR_ITEM_KEY.get());
        }
    }
}
