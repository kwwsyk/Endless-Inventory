package com.kwwsyk.endinv.client.gui.bg;

import com.kwwsyk.endinv.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class FromResource implements ScreenRenderer {
    private static final ResourceLocation BLANK_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png");
    private static final ResourceLocation CONTAINER_TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
    private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private static final ResourceLocation TAB_ITEM_SEARCH_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tab_item_search.png");
    private static final ResourceLocation TAB_LEFT_MIDDLE_SPRITE = ResourceLocation.withDefaultNamespace("advancements/tab_left_middle");
    private static final ResourceLocation TAB_LEFT_TOP_SELECTED = ResourceLocation.withDefaultNamespace("advancements/tab_left_top_selected");
    private static final ResourceLocation TAB_LEFT_MIDDLE_SELECTED = ResourceLocation.withDefaultNamespace("advancements/tab_left_middle_selected");
    private static final ResourceLocation TAB_LEFT_BOTTOM_SELECTED = ResourceLocation.withDefaultNamespace("advancements/tab_left_bottom_selected");
    private final int leftPos;
    private final int topPos;
    private final AbstractContainerScreen<?> screen;
    public ScreenLayoutMode screenLayoutMode;
    public ScreenRectangleWidgetParam configButtonParam;
    public ScreenRectangleWidgetParam pageSwitchTabParam;
    public ScreenRectangleWidgetParam searchBoxParam;
    private int imageWidth;
    private int containerRows;
    private PageMetaDataManager menu;
    private boolean shouldRenderPlayerInv = true;


    public FromResource(EndlessInventoryScreen screen){
        this.screen = screen;
        this.imageWidth = screen.getXSize();
        this.menu = screen.getMenu();
        this.containerRows = menu.getRowCount();
        this.leftPos = screen.getGuiLeft();
        this.topPos = screen.getGuiTop();
    }
    public FromResource(AbstractContainerScreen<?> screen, PageMetaDataManager menu){
        this.screen = screen;
        this.imageWidth = screen.getXSize();
        this.menu = menu;
        this.containerRows = menu.getRowCount();
        this.leftPos = screen.getGuiLeft();
        this.topPos = screen.getGuiTop();
    }

    public static FromResource createDefaultMode(EndlessInventoryScreen screen){
        FromResource ret = new FromResource(screen);
        ret.screenLayoutMode = new ScreenLayoutMode(ret.leftPos,ret.topPos,false, ret.leftPos, ret.topPos + ret.containerRows*18+25);
        ret.pageSwitchTabParam = new ScreenRectangleWidgetParam(ret.leftPos-32,ret.topPos+1,32,28);
        return ret;
    }
    public static FromResource createLeftMode(EndlessInventoryScreen screen){
        FromResource ret = new FromResource(screen);
        ret.screenLayoutMode = new ScreenLayoutMode(ret.leftPos,ret.topPos,true, (screen.width-screen.getXSize())/2, (screen.height-screen.getYSize())/2);
        ret.pageSwitchTabParam = new ScreenRectangleWidgetParam(ret.leftPos-32,ret.topPos+1,32,28);
        return ret;
    }
    public static FromResource createLeftMode(AbstractContainerScreen<?> screen, PageMetaDataManager menu){
        FromResource ret = new FromResource(screen,menu);
        ret.screenLayoutMode = new ScreenLayoutMode(28,18,true, (screen.width-screen.getXSize())/2, (screen.height-screen.getYSize())/2);
        ret.pageSwitchTabParam = new ScreenRectangleWidgetParam(0,20,32,28);
        ret.shouldRenderPlayerInv = false;
        return ret;
    }

    public void init(){

    }

    @Override
    public ScreenLayoutMode screenLayoutMode() {
        return screenLayoutMode;
    }

    @Override
    public ScreenRectangleWidgetParam pageSwitchBarParam() {
        return pageSwitchTabParam;
    }

    @Override
    public void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int startX = screenLayoutMode.menuXPos();
        int startY = screenLayoutMode.menuYPos();

        guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY, 0, 0,
                imageWidth, 17, 256, 256);
        startY += 17;
        int rowsToRender = containerRows;
        while (rowsToRender > 6) {
            guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY,
                    0.0F, 17.0F, imageWidth, 108, 256, 256);
            rowsToRender -= 6;
            startY += 108;
        }
        if (rowsToRender != 0) {
            guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY,
                    0.0F, 17.0F, imageWidth, rowsToRender * 18, 256, 256);
            startY += rowsToRender * 18;
        }
        if(shouldRenderPlayerInv)
            renderPlayerInv(guiGraphics,partialTick,mouseX,mouseY, startX, startY);

        int pageX = pageSwitchTabParam.XPos();
        int pageY = pageSwitchTabParam.YPos();
        int selectedPageIndex = menu.getDisplayingPageId();
        for (int i = 0; i < menu.getPages().size(); ++i) {
            if (i == selectedPageIndex) {
                if (i == 1) {
                    guiGraphics.blitSprite(TAB_LEFT_TOP_SELECTED, pageX,pageY,32,28);
                } else if (i == menu.getPages().size() - 1) {
                    guiGraphics.blitSprite(TAB_LEFT_BOTTOM_SELECTED, pageX,pageY,32,28);
                } else
                    guiGraphics.blitSprite(TAB_LEFT_MIDDLE_SELECTED, pageX,pageY,32,28);
            } else {
                guiGraphics.blitSprite(TAB_LEFT_MIDDLE_SPRITE, pageX+4,pageY,32,28);
            }
            menu.getPages().get(i).renderPageIcon(guiGraphics,pageX+15,pageY+5,partialTick);
            if(mouseX>pageX&&mouseX<pageX+32&&mouseY>pageY&&mouseY<pageY+28){
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0,0,550.0f);
                String s = menu.getPages().get(i).getItemClassify().getRegisteredName();
                guiGraphics.renderTooltip(screen.getMinecraft().font, Component.literal(s),mouseX,mouseY);
                guiGraphics.pose().popPose();
            }
            pageY += 28;
        }
    }

    private void renderPlayerInv(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY,int startX,int startY){
        if(screenLayoutMode.independentInventory()){
            startX = screenLayoutMode().inventoryX();
            startY = screenLayoutMode().inventoryY();
            guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY, 0, 0,
                    imageWidth, 17, 256, 256);
            startY+=17;
        }
        guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY,
                0.0F, 126.0F, imageWidth, 96, 256, 256);
    }
}
