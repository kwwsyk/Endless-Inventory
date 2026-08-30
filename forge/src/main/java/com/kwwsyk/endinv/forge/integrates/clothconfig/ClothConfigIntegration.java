package com.kwwsyk.endinv.forge.integrates.clothconfig;

import com.kwwsyk.endinv.common.client.ClientModInfo;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

public final class ClothConfigIntegration {

    private ClothConfigIntegration() {
    }

    public static void register(ModContainer container) {
        if (!ModList.get().isLoaded("cloth_config")) {
            return;
        }
        ClientModInfo.setConfigScreenFactory(ClothConfigScreenBuilder::create);
        container.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> ClothConfigScreenBuilder.create(parent))
        );
    }
}
