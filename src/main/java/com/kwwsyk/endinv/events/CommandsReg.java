package com.kwwsyk.endinv.events;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.commands.ConfigCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ModInitializer.MOD_ID)
public class CommandsReg {
    @SubscribeEvent
    public static void regCommands(final RegisterCommandsEvent event){
        ConfigCommand.register(event.getDispatcher());
    }
}
