package com.kwwsyk.endinv.client.gui.widget;

import com.kwwsyk.endinv.client.gui.SortTypeSwitcher;
import com.kwwsyk.endinv.client.gui.bg.ScreenRectangleWidgetParam;
import com.kwwsyk.endinv.options.SortType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class SortTypeSwitchBox extends AbstractWidget {

    public SortTypeSwitcher screen;
    private int x;
    private int y;
    private final int singleBoxHeight;
    private boolean isOpen;

    
    public SortTypeSwitchBox(SortTypeSwitcher screen,int x, int y, int width, int height){
        super(x,y,width,height, Component.empty());
        this.screen = screen;
        this.visible = screen.getMenu().getDisplayingPage().hasSortTypeSwitchBar();
        this.x = x;
        this.y = y;
        this.singleBoxHeight = height;
    }

    public SortTypeSwitchBox(SortTypeSwitcher screen, ScreenRectangleWidgetParam sortTypeSwitchBoxParam){
        this(screen,
                sortTypeSwitchBoxParam.XPos(),
                sortTypeSwitchBoxParam.YPos(),
                sortTypeSwitchBoxParam.XSize(),
                sortTypeSwitchBoxParam.YSize()
        );
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
        this.height = open ? singleBoxHeight*(1+SortType.values().length) : singleBoxHeight;
    }

    public void onClick(double mouseX,double mouseY, int button){
        if(!isOpen){
            setOpen(true);
        }else {
            int y1 = y+singleBoxHeight;
            for(SortType type : SortType.values()){
                if(isHoveringOnSingleBox((int)mouseY,y1)){
                    screen.switchSortTypeTo(type);
                    return;
                }
                y1+= singleBoxHeight;
            }
            setOpen(false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button){
        if(active && visible && !this.clicked(mouseX,mouseY) && isOpen){
            setOpen(false);
            return true;
        }else return super.mouseClicked(mouseX,mouseY,button);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if(isHovered) screen.setHoveringOnSortBox(true);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F,0.0F,500.0F);
        guiGraphics.fill(x,y,x+width,y+singleBoxHeight,0xff888888);
        guiGraphics.fill(x+1,y+1,x+width-1,y+singleBoxHeight-1,0xff000000);
        if(isHoveringOnSingleBox(mouseY,y))
            guiGraphics.fillGradient(RenderType.guiOverlay(),x,y,x+width,y+singleBoxHeight,0x80ffffff,0x80ffffff,0);
        SortType sortType = screen.getMenu().sortType();
        String s = sortType.toString();
        guiGraphics.drawString(screen.getScreen().getMinecraft().font, s,x+2,y+2,0xffffffff);
        if(isOpen){
            int y1 = y+singleBoxHeight;
            for(SortType type : SortType.values()){
                guiGraphics.fill(x,y1,x+width,y1+singleBoxHeight,0xff888888);
                guiGraphics.fill(x+1,y1+1,x+width-1,y1+singleBoxHeight-1,0xff000000);
                if(isHoveringOnSingleBox(mouseY,y1))
                    guiGraphics.fillGradient(RenderType.guiOverlay(),x,y1,x+width,y1+singleBoxHeight,0x80ffffff,0x80ffffff,0);
                s = type.toString();
                guiGraphics.drawString(screen.getScreen().getMinecraft().font, s,x+2,y1+2,0xffffffff);
                y1+=singleBoxHeight;
            }
        }
        guiGraphics.pose().popPose();
    }
    private boolean isHoveringOnSingleBox(int mouseY,int minY){
        return mouseY>minY && mouseY<minY+singleBoxHeight && isHovered;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
