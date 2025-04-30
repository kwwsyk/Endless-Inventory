package com.kwwsyk.endinv.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.NotNull;

public class EndInvConfigScreen extends Screen implements IConfigScreenFactory {

    private Screen backScreen;

    protected EndInvConfigScreen(Screen backScreen) {
        super(Component.translatable("screen.endinv.configuration"));
        this.backScreen = backScreen;
    }

    @Override
    public @NotNull Screen createScreen(@NotNull ModContainer modContainer, @NotNull Screen backScreen) {
        return new EndInvConfigScreen(backScreen);
    }
}
