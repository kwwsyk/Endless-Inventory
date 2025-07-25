package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.menu.page.pageManager.AttachingManager;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

/**Send to server,
 * to open {@link EndlessInventoryMenu} when player is not opening a menu,
 * or to mention server to attach an {@link AttachingManager} if player is opening a menu.
 */
public record OpenEndInvPayload(boolean openNew) implements ModPacketPayload {

    public static void encode(OpenEndInvPayload payload, FriendlyByteBuf o){
        o.writeBoolean(payload.openNew);
    }

    public static OpenEndInvPayload decode(FriendlyByteBuf o){
        return new OpenEndInvPayload(o.readBoolean());
    }

    @Override
    public String id() {
        return "open_endinv";
    }

    @Override
    public void handle(ModPacketContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
        if(player==null) return;
        if(!ModRegistries.NbtAttachments.getSyncedConfig().computeIfAbsent(player).attaching()) return;
        if(player.containerMenu == player.inventoryMenu && openNew()){
            player.openMenu(new SimpleMenuProvider(EndlessInventoryMenu::createServer, Component.empty()));
        }else if(!openNew()){
            ServerLevelEndInv.getEndInvForPlayer(player).ifPresent(endInv->{
                AttachingManager manager = new AttachingManager(player.containerMenu, endInv ,player);
                ServerLevelEndInv.PAGE_META_DATA_MANAGER.put(player,manager);
                manager.sendEndInvData();
            });

        }

    }

}
