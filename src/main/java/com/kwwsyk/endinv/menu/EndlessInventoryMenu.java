package com.kwwsyk.endinv.menu;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.SourceInventory;
import com.kwwsyk.endinv.client.CachedSrcInv;
import com.kwwsyk.endinv.menu.page.DisplayPage;
import com.kwwsyk.endinv.menu.page.ItemPage;
import com.kwwsyk.endinv.menu.page.PageType;
import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.network.payloads.PageData;
import com.kwwsyk.endinv.network.payloads.SyncedConfig;
import com.kwwsyk.endinv.network.payloads.toClient.EndInvMetadata;
import com.kwwsyk.endinv.util.SortType;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kwwsyk.endinv.EndlessInventory.getEndInvForPlayer;
import static com.kwwsyk.endinv.ModInitializer.ENDLESS_INVENTORY_MENU_TYPE;
import static com.kwwsyk.endinv.ModInitializer.SYNCED_CONFIG;

public class EndlessInventoryMenu extends AbstractContainerMenu implements PageMetaDataManager {


    private final SourceInventory sourceInventory;

    public final Player player;
    int quickcraftStatus;
    int quickcraftType;
    Set<Slot> quickcraftSlots = new HashSet<>();
    private final DataSlot rowsData = DataSlot.standalone();
    private final DataSlot itemSize = DataSlot.standalone();
    private final DataSlot maxStackSize = DataSlot.standalone();
    private final DataSlot infinityMode = DataSlot.standalone();
    private DisplayPage displayingPage;
    //client for page switch
    private int displayingPageIndex;
    public final List<DisplayPage> pages;
    public SortType sortType;
    public String searching;
    private boolean reverseSort;


    //Client constructor
    public static EndlessInventoryMenu createClient(int id, Inventory playerInv){
        var ret = new EndlessInventoryMenu(id,playerInv,null);
        if (Minecraft.getInstance().player != null) {
            SyncedConfig config = Minecraft.getInstance().player.getData(SYNCED_CONFIG);
            ret.init(config.pageData());
            ret.addStandardInventorySlots(playerInv, 8, 18 * ret.getRowCount() + 18 + 13);
        }

        return ret;
    }

    //Server constructor
    public static AbstractContainerMenu createServer(int i, Inventory inventory, Player player) {
        EndlessInventory endlessInventory = getEndInvForPlayer(player);
        SyncedConfig config = player.getData(SYNCED_CONFIG);
        var ret = new EndlessInventoryMenu(i,inventory,endlessInventory);
        ret.init(config.pageData());
        ret.addStandardInventorySlots(inventory, 8, 18 * ret.getRowCount() + 18 + 13);
        return ret;
    }

    //Common constructor
    public EndlessInventoryMenu(int id , Inventory playerInv, EndlessInventory endlessInventory){
        super(ENDLESS_INVENTORY_MENU_TYPE.get(),id);
        this.player = playerInv.player;
        this.sourceInventory = endlessInventory!=null ? endlessInventory : CachedSrcInv.INSTANCE;
        this.pages = buildPages();
        //build data slots
        itemSize.set( endlessInventory!=null ? endlessInventory.getItemSize() : 0);
        maxStackSize.set(endlessInventory!=null? endlessInventory.getMaxItemStackSize() : Integer.MAX_VALUE);
        infinityMode.set(endlessInventory!=null && endlessInventory.isInfinityMode() ? 1 : 0);
        addDataSlot(rowsData);
        addDataSlot(itemSize);
        addDataSlot(maxStackSize);
        addDataSlot(infinityMode);
    }

    private void init(int rows, SortType sortType, String searching, PageType type){
        rowsData.set(rows);
        this.sortType = sortType;
        this.searching = searching;
        this.switchPageWithType(type);
    }

    private void init(PageData pageData){
        init(pageData.rows()-4,pageData.sortType(),pageData.search(),pageData.pageType().value());//4: reserved rows for inventory.
    }

    private void addStandardInventorySlots(Inventory playerInventory, int x, int y){
        for (int l = 0; l < 3; l++) {
            for (int j1 = 0; j1 < 9; j1++) {
                this.addSlot(new Slot(playerInventory, j1 + l * 9 + 9, x + j1 * 18, y + l * 18 ));
            }
        }

        for (int i1 = 0; i1 < 9; i1++) {
            this.addSlot(new Slot(playerInventory, i1, x + i1 * 18, y+58));
        }
    }

    //supposed to be the only method to change displaying page and index value; in order to sync.
    public void switchPageWithIndex(int index){
        this.displayingPageIndex = index;
        this.displayingPage = this.pages.get(index);
        SyncedConfig.updateClientConfigAndSync(player.getData(SYNCED_CONFIG).pageTypeChanged(displayingPage.getPageType()));
        this.displayingPage.init(0,9*rowsData.get());
    }

    public void scrollTo(float pos){
        this.displayingPage.scrollTo(pos);
    }

    public int getItemSize(){
        return itemSize.get();
    }

    public void setItemSize(int i){
        this.itemSize.set(i);
    }

    public float subtractInputFromScroll(float scrollOffs, double input) {
        return Mth.clamp(scrollOffs - (float)(input / (double)this.rowsData.get()), 0.0F, 1.0F);
    }

    public boolean enableInfinity(){
        return infinityMode.get() > 0;
    }

    public int getMaxStackSize(){
        return maxStackSize.get();
    }

    public DisplayPage getDisplayingPage(){
        return displayingPage;
    }

