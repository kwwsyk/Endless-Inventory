package com.kwwsyk.endinv.common.commands;

import com.kwwsyk.endinv.common.options.config.ConfigEntryImpl;
import com.kwwsyk.endinv.common.options.config.command.CommandBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ConfigCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                List<ConfigEntryImpl<?>> configEntries, String subEntryName) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("endinv");
        LiteralArgumentBuilder<CommandSourceStack> configs = Commands.literal(subEntryName)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("Endinv configs root. Use /endinv config <path>"), false);
                    return 1;
                });
        for (ConfigEntryImpl<?> cfg : configEntries) configs.then(CommandBuilder.buildNode(cfg));
        root.then(configs);
        dispatcher.register(root);
    }
}
