package com.kwwsyk.endinv;

import com.kwwsyk.endinv.client.LocalData;
import com.kwwsyk.endinv.network.payloads.EndInvRequestContentPayload;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static com.kwwsyk.endinv.EndlessInventory.getEndInvForPlayer;
import static com.kwwsyk.endinv.ModInitializer.ENDINV_SETTINGS;
import static com.kwwsyk.endinv.ModInitializer.ENDLESS_INVENTORY_MENU_TYPE;

public class EndlessInventoryMenu extends AbstractContainerMenu {

    private static final int DEFAULT_ROWS = 15;

    private final Inventory playerInv;
    private int rows;
    private ItemDisplay container;
    private final Player player;
    int quickcraftStatus;
    int quickcraftType;
    Set<Slot> quickcraftSlots = new HashSet<>();
    private final DataSlot rowsData = DataSlot.standalone();
    private final DataSlot itemSize = DataSlot.standalone();

    //Client constructor
    public EndlessInventoryMenu(int id, Inventory playerInv){
        this(id,playerInv, LocalData.settings.rows(), null);
    }
    //Server constructor
    public EndlessInventoryMenu(int id , Inventory playerInv, int rows,@Nullable EndlessInventory endlessInventory){
        super(ENDLESS_INVENTORY_MENU_TYPE.get(),id);
        this.container = new ItemDisplay(this,endlessInventory,9*rows);
        this.playerInv = playerInv;
        this.player = playerInv.player;
        rowsData.set(rows);
        itemSize.set( endlessInventory!=null ? endlessInventory.getItemSize() : 0);
        addDataSlot(rowsData);
        addDataSlot(itemSize);
        this.refreshSlots(rows,playerInv);
    }

    private void refreshSlots(int rows,Inventory playerInv){
        this.rows = rows;
        for(int i=0;i<rows;++i){
            for(int j=0;j<9;++j){
                //refer: CreativeModeInventoryMenu
                this.addSlot(new Slot(this.container,9*i+j,8+j*18,18+i*18));
            }
        }
        this.addStandardInventorySlots(playerInv,8,18*rows+18+13);
        this.scrollTo(0.0F);
    }

    private void addStandardInventorySlots(Inventory playerInventory, int x, int y){
        int i = (this.rows - 4) * 18;
        for (int l = 0; l < 3; l++) {
            for (int j1 = 0; j1 < 9; j1++) {
                this.addSlot(new Slot(playerInventory, j1 + l * 9 + 9, 8 + j1 * 18, 103 + l * 18 + i));
            }
        }

        for (int i1 = 0; i1 < 9; i1++) {
            this.addSlot(new Slot(playerInventory, i1, 8 + i1 * 18, 161 + i));
        }
    }

    public void scrollTo(float pos){
        int startIndex = this.getRowIndexForScroll(pos) * 9;
        // 重新设置 `ItemDisplay` 的视图
        this.container.setDisplay(startIndex, this.rows * 9);
    }

    public int getItemSize(){
        return itemSize.get();
    }

    public void setItemSize(int i){
        this.itemSize.set(i);
    }

    protected int calculateRowCount() {
        return Mth.positiveCeilDiv(this.getItemSize(), 9) - this.rows;
    }

    public int getRowIndexForScroll(float scrollOffs) {
        return Math.max((int)((double)(scrollOffs * (float)this.calculateRowCount()) + 0.5), 0);
    }

    public float getScrollForRowIndex(int rowIndex) {
        return Mth.clamp((float)rowIndex / (float)this.calculateRowCount(), 0.0F, 1.0F);
    }

    public float subtractInputFromScroll(float scrollOffs, double input) {
        return Mth.clamp(scrollOffs - (float)(input / (double)this.calculateRowCount()), 0.0F, 1.0F);
    }

    public static AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        EndlessInventory endlessInventory = getEndInvForPlayer(player);
        return new EndlessInventoryMenu(i,inventory,player.getData(ENDINV_SETTINGS).rows(),endlessInventory);
    }

    public ItemDisplay getContainer(){
        return this.container;
    }

    public SourceInventory getSourceInventory(){
        return this.container.getSourceInventory();
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
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
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
            if(this.container.getSourceInventory() instanceof EndlessInventory) this.container.refreshItems();
            if(this.container.getSourceInventory() instanceof EndlessInventory endinv){
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
            public ItemStack get() {
                return EndlessInventoryMenu.this.getCarried();
            }

            @Override
            public boolean set(ItemStack itemStack) {
                EndlessInventoryMenu.this.setCarried(itemStack);
                return true;
            }
        };
    }


    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            if (index < this.rows * 9) { // in EndInv.
                /*
                   EndInv slot may have more than 64/16 items,
                   but maybe it's no necessary branching two situations,
                   we take its max stack size count items and put into itemStack field.
                 */
                //先“借”至多一组的物品
                itemstack = this.container.takeItem(itemstack1);
                //moveItemStackTo : 若itemStack被更改（进入物品栏或部分分离进入），则返回true
                //所以 可能削减itemStack的数量
                boolean isItemStackMoved = this.moveItemStackTo(itemstack, this.rows * 9, this.slots.size(), true);
                //“还”
                if(!itemstack.isEmpty()) this.container.addItem(itemstack);
                if(!isItemStackMoved) return ItemStack.EMPTY;
            } else { // in player inventory
                if(itemstack1.getItem()== ModInitializer.testEndInv.get()) return ItemStack.EMPTY;
                itemstack =itemstack1.copy();
                this.container.addItem(itemstack);
                itemstack1.setCount(0);
                slot.setByPlayer(ItemStack.EMPTY);

            }

        this.container.setChanged();
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }


    public void requestContent(int startIndex, int length) {
        if(this.player instanceof LocalPlayer){
            PacketDistributor.sendToServer(new EndInvRequestContentPayload(startIndex,length,SortType.DEFAULT));
        }
    }
}
