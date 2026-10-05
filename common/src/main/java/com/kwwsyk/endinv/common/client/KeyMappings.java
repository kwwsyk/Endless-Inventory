package com.kwwsyk.endinv.common.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.InputWithModifiers;
import org.lwjgl.glfw.GLFW;

public class KeyMappings {

    public static final String CATEGORY = "key.categories.endinv";

    public enum ActiveCondition{
        GUI {
            @Override
            public boolean isActive() {
                return Minecraft.getInstance().gui.screen() != null;
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
        CTRL(InputConstants.MOD_CONTROL),
        NONE(0);
        @InputWithModifiers.Modifiers
        int modifier;

        Modifier(int modifier){
            this.modifier = modifier;
        }

        public boolean matchesModifier(InputWithModifiers input){
            return input.modifiers() == modifier;
        }
    }

    public record KeyParam(String key, InputConstants.Type type, int keyCode, ActiveCondition condition, Modifier modifier, KeyMapping.Category category){}

    public static final KeyParam OPEN_MENU = new KeyParam(
            "key.endinv.open_endinv_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            ActiveCondition.IN_GAME,
            Modifier.NONE,
            KeyMapping.Category.INVENTORY
    );
    public static final KeyParam QUICK_MOVE = new KeyParam(
            "key.endinv.quick_move_item",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_1,
            ActiveCondition.GUI,
            Modifier.CTRL,
            KeyMapping.Category.INVENTORY
    );
    public static final KeyParam STAR_ITEM = new KeyParam(
            "key.endinv.star_item",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_A,
            ActiveCondition.GUI,
            Modifier.NONE,
            KeyMapping.Category.INVENTORY
    );
    public static final KeyParam STAR_ITEM_ALTER = new KeyParam(
            "key.endinv.star_item",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F13,
            ActiveCondition.GUI,
            Modifier.NONE,
            KeyMapping.Category.INVENTORY
    );
}
