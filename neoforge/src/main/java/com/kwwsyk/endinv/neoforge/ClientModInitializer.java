package com.kwwsyk.endinv.neoforge;

import com.kwwsyk.endinv.common.AbstractClientModInitializer;
import com.kwwsyk.endinv.common.client.IContainerScreenHelper;
import com.kwwsyk.endinv.common.client.IInputHandler;
import com.kwwsyk.endinv.common.client.KeyMappings;
import com.kwwsyk.endinv.neoforge.client.config.ClientConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.util.Lazy;

import static com.kwwsyk.endinv.common.client.KeyMappings.*;

public class ClientModInitializer extends AbstractClientModInitializer {

    public static Lazy<KeyMapping> OPEN_MENU_KEY = regKey(OPEN_MENU);
    public static Lazy<KeyMapping> QUICK_MOVE_KEY = regKey(QUICK_MOVE);
    public static Lazy<KeyMapping> STAR_ITEM_KEY = regKey(STAR_ITEM);


    static void init(IEventBus modEventBus, ModContainer container){
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG_SPEC);
        new ClientModInitializer();
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(ClientModInitializer::regKeyMapping);
    }

    private static void regKeyMapping(RegisterKeyMappingsEvent event){
        event.register(OPEN_MENU_KEY.get());
        event.register(QUICK_MOVE_KEY.get());
        if(!ModList.get().isLoaded("jei")) {
            event.register(STAR_ITEM_KEY.get());
        } else {
            // Ensure the input handler checks the same mapping we register
            STAR_ITEM_KEY = Lazy.of(() -> new KeyMapping(
                    STAR_ITEM.key(),
                    KeyConflictContext.GUI,
                    KeyModifier.NONE,
                    STAR_ITEM.type(),
                    302, // F13 (Insert on some keyboards)
                    KeyMappings.CATEGORY
            ));
            event.register(STAR_ITEM_KEY.get());
        }
    }

    private static Lazy<KeyMapping> regKey(KeyMappings.EndInvKey key) {
        return Lazy.of(
                    ()-> new KeyMapping(
                            key.key(),
                            switch (key.condition()){
                                case GUI -> KeyConflictContext.GUI;
                                case IN_GAME -> KeyConflictContext.IN_GAME;
                            },
                            key.modifier() == KeyMappings.Modifier.CTRL ? KeyModifier.CONTROL : KeyModifier.NONE,
                            key.type(),
                            key.keyCode(),
                            KeyMappings.CATEGORY
                            )
            );
    }

    @Override
    protected void loadClientConfig() {
    }

    @Override
    protected IInputHandler getInputHandler() {
        return new IInputHandler() {
            @Override
            public boolean isActiveAndMatches(KeyMapping keyMapping, InputConstants.Key key) {
                return keyMapping.isActiveAndMatches(key);
            }

            @Override
            public boolean isActiveAndMatches(EndInvKey endInvKey, InputConstants.Key key) {
                if (endInvKey.equals(OPEN_MENU)) {
                    return OPEN_MENU_KEY.get().isActiveAndMatches(key);
                } else if (endInvKey.equals(QUICK_MOVE)) {
                    return QUICK_MOVE_KEY.get().isActiveAndMatches(key);
                } else if (endInvKey.equals(STAR_ITEM)) {
                    return STAR_ITEM_KEY.get().isActiveAndMatches(key);
                } else {
                    return regKey(endInvKey).get().isActiveAndMatches(key);
                }
            }
        };
    }

    @Override
    protected IContainerScreenHelper getScreenHelper() {
        return new IContainerScreenHelper() {
            @Override
            public int getGuiLeft(AbstractContainerScreen<?> screen) {
                return screen.getGuiLeft();
            }

            @Override
            public int getGuiTop(AbstractContainerScreen<?> screen) {
                return screen.getGuiTop();
            }

            @Override
            public int getGuiXSize(AbstractContainerScreen<?> screen) {
                return screen.getXSize();
            }

            @Override
            public int getGuiYSize(AbstractContainerScreen<?> screen) {
                return screen.getYSize();
            }
        };
    }
}
