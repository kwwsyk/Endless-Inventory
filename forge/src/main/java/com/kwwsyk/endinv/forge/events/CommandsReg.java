package com.kwwsyk.endinv.forge.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.commands.ConfigCommand;
import com.kwwsyk.endinv.common.commands.EndInvCommand;
import com.kwwsyk.endinv.common.options.ServerConfigs;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID)
public class CommandsReg {
    @SubscribeEvent
    public static void regCommands(final RegisterCommandsEvent event){
        EndInvCommand.register(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher(), ServerConfigs.getConfigs(), "config");
    }
}
