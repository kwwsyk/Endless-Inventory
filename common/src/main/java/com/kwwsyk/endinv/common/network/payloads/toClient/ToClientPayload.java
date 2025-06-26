package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;

import java.util.Optional;

public interface ToClientPayload extends ModPacketPayload {

    void handle(ToClientPacketContext context);

    static Optional<PageMetaDataManager> getClientPageMeta(){
        return Optional.ofNullable(ScreenFramework.getInstance()).map(fr->fr.meta);
    }
}
