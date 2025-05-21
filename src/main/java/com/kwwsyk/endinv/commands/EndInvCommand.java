package com.kwwsyk.endinv.commands;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.ServerLevelEndInv;
import com.kwwsyk.endinv.data.EndlessInventoryData;
import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.util.Accessibility;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public class EndInvCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("endinv")
                .then(
                        Commands.literal("backup")
                        .executes(
                                context -> {
                                    var result = EndlessInventoryData.backup(context.getSource().getLevel());
                                    if(result.success()){
                                        context.getSource().sendSuccess(() -> Component.literal("Backed up at "+result.message()), true);
                                        return 1;
                                    }else {
                                        context.getSource().sendFailure(Component.literal("Cannot backup as"+result.message()));
                                        return 0;
                                    }
                                }
                        )
                )
                .then(
                        Commands.literal("ofIndex")
                        .executes(
                                context->
                                        getCurrentIndex(context.getSource()))
                        .then(
                                Commands.argument("index", IntegerArgumentType.integer())
                                .executes(
                                    context ->
                                            byIndexGet(context.getSource(),
                                                    IntegerArgumentType.getInteger(context,"index")
                                            )
                                )
                                .then(
                                        Commands.literal("open")
                                        .executes(
                                            context ->
                                                    byIndexOpen(context.getSource(),IntegerArgumentType.getInteger(context,"index"))
                                        )
                                )
                                .then(
                                        Commands.literal("setDefault")
                                        .executes(context ->
                                                byIndexSetDefault(context.getSource(),IntegerArgumentType.getInteger(context,"index"))
                                        )
                                )
                                .then(
                                        Commands.literal("setOwner")
                                        .executes(
                                                context ->
                                                        byIndexSetOwner(context.getSource(),IntegerArgumentType.getInteger(context,"index"))
                                        )
                                )
                                .then(
                                        Commands.literal("addWhitelist")
                                        .executes(
                                                context ->
                                                        byIndexAddWhitelist(context.getSource(),IntegerArgumentType.getInteger(context,"index"))
                                        )
                                )
                                .then(
                                        Commands.literal("removeWhitelist")
                                        .executes(
                                                context ->
                                                        byIndexRemoveWhitelist(context.getSource(),IntegerArgumentType.getInteger(context,"index"))
                                        )
                                )
                                .then(
                                        Commands.literal("setAccessibility")
                                        .then(
                                                Commands.literal("public")
                                                .executes(
                                                        context ->
                                                                byIndexSetAccessibility(context.getSource(),IntegerArgumentType.getInteger(context,"index"),Accessibility.PUBLIC)
                                                )
                                        ).then(
                                                Commands.literal("restricted")
                                                .executes(
                                                        context ->
                                                                byIndexSetAccessibility(context.getSource(),IntegerArgumentType.getInteger(context,"index"),Accessibility.RESTRICTED)
                                                )
                                        ).then(
                                                Commands.literal("private")
                                                .executes(
                                                        context ->
                                                                byIndexSetAccessibility(context.getSource(),IntegerArgumentType.getInteger(context,"index"),Accessibility.PRIVATE)
                                                )
                                        )
                                )
                                .then(
                                        Commands.literal("remove")
                                        .then(
                                                Commands.argument("forceRemove", BoolArgumentType.bool())
                                                .executes(
                                                        context ->
                                                                byIndexRemove(context.getSource(),IntegerArgumentType.getInteger(context,"index"), BoolArgumentType.getBool(context,"forceRemove"))
                                                )
                                        )
                                )
                        )
                )
                .then(
                        Commands.literal("new")
                        .executes(
                                context ->
                                        createNew(context.getSource(),Accessibility.PUBLIC)
                        )
                        .then(
                                Commands.literal("public")
                                .executes(
                                        context ->
                                                createNew(context.getSource(),Accessibility.PUBLIC)
                                )
                        ).then(
                                Commands.literal("restricted")
                                .executes(
                                        context ->
                                                createNew(context.getSource(),Accessibility.RESTRICTED)
                                )
                        ).then(
                                Commands.literal("private")
                                .executes(
                                        context ->
                                                createNew(context.getSource(),Accessibility.PRIVATE)
                                )
                        )
                )
        );
    }



    private static int byIndexRemove(CommandSourceStack source, int index, boolean forced) {
        //check index
        EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);
        if(endlessInventory==null){
            source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
            return 0;
        }
        //try backup
        EndlessInventoryData.BackupResult result = EndlessInventoryData.backup(source.getLevel());
        //handle result
        if(!result.success()){
            if(forced){
                ServerLevelEndInv.levelEndInvData.byIndexRemove(index);
                source.sendSuccess(()->Component.literal("Force-removed "+endlessInventory.getUuid()),true);
                return index;
            }else {
                source.sendFailure(Component.literal("Cannot backup as "+ result.message()));
                return 0;
            }
        }else {
            source.sendSuccess(() -> Component.literal("Backed up at "+result.message()), true);
            source.sendSuccess(() -> Component.literal("Removed " + endlessInventory.getUuid()), true);
            return index;
        }
    }

    private static int byIndexAddWhitelist(CommandSourceStack source, int index) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);

            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return 0;
            }
            {
                endlessInventory.white_list.add(player.getUUID());
                source.sendSuccess(()->Component.literal("Add "+player.getName().getString()+" to "+endlessInventory.getUuid()+"'s whitelist."),true);
            }
            return index;
        }catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return 0;
        }
    }

    private static int byIndexRemoveWhitelist(CommandSourceStack source, int index) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);

            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return 0;
            }
            {
                if(endlessInventory.white_list.remove(player.getUUID())) {
                    source.sendSuccess(() -> Component.literal("Remove " + player.getName().getString() + " from " + endlessInventory.getUuid() + "'s whitelist."), true);
                }else {
                    source.sendFailure(Component.literal(player.getName().getString() + " is not in " + endlessInventory.getUuid() + "'s whitelist."));
                    return -1;
                }
            }
            return index;
        }catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return 0;
        }
    }

    private static int byIndexSetAccessibility(CommandSourceStack source, int index, Accessibility accessibility) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);

            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return 0;
            }
            {
                endlessInventory.setAccessibility(accessibility);
                source.sendSuccess(()->Component.literal("Set "+endlessInventory.getUuid()+"'s accessibility to"+accessibility),true);
            }
            return index;
        }catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return 0;
        }
    }

    private static int byIndexSetOwner(CommandSourceStack source, int index) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);

            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return 0;
            }
            {
                endlessInventory.setOwner(player.getUUID());
                source.sendSuccess(()->Component.literal("Set "+endlessInventory.getUuid()+"'s owner to"+player.getName().getString()),true);
            }
            return index;
        }catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return 0;
        }
    }

    private static int createNew(CommandSourceStack source, Accessibility accessibility) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endInv;
            switch (accessibility){
                case PUBLIC -> {
                    endInv = ServerLevelEndInv.createPublicEndInv();
                    source.sendSuccess(()->Component.literal("Created a new public endInv with uuid: "+endInv.getUuid()),true);
                }
                case RESTRICTED -> {
                    endInv = ServerLevelEndInv.createPublicEndInv();
                    endInv.setAccessibility(Accessibility.RESTRICTED);
                    source.sendSuccess(()->Component.literal("Created a new white_list endInv with uuid: "+endInv.getUuid()),true);
                    endInv.white_list.add(player.getUUID());
                    source.sendSuccess(()->Component.literal("Add current player to white list"),true);
                }
                case PRIVATE -> {
                    endInv = ServerLevelEndInv.createPublicEndInv();
                    endInv.setAccessibility(Accessibility.PRIVATE);
                    endInv.setOwner(player.getUUID());
                    source.sendSuccess(
                            ()->Component.literal("Created a new private endInv with uuid: "+endInv.getUuid()
                                                            +", with owner : "+player.getName().getString())
                            ,true);
                }
            }
            return 1;
        }catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return 0;
        }
    }

    private static int byIndexSetDefault(CommandSourceStack source, int index) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);

            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return 0;
            }
            {
                player.setData(ModInitializer.ENDINV_UUID,endlessInventory.getUuid());
                source.sendSuccess(()->Component.literal("Set player's default endInv with uuid: "+endlessInventory.getUuid()),true);
            }
            return index;
        }catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return 0;
        }
    }

    private static int byIndexOpen(CommandSourceStack source, int index) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);
            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return 0;
            }
            {
                ServerLevelEndInv.TEMP_ENDINV_REG.put(player, endlessInventory);
                player.openMenu(new SimpleMenuProvider(EndlessInventoryMenu::createWithTemp, Component.empty()));
            }
            return index;
        }catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return 0;
        }
    }

    private static int byIndexGet(CommandSourceStack source, int index) {
        try{
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);
            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return 0;
            }
            source.sendSuccess(()->Component.literal("Found endInv with uuid: "+endlessInventory.getUuid()),true);
            return index;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int getCurrentIndex(CommandSourceStack source) {
        try {
            ServerPlayer serverPlayer = source.getPlayerOrException();
            if (!ServerLevelEndInv.hasEndInvUuid(serverPlayer)) {
                source.sendFailure(Component.literal("This player has not EndInv."));
                return 0;
            }
            var optional = ServerLevelEndInv.getEndInvForPlayer(serverPlayer);
            if(optional.isPresent()){
                EndlessInventory endlessInventory = optional.get();
                int index = ServerLevelEndInv.levelEndInvData.getIndex(endlessInventory);
                source.sendSuccess(() -> Component.literal("EndInv index: " + index), true);
                return index;
            }else {
                source.sendFailure(Component.literal("Cannot get EndInv for player."));
                return 0;
            }

        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return 0;
        }
    }
}
