package com.kwwsyk.endinv.neoforge;

import com.kwwsyk.endinv.common.AbstractClientModInitializer;
import com.kwwsyk.endinv.common.client.IContainerScreenHelper;
import com.kwwsyk.endinv.common.client.KeyMappings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.util.Lazy;

import static com.kwwsyk.endinv.common.client.KeyMappings.*;

public class ClientModInitializer extends AbstractClientModInitializer {

    public ClientModInitializer(){
        super();
        AbstractClientModInitializer.ENDINV_CLIENT = this;
    }

    void init(IEventBus modEventBus){
        modEventBus.addListener(this::regKeyMapping);
    }

    private void regKeyMapping(RegisterKeyMappingsEvent event){
        event.register(KEY_MAPPING_MAP.get(OPEN_MENU));
        event.register(KEY_MAPPING_MAP.get(QUICK_MOVE));
        if(!ModList.get().isLoaded("jei")) {
            event.register(KEY_MAPPING_MAP.get(STAR_ITEM));
        } else {
            event.register(KEY_MAPPING_MAP.get(STAR_ITEM_ALTER));
        }
    }

    @Override
    protected void regKeyParam(KeyParam key) {
        var reg = Lazy.of(
                    ()-> new KeyMapping(
                            key.key(),
                            switch (key.condition()){
                                case GUI -> KeyConflictContext.GUI;
                                case IN_GAME -> KeyConflictContext.IN_GAME;
                            },
                            key.modifier() == KeyMappings.Modifier.CTRL ? KeyModifier.CONTROL : KeyModifier.NONE,
                            key.type(),
                    key.keyCode(),
                    key.category()
                            )
            );
        KEY_MAPPING_MAP.put(key, reg.get());
    }

    @Override
    protected IContainerScreenHelper getScreenHelper() {
        return new IContainerScreenHelper() {
            @Override
            public int getGuiLeft(AbstractContainerScreen<?> screen) {
                return screen.getLeftPos();
            }

            @Override
            public int getGuiTop(AbstractContainerScreen<?> screen) {
                return screen.getTopPos();
            }

            @Override
            public int getGuiXSize(AbstractContainerScreen<?> screen) {
                return screen.getImageWidth();
            }

            @Override
            public int getGuiYSize(AbstractContainerScreen<?> screen) {
                return screen.getImageHeight();
            }
        };
    }
}
