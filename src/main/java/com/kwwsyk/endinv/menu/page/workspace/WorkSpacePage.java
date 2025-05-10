package com.kwwsyk.endinv.menu.page.workspace;

import com.kwwsyk.endinv.menu.page.DisplayPage;
import com.kwwsyk.endinv.menu.page.PageType;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;

public class WorkSpacePage extends DisplayPage {


    public WorkSpacePage(PageType type, PageMetaDataManager metaDataManager) {
        super(type,metaDataManager);
    }

    @Override
    public void scrollTo(float pos) {

    }

    @Override
    public void init(int startIndex, int length) {

    }

    @Override
    public void syncContentToServer() {

    }

    @Override
    public void syncContentToClient(ServerPlayer player) {

    }

    @Override
    public void handleStarItem(double XOffset, double YOffset) {

    }

    @Override
    public boolean canScroll() {
        return false;
    }

    @Override
    public boolean doubleClicked(double XOffset, double YOffset, double lastX, double lastY, long clickInterval) {
        return false;
    }

    @Override
    public void pageClicked(double XOffset, double YOffset, int keyCode, ClickType clickType) {

    }

    @Override
    public void renderPage(GuiGraphics graphics, int pageXPos, int pageYPos) {

    }

    @Override
    public void renderHovering(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

    }

    @Override
    public boolean hasSearchBar() {
        return false;
    }

    @Override
    public boolean hasSortTypeSwitchBar() {
        return false;
    }
}
