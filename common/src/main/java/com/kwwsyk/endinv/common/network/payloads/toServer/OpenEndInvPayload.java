package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.menu.page.pageManager.AttachingManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

/**Send to server,
 * to open {@link EndlessInventoryMenu} when player is not opening a menu,
 * or to mention server to attach an {@link AttachingManager} if player is opening a menu.
 */
public record OpenEndInvPayload(boolean openNew) implements ToServerPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf,OpenEndInvPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,OpenEndInvPayload::openNew,
            OpenEndInvPayload::new
    );

    @Override
    public String id() {
        return "open_endinv";
    }

    @Override
    public void handle(ToServerPacketContext iPayloadContext) {
        ServerPlayer player = (ServerPlayer) iPayloadContext.player();
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
