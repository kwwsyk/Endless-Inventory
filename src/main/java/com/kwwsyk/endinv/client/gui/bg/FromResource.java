package com.kwwsyk.endinv.client.gui.bg;

import com.kwwsyk.endinv.client.gui.ScreenFrameWork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class FromResource extends ScreenBgRendererImpl {


    private static final ResourceLocation CONTAINER_TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
    private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private static final ResourceLocation TAB_ITEM_SEARCH_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tab_item_search.png");
    private static final ResourceLocation TAB_LEFT_MIDDLE_SPRITE = ResourceLocation.withDefaultNamespace("advancements/tab_left_middle");
    private static final ResourceLocation TAB_LEFT_TOP_SELECTED = ResourceLocation.withDefaultNamespace("advancements/tab_left_top_selected");
    private static final ResourceLocation TAB_LEFT_MIDDLE_SELECTED = ResourceLocation.withDefaultNamespace("advancements/tab_left_middle_selected");
    private static final ResourceLocation TAB_LEFT_BOTTOM_SELECTED = ResourceLocation.withDefaultNamespace("advancements/tab_left_bottom_selected");

    public FromResource(ScreenFrameWork frameWork){
        super(frameWork);
    }

    public static class MenuMode extends FromResource{

        public MenuMode(ScreenFrameWork frameWork, ScreenRectangleWidgetParam pageSwitchTabParam) {
            super(frameWork);
            this.pageSwitchTabParam = pageSwitchTabParam;
        }

        @Override
        public void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            super.renderBg(guiGraphics, partialTick, mouseX, mouseY);

            int startY = menuTop + 17 + rows*18;
            renderPlayerInv(guiGraphics,partialTick,mouseX,mouseY,menuLeft,startY);
        }

        private void renderPlayerInv(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, int startX, int startY){
            guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY,
                    0.0F, 126.0F, imageWidth, 96, 256, 256);
        }
    }

    public static class LeftLayout extends FromResource{

        public LeftLayout(ScreenFrameWork frameWork, ScreenRectangleWidgetParam pageSwitchTabParam){
            super(frameWork);
            this.pageSwitchTabParam = pageSwitchTabParam;
        }
    }

    public class GridPageRenderer implements ScreenBgRenderer.BgRenderer{

        @Override
        public void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            int startX = pageLeft;
            int startY = pageTop;

            if(columns!=9){
                renderSpecialBg(guiGraphics,partialTick,mouseX,mouseY,startX,startY);
            }else {
                guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY, 0, 0,
                        imageWidth, 17, 256, 256);
                startY += 17;
                int rowsToRender = rows;
                while (rowsToRender > 0) {
                    int height = 18 * Math.min(rowsToRender, 6);
                    guiGraphics.blit(CONTAINER_TEXTURE_LOCATION, startX, startY,
                            0.0F, 17.0F, imageWidth, height, 256, 256);
                    rowsToRender -= 6;
                    startY += height;
                }
            }
            guiGraphics.blit(CONTAINER_TEXTURE_LOCATION,startX,pageTop+17+18*rows,0,124,imageWidth,12,256,256);
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
    }

    @Override
    public Optional<BgRenderer> getDefaultPageBgRenderer() {
        return Optional.of(new GridPageRenderer());
    }

    @Override
    public void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int pageX = pageSwitchTabParam.XPos();
        int pageY = pageSwitchTabParam.YPos();
        int selectedPageIndex = manager.getDisplayingPageIndex();
        for (int i = frameWork.firstPageIndex; i < frameWork.firstPageIndex + frameWork.pageBarCount; ++i) {
            if (i == selectedPageIndex) {
                if (i == 0) {
                    guiGraphics.blitSprite(TAB_LEFT_TOP_SELECTED, pageX,pageY,32,28);
                } else if (i == frameWork.firstPageIndex + frameWork.pageBarCount-1) {
                    guiGraphics.blitSprite(TAB_LEFT_BOTTOM_SELECTED, pageX,pageY,32,28);
                } else
                    guiGraphics.blitSprite(TAB_LEFT_MIDDLE_SELECTED, pageX,pageY,32,28);
            } else {
                guiGraphics.blitSprite(TAB_LEFT_MIDDLE_SPRITE, pageX+4,pageY,32,28);
            }
            pageY+=28;
        }

        renderPageBarContent(guiGraphics, partialTick, mouseX, mouseY);
    }
}
