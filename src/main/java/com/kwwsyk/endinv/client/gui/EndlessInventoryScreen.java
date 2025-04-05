package com.kwwsyk.endinv.client.gui;

import com.kwwsyk.endinv.EndlessInventoryMenu;
import com.kwwsyk.endinv.ItemDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public class EndlessInventoryScreen extends AbstractContainerScreen<EndlessInventoryMenu> {

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private int containerRows;
    private float scrollOffs = 0;

    public EndlessInventoryScreen(EndlessInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        this.refresh();
    }

    public void init(){
        super.init();
        this.leftPos = 10;
        this.topPos = 10;
    }

    public void refresh(){
        this.containerRows = menu.getRowCount();
        this.imageHeight = 114 + this.containerRows*18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    public void render(GuiGraphics gui,int x,int y,float v){
        super.render(gui,x,y,v);
        this.renderTooltip(gui,x,y);
    }

    private boolean canScroll(){

        return this.menu.getItemSize() > 9*this.containerRows || this.menu.getContainer().getStartIndex()>0;
    }

    public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY){
        if(super.mouseScrolled(mouseX,mouseY,scrollX,scrollY)){
            return true;
        }else if(!this.canScroll()){
            return false;
        }else{
            this.scrollOffs = this.menu.subtractInputFromScroll(this.scrollOffs,scrollY);
            this.menu.scrollTo(scrollOffs);
            //int startIndex = menu.getRowIndexForScroll(scrollOffs) * 9;
            //PacketDistributor.sendToServer(new EndInvRequestContentPayload(startIndex,9*this.containerRows, SortType.DEFAULT));
            return true;
        }

    }

    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType type) {
        super.slotClicked(slot,slotId,mouseButton,type);
        ItemDisplay itemDisplay = this.menu.getContainer();
        itemDisplay.tryRequestContents(itemDisplay.getStartIndex(),itemDisplay.getContainerSize());
        this.menu.broadcastChanges();

    }




    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i0, int i1) {
        int startX = leftPos;
        int startY = topPos;
        if(this.containerRows<=6) renderNG6Rows(guiGraphics,startX,startY);
        else renderG6Rows(guiGraphics,startX,startY);
    }

    private void renderNG6Rows(GuiGraphics guiGraphics,int startX,int startY){
        guiGraphics.blit(TEXTURE_LOCATION,startX,startY,0,0,
                this.imageWidth,this.containerRows*18 + 17,256,256);
        guiGraphics.blit(TEXTURE_LOCATION, startX, startY + this.containerRows * 18 + 17,
                0.0F, 126.0F, this.imageWidth, 96, 256, 256);
    }

    private void renderG6Rows(GuiGraphics guiGraphics, int startX, int startY){
        guiGraphics.blit(TEXTURE_LOCATION,startX,startY,0,0,
                this.imageWidth,17,256,256);
        startY+=17;
        int rowsToRender = this.containerRows;
        while(rowsToRender>6){
            guiGraphics.blit(TEXTURE_LOCATION,startX,startY,
                    0.0F,17.0F,this.imageWidth,108,256,256);
            rowsToRender-=6;
            startY+=108;
        }
        if(rowsToRender!=0) {
            guiGraphics.blit(TEXTURE_LOCATION, startX, startY,
                    0.0F, 17.0F, this.imageWidth, rowsToRender * 18, 256, 256);
            startY+=rowsToRender*18;
        }
        guiGraphics.blit(TEXTURE_LOCATION, startX, startY,
                0.0F, 126.0F, this.imageWidth, 96, 256, 256);
    }

}
