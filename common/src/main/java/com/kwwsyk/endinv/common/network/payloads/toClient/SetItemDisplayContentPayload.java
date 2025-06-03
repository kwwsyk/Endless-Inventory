package com.kwwsyk.endinv.common.network.payloads.toClient;

import com.kwwsyk.endinv.common.menu.page.ItemPage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record SetItemDisplayContentPayload(List<ItemStack> stacks) implements ToClientPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, SetItemDisplayContentPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC,SetItemDisplayContentPayload::stacks,
            SetItemDisplayContentPayload::new
    );

    @Override
    public String id() {
        return "itemdisplay_content";
    }

    public void handle(ToClientPacketContext context) {
        ToClientPayload.getClientPageMeta().ifPresent(mng->{
            if(mng.getDisplayingPage() instanceof ItemPage itemPage){
                itemPage.initializeContents(stacks);
            }
        });
    }
}