    public int getDisplayingPageIndex(){return displayingPageIndex;}

    @Override
    public AbstractContainerMenu getMenu() {
        return this;
    }

    public SourceInventory getSourceInventory(){
        return this.sourceInventory;
    }

    @Override
    public List<DisplayPage> getPages() {
        return pages;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public int getRowCount(){
        return  this.rowsData.get();
    }

    @Override
    public int getColumnCount() {
        return 9;
    }

    /**Override {@link AbstractContainerMenu#clicked(int, int, ClickType, Player)}
     * Invoked when Client click in container screen/Server handle click packet.
     * for details see below.
     * @param slotId index
     * @param button ...0: left 1: right 2: middle ? Is there anyone who can explain?
     * @param clickType {@link ClickType}
     * @param player player performing menu click
     */
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        try {
            if(clickType==ClickType.QUICK_CRAFT){
                MenuClickHandler.handleQuickCraft(this,slotId,button,player);
            }else if(this.quickcraftStatus!=0){
                this.resetQuickCraft();
            }
            switch (clickType){
                case PICKUP -> MenuClickHandler.handlePickup(this,slotId,button,player);
                case QUICK_MOVE -> MenuClickHandler.handleQuickMove(this,slotId,button,player);
                case SWAP -> MenuClickHandler.handleSwap(this,slotId,button,player);
                case THROW -> MenuClickHandler.handleThrow(this,slotId,button,player);
                case CLONE -> MenuClickHandler.handleClone(this,slotId,button,player);
                case PICKUP_ALL -> MenuClickHandler.handlePickupAll(this,slotId,button,player);
                default -> {
                    return;
                }
            }
            if(this.getSourceInventory() instanceof EndlessInventory && this.displayingPage instanceof ItemPage itemPage)
                itemPage.refreshItems();
            if(this.getSourceInventory() instanceof EndlessInventory endinv){
                this.setItemSize(endinv.getItemSize());
            }
        } catch (Exception exception) {
            CrashReport crashreport = CrashReport.forThrowable(exception, "Container click");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Click info");
            crashreportcategory.setDetail("Menu Type", "endless_inventory");
            crashreportcategory.setDetail("Menu Class", () -> this.getClass().getCanonicalName());
            crashreportcategory.setDetail("Slot Count", this.slots.size());
            crashreportcategory.setDetail("Slot", slotId);
            crashreportcategory.setDetail("Button", button);
            crashreportcategory.setDetail("Type", clickType);
            throw new ReportedException(crashreport);
        }
    }

    boolean tryItemClickBehaviourOverride(Player player, ClickAction action, Slot slot, ItemStack clickedItem, ItemStack carriedItem) {
        // Neo: Fire the ItemStackedOnOtherEvent, and return true if it was cancelled (meaning the event was handled). Returning true will trigger the container to stop processing further logic.
        if (CommonHooks.onItemStackedOn(clickedItem, carriedItem, slot, action, player, createCarriedSlotAccess())) {
            return true;
        }

        FeatureFlagSet featureflagset = player.level().enabledFeatures();
        //item combining in menu, bundle, etc
        return carriedItem.isItemEnabled(featureflagset) && carriedItem.overrideStackedOnOther(slot, action, player)
                || clickedItem.isItemEnabled(featureflagset)
                    && clickedItem.overrideOtherStackedOnMe(carriedItem, slot, action, player, this.createCarriedSlotAccess());
    }

    private SlotAccess createCarriedSlotAccess() {
        return new SlotAccess() {
            @Override
            public @NotNull ItemStack get() {
                return EndlessInventoryMenu.this.getCarried();
            }

            @Override
            public boolean set(@NotNull ItemStack itemStack) {
                EndlessInventoryMenu.this.setCarried(itemStack);
                return true;
            }
        };
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            if(itemstack1.getItem()== ModInitializer.testEndInv.get()) return ItemStack.EMPTY;
            itemstack =itemstack1.copy();
            ItemStack remain = this.displayingPage.tryQuickMoveStackTo(itemstack);
            itemstack1.setCount(remain.getCount());
            slot.setByPlayer(ItemStack.EMPTY);
            displayingPage.setChanged();
        }

        return itemstack;
    }
    public ItemStack quickMoveFromPage(ItemStack stack){
        moveItemStackTo(stack,0,this.slots.size()-1,true);
        return stack;
    }

    @Override
    public SortType sortType() {
        return sortType;
    }

    @Override
    public void setSortType(SortType sortType) {
        this.sortType = sortType;
    }

    @Override
    public boolean isSortReversed() {
        return reverseSort;
    }

    @Override
    public void switchSortReversed() {
        reverseSort=!reverseSort;
    }

    @Override
    public void setSortReversed(boolean reversed) {
        this.reverseSort = reversed;
    }

    @Override
    public String searching() {
        return searching;
    }

    @Override
    public void setSearching(String searching) {
        this.searching = searching;
    }

    /**
     *Send operation will be accomplished in {@link AbstractContainerMenu#broadcastChanges()}
     */
    @Override
    public void sendEndInvMetadataToRemote(){
        if(sourceInventory instanceof EndlessInventory endlessInventory){
            itemSize.set(endlessInventory.getItemSize());
            maxStackSize.set(endlessInventory.getMaxItemStackSize());
            infinityMode.set((endlessInventory.isInfinityMode()?1:0));
        }
    }

    @Override
    public EndInvMetadata getEndInvMetadata() {
        return new EndInvMetadata(itemSize.get(),maxStackSize.get(),infinityMode.get()>0);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

}
