package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.menu.page.ItemPage;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.mojang.logging.LogUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import org.slf4j.Logger;

import java.util.Optional;

public record PageClickPayload(int containerId, PageContext context, double XOffset, double YOffset, int keyCode, ClickType clickType)
implements ToServerPayload {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final StreamCodec<RegistryFriendlyByteBuf,PageClickPayload> STREAM_CODEC =
            StreamCodec.of(PageClickPayload::write,PageClickPayload::createPageClickPayload);

    private static PageClickPayload createPageClickPayload(RegistryFriendlyByteBuf buffer){
        return new PageClickPayload(
                buffer.readInt(),
                PageContext.STREAM_CODEC.decode(buffer),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readEnum(ClickType.class));
    }
    private static void write(RegistryFriendlyByteBuf buffer,PageClickPayload payload){
        buffer.writeInt(payload.containerId);
        PageContext.STREAM_CODEC.encode(buffer,payload.context);
        buffer.writeDouble(payload.XOffset);
        buffer.writeDouble(payload.YOffset);
        buffer.writeInt(payload.keyCode);
        buffer.writeEnum(payload.clickType);
    }

    @Override
    public String id() {
        return "page_click";
    }

    public void handle(ToServerPacketContext context){//todo click item in itempage
        ServerPlayer player = (ServerPlayer) context.player();
        Optional<PageMetaDataManager> optional = ServerLevelEndInv.checkAndGetManagerForPlayer(player);
        if(player.containerMenu.containerId != containerId() || optional.isEmpty()) return;
        PageMetaDataManager manager = optional.get();

        ToServerPayload.syncPageContext(manager,context(),false);

        if(player.isSpectator()){
            manager.getMenu().sendAllDataToRemote();
            return;
        }else if(!manager.getMenu().stillValid(player)){
            LOGGER.debug("Player {} interacted with invalid menu {}", player, manager.getMenu());
            return;
        }
        manager.getMenu().suppressRemoteUpdates();
        manager.getDisplayingPage().pageClicked(XOffset(),YOffset(),keyCode(),clickType());
        if(manager.getDisplayingPage() instanceof ItemPage itemPage) itemPage.refreshItems();
        manager.getMenu().resumeRemoteUpdates();

        manager.getDisplayingPage().syncContentToClient(player);

        manager.getMenu().broadcastChanges();
    }
}
