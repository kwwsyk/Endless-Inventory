package com.kwwsyk.endinv.common.client.gui.widget;

import com.kwwsyk.endinv.common.AbstractModInitializer;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.client.option.ClientConfigs;
import com.kwwsyk.endinv.common.client.option.PageSwitchBarConfig;
import com.kwwsyk.endinv.common.client.option.TextureMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class PageSwitchBar extends AbstractWidget {

    //public static final Identifier TABS_RESOURCE = Identifier.withDefaultNamespace("textures/gui/advancements/tabs.png");
    private static final Identifier TAB_LEFT_MIDDLE_SPRITE = Identifier.withDefaultNamespace("advancements/tab_left_middle");
    private static final Identifier TAB_LEFT_TOP_SELECTED = Identifier.withDefaultNamespace("advancements/tab_left_top_selected");
    private static final Identifier TAB_LEFT_MIDDLE_SELECTED = Identifier.withDefaultNamespace("advancements/tab_left_middle_selected");
    private static final Identifier TAB_LEFT_BOTTOM_SELECTED = Identifier.withDefaultNamespace("advancements/tab_left_bottom_selected");

    private static final Identifier TAB_UNSELECTED = AbstractModInitializer.withModLocation("textures/gui/tab_left_middle.png");
    private static final Identifier TAB_TOP = AbstractModInitializer.withModLocation("textures/gui/tab_left_top_selected.png");
    private static final Identifier TAB_MIDDLE = AbstractModInitializer.withModLocation("textures/gui/tab_left_middle_selected.png");
    private static final Identifier TAB_BOTTOM = AbstractModInitializer.withModLocation("textures/gui/tab_left_bottom_selected.png");

    private static final int PAGE_FRAME_COLOR = 0x80A0A0A0;
    private static final int PAGE_BG_COLOR = 0x30373737;

    private static Identifier getTabsTexture(TabType type){
        return ClientConfigs.ATTACHED_MENU_CONFIG.TextureMode.get() == TextureMode.DEDICATED_LOCATION ? type.dedicatedLocation : type.vanillaTexture;
    }

    private enum TabType{
        UNSELECTED(TAB_LEFT_MIDDLE_SPRITE, TAB_UNSELECTED),
        TOP(TAB_LEFT_TOP_SELECTED, TAB_TOP),
        MIDDLE(TAB_LEFT_MIDDLE_SELECTED, TAB_MIDDLE),
        BOTTOM(TAB_LEFT_BOTTOM_SELECTED, TAB_BOTTOM);

        final Identifier vanillaTexture;
        final Identifier dedicatedLocation;

        TabType(Identifier vanillaTexture, Identifier dedicatedLocation){
            this.vanillaTexture = vanillaTexture;
            this.dedicatedLocation = dedicatedLocation;
        }
    }

    ScreenFramework framework;
    int maxBars;
    TextureMode textureMode;

    boolean direction_isVertical;
    int tabWidth,tabHeight;

    public PageSwitchBar(ScreenFramework screenFramework, PageSwitchBarConfig.Param param, int adjustedMaxBars, TextureMode textureMode) {
        super(
                param.tabParam().x(),
                param.tabParam().y(),
                param.direction_isVertical() ? param.tabParam().width() : param.tabParam().height() * adjustedMaxBars,
                param.direction_isVertical() ? param.tabParam().height() * adjustedMaxBars : param.tabParam().width(),
                Component.empty()
        );
        framework = screenFramework;
        maxBars = adjustedMaxBars;
        this.textureMode = textureMode;

        direction_isVertical = param.direction_isVertical();
        tabWidth = param.tabParam().width();
        tabHeight = param.tabParam().height();
    }

    public void onClick(MouseButtonEvent event, boolean pre) {
        double mouseX = event.x(); double mouseY = event.y();
        double XOffset = mouseX - getX();
        double YOffset = mouseY - getY();
        if(XOffset < 0 || XOffset > getWidth() || YOffset < 0 || YOffset > getHeight()) return;
        int index;
        if(direction_isVertical){
            index = (int)YOffset/tabHeight;
        }else index = (int)XOffset/tabWidth;
        framework.pageSwitched(index);
    }

    @Override
    protected void extractWidgetRenderState(@NotNull GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        int tabX = getX();
        int tabY = getY();
        int selectedPageIndex = framework.getDisplayingPageIndex();
        if(textureMode == TextureMode.TRANSPARENT){
            for (int i = ScreenFramework.firstPageIndex; i < ScreenFramework.firstPageIndex + framework.pageBarCount; ++i) {
                if (i == selectedPageIndex) {
                    GuiGraphicsExtractor.fill(tabX,tabY,tabX+tabWidth,tabY+tabHeight,PAGE_FRAME_COLOR);
                } else {
                    GuiGraphicsExtractor.fill(tabX+4,tabY,tabX+tabWidth,tabY+tabHeight,PAGE_BG_COLOR);
                }
                if(direction_isVertical) tabY+=tabHeight; else tabX+=tabWidth;
            }
        }else {
            for (int i = ScreenFramework.firstPageIndex; i < ScreenFramework.firstPageIndex + framework.pageBarCount; ++i) {
                if (i == selectedPageIndex) {
                    if (i == 0) {
                        var tex = getTabsTexture(TabType.TOP);
                        if (tex.equals(TabType.TOP.vanillaTexture)) tex = TabType.TOP.dedicatedLocation;
                        GuiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, tex, tabX, tabY, 0.0F, 0.0F, tabWidth, tabHeight, tabWidth, tabHeight);
                    } else if (i == ScreenFramework.firstPageIndex + framework.pageBarCount - 1) {
                        var tex = getTabsTexture(TabType.BOTTOM);
                        if (tex.equals(TabType.BOTTOM.vanillaTexture)) tex = TabType.BOTTOM.dedicatedLocation;
                        GuiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, tex, tabX, tabY, 0.0F, 0.0F, tabWidth, tabHeight, tabWidth, tabHeight);
                    } else {
                        var tex = getTabsTexture(TabType.MIDDLE);
                        if (tex.equals(TabType.MIDDLE.vanillaTexture)) tex = TabType.MIDDLE.dedicatedLocation;
                        GuiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, tex, tabX, tabY, 0.0F, 0.0F, tabWidth, tabHeight, tabWidth, tabHeight);
                    }
                } else {
                    var tex = getTabsTexture(TabType.UNSELECTED);
                    if (tex.equals(TabType.UNSELECTED.vanillaTexture)) tex = TabType.UNSELECTED.dedicatedLocation;
                    GuiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, tex, tabX + 8, tabY, 4.0F, 0.0F, tabWidth - 4, tabHeight, tabWidth, tabHeight);
                }
                if(direction_isVertical) tabY+=tabHeight; else tabX+=tabWidth;
            }
        }
        tabX = getX();
        tabY = getY();
        for (int i = ScreenFramework.firstPageIndex; i < ScreenFramework.firstPageIndex + framework.pageBarCount; ++i) {
            framework.getPages().get(i).renderPageIcon(GuiGraphicsExtractor, tabX + 15, tabY + 5, partialTick);
            if (mouseX > tabX && mouseX < tabX + tabWidth && mouseY > tabY && mouseY < tabY + tabHeight) {
                GuiGraphicsExtractor.tooltip(
                        Minecraft.getInstance().font,
                        java.util.List.of(
                                net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(
                                        framework.getPages().get(i).name.getVisualOrderText()
                                )
                        ),
                        mouseX,
                        mouseY,
                        net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE,
                        null,
                        false
                );
            }
            if(direction_isVertical) tabY+=tabHeight; else tabX+=tabWidth;
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}

