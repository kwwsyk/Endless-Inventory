package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.network.payloads.PageData;
import com.kwwsyk.endinv.common.network.payloads.toClient.EndInvContent;
import com.kwwsyk.endinv.common.network.payloads.toClient.SetItemDisplayContentPayload;
import com.kwwsyk.endinv.common.options.ContentTransferMode;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

import static com.kwwsyk.endinv.common.ModInfo.getPacketDistributor;
import static com.kwwsyk.endinv.common.ModInfo.getServerConfig;

/**
 * In page context used on Page operations.
 *
 * @param startIndex
 * @param length
 * @param pageData
 */
public record ItemPageContext(int startIndex, int length, PageData pageData) implements ModPacketPayload {

    public static void encode(ItemPageContext context, FriendlyByteBuf o) {
        o.writeInt(context.startIndex);
        o.writeInt(context.length);
        PageData.encode(o, context.pageData);
    }

    public static ItemPageContext decode(FriendlyByteBuf o) {
        return new ItemPageContext(o.readInt(), o.readInt(), PageData.decode(o));
    }

    public SortType sortType() {
        return pageData.sortType();
    }

    public String search() {
        return pageData.search();
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ItemPageContext context)) return false;
        return length == context.length && startIndex == context.startIndex && Objects.equals(pageData, context.pageData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startIndex, length, pageData);
    }

    @Override
    public String id() {
        return "page_context";
    }

    public void handle(ModPacketContext iPayloadContext) {
        ServerPlayer serverPlayer = (ServerPlayer) iPayloadContext.player();
        var optional = ServerLevelEndInv.checkAndGetManagerForPlayer(serverPlayer);
        optional.ifPresent(manager -> {

            if (!Objects.equals(manager.getInPageContext(), this)) {
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
            if (getServerConfig().transferMode().get() == ContentTransferMode.PART) {
                List<ItemStack> view = endInv.getSortedAndFilteredItemView(startIndex, length,
                        manager.sortType(), manager.isSortReversed(),
                        manager.getDisplayingPageType().itemClassify, manager.searching());

                NonNullList<ItemStack> stacks = NonNullList.withSize(length, ItemStack.EMPTY);
                for (int i = 0; i < view.size(); ++i) {
                    stacks.set(i, view.get(i));
                }
                getPacketDistributor().sendToPlayer(serverPlayer, new SetItemDisplayContentPayload(stacks));
            } else if (getServerConfig().transferMode().get() == ContentTransferMode.ALL) {
                getPacketDistributor().sendToPlayer(serverPlayer, new EndInvContent(endInv.getItemMap()));
            }
        });
    }
}
