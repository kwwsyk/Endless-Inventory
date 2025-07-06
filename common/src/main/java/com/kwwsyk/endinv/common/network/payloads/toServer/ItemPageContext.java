package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.network.payloads.PageData;
import com.kwwsyk.endinv.common.network.payloads.toClient.EndInvContent;
import com.kwwsyk.endinv.common.network.payloads.toClient.SetItemDisplayContentPayload;
import com.kwwsyk.endinv.common.options.ContentTransferMode;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

import static com.kwwsyk.endinv.common.ModInfo.getPacketDistributor;
import static com.kwwsyk.endinv.common.ModInfo.getServerConfig;

/**In page context used on Page operations.
 * @param startIndex
 * @param length
 * @param pageData
 */
public record ItemPageContext(int startIndex, int length, PageData pageData) implements ToServerPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemPageContext> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ItemPageContext::startIndex,
            ByteBufCodecs.INT, ItemPageContext::length,
            PageData.STREAM_CODEC, ItemPageContext::pageData,
            ItemPageContext::new
    );


    public SortType sortType() {
        return pageData.sortType();
    }

    public String search() {
        return pageData.search();
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ItemPageContext(int index, int length1, PageData data))) return false;
        return length == length1 && startIndex == index && Objects.equals(pageData,data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startIndex, length, pageData);
    }

    @Override
    public String id() {
        return "page_context";
    }

    public void handle(ToServerPacketContext iPayloadContext){
        ServerPlayer serverPlayer = (ServerPlayer) iPayloadContext.player();
        var optional = ServerLevelEndInv.checkAndGetManagerForPlayer(serverPlayer);
        optional.ifPresent(manager -> {

            if(!Objects.equals(manager.getInPageContext(),this)) {
                SortType sortType = pageData.sortType();
                boolean reverseSort = pageData.reverseSort();
                String search = pageData.search();


                manager.setSortType(sortType);
                manager.setSortReversed(reverseSort);
                manager.setSearching(search);

                manager.getDisplayingPage().setChanged();

                manager.switchPageWithId(pageData().pageRegKey());
            }


            EndlessInventory endInv = (EndlessInventory) manager.getSourceInventory();
            if(getServerConfig().transferMode().get()== ContentTransferMode.PART) {
                List<ItemStack> view = endInv.getSortedAndFilteredItemView(startIndex, length,
                        manager.sortType(), manager.isSortReversed(),
                        manager.getDisplayingPageType().itemClassify, manager.searching());

                NonNullList<ItemStack> stacks = NonNullList.withSize(length, ItemStack.EMPTY);
                for (int i = 0; i < view.size(); ++i) {
                    stacks.set(i, view.get(i));
                }
                getPacketDistributor().sendToPlayer(serverPlayer, new SetItemDisplayContentPayload(stacks));
            } else if (getServerConfig().transferMode().get() == ContentTransferMode.ALL) {
                getPacketDistributor().sendToPlayer(serverPlayer,new EndInvContent(endInv.getItemMap()));
            }
        });
    }
}
