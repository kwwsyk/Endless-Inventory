package com.kwwsyk.endinv.common.client.gui.page;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.client.gui.page.slotView.ItemPageSlotView;
import com.kwwsyk.endinv.common.client.gui.page.slotView.PageViewContainer;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageQuickMoveHandler;
import com.kwwsyk.endinv.common.network.payloads.PageData;
import com.kwwsyk.endinv.common.network.payloads.toServer.*;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.NotNullWhenInitialized;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

import static com.kwwsyk.endinv.common.ModInfo.getPacketDistributor;

/**Page that holds ItemStack list. It's the main type of pages.
 *
 */
public abstract class ItemPage extends GridPage {

    private static final Logger LOGGER = LogUtils.getLogger();
    @NotNullWhenInitialized
    protected PageViewContainer viewContainer;
    @Nullable
    protected List<ItemKey> inQueueStacks = null;

    public ItemPage(PageType pageType, ScreenFramework framework) {
        super(pageType, framework);
    }

    @Override
    public void initializeContents() {
        super.initializeContents();
        this.viewContainer = buildView(getViewForPage());
    }

    /**
     * Adjust the number of slots maintained by the item page so it matches the latest menu layout.
     */
    @Override
    public void resize(int rows) {
        int targetRows = Math.max(1, rows);
        this.length = targetRows * framework.columns();
        setVisibleRange(this.startIndex, this.length);
    }

    /**Configure the visible window and reload items.
     * <p>
     * Updates the page's starting index and slot count, ensures the internal
     * {@code items} list matches the requested size, clears transient state via {@link #release()},
     * then calls {@link #refreshItems()} to populate the visible entries.
     * </p>
     * @param startIndex the source-inventory index of the first visible item
     * @param length the number of slots to display (typically rows * columns)
     */
    @Override
    protected void setVisibleRange(int startIndex, int length) {
        this.startIndex = startIndex;
        this.length = Math.min(length, framework.rows()* framework.columns());

        release();
        this.refreshItems();
    }

    /**The <em>refresh</em> method of ItemPage, this method shall keep the startIndex and length and fill {@link #viewContainer}
     * with such and srcInv.<br>
     * This may invoke {@link #requestRemoteContents()} or other send packet methods.
     * So be caution use this method especially on receiving request-remote-callback packets. Like {@link com.kwwsyk.endinv.common.network.payloads.toClient.EndInvContent}
     */
    public abstract void refreshItems();

    protected List<ItemKey> getViewForPage() {
        return CachedSrcInv.INSTANCE.getItemView(getStartIndex(), length,
                framework.sortType(), framework.isSortReversed(),
                getClassify(), framework.searching());
    }

    public abstract void requestRemoteContents();

    /**The change usually means pageMetaData changes and called by framework in sort,search,... changes.<br>
     * For ItemPage and ItemDisplay: also used to request contents as sending {@link ItemPageContext} has such side effect.
     */
    public void sendChangesToServer() {
        var layout = new PageData(this.id, framework.rows(), framework.columns(), framework.sortType(), framework.isSortReversed(), framework.searching());
        getPacketDistributor().sendToServer(new ItemPageContext(getStartIndex(), length, layout));//send this packet will receive a content packet as callback
    }

    public boolean isEmpty() {
        return viewContainer.isEmpty();
    }

    @Override
    public boolean canScroll() {
        return getStartIndex() >0 ||(getStartIndex() +length <= srcInv.getItemSize());
    }//todo

