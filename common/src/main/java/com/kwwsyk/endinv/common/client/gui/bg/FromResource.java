package com.kwwsyk.endinv.common.client.gui.bg;

import com.kwwsyk.endinv.common.AbstractModInitializer;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.ClientModInfo;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.client.option.TextureMode;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@SuppressWarnings("removal")
public abstract class FromResource extends SFBgRendererImpl {


    public static final ResourceLocation CONTAINER_TEXTURE_RESOURCE = new ResourceLocation("minecraft","textures/gui/container/generic_54.png");
    public static final ResourceLocation TABS_RESOURCE = new ResourceLocation("minecraft","textures/gui/advancements/tabs.png");

    public static final ResourceLocation DEDICATED_CONTAINER_TEXTURE = new ResourceLocation(ModInfo.MOD_ID,"textures/gui/item_grid.png");
    public static final ResourceLocation DEDICATED_TABS = new ResourceLocation(ModInfo.MOD_ID,"textures/gui/tabs.png");
    public static final ResourceLocation ITEM_ENTRY_DISPLAY_RESOURCE = new ResourceLocation(ModInfo.MOD_ID,"textures/gui/item_entry.png");

    private static final ResourceLocation TAB_LEFT_MIDDLE_SPRITE = AbstractModInitializer.withMcLocation("advancements/tab_left_middle");
    private static final ResourceLocation TAB_LEFT_TOP_SELECTED = AbstractModInitializer.withMcLocation("advancements/tab_left_top_selected");
    private static final ResourceLocation TAB_LEFT_MIDDLE_SELECTED = AbstractModInitializer.withMcLocation("advancements/tab_left_middle_selected");
    private static final ResourceLocation TAB_LEFT_BOTTOM_SELECTED = AbstractModInitializer.withMcLocation("advancements/tab_left_bottom_selected");

    private static final ResourceLocation TAB_UNSELECTED = AbstractModInitializer.withModLocation("textures/gui/tab_left_middle.png");
    private static final ResourceLocation TAB_TOP = AbstractModInitializer.withModLocation("textures/gui/tab_left_top_selected.png");
    private static final ResourceLocation TAB_MIDDLE = AbstractModInitializer.withModLocation("textures/gui/tab_left_middle_selected.png");
    private static final ResourceLocation TAB_BOTTOM = AbstractModInitializer.withModLocation("textures/gui/tab_left_bottom_selected.png");

    private static final int PAGE_FRAME_COLOR = 0x80A0A0A0;
    private static final int PAGE_BG_COLOR = 0x30373737;

    private static ResourceLocation getTabsTexture(TabType type) {
        return ClientModInfo.getClientConfig().textureMode().get() == TextureMode.DEDICATED_LOCATION ? type.dedicatedLocation : type.vanillaTexture;
    }

    private enum TabType {
        UNSELECTED(TAB_LEFT_MIDDLE_SPRITE, TAB_UNSELECTED),
        TOP(TAB_LEFT_TOP_SELECTED, TAB_TOP),
        MIDDLE(TAB_LEFT_MIDDLE_SELECTED, TAB_MIDDLE),
        BOTTOM(TAB_LEFT_BOTTOM_SELECTED, TAB_BOTTOM);

        final ResourceLocation vanillaTexture;
        final ResourceLocation dedicatedLocation;

        TabType(ResourceLocation vanillaTexture, ResourceLocation dedicatedLocation) {
            this.vanillaTexture = vanillaTexture;
            this.dedicatedLocation = dedicatedLocation;
        }
    }


    private static ResourceLocation getContainerTexture(){
        return ClientModInfo.getClientConfig().textureMode().get() == TextureMode.DEDICATED_LOCATION ? DEDICATED_CONTAINER_TEXTURE : CONTAINER_TEXTURE_RESOURCE;
    }

    private static ResourceLocation getTabsTexture(){
        return ClientModInfo.getClientConfig().textureMode().get() == TextureMode.DEDICATED_LOCATION ? DEDICATED_TABS : TABS_RESOURCE;
    }

    public FromResource(ScreenFramework frameWork){
        super(frameWork);
    }

    public static class MenuMode extends FromResource{

        public MenuMode(ScreenFramework frameWork, ScreenRectangleWidgetParam pageSwitchTabParam) {
            super(frameWork);
            this.pageSwitchTabParam = pageSwitchTabParam;
        }

        @Override
        public void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            super.renderBg(guiGraphics, partialTick, mouseX, mouseY);

            int baseRows = frameWork.menu instanceof EndlessInventoryMenu endless ? endless.getBaseRows() : rows;

            int startY = menuTop + 17 + baseRows*18;
            renderPlayerInv(guiGraphics,partialTick,mouseX,mouseY,menuLeft,startY);
        }

