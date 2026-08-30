package com.kwwsyk.endinv.common.menu;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.option.CachedConfig;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.menu.page.PageTypeRegistry;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageQuickMoveHandler;
import com.kwwsyk.endinv.common.network.payloads.PageData;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.kwwsyk.endinv.common.ModRegistries.Items;
import static com.kwwsyk.endinv.common.ModRegistries.Menus;
import static com.kwwsyk.endinv.common.ServerLevelEndInv.getEndInvForPlayer;

/**The menu who links to EndlessInventory
 * Implementation: maybe let EIS implements them
 */
public class EndlessInventoryMenu extends AbstractContainerMenu implements PageMetaDataManager, PageQuickMoveHandler.PageQuickMoveOverride {


    private final SourceInventory sourceInventory;

    private final CraftingContainer craftMatrix = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer craftResult = new ResultContainer();

    private static final int CRAFT_GRID_WIDTH = 3;
    private static final int CRAFT_GRID_HEIGHT = 3;
    private static final int RESULT_SLOT_INDEX = 0;
    private static final int CRAFT_SLOT_START = RESULT_SLOT_INDEX + 1;
    private static final int CRAFT_SLOT_COUNT = CRAFT_GRID_WIDTH * CRAFT_GRID_HEIGHT;
    private static final int CRAFT_SLOT_END = CRAFT_SLOT_START + CRAFT_SLOT_COUNT;
    private static final int PLAYER_INV_SLOT_COUNT = 27;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INV_START = CRAFT_SLOT_END;
    private static final int PLAYER_INV_END = PLAYER_INV_START + PLAYER_INV_SLOT_COUNT + HOTBAR_SLOT_COUNT;

    public final Player player;
    int quickcraftStatus;
    int quickcraftType;
    Set<Slot> quickcraftSlots = new HashSet<>();
    private final DataSlot rowsData = DataSlot.standalone();
    private final DataSlot itemSize = DataSlot.standalone();
    private final DataSlot maxStackSize = DataSlot.standalone();
    private static final int CRAFTING_ROWS = CRAFT_GRID_HEIGHT;

    private final DataSlot infinityMode = DataSlot.standalone();
    private int displayingPageIndex;
    private String displayingPageId;
    private PageType displayingPageType;
    private int baseRows = 1;
    private int visibleRows = 1;
    private boolean craftingVisible = false;
    public SortType sortType;
    public String searching;
    private boolean reverseSort;


    //Client constructor
    //should be only invoked on client thread
    public static EndlessInventoryMenu createClient(int id, Inventory playerInv){
        PageData layout = CachedConfig.resolveLayout(null, true);
        var ret = new EndlessInventoryMenu(id, playerInv, null);
        ret.init(layout);
        ret.switchPageWithId(layout.pageRegKey());
        ret.buildSlotLayout(playerInv);
        return ret;
    }

    public static MenuProvider provide(int rows){
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.empty();
            }

