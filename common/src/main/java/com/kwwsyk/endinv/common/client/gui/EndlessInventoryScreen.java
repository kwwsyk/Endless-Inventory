package com.kwwsyk.endinv.common.client.gui;

import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.kwwsyk.endinv.common.ModRegistries.NbtAttachments.getSyncedConfig;

public class EndlessInventoryScreen extends AbstractContainerScreen<EndlessInventoryMenu> implements SortTypeSwitcher {


    private ScreenFramework frameWork;
    public boolean isHoveringOnSortBox;

    public EndlessInventoryScreen(EndlessInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        int containerRows = this.menu.getRowCount();
        this.imageHeight = 114 + containerRows *18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    public void init(){
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.frameWork = new ScreenFramework(this);

        frameWork.addWidgetToScreen(this::addRenderableWidget);
    }

    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick){
        frameWork.renderPre(gui,mouseX,mouseY,partialTick);

        super.render(gui,mouseX,mouseY,partialTick);

        frameWork.render(gui,mouseX,mouseY,partialTick);
        this.renderTooltip(gui,mouseX,mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int keyCode){
        for(GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener.mouseClicked(mouseX, mouseY, keyCode)) {
                this.setFocused(guieventlistener);
                if (keyCode == 0) {
                    this.setDragging(true);
                }
                return true;
            }
        }
        return frameWork.mouseClicked(mouseX,mouseY,keyCode) || super.mouseClicked(mouseX,mouseY,keyCode);
    }


    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return frameWork.mouseDragged(mouseX,mouseY,button,dragX,dragY) || super.mouseDragged(mouseX,mouseY,button,dragX,dragY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int keyCode){
        return frameWork.mouseReleased(mouseX,mouseY,keyCode) || super.mouseReleased(mouseX,mouseY,keyCode);
    }

    public boolean mouseScrolled(double mouseX,double mouseY,double scrollY){
        return super.mouseScrolled(mouseX,mouseY,scrollY) || frameWork.mouseScrolled(mouseX,mouseY,scrollY);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        return frameWork.keyPressed(keyCode,scanCode,modifiers) || super.keyPressed(keyCode,scanCode,modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers){
        return frameWork.charTyped(codePoint,modifiers);
    }

    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, @NotNull ClickType type) {
        super.slotClicked(slot,slotId,mouseButton,type);
        this.menu.broadcastChanges();
    }

    public void onClose(){
        super.onClose();
        frameWork.onClose();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        this.frameWork.renderBg(guiGraphics,mouseX,mouseY,partialTick);
    }
    @Override
    public void switchSortTypeTo(SortType type) {
        menu.sortType = type;
        SyncedConfig.updateSyncedConfig(getSyncedConfig().computeIfAbsent(menu.getPlayer()).sortTypeChanged(type));
        menu.getDisplayingPage().release();
        menu.getDisplayingPage().sendChangesToServer();
    }

    @Override
    public void setHoveringOnSortBox(boolean isHovering) {
        this.isHoveringOnSortBox = isHovering;
    }

    @Override
    public boolean isHoveringOnSortBox() {
        return isHoveringOnSortBox;
    }
    @Override
    public PageMetaDataManager getPageMetadata() {
        return menu;
    }

    @Override
    public AbstractContainerScreen<?> getScreen() {
        return this;
    }

    public ScreenFramework getFrameWork() {
        return frameWork;
    }

    public int getGuiLeft() {
        return leftPos;
    }

    public int getGuiTop() {
        return topPos;
    }

    public int getXSize() {
        return imageWidth;
    }

    public int getYSize() {
        return imageHeight;
    }
}
