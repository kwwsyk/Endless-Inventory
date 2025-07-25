package com.kwwsyk.endinv.common.network.payloads;

import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;

import java.util.Optional;

public interface ModPacketPayload{

    String id();

    void handle(ModPacketContext context);

    static Optional<PageMetaDataManager> getClientPageMeta(){
        return Optional.ofNullable(ScreenFramework.getInstance()).map(fr->fr.meta);
    }
}
