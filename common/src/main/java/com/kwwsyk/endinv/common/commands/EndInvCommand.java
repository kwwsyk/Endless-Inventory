package com.kwwsyk.endinv.common.commands;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.autopick.options.PickupHelperOptions;
import com.kwwsyk.endinv.common.data.EndlessInventoryData;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.options.ServerConfigs;
import com.kwwsyk.endinv.common.options.config.IConfigValue;
import com.kwwsyk.endinv.common.util.Accessibility;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

import java.util.LinkedHashMap;
import java.util.Map;

public class EndInvCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("endinv").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("backup")
                        .executes(context -> {
                            var result = EndlessInventoryData.backup(context.getSource().getLevel());
                            if(result.success()){
                                context.getSource().sendSuccess(() -> Component.literal("Backed up at "+result.message()), true);
                                return 1;
                            } else {
                                context.getSource().sendFailure(Component.literal("Cannot backup as "+result.message()));
                                return -1;
                            }
                        })
                )
                .then(Commands.literal("ofIndex")
                        .executes(context -> getCurrentIndex(context.getSource()))
                        .then(Commands.argument("index", IntegerArgumentType.integer())
                                .executes(context -> byIndexGet(context.getSource(), IntegerArgumentType.getInteger(context,"index")))
                                .then(Commands.literal("open")
                                        .executes(context -> byIndexOpen(context.getSource(),IntegerArgumentType.getInteger(context,"index")))
                                )
                                .then(Commands.literal("setDefault")
                                        .executes(context -> byIndexSetDefault(context.getSource(),IntegerArgumentType.getInteger(context,"index")))
                                )
                                .then(Commands.literal("setOwner")
                                        .executes(context -> byIndexSetOwner(context.getSource(),IntegerArgumentType.getInteger(context,"index")))
                                )
                                .then(Commands.literal("addWhitelist")
                                        .executes(context -> byIndexAddWhitelist(context.getSource(),IntegerArgumentType.getInteger(context,"index")))
                                )
                                .then(Commands.literal("removeWhitelist")
                                        .executes(context -> byIndexRemoveWhitelist(context.getSource(),IntegerArgumentType.getInteger(context,"index")))
                                )
                                .then(Commands.literal("setAccessibility")
                                        .then(Commands.literal("public")
                                                .executes(context -> byIndexSetAccessibility(context.getSource(),IntegerArgumentType.getInteger(context,"index"), Accessibility.PUBLIC)))
                                        .then(Commands.literal("restricted")
                                                .executes(context -> byIndexSetAccessibility(context.getSource(),IntegerArgumentType.getInteger(context,"index"), Accessibility.RESTRICTED)))
                                        .then(Commands.literal("private")
                                                .executes(context -> byIndexSetAccessibility(context.getSource(),IntegerArgumentType.getInteger(context,"index"), Accessibility.PRIVATE)))
                                )
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("forceRemove", BoolArgumentType.bool())
                                                .executes(context -> byIndexRemove(context.getSource(),IntegerArgumentType.getInteger(context,"index"), BoolArgumentType.getBool(context,"forceRemove"))))
                                )
                        )
                )
                .then(Commands.literal("new")
                        .executes(context -> createNew(context.getSource(),Accessibility.PUBLIC))
                        .then(Commands.literal("public")
                                .executes(context -> createNew(context.getSource(),Accessibility.PUBLIC))
                        ).then(Commands.literal("restricted")
                                .executes(context -> createNew(context.getSource(),Accessibility.RESTRICTED))
                        ).then(Commands.literal("private")
                                .executes(context -> createNew(context.getSource(),Accessibility.PRIVATE))
                        )
                )
                .then(autoPickCommand())
        );
    }

    private static final PickupHelperOptions PICKUP_HELPER = ServerConfigs.PICKUP_HELPER;
    private static final Map<IConfigValue<Boolean>, Boolean> DEFAULT_AUTOPICK_PLAN = createDefaultAutoPickPlan();

    private static Map<IConfigValue<Boolean>, Boolean> createDefaultAutoPickPlan() {
        Map<IConfigValue<Boolean>, Boolean> plan = new LinkedHashMap<>();
        plan.put(PICKUP_HELPER.EXP_DROPS.PROTECT_DROPS, true);
        plan.put(PICKUP_HELPER.EXP_DROPS.TOUCH_DIRECTLY, true);
        plan.put(PICKUP_HELPER.ITEM_DROPS.PROTECT_DROPS, true);
        plan.put(PICKUP_HELPER.ITEM_DROPS.SEND_TO_INVENTORY, true);
        plan.put(PICKUP_HELPER.ITEM_DROPS.PICK_TO_ENDINV, true);
        return plan;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> autoPickCommand() {
        return Commands.literal("autoPick")
                .executes(context -> printAutoPickStatus(context.getSource()))
                .then(Commands.literal("print").executes(context -> printAutoPickSettings(context.getSource())))
                .then(Commands.literal("enable").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> setRecommendedAutoPick(context.getSource(), true)))
                .then(Commands.literal("disable").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> setRecommendedAutoPick(context.getSource(), false)));
    }

    private static int printAutoPickStatus(CommandSourceStack source) {
        boolean enabled = DEFAULT_AUTOPICK_PLAN.entrySet().stream().allMatch(entry -> entry.getKey().get().equals(entry.getValue()))
                && PICKUP_HELPER.EXP_DROPS.DIRECTED_DISTRIBUTE.get() == 0
                && PICKUP_HELPER.ITEM_DROPS.DIRECTED_DISTRIBUTE.get() == 0
                && !PICKUP_HELPER.ITEM_DROPS.DIRECTLY_SEND_TO_ENDINV.get();
        boolean disabled = DEFAULT_AUTOPICK_PLAN.keySet().stream().noneMatch(IConfigValue::get);
        String status = enabled ? "enabled" : disabled ? "disabled" : "custom";
        source.sendSystemMessage(Component.literal("AutoPick is in " + status + " configuration status."));
        return 0;
    }

    private static int printAutoPickSettings(CommandSourceStack source) {
        source.sendSystemMessage(Component.literal("pickup_helper.item_drops_config"));
        for (var entry : PICKUP_HELPER.ITEM_DROPS.fields()) {
            source.sendSystemMessage(Component.literal("  " + entry.key() + " = " + entry.get()));
        }
        source.sendSystemMessage(Component.literal("pickup_helper.exp_drops_config"));
        for (var entry : PICKUP_HELPER.EXP_DROPS.fields()) {
            source.sendSystemMessage(Component.literal("  " + entry.key() + " = " + entry.get()));
        }
        return 0;
    }

    private static int setRecommendedAutoPick(CommandSourceStack source, boolean enabled) {
        for (var entry : DEFAULT_AUTOPICK_PLAN.entrySet()) {
            entry.getKey().set(enabled && entry.getValue());
        }
        PICKUP_HELPER.ITEM_DROPS.DIRECTED_DISTRIBUTE.set(0);
        PICKUP_HELPER.ITEM_DROPS.DIRECTLY_SEND_TO_ENDINV.set(false);
        PICKUP_HELPER.ITEM_DROPS.ENDINV_AFTER_INVENTORY.set(enabled);
        PICKUP_HELPER.EXP_DROPS.DIRECTED_DISTRIBUTE.set(0);
        PICKUP_HELPER.EXP_DROPS.GIVE_DIRECTLY.set(false);
        source.sendSystemMessage(Component.literal("AutoPick has been " + (enabled ? "enabled" : "disabled") + "."));
        return 1;
    }

    private static int byIndexRemove(CommandSourceStack source, int index, boolean forced) {
        EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);
        if(endlessInventory==null){
            source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
            return -1;
        }
        EndlessInventoryData.BackupResult result = EndlessInventoryData.backup(source.getLevel());
        if(!result.success() && !forced){
            source.sendFailure(Component.literal("Cannot backup as "+ result.message()));
            return -1;
        }
        ServerLevelEndInv.levelEndInvData.byIndexRemove(index);
        source.sendSuccess(() -> Component.literal("Removed " + endlessInventory.getUuid()), true);
        return index;
    }

    private static int byIndexAddWhitelist(CommandSourceStack source, int index) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);
            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return -1;
            }
            if(endlessInventory.isOwner(player) || Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)) {
                endlessInventory.white_list.add(player.getUUID());
                source.sendSuccess(() -> Component.literal("Add " + player.getName().getString() + " to " + endlessInventory.getUuid() + "'s whitelist."), true);
                return index;
            } else {
                source.sendFailure(Component.translatable("endinv.callback.not_owner"));
                return -1;
            }
        }catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return -1;
        }
    }

    private static int byIndexRemoveWhitelist(CommandSourceStack source, int index) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);
            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return -1;
            }
            if(endlessInventory.isOwner(player) || Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)) {
                if(endlessInventory.white_list.remove(player.getUUID())) {
                    source.sendSuccess(() -> Component.literal("Remove " + player.getName().getString() + " from " + endlessInventory.getUuid() + "'s whitelist."), true);
                } else {
                    source.sendFailure(Component.literal(player.getName().getString() + " is not in " + endlessInventory.getUuid() + "'s whitelist."));
                    return -1;
                }
            } else {
                source.sendFailure(Component.translatable("endinv.callback.not_owner"));
                return -1;
            }
            return index;
        }catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return -1;
        }
    }

    private static int byIndexSetAccessibility(CommandSourceStack source, int index, Accessibility accessibility) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);

            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return -1;
            }
            if(endlessInventory.isOwner(player) || Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)) {
                endlessInventory.setAccessibility(accessibility);
                source.sendSuccess(()->Component.literal("Set "+endlessInventory.getUuid()+"'s accessibility to "+accessibility),true);
                return 1;
            } else {
                source.sendFailure(Component.translatable("endinv.callback.not_owner"));
                return -1;
            }
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return -1;
        }
    }

    private static int createNew(CommandSourceStack source, Accessibility accessibility) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endInv;
            switch (accessibility) {
                case PUBLIC -> {
                    endInv = ServerLevelEndInv.createPublicEndInv();
                    endInv.setAccessibility(Accessibility.PUBLIC);
                    source.sendSuccess(() -> Component.literal("Created a new public endInv with uuid: "+endInv.getUuid()),true);
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
                    source.sendSuccess(() -> Component.literal("Created a new private endInv with uuid: "+endInv.getUuid()
                            +", with owner : "+player.getName().getString()), true);
                }
            }
            return 1;
        }catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return -1;
        }
    }

    private static int byIndexSetDefault(CommandSourceStack source, int index) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);

            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return -1;
            }
            ModRegistries.NbtAttachments.getEndInvUUID().setTo(player,endlessInventory.getUuid());
            source.sendSuccess(()->Component.literal("Set player's default endInv with uuid: "+endlessInventory.getUuid()),true);
            return index;
        }catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return -1;
        }
    }

    private static int byIndexOpen(CommandSourceStack source, int index) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);
            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return -1;
            }
            if(endlessInventory.accessible(player)){
                ServerLevelEndInv.TEMP_ENDINV_REG.put(player, endlessInventory);
                player.openMenu(new SimpleMenuProvider(EndlessInventoryMenu::createWithTemp, Component.empty()));
            } else if(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)){
                ServerLevelEndInv.TEMP_ENDINV_REG.put(player, endlessInventory);
                player.openMenu(new SimpleMenuProvider(EndlessInventoryMenu::createWithTemp, Component.empty()));
                source.sendSuccess(()->Component.literal("Opened an unaccessible endInv for op"),true);
            } else {
                source.sendFailure(Component.translatable("endinv.callback.no_access"));
                return -1;
            }
            return index;
        }catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return -1;
        }
    }

    private static int byIndexGet(CommandSourceStack source, int index) {
        try{
            EndlessInventory endlessInventory = ServerLevelEndInv.levelEndInvData.fromIndex(index);
            if(endlessInventory==null){
                source.sendFailure(Component.literal("Cannot get EndInv by index "+index));
                return -1;
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
                return -1;
            }
            var optional = ServerLevelEndInv.getEndInvForPlayer(serverPlayer);
            if(optional.isPresent()){
                EndlessInventory endlessInventory = optional.get();
                int index = ServerLevelEndInv.levelEndInvData.getIndex(endlessInventory);
                source.sendSuccess(() -> Component.literal("EndInv index: " + index), true);
                return index;
            } else {
                source.sendFailure(Component.literal("Cannot get EndInv for player."));
                return -1;
            }
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return -1;
        }
    }

    private static int byIndexSetOwner(CommandSourceStack source, int index) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EndlessInventory endInv = ServerLevelEndInv.levelEndInvData.fromIndex(index);
            if (endInv == null) {
                source.sendFailure(Component.literal("Cannot get EndInv by index " + index));
                return -1;
            }
            if (!Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)) {
                source.sendFailure(Component.translatable("commands.generic.permission"));
                return -1;
            }
            endInv.setOwner(player.getUUID());
            source.sendSuccess(() -> Component.literal("Set owner for endInv " + endInv.getUuid() + " to " + player.getName().getString()), true);
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("A player must execute this command."));
            return -1;
        }
    }
}