    /**Get mouse hovered or clicked item by mouse offset.
     * @param XOffset mouseX-pageX
     * @param YOffset mouseY-pageY
     * @return hovered or clicked item
     */
    @Override
    public ItemStack getItemByMouseOffset(double XOffset, double YOffset){
        int slot = getSlotByMouseOffset(XOffset,YOffset);
        if(slot>=0 && slot < viewContainer.getContainerSize()) {
            return viewContainer.getItem(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void handleStarItem(double XOffset, double YOffset) {
        ItemStack clicked = getItemByMouseOffset(XOffset, YOffset);
        if(clicked.isEmpty()) return;
        getPacketDistributor().sendToServer(new StarItemPayload(clicked,true));
    }

    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTicks){
        for(ItemPageSlotView slot : viewContainer.slots()){
            slot.render(GuiGraphicsExtractor, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void pageClicked(double XOffset, double YOffset, int button, ContainerInput ContainerInput) {
        int slot = getSlotByMouseOffset(XOffset,YOffset);
        if(slot>=0 && slot < viewContainer.getContainerSize()) {
            ItemStack clicked = getItemByMouseOffset(XOffset, YOffset).copy();
            switch (ContainerInput){
                case PICKUP -> handlePickup(clicked, button);
                case QUICK_MOVE -> handleQuickMove(clicked);
                case SWAP -> handleSwap(clicked, slot);
                case THROW -> handleThrow(clicked);
                case CLONE -> handleClone(clicked);
                case PICKUP_ALL -> handlePickupAll(clicked);
            }
            LOGGER.info("EI:sending:ItemClickPayload: player={} ContainerInput={} button={} stack={}"
                    , framework.getPlayer(),ContainerInput,button, clicked);
            if(ContainerInput == ContainerInput.PICKUP_ALL && Minecraft.getInstance().hasShiftDown()) return;
            ModInfo.getPacketDistributor().sendToServer(new ItemClickPayload(
                    ItemKey.asKey(clicked),
                    ContainerInput == ContainerInput.PICKUP
                            ? (button == InputConstants.MOUSE_BUTTON_LEFT ? 0 : 1)
                            : button,
                    ContainerInput));
            this.refreshItems();
        }
    }

    public ItemStack takeItem(ItemStack itemStack){
        return takeItem(itemStack,itemStack.getMaxStackSize());
    }

    public ItemStack takeItem(ItemStack itemStack,int count){
        setChanged();
        return this.srcInv.takeItem(itemStack,count);
    }

    public ItemStack takeItem(int index, int count){
        ItemStack itemStack = viewContainer.getItem(index);
        setChanged();
        return srcInv.takeItem(itemStack,count);
    }

    public ItemStack addItem(ItemStack itemStack){
        setChanged();
        return srcInv.addItem(itemStack.copy());
    }

    public boolean isFull(ItemStack itemStack){
        return itemStack.getCount() >= framework.getMaxStackSize();
    }

    public boolean isInfinite(ItemStack itemStack){
        return  isFull(itemStack) && framework.enableInfinity();
    }

    protected void handleQuickMove(ItemStack clicked){
        if(framework.getMenu() instanceof CreativeModeInventoryScreen.ItemPickerMenu) return;
        ItemStack taken = takeItem(clicked);
        ItemStack remain = framework.quickMoveFromPage(taken);
        addItem(remain);
        setChanged();
    }
    @Override
    public ItemStack tryInsertItem(ItemStack stack) {
        var remain = addItem(stack);
        initializeContents();
        return remain;
    }
    @Override
    public ItemStack tryExtractItem(ItemStack stack,int count){
        return takeItem(stack,count);
    }

    protected void handlePickup(ItemStack clicked, int keyCode){
        ItemStack carried = framework.getMenu().getCarried();
        if(!carried.isEmpty()){
            ItemStack remain = addItem(carried.copy());
            if(ModInfo.isClientLoaded() && framework.getMenu() instanceof CreativeModeInventoryScreen.ItemPickerMenu){
                getPacketDistributor().sendToServer(new CreativeItemModPayload(carried.copy(),true));
            }
            framework.getMenu().setCarried(remain);
            setChanged();
        }else{
            int count = Math.min(clicked.getCount(),clicked.getMaxStackSize());
            int takenCount = keyCode == InputConstants.MOUSE_BUTTON_LEFT ? count : (count + 1) / 2;
            framework.getMenu().setCarried(takeItem(clicked,takenCount));
            if(!framework.getMenu().getCarried().isEmpty()) setChanged();
        }
    }

    protected void handleSwap(ItemStack clicked, int inventorySlotId){
        Player player = framework.getPlayer();
        Inventory inventory = player.getInventory();
        ItemStack inventoryItem = inventory.getItem(inventorySlotId);
        boolean a = !inventoryItem.isEmpty();
        boolean b = !clicked.isEmpty();
        if( a && !b ){
            ItemStack remain = addItem(inventoryItem);
            inventory.setItem(inventorySlotId, remain);
        }
        if( !a && b ){
            ItemStack swapping = takeItem(clicked); //take most
            inventory.setItem(inventorySlotId,swapping);
        }
        if( a && b ){
            ItemStack remain = addItem(inventoryItem);
            if(remain.isEmpty()) {
                ItemStack swapping = takeItem(clicked); //take most
                inventory.setItem(inventorySlotId, swapping);
            }else {
                inventory.setItem(inventorySlotId,remain);
            }
        }
        setChanged();
    }
    protected void handleThrow(ItemStack clicked){
        Player player = framework.getPlayer();
        ItemStack thrown = takeItem(clicked);
        player.drop(thrown,true, net.minecraft.util.Prediction.PREDICTED);
        setChanged();
    }
    protected void handlePickupAll(ItemStack clicked){
        // Shift + Double Click: bulk quick-move from Endless Inventory page into the open container
        if (Minecraft.getInstance().hasShiftDown()) {
            ModInfo.getPacketDistributor().sendToServer(new BulkQuickMoveFromPagePayload(ItemKey.asKey(clicked.copyWithCount(1))));
            int iterations = 0;
            var mover = new PageQuickMoveHandler(framework);
            while (iterations++ < 32768) {
                ItemStack taken = takeItem(clicked.copyWithCount(1));
                if (taken.isEmpty()) break;
                ItemStack remain = mover.quickMoveFromPage(taken);
                if (!remain.isEmpty()) {
                    // Could not insert fully; put the remainder back and stop.
                    addItem(remain);
                    break;
                }
            }
            setChanged();
            release();
            // Ensure the view updates immediately so no temporary empty slot remains
            this.refreshItems();
            return;
        }
        Player player = framework.getPlayer();
        ItemStack carried = framework.getMenu().getCarried();
        int startIndex = framework.getMenu().slots.size() - 1; //changed: reversed button==0 condition
        for(int index = startIndex; index>=0 ; --index){
            Slot scanning = framework.getMenu().slots.get(index);
            if(!(scanning.container instanceof Inventory)) break;
            ItemStack scanningItem =scanning.getItem();
            if (ItemStack.isSameItemSameComponents(carried, scanningItem)) {
                ItemStack taken = scanning.safeTake(scanningItem.getCount(), scanningItem.getCount(), player);
                ItemStack remain = addItem(taken);
                if(!remain.isEmpty()) scanning.set(remain);
                setChanged();
            }
        }
    }
    protected void handleClone(ItemStack clicked){
        Player player = framework.getPlayer();
        if(player.getAbilities().instabuild && framework.getMenu().getCarried().isEmpty()){
            framework.getMenu().setCarried(clicked.copyWithCount(clicked.getMaxStackSize()));
        }
    }
}



