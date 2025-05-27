package com.kwwsyk.endinv.neoforge.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.commands.EndInvCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = ModInfo.MOD_ID)
public class CommandsReg {
    @SubscribeEvent
    public static void regCommands(final RegisterCommandsEvent event){
        EndInvCommand.register(event.getDispatcher());
    }
}
