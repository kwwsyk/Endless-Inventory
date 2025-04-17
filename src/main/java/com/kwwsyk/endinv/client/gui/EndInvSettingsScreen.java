package com.kwwsyk.endinv.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EndInvSettingsScreen extends Screen {
    public EndInvSettingsScreen(EndlessInventoryScreen endlessInventoryScreen) {
        super(Component.literal("Settings"));
    }
}