        private void renderPlayerInv(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, int startX, int startY){
            guiGraphics.blit(getContainerTexture(), startX, startY,//WARN
                    0.0F, 126.0F, imageWidth, 96, 256, 256);
        }
    }

    public static class LeftLayout extends FromResource{

        public LeftLayout(ScreenFramework frameWork, ScreenRectangleWidgetParam pageSwitchTabParam){
            super(frameWork);
            this.pageSwitchTabParam = pageSwitchTabParam;
        }
    }

    public abstract class PagePainter implements PageBgRender {

        public abstract ResourceLocation texture();

        @Override
        public void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            int startX = pageLeft;
            int startY = pageTop;

            if(columns!=9){
                renderSpecialBg(guiGraphics,partialTick,mouseX,mouseY,startX,startY);
            }else {
                guiGraphics.blit(texture(), startX, startY, 0, 0,
                        imageWidth, 17, 256, 256);
                startY += 17;
                int rowsToRender = rows;
                while (rowsToRender > 0) {
                    int height = 18 * Math.min(rowsToRender, 6);
                    guiGraphics.blit(texture(), startX, startY,
                            0.0F, 17.0F, imageWidth, height, 256, 256);
                    rowsToRender -= 6;
                    startY += height;
                }
                guiGraphics.blit(texture(),startX,pageTop+17+18*rows,0,124,imageWidth,12,256,256);
            }

        }

        private void renderSpecialBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY,
                                     int startX, int startY) {
            int initialX = startX;

            guiGraphics.blit(texture(), startX, startY, 0, 0,
                    7, 17, 256, 256);
            startX+=7;
            for (int columnsToRender = columns;columnsToRender>0;columnsToRender-=9) {
                int width = 18 * Math.min(9,columnsToRender);
                guiGraphics.blit(texture(), startX, startY, 7, 0,
                        width, 17, 256,256);

                startX+=width;
            }
            guiGraphics.blit(texture(), startX, startY, 168, 0,
                    8, 17, 256, 256);
            startX = initialX;

            startY+=17;
            for (int rowsToRender = rows;rowsToRender > 0;rowsToRender -= 6) {
                int height = 18*Math.min(rowsToRender,6);


                guiGraphics.blit(texture(), startX, startY, 0, 17,
                        7, height, 256, 256);
                startX+=7;
                for (int columnsToRender = columns;columnsToRender>0;columnsToRender-=9) {
                    int width = 18 * Math.min(9,columnsToRender);
                    guiGraphics.blit(texture(), startX, startY, 7, 17,
                            width, height, 256, 256);
                    startX+=width;
                }
                guiGraphics.blit(texture(), startX, startY, 168, 17,
                        8, height, 256, 256);
                startX = initialX;
                startY += height;
            }

            guiGraphics.blit(texture(), startX, startY, 0, 124,
                    7, 12, 256, 256);
            startX+=7;
            for (int columnsToRender = columns;columnsToRender>0;columnsToRender-=9) {
                int width = 18 * Math.min(9,columnsToRender);
                guiGraphics.blit(texture(), startX, startY, 7, 124,
                        width, 12, 256, 256);

                startX+=width;
            }
            guiGraphics.blit(texture(), startX, startY, 168, 124,
                    8, 12, 256, 256);
            startX = initialX;
        }
    }

    @Override
    public Optional<PageBgRender> getDefaultPageBgRenderer() {
        return Optional.of(new PagePainter(){

            @Override
            public ResourceLocation texture() {
                return getContainerTexture();
            }
        });
    }

    public PagePainter dedicatePageBgRender(ResourceLocation texture){
        return new PagePainter() {
            @Override
            public ResourceLocation texture() {
                return texture;
            }
        };
    }

    @Override
    public void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int pageX = pageSwitchTabParam.XPos();
        int pageY = pageSwitchTabParam.YPos();
        int selectedPageIndex = frameWork.getDisplayingPageIndex();
        for (int i = frameWork.firstPageIndex; i < frameWork.firstPageIndex + frameWork.pageBarCount; ++i) {
            if (i == selectedPageIndex) {
                if (i == 0) {
                    guiGraphics.blitSprite(getTabsTexture(TabType.TOP),pageX,pageY,32,28);
                } else if (i == frameWork.firstPageIndex + frameWork.pageBarCount-1) {
                    guiGraphics.blitSprite(getTabsTexture(TabType.BOTTOM),pageX,pageY,32,28);
                } else
                    guiGraphics.blitSprite(getTabsTexture(TabType.MIDDLE),pageX,pageY,32,29);
            } else {
                guiGraphics.blitSprite(getTabsTexture(TabType.UNSELECTED),pageX+10,pageY,24,27);
            }
            pageY+=28;
        }

        renderPageBarContent(guiGraphics, partialTick, mouseX, mouseY);
    }
}
