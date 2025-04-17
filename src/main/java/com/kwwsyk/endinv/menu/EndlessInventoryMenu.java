package com.kwwsyk.endinv.menu;

import com.kwwsyk.endinv.EndlessInventory;
import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.SourceInventory;
import com.kwwsyk.endinv.client.config.ClientConfig;
import com.kwwsyk.endinv.menu.page.DefaultPages;
import com.kwwsyk.endinv.menu.page.DisplayPage;
import com.kwwsyk.endinv.menu.page.ItemDisplay;
import com.kwwsyk.endinv.network.payloads.PageChangePayload;
import com.kwwsyk.endinv.options.ItemClassify;
import com.kwwsyk.endinv.options.SortType;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kwwsyk.endinv.EndlessInventory.getEndInvForPlayer;
import static com.kwwsyk.endinv.ModInitializer.ENDINV_SETTINGS;
import static com.kwwsyk.endinv.ModInitializer.ENDLESS_INVENTORY_MENU_TYPE;

public class EndlessInventoryMenu extends AbstractContainerMenu {


    private final Inventory playerInv;
    private final SourceInventory sourceInventory;
    private int rows;

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
    private int inventoryX;
    private int inventoryY;
    public final SourceInventory REMOTE = new SourceInventory() {
        public ItemStack getItem(int i){
            return ItemStack.EMPTY;
        }

        public int getItemSize(){
            return EndlessInventoryMenu.this.getItemSize();
        }

        @Override
        public ItemStack takeItem(ItemStack itemStack) {
            setChanged();
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack takeItem(ItemStack itemStack, int count) {
            setChanged();
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack addItem(ItemStack itemStack) {
            setChanged();
            return ItemStack.EMPTY;
        }

        @Override
        public void setChanged() {
            EndlessInventoryMenu.this.displayingPage.setChanged();

        }

    };

    //Client constructor
    public EndlessInventoryMenu(int id, Inventory playerInv){
        this(id,playerInv, ClientConfig.CONFIG.ROWS.getAsInt(), null);
    }
    //Server constructor
    public EndlessInventoryMenu(int id , Inventory playerInv, int rows,EndlessInventory endlessInventory){
        super(ENDLESS_INVENTORY_MENU_TYPE.get(),id);
        this.rows = rows;
        this.playerInv = playerInv;
        this.player = playerInv.player;
        this.sourceInventory = endlessInventory!=null ? endlessInventory : REMOTE;
        this.pages = buildPages();
        this.sortType = SortType.DEFAULT;
        this.searching = "";
        //build data slots
        rowsData.set(rows);
        itemSize.set( endlessInventory!=null ? endlessInventory.getItemSize() : 0);
        maxStackSize.set(endlessInventory!=null? endlessInventory.getMaxItemStackSize() : Integer.MAX_VALUE);
        infinityMode.set(endlessInventory!=null && endlessInventory.isInfinityMode() ? 1 : 0);
        addDataSlot(rowsData);
        addDataSlot(itemSize);
        addDataSlot(maxStackSize);
        addDataSlot(infinityMode);
        //build inventory slots
        this.inventoryX = 8;
        this.inventoryY = 18*rows +18+13;
        this.addStandardInventorySlots(playerInv,inventoryX,inventoryY);
        //build default page
        this.switchPage(0);
    }
    //build pages by default registered item classifies on server and with config hiding pages on client.
    // will change when need to compatible for reg more pages.
    private List<DisplayPage> buildPages(){
        List<DisplayPage> ret = new ArrayList<>();
        for(int i=0; i<ItemClassify.DEFAULT_CLASSIFIES.size(); ++i){
            Holder<ItemClassify> classify = ItemClassify.DEFAULT_CLASSIFIES.get(i);
            if(player instanceof LocalPlayer) {
                boolean hidden = ClientConfig.CONFIG.PAGES.get(i).getAsBoolean();
                if (!hidden) {
                    DisplayPage page = DefaultPages.CLASSIFY2PAGE.get(classify).create(this, classify, i);
                    page.icon = DefaultPages.CLASSIFY2RSRC.get(classify);
                    ret.add(page);
                }
            }else {
                DisplayPage page = DefaultPages.CLASSIFY2PAGE.get(classify).create(this, classify, i);
                ret.add(page);
            }
        }
        return ret;
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
    public void switchPage(int index){
        this.displayingPageIndex = index;
        this.displayingPage = this.pages.get(index);
        this.displayingPage.scrollTo(0);
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

    public int calculateRowCount() {
        return displayingPage.calculateRowCount();
    }

    public float subtractInputFromScroll(float scrollOffs, double input) {
        return Mth.clamp(scrollOffs - (float)(input / (double)this.calculateRowCount()), 0.0F, 1.0F);
    }

    public boolean enableInfinity(){
        return infinityMode.get() > 0;
    }

    public int getMaxStackSize(){
        return maxStackSize.get();
    }

    public static AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        EndlessInventory endlessInventory = getEndInvForPlayer(player);
        return new EndlessInventoryMenu(i,inventory,player.getData(ENDINV_SETTINGS).rows(),endlessInventory);
    }
    public int getDisplayingPageId(){
        return displayingPage.pageId;
    }
    public DisplayPage getDisplayingPage(){
        return displayingPage;
    }
    public int getDisplayingPageIndex(){return displayingPageIndex;}
    public SourceInventory getSourceInventory(){
        return this.sourceInventory;
    }

    public int getRowCount(){
        return  this.rows;
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
            if(this.getSourceInventory() instanceof EndlessInventory && this.displayingPage instanceof ItemDisplay itemDisplay)
                itemDisplay.refreshItems();
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
        if (net.neoforged.neoforge.common.CommonHooks.onItemStackedOn(clickedItem, carriedItem, slot, action, player, createCarriedSlotAccess())) {
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
    public boolean stillValid(@NotNull Player player) {
        return true;
    }


    public void requestContent(int startIndex, int length) { //将要废弃
        if(this.player instanceof LocalPlayer){
            PacketDistributor.sendToServer(new PageChangePayload(startIndex,length, sortType, displayingPage.getItemClassify(), searching));
        }
    }
    public void syncContent(){
        displayingPage.syncContentToServer();
    }
}
