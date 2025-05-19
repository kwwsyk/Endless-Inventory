package com.kwwsyk.endinv.commands;

import com.kwwsyk.endinv.ServerLevelEndInv;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.kwwsyk.endinv.ModInitializer.SYNCED_CONFIG;

public class ConfigCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("endinv")
                .then(
                        Commands.literal("rows")
                        .executes(
                                context->
                                        getRows(context.getSource()))
                        .then(
                                Commands.argument("rows", IntegerArgumentType.integer())
                                .executes(
                                    context ->
                                            setRows(context.getSource(),
                                                    IntegerArgumentType.getInteger(context,"rows")
                                            )
                                )
                        )
                )
        );
    }

    private static int getRows(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer serverPlayer = source.getPlayerOrException();
        if(!ServerLevelEndInv.hasEndInvUuid(serverPlayer)){
            source.sendFailure(Component.literal("This player has not EndInv."));
            return 0;
        }
        int ret = serverPlayer.getData(SYNCED_CONFIG).pageData().rows();
        source.sendSuccess(()-> Component.literal(""+ret),true);
        return ret;
    }

    private static int setRows(CommandSourceStack source, int rows) throws CommandSyntaxException{
        ServerPlayer serverPlayer = source.getPlayerOrException();
        if(!ServerLevelEndInv.hasEndInvUuid(serverPlayer)){
            source.sendFailure(Component.literal("This player has not EndInv."));
            return 0;
        }
        serverPlayer.setData(SYNCED_CONFIG, serverPlayer.getData(SYNCED_CONFIG).ofRowChanged(rows));
        PacketDistributor.sendToPlayer(serverPlayer,serverPlayer.getData(SYNCED_CONFIG).ofRowChanged(rows));
        source.sendSuccess(()->Component.literal(""+rows),true);
        return rows;
    }
}
