package com.kwwsyk.endinv.common.client.gui;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.network.payloads.toServer.ToggleCraftingPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("removal")
public class EndlessInventoryScreen extends AbstractContainerScreen<EndlessInventoryMenu> {
    private static final ResourceLocation CRAFTING_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/crafting_table.png");
    private ScreenFramework frameWork;
    private CycleButton<Boolean> craftingToggleButton;
    private boolean craftingVisible;

    public EndlessInventoryScreen(EndlessInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        recalcDimensions();
    }

    private void recalcDimensions() {
        int baseRows = menu.getBaseRows();
        this.imageHeight = 114 + baseRows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    public void init(){
        super.init();
        craftingVisible = menu.isCraftingVisible();
        recalcDimensions();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        var existing = ScreenFramework.getInstance();
        if (existing != null) {
            existing.onClose();
        }
        this.frameWork = new ScreenFramework(this);

        frameWork.addWidgetToScreen(this::addRenderableWidget);
        addCraftingToggleButton();
    }

    private void addCraftingToggleButton() {
        int width = 70;
        this.craftingToggleButton = CycleButton.onOffBuilder()
                .withInitialValue(false)
                .create(0,0,width,20,Component.literal("Crafter"), (it,on)->{
                    toggleCrafting();
                    if(it.getValue()!=craftingVisible) it.setValue(craftingVisible);
                });
        updateCraftingToggleButtonPosition();
        addRenderableWidget(this.craftingToggleButton);
    }

    /**
     * Keeps the crafting toggle anchored to the screen chrome after layout changes.
     */
    private void updateCraftingToggleButtonPosition() {
        if (this.craftingToggleButton == null) {
            return;
        }
        int width = this.craftingToggleButton.getWidth();
        int x = this.leftPos + this.imageWidth - width - 8;
        int y = this.topPos - 20;
        this.craftingToggleButton.setX(x);
        this.craftingToggleButton.setY(y);
    }

    /**
     * Toggle crafter visibility and realign the surrounding widgets without rebuilding the screen.
     */
    private void toggleCrafting() {
        craftingVisible = !craftingVisible;
        menu.setCraftingVisible(craftingVisible);
        ModInfo.getPacketDistributor().sendToServer(new ToggleCraftingPayload(craftingVisible));
        int previousTop = this.topPos;
        recalcDimensions();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        updateCraftingToggleButtonPosition();
        if (frameWork != null) {
            frameWork.resizePageRows(menu.getVisibleRows());
            frameWork.move(0, this.topPos - previousTop);
        }
    }

    private void drawCraftingBackground(GuiGraphics guiGraphics) {
        int craftX = this.leftPos;
        int craftY = this.topPos + 18 * menu.getVisibleRows() + 18;
        guiGraphics.blit(CRAFTING_TEXTURE, craftX , craftY , 0, 12, 176, 58);
    }

    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick){
        this.renderBackground(gui);
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
        if (menu.isCraftingVisible()) {
            drawCraftingBackground(guiGraphics);
        }
    }

    public com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager getPageManager() {
        return menu;
    }

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

