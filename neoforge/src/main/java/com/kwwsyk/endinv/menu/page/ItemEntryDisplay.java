package com.kwwsyk.endinv.menu.page;

import com.kwwsyk.endinv.client.TextureMode;
import com.kwwsyk.endinv.client.config.ClientConfig;
import com.kwwsyk.endinv.client.events.ScreenAttachment;
import com.kwwsyk.endinv.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.client.gui.bg.ScreenBgRenderer;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ItemEntryDisplay extends ItemDisplay{

    private static final int TOOLTIP_X_SEP = 5;

    /**
     * Used to disable Item name rendering.
     */
    protected boolean jmpTooltip1st = true;

    public ItemEntryDisplay(PageType pageType, PageMetaDataManager metaDataManager) {
        super(pageType,metaDataManager);
        this.length = metadata.getRowCount();
    }

    @Override
    public void scrollTo(float pos) {
        int startIndex = getRowIndexForScroll(pos);
        this.init(startIndex,this.length);
    }

    @Override
    public void init(int startIndex, int length) {
        this.startIndex = startIndex;
        this.length = Math.min(length,metadata.getRowCount());
        if(items==null || length!=this.items.size()){
            this.items = NonNullList.withSize(length,ItemStack.EMPTY);
        }
        release();
        if(srcInv.isRemote()) {
            requestContents();
        }else {
            refreshItems();
        }
    }

    public void toggleJmpItemName(boolean jmpTooltip1st){
        this.jmpTooltip1st = jmpTooltip1st;
    }

    @Override
    protected boolean clickedInOneSlot(double XOffset, double YOffset, double lastX, double lastY) {
        return (int)YOffset/18 == (int)lastY/18;
    }

    @Override
    public int getSlotForMouseOffset(double XOffset, double YOffset) {
        if(XOffset<0||YOffset<0||XOffset>18*metadata.getColumnCount()||YOffset>18*metadata.getRowCount()) return -1;
        return (int)YOffset/18;
    }

    @Override
    protected boolean isHiddenBySortBox(int rowIndex, int columnIndex) {
        return rowIndex<=2 && Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && (
                screen instanceof EndlessInventoryScreen EIS && EIS.getFrameWork().sortTypeSwitchBox.isOpen()
                        || ScreenAttachment.ATTACHMENT_MANAGER.get(screen)!=null
                        && ScreenAttachment.ATTACHMENT_MANAGER.get(screen).getFrameWork().sortTypeSwitchBox.isOpen()
        );
    }

    @Override
    public void renderPage(GuiGraphics guiGraphics, int x, int y) {
        this.leftPos=x;
        this.topPos=y;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
        int rowIndex = 0;
        int columnIndex = 0;
        for(ItemStack stack : items){
            guiGraphics.renderItem(stack,x,y+rowIndex*18+1,columnIndex+rowIndex<<8);
            if(!stack.isEmpty())
                renderItemEntry(stack,x+18,y+rowIndex*18+5,guiGraphics);
            if(!isHiddenBySortBox(rowIndex,columnIndex))
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, x,y+rowIndex*18+1, getDisplayAmount(stack));
            rowIndex++;
            if(rowIndex>= metadata.getRowCount()) break;
        }
        guiGraphics.pose().popPose();
    }

    private void renderItemEntry(ItemStack item, int x, int y, GuiGraphics graphics){
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        List<Component> tooltips = AbstractContainerScreen.getTooltipFromItem(mc,item);
        int strX = x;
        boolean jmp = jmpTooltip1st;
        for(var tip : tooltips){
            if(jmp){
                jmp = false;
                continue;
            }
            Component tip1 = Component.literal(tip.getString());
            int strX1 = strX + font.width(tip.getVisualOrderText());
            if(strX1 >= x + metadata.getColumnCount()*18 -18-3){
                graphics.drawString(font,Component.literal("..."),strX,y,0xFFFFFF00);
                break;
            }
            graphics.drawString(font,tip1,strX,y,0xFFFFFF00);
            strX = strX1 + TOOLTIP_X_SEP;
        }
    }

    @Override
    public void renderHovering(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderSlotHighlight(graphics, mouseX, mouseY, partialTick);
        int hoveringSlot = getSlotForMouseOffset(mouseX-leftPos,mouseY-topPos);
        if(hoveringSlot>=0&&hoveringSlot<items.size()){
            ItemStack hovering = items.get(hoveringSlot);
            if(hovering.isEmpty()) return;
            graphics.pose().pushPose();
            graphics.pose().translate(0,0,550.0F);
            graphics.renderTooltip(Minecraft.getInstance().font,
                    AbstractContainerScreen.getTooltipFromItem(Minecraft.getInstance(),hovering),
                    hovering.getTooltipImage(),
                    hovering, mouseX, mouseY);
            graphics.pose().popPose();
        }
    }

    protected void renderSlotHighlight(GuiGraphics graphics, int mouseX, int mouseY, float partialTick){
        for(int v=0;v<metadata.getRowCount();++v){
            int x1 = leftPos;
            int x2 = leftPos + metadata.getColumnCount()*18 -2;
            int y1 = topPos+18*v+1;
            int y2 = topPos+18*v+18;
            if(mouseX>x1 && mouseX<x2 && mouseY>y1 && mouseY<y2){
                if(!metadata.getMenu().getCarried().isEmpty()) return;
                graphics.fillGradient(RenderType.guiOverlay(),x1,y1,x2,y2,0x80ffffff,0x80ffffff,0);
            }
        }
    }

    @Override
    public void renderBg(ScreenBgRenderer screenBgRenderer, GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(screenBgRenderer, guiGraphics, partialTicks, mouseX, mouseY);
        int pageX = screenBgRenderer.getScreenFrameWork().pageX;
        int startY = screenBgRenderer.getScreenFrameWork().pageY;
        int bgColor = ClientConfig.CONFIG.TEXTURE.get() == TextureMode.FROM_RESOURCE ? 0xFF8b8b8b : 0x37606037;
        for(int i=0; i< metadata.getRowCount(); ++i){
            guiGraphics.fill(pageX,startY,pageX+18*metadata.getColumnCount()-2,startY+1,0xFF373737);
            guiGraphics.fill(pageX,startY+1,pageX+18*metadata.getColumnCount()-2,startY+17,bgColor);
            guiGraphics.fill(pageX,startY+17,pageX+18*metadata.getColumnCount()-2,startY+18,0xFFFFFFFF);
            startY+=18;
        }
    }
}
