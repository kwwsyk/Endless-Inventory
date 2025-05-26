package com.kwwsyk.endinv.common.menu.page;

import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.network.payloads.toClient.SetStarredPagePayload;
import com.kwwsyk.endinv.common.network.payloads.toServer.page.StarItemPayload;
import com.kwwsyk.endinv.neoforge.EndlessInventory;
import com.kwwsyk.endinv.neoforge.util.ItemStackLike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StarredItemPage extends ItemPage{

    public ResourceLocation icon = ResourceLocation.withDefaultNamespace("book");
    private int[] countArray;

    public StarredItemPage(PageType type,PageMetaDataManager metaDataManager) {
        super(type, metaDataManager);
    }

    public void starItem(ItemStack stack, boolean isAdding){
        if(stack.isEmpty()) return;
        PacketDistributor.sendToServer(new StarItemPayload(stack,isAdding));
        requestContents();
    }

    @Override
    public ResourceLocation getIcon(){
        return icon;
    }

    @Override
    public void init(int startIndex,int length){
        this.startIndex = startIndex;
        this.length = Math.min(length,metadata.getRowCount()*metadata.getColumnCount());
        this.items = NonNullList.withSize(length, ItemStack.EMPTY);
        this.countArray = new int[length];
        refreshItems();
    }

    @Override
    public void syncContentToClient(ServerPlayer player) {
        var items = ((EndlessInventory)srcInv).getStarredItems(startIndex,length);
        PacketDistributor.sendToPlayer(player,new SetStarredPagePayload(items));
    }

    public void refreshItems(){
        if(srcInv.isRemote()){
            requestContents();
        }else {
            var items = ((EndlessInventory)srcInv).getStarredItems(startIndex,length);
            initializeAsMap(items);
        }
    }

    public void initializeContents(@NotNull List<ItemStack> stacks){
        if(holdOn){
            inQueueStacks = stacks;
            return;
        }
        for(int i=0; i<items.size(); ++i){
            if(i<stacks.size() && stacks.get(i)!=null){
                items.set(i,stacks.get(i).copyWithCount(1));
                countArray[i]=stacks.get(i).getCount();
            }else {
                items.set(i,ItemStack.EMPTY);
            }
        }
    }

    public void initializeAsMap(@NotNull List<ItemStackLike> stacks){
        for(int i=0; i<items.size(); ++i){
            if(i<stacks.size() && stacks.get(i)!=null){
                items.set(i,stacks.get(i).toKey());
                countArray[i]=stacks.get(i).count();
            }else {
                items.set(i,ItemStack.EMPTY);
            }
        }
    }

    public void requestContents(){
        syncContentToServer();
    }

    @Override
    public boolean hasSearchBar() {
        return true;
    }

    @Override
    public boolean hasSortTypeSwitchBar() {
        return false;
    }

    @Override
    public void renderPage(GuiGraphics guiGraphics, int x, int y){
        this.leftPos=x;
        this.topPos=y;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
        int rowIndex = 0;
        int columnIndex = 0;
        for(int i=0; i<length; ++i){
            ItemStack stack = items.get(i);
            int count = countArray[i];
            guiGraphics.renderItem(stack,x+columnIndex*18,y+rowIndex*18+1,columnIndex+rowIndex*180);
            if(!isHiddenBySortBox(rowIndex,columnIndex))
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack,x+columnIndex*18,y+rowIndex*18+1, getDisplayAmount(stack.copyWithCount(count)));
            columnIndex++;
            if(columnIndex>= metadata.getColumnCount()){
                columnIndex=0;
                rowIndex++;
            }
        }
        guiGraphics.pose().popPose();
    }

    @Override
    public void handleStarItem(double XOffset, double YOffset) {
        int slot = getSlotForMouseOffset(XOffset,YOffset);
        if(slot>=0&&slot<items.size()) {
            ItemStack clicked = items.get(slot);
            starItem(clicked,false);
        }
    }
}
