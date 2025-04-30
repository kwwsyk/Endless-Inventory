package com.kwwsyk.endinv.client.gui.bg;

import com.kwwsyk.endinv.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class FromResource implements ScreenBgRenderer {
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
    private final int imageWidth;
    private final int rows;
    private final int columns;

    private PageMetaDataManager menu;
    private boolean shouldRenderPlayerInv = true;


    public FromResource(EndlessInventoryScreen screen){
        this.screen = screen;
        this.imageWidth = screen.getXSize();
        this.menu = screen.getMenu();
        this.rows = menu.getRowCount();
        this.columns = 9;
        this.leftPos = screen.getGuiLeft();
        this.topPos = screen.getGuiTop();
    }
    public FromResource(AbstractContainerScreen<?> screen, PageMetaDataManager menu){
        this.screen = screen;
        this.imageWidth = 256;
        this.menu = menu;
        this.rows = menu.getRowCount();
        this.columns = menu.getColumnCount();
        this.leftPos = screen.getGuiLeft();
        this.topPos = screen.getGuiTop();
    }

    public static FromResource createDefaultMode(EndlessInventoryScreen screen, ScreenLayoutMode layoutMode, ScreenRectangleWidgetParam pageSwitchTabParam){
        FromResource ret = new FromResource(screen);
        ret.screenLayoutMode = layoutMode;
        ret.pageSwitchTabParam = pageSwitchTabParam;
        return ret;
    }
    public static FromResource createLeftMode(AbstractContainerScreen<?> screen, PageMetaDataManager menu,
                                              ScreenLayoutMode screenLayoutMode, ScreenRectangleWidgetParam pageSwitchTabParam){
        FromResource ret = new FromResource(screen,menu);
        ret.screenLayoutMode = screenLayoutMode;
        ret.pageSwitchTabParam = pageSwitchTabParam;
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

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0,0,0);

        renderGrid:
        {
            if(columns!=9){
                renderSpecialBg(guiGraphics,partialTick,mouseX,mouseY,startX,startY);
                break renderGrid;
            }

            guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY, 0, 0,
                    imageWidth, 17, 256, 256);
            startY += 17;
            int rowsToRender = rows;
            while (rowsToRender > 0) {
                int height = 18*Math.min(rowsToRender,6);
                guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY,
                        0.0F, 17.0F, imageWidth, height, 256, 256);
                rowsToRender -= 6;
                startY += height;
            }

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

        guiGraphics.pose().popPose();
    }

    private void renderSpecialBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY,
                                 int startX, int startY) {
        int initialX = startX;


        guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY, 0, 0,
                7, 17, 256, 256);
        startX+=7;
        for (int columnsToRender = columns;columnsToRender>0;columnsToRender-=9) {
            int width = 18 * Math.min(9,columnsToRender);
            guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY, 7, 0,
                    width, 17, 256, 256);

            startX+=width;
        }
        guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY, 168, 0,
                8, 17, 256, 256);
        startX = initialX;

        startY+=17;
        for (int rowsToRender = rows;rowsToRender > 0;rowsToRender -= 6) {
            int height = 18*Math.min(rowsToRender,6);


            guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY, 0, 17,
                    7, height, 256, 256);
            startX+=7;
            for (int columnsToRender = columns;columnsToRender>0;columnsToRender-=9) {
                int width = 18 * Math.min(9,columnsToRender);
                guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY, 7, 17,
                        width, height, 256, 256);
                startX+=width;
            }
            guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY, 168, 17,
                    8, height, 256, 256);
            startX = initialX;


            startY += height;
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
