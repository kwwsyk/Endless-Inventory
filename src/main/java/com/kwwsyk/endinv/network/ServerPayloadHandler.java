package com.kwwsyk.endinv.network;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.EndlessInventoryMenu;
import com.kwwsyk.endinv.network.payloads.EndInvRequestContentPayload;
import com.kwwsyk.endinv.network.payloads.EndInvSettings;
import com.kwwsyk.endinv.network.payloads.SetItemDisplayContentPayload;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.kwwsyk.endinv.ModInitializer.ENDINV_SETTINGS;


public abstract class ServerPayloadHandler {

    public static void handleEndInvSettings(EndInvSettings endInvSettings, IPayloadContext iPayloadContext){
        Player player = iPayloadContext.player();
        player.setData(ENDINV_SETTINGS,endInvSettings);
    }

    public static void handleEndInvRequests(EndInvRequestContentPayload payload, IPayloadContext iPayloadContext){
        ServerPlayer serverPlayer = (ServerPlayer) iPayloadContext.player();
        if(serverPlayer.containerMenu instanceof EndlessInventoryMenu menu){
            EndlessInventory endInv = (EndlessInventory) menu.getSourceInventory();
            int startIndex = payload.startIndex();
            int length = payload.length();
            NonNullList<ItemStack> stacks = NonNullList.withSize(length, ItemStack.EMPTY);
            for(int i=0;i<length;++i){
                int srcIndex = startIndex + i;
                if(srcIndex<endInv.getItemSize()) {
                    stacks.set(i, endInv.getItemStack(srcIndex));
                }else{
                    stacks.set(i,ItemStack.EMPTY);
                }
            }
            menu.getContainer().setChanged();
            menu.getContainer().setDisplay(startIndex,length);
            PacketDistributor.sendToPlayer(serverPlayer,new SetItemDisplayContentPayload(stacks));
        }
    }
}
