package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public interface ToServerPayload extends ModPacketPayload {

    void handle(ToServerPacketContext context);

    /**Synchronize server page context from context in payloads, if {@code serverPlayer} is not null,
     *  will send content back to client.
     * @param meta to change object who is holding page context.
     * @param context page context, as independent payload or included in other payloads
     * @param sync send context to set client page contents.
     */
    static void syncPageContext(PageMetaDataManager meta, PageContext context, boolean sync){

        if(!Objects.equals(meta.getInPageContext(),context)) {

            int startIndex = context.startIndex();
            int length = context.length();

            SortType sortType = context.sortType();
            boolean reverseSort = context.pageData().reverseSort();
            String search = context.search();


            meta.setSortType(sortType);
            meta.setSortReversed(reverseSort);
            meta.setSearching(search);
            meta.getDisplayingPage().setChanged();
            if(!Objects.equals(context.pageData().pageRegKey(),meta.getDisplayingPageId())) {
                meta.switchPageWithId(context.pageData().pageRegKey());
            }else meta.getDisplayingPage().refreshContents(startIndex,length);
        }

        if(sync){
            meta.getDisplayingPage().syncContentToClient((ServerPlayer) meta.getPlayer());
        }
    }
}
