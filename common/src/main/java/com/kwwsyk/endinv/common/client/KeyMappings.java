package com.kwwsyk.endinv.common.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class KeyMappings {

    public static final String CATEGORY = "key.categories.endinv";

    public enum ActiveCondition{
        GUI {
            @Override
            public boolean isActive() {
                return Minecraft.getInstance().screen != null;
            }
        },
        IN_GAME {
            @Override
            public boolean isActive() {
                return !GUI.isActive();
            }
        };

        public abstract boolean isActive();
    }

    public enum Modifier{
        CTRL,NONE;
    }

    public record EndInvKey(String key, InputConstants.Type type, int keyCode, ActiveCondition condition, Modifier modifier){}

    public static final EndInvKey OPEN_MENU = new EndInvKey("key.endinv.open_endinv_menu",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_I,ActiveCondition.IN_GAME,Modifier.NONE);
    public static final EndInvKey QUICK_MOVE = new EndInvKey("key.endinv.quick_move_item",InputConstants.Type.MOUSE,GLFW.GLFW_MOUSE_BUTTON_1,ActiveCondition.GUI,Modifier.CTRL);
    public static final EndInvKey STAR_ITEM = new EndInvKey("key.endinv.star_item",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_A,ActiveCondition.GUI,Modifier.NONE);
}
