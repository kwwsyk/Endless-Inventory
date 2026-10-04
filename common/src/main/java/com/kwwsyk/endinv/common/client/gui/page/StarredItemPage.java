package com.kwwsyk.endinv.common.client.gui.page;

import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.network.payloads.toServer.StarItemPayload;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemStackLike;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.kwwsyk.endinv.common.ModInfo.getPacketDistributor;

public class StarredItemPage extends ItemDisplay{

    public StarredItemPage(PageType type, ScreenFramework screenFramework) {
        super(type, screenFramework);
    }

    @Override
    protected List<ItemKey> getViewForPage() {
        return CachedSrcInv.INSTANCE.affinities.starredItems;
    }

    public void starItem(ItemStack stack, boolean isAdding){
        if(stack.isEmpty()) return;
        getPacketDistributor().sendToServer(new StarItemPayload(stack,isAdding));
        requestRemoteContents();
    }

    @Override
    protected void setVisibleRange(int startIndex, int length){
        this.startIndex = startIndex;
        this.length = Math.min(length, framework.rows()* framework.columns());
        this.refreshItems();
    }

    /**
     * The <em>refresh</em> method of ItemPage, this method shall keep the startIndex and length and fill {@link #items}
     * with such and srcInv.
     */
    @Override
    public void refreshItems() {
        requestRemoteContents();
    }

    public void initializeAsMap(List<ItemStackLike> stacks){
        this.viewContainer = buildView(stacks.stream().map(ItemStackLike::toKey).toList());
    }

    public void requestRemoteContents(){
        getPacketDistributor().sendToServer(new StarItemPayload(ItemStack.EMPTY,false));
    }

    @Override
    public void handleStarItem(double XOffset, double YOffset) {
        starItem(getItemByMouseOffset(XOffset, YOffset), false);
    }
}