            @Override @Nullable
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return createServer(id,inventory,player,rows);
            }
        };
    }

    //Server constructor
    @Nullable
    public static AbstractContainerMenu createServer(int i, Inventory inventory, Player player, int rows) {
        EndlessInventory endlessInventory = getEndInvForPlayer(player).orElse(null);
        if(endlessInventory==null) return null;
        var ret = new EndlessInventoryMenu(i, inventory, endlessInventory);
        ret.init(new PageData(rows,9));
        ret.buildSlotLayout(inventory);
        return ret;
    }

    public static EndlessInventoryMenu createWithTemp(int i,Inventory inventory, Player player){
        EndlessInventory endInv = ServerLevelEndInv.TEMP_ENDINV_REG.get((ServerPlayer) player);
        if(endInv==null) throw new IllegalStateException("Try to create tmp menu without tmp EndInv.");
        var ret = new EndlessInventoryMenu(i,inventory,endInv);
        ret.init(PageData.DEFAULT);
        ret.buildSlotLayout(inventory);
        return ret;
    }

    //Common constructor
    public EndlessInventoryMenu(int id , Inventory playerInv,@Nullable EndlessInventory endlessInventory){
        super(Menus.getEndInvMenuType(),id);
        this.player = playerInv.player;
        this.sourceInventory = endlessInventory!=null ? endlessInventory : CachedSrcInv.INSTANCE;

        PageType initialType = PageTypeRegistry.byId(PageType.DEFAULT_KEY);
        if (initialType == null && PageTypeRegistry.size() > 0) {
            initialType = PageTypeRegistry.byIndex(0);
        }
        if (initialType == null) {
            initialType = PageType.ALL_ITEMS;
        }
        this.displayingPageType = initialType;
        this.displayingPageId = initialType.registerName;
        this.displayingPageIndex = Math.max(0, PageTypeRegistry.getIndexOf(this.displayingPageId));

        // pages are client-side; slots are built after layout initialization
        //build data slots
        itemSize.set( endlessInventory!=null ? endlessInventory.getItemSize() : 0);
        maxStackSize.set(endlessInventory!=null? endlessInventory.getMaxItemStackSize() : Integer.MAX_VALUE);
        infinityMode.set(endlessInventory!=null && endlessInventory.isInfinityMode() ? 1 : 0);
        addDataSlot(rowsData);
        addDataSlot(itemSize);
        addDataSlot(maxStackSize);
        addDataSlot(infinityMode);
    }

    public void applyPageData(PageData pageData){
        init(pageData);
    }

    private void init(PageData pageData){
        int rows = Math.max(1, pageData.rows());
        this.baseRows = rows;
        this.visibleRows = craftingVisible ? Math.max(1, rows - CRAFTING_ROWS) : rows;
        rowsData.set(this.visibleRows);
        this.sortType = pageData.sortType();
        this.searching = pageData.search();
        this.reverseSort = pageData.reverseSort();
    }

    private void buildSlotLayout(Inventory playerInventory) {
        if (!this.slots.isEmpty()) {
            return;
        }
        int craftX = 30;
        int craftRowsForPosition = Math.max(1, baseRows - CRAFTING_ROWS);
        int craftY = 18 * craftRowsForPosition + 18 + 5;
        int resultX = craftX + CRAFT_GRID_WIDTH * 18 + 40;
        int resultY = craftY + 18;
        this.addSlot(new CraftingResultSlot(this.player, this.craftMatrix, this.craftResult, 0, resultX, resultY));
        for (int row = 0; row < CRAFT_GRID_HEIGHT; ++row) {
            for (int col = 0; col < CRAFT_GRID_WIDTH; ++col) {
                this.addSlot(new CraftingGridSlot(this.craftMatrix, col + row * CRAFT_GRID_WIDTH, craftX + col * 18, craftY + row * 18));
            }
        }
        int invY = 18 * baseRows + 31;
        addStandardInventorySlots(playerInventory, 8, invY);
    }

    public List<Slot> getCraftingSlots(){
        return slots.subList(1,1+9);
    }

    public List<Slot> getPlayerInvSlots(){
        return slots.subList(10,10+36);
    }

    @TestOnly
    public boolean validateSlotStatus() throws IllegalStateException{
        for(Slot slot : getCraftingSlots()){
            if(!(slot instanceof CraftingGridSlot)) throw new IllegalStateException("getCraftingSlots do not correspond menu's crafter slots.");
        }
        for(Slot slot : getPlayerInvSlots()){
            if(!(slot.container instanceof Inventory)) throw new IllegalStateException("getPlayerInvSlots contains slots whose container is not inventory");
        }
        if(!(slots.get(0) instanceof CraftingGridSlot)) throw new IllegalStateException("the first slot is not Crafter's result slot, check ADD SLOT process");
        return true;
    }

    public boolean isCrafterEnabled(){
        return true;
    }

    public void setCraftingVisible(boolean visible) {
        if (this.craftingVisible == visible) {
            return;
        }
        this.craftingVisible = visible;
        this.visibleRows = visible ? Math.max(1, baseRows - CRAFTING_ROWS) : baseRows;
        rowsData.set(this.visibleRows);
        if (!visible) {
            returnCraftingToPlayer();
        }
    }

    private void returnCraftingToPlayer() {
        if (player.level().isClientSide) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < craftMatrix.getContainerSize(); ++i) {
            ItemStack stack = craftMatrix.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                inventory.placeItemBackInInventory(stack);
            }
        }
        ItemStack result = craftResult.removeItemNoUpdate(0);
        if (!result.isEmpty()) {
            inventory.placeItemBackInInventory(result);
        }
        craftMatrix.setChanged();
        craftResult.setChanged();
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == this.craftMatrix) {
            this.updateCraftingResult();
        }
        super.slotsChanged(container);
    }

    public boolean isCraftingVisible() {
        return craftingVisible;
    }

    public int getVisibleRows() {
        return visibleRows;
    }

    public int getBaseRows() {
        return baseRows;
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

    //supposed to be the only method to change displaying page and index value; to sync.
    public void switchPageWithIndex(int index){
        if(index < 0 || index >= PageTypeRegistry.size()) {
            return;
        }
        PageType type = PageTypeRegistry.byIndex(index);
        if (type != null) {
            applySelectedPage(type);
        }
    }

    private void applySelectedPage(PageType type){
        this.displayingPageType = type;
        this.displayingPageId = type.registerName;
        this.displayingPageIndex = Math.max(0, PageTypeRegistry.getIndexOf(this.displayingPageId));
    }

    public int getItemSize(){
        return itemSize.get();
    }

    public void setItemSize(int i){
        this.itemSize.set(i);
    }

    public boolean enableInfinity(){
        return infinityMode.get() > 0;
    }

    public int getMaxStackSize(){
        return maxStackSize.get();
    }

    @Override
    public AbstractContainerMenu getMenu() {
        return this;
    }

    public SourceInventory getSourceInventory(){
        return this.sourceInventory;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public int rows(){
        return this.visibleRows;
    }

    @Override
    public int columns() {
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
                case PICKUP_ALL -> this.handlePickupAll(slotId, button, player);
                default -> {
                    return;
                }
            }
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
        if (ModInfo.platformContext.onItemStackedOn(clickedItem, carriedItem, slot, action, player, createCarriedSlotAccess())) {
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

    /**
     * Handles vanilla quick-move (shift-click) transfers, prioritising crafter slots before the player inventory
     * and falling back to the endless inventory page when needed.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        if (slotStack.getItem() == Items.getTestEndInv()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slotStack.copy();
        boolean handled;

        if (index == RESULT_SLOT_INDEX) {
            handled = handleQuickMoveResult(slot, slotStack, original);
        } else if (isCrafterSlot(index)) {
            handled = handleQuickMoveFromCrafterSlot(slot, slotStack);
        } else if (isPlayerInventorySlot(index)) {
            handled = handleQuickMoveFromPlayerInventory(slotStack);
        } else {
            handled = this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, false);
        }

        if (!handled) {
            return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (slotStack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, slotStack);
        if (index == RESULT_SLOT_INDEX) {
            player.drop(slotStack, false);
        }

        return original;

    }



    /**
     * @return true when the quick-move source slot belongs to the player inventory range.
     */
    private boolean isPlayerInventorySlot(int index) {
        return index >= PLAYER_INV_START && index < PLAYER_INV_END;
    }

    /**
     * @return true when the slot index falls inside the 3x3 crafter grid.
     */
    private boolean isCrafterSlot(int index) {
        return index >= CRAFT_SLOT_START && index < CRAFT_SLOT_END;
    }

    /**
     * Push a stack back into the player inventory when it originated from the crafter grid.
     */
    private boolean handleQuickMoveFromCrafterSlot(Slot slot, ItemStack stack) {
        boolean moved = this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false);
        if (moved) {
            craftMatrix.setChanged();
            updateCraftingResult();
        }
        return moved;
    }

    /**
     * Try to populate the crafter grid before resorting to storing items in the endless inventory page.
     */
    private boolean moveStackIntoCrafter(ItemStack stack) {
        int before = stack.getCount();
        boolean moved = this.moveItemStackTo(stack, CRAFT_SLOT_START, CRAFT_SLOT_END, false);
        if (!moved || stack.getCount() == before) {
            return false;
        }
        // Prevent ghost copies on the client by only flagging a move when the stack actually shrank.
        craftMatrix.setChanged();
        updateCraftingResult();
        return true;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (slot.container == this.craftResult) {
            return false;
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    private ItemStack quickMoveIntoPage(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remain = this.sourceInventory.addItem(stack);
        this.sourceInventory.setChanged();
        return remain;
    }

    private void handlePickupAll(int slotId, int button, Player player) {
        if (slotId < 0) {
            return;
        }
        Slot clickedSlot = this.slots.get(slotId);
        ItemStack carried = this.getCarried();
        if (carried.isEmpty()) {
            return;
        }

        if (!clickedSlot.hasItem() || !clickedSlot.mayPickup(player)) {
            int startIndex = button == 0 ? 0 : this.slots.size() - 1;
            int step = button == 0 ? 1 : -1;

            for (int pass = 0; pass < 2; pass++) {
                for (int index = startIndex; index >= 0 && index < this.slots.size() && carried.getCount() < carried.getMaxStackSize(); index += step) {
                    Slot scanningSlot = this.slots.get(index);
                    if (AbstractContainerMenu.canItemQuickReplace(scanningSlot, carried, true)
                            && scanningSlot.mayPickup(player)
                            && this.canTakeItemForPickAll(carried, scanningSlot)) {
                        ItemStack scanningItem = scanningSlot.getItem();
                        if (pass != 0 || scanningItem.getCount() != scanningItem.getMaxStackSize()) {
                            ItemStack taken = scanningSlot.safeTake(scanningItem.getCount(), carried.getMaxStackSize() - carried.getCount(), player);
                            carried.grow(taken.getCount());
                        }
                    }
                }

                if (carried.getCount() < carried.getMaxStackSize()) {
                    ItemStack extracted = tryExtractFromPage(carried, carried.getMaxStackSize() - carried.getCount());
                    carried.grow(extracted.getCount());
                    carried.setCount(Math.min(carried.getCount(), carried.getMaxStackSize()));
                }
            }
        }
    }

    public ItemStack tryExtractFromPage(ItemStack template, int count) {
        if (count <= 0 || template.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack request = template.copy();
        return this.sourceInventory.takeItem(request, count);
    }

    /**
     * Send stacks from the current page into the crafter (when visible) and then the player inventory.
     */
    public ItemStack quickMoveFromPage(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }

        boolean crafterAccepted = craftingVisible && moveStackIntoCrafter(stack);
        // Let the simulated transfer mirror the server: only spill into the inventory when there is a real remainder after filling the crafter.
        if (!stack.isEmpty()) {
            boolean movedToMain = this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_START + PLAYER_INV_SLOT_COUNT, false);
            if (!movedToMain) {
                this.moveItemStackTo(stack, PLAYER_INV_START + PLAYER_INV_SLOT_COUNT, PLAYER_INV_END, false);
            }
        }
        return crafterAccepted && stack.isEmpty() ? ItemStack.EMPTY : stack;
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
     * Send operation will be accomplished in {@link AbstractContainerMenu#broadcastChanges()}
     */
    @Override
    public PageData getPageData() {
        return new PageData(getDisplayingPageId(), baseRows, columns(), sortType(), isSortReversed(), searching());
    }

    @Override
    public void sendEndInvData(){
        if(sourceInventory instanceof EndlessInventory endlessInventory){
            itemSize.set(endlessInventory.getItemSize());
            maxStackSize.set(endlessInventory.getMaxItemStackSize());
            infinityMode.set((endlessInventory.isInfinityMode()?1:0));
        }
    }

    @Override
    public String getDisplayingPageId() {
        return this.displayingPageId;
    }

    @Override
    public void switchPageWithId(String id) {
        PageType type = PageTypeRegistry.byId(id);
        if (type != null) {
            applySelectedPage(type);
        }
    }

    @Override
    public PageType getDisplayingPageType() {
        return this.displayingPageType;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        returnCraftingToPlayer();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }


    private int insertStackIntoPage(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        ItemStack attempt = stack.copy();
        ItemStack remainder = quickMoveIntoPage(attempt);
        int inserted = stack.getCount() - remainder.getCount();
        if (inserted > 0) {
            stack.shrink(inserted);
        }
        return inserted;
    }

    /**
     * Route crafted results into the player inventory first, falling back to the endless inventory page.
     */
    private boolean handleQuickMoveResult(Slot slot, ItemStack stack, ItemStack original) {
        boolean moved = this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)
                || this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false);
        if (!moved) {
            insertStackIntoPage(stack);
        }
        slot.onQuickCraft(stack, original);
        updateCraftingResult();
        return true;
    }

    /**
     * Determine the quick-move destination for items grabbed from the player inventory.
     */
    private boolean handleQuickMoveFromPlayerInventory(ItemStack stack) {
        if (craftingVisible) {
            return moveStackIntoCrafter(stack);
        }
        return insertStackIntoPage(stack) > 0;
    }

    private void updateCraftingResult() {
        if (!(this.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        var level = serverPlayer.level();
        ItemStack resultStack = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional = serverPlayer.server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, this.craftMatrix, level);
        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = optional.get();
            CraftingRecipe recipe = holder.value();
            if (this.craftResult.setRecipeUsed(level, serverPlayer, holder)) {
                ItemStack assembled = recipe.assemble(this.craftMatrix, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    resultStack = assembled;
                }
            }
        }
        this.craftResult.setItem(RESULT_SLOT_INDEX, resultStack);
        this.setRemoteSlot(RESULT_SLOT_INDEX, resultStack);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), RESULT_SLOT_INDEX, resultStack));
    }

    private class CraftingGridSlot extends Slot {
        CraftingGridSlot(CraftingContainer matrix, int slot, int x, int y) {
            super(matrix, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return craftingVisible;
        }
    }

    private class CraftingResultSlot extends ResultSlot {
        CraftingResultSlot(Player player, CraftingContainer matrix, ResultContainer result, int slotIndex, int x, int y) {
            super(player, matrix, result, slotIndex, x, y);
        }

        @Override
        public boolean isActive() {
            return craftingVisible;
        }
    }

    public interface ClientPageBinding {
        String pageId();

        PageType pageType();

        void onPageSelected();

        void scrollTo(float pos);

        ItemStack quickMoveIntoPage(ItemStack stack);

        void markPageChanged();

        ItemStack extractItemFromPage(ItemStack template, int maxCount);

        void refreshAfterMenuInteraction(SourceInventory source);
    }
}

