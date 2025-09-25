package com.kwwsyk.endinv.common.client.gui.page;

import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.network.payloads.toServer.ItemClickPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.kwwsyk.endinv.common.ModInfo.getPacketDistributor;

/**
 * Item display page that groups entries into segments defined by sub classifications.
 * Each segment is rendered on its own set of rows. Items that do not match any subclassify
 * predicate can optionally form an additional trailing segment.
 */
public class SegClassifyItemDisplay extends ItemDisplay {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<Predicate<ItemStack>> subClassifies;
    private final boolean includeRemainItems;
    private final boolean keepClassifiedItemInNextSeg;

    private final List<ItemStack> segmentedView = new ArrayList<>();
    /**
     * Slot indexes inside {@link #segmentedView} that represent the first column of each segment.
     * This list preserves insertion order so we can translate them into row numbers for the
     * currently displayed slice during {@link #updateDisplayedSlice()}.
     */
    private final List<Integer> segmentStartSlots = new ArrayList<>();
    /** Row indexes (relative to the currently rendered page) that should render a separator line. */
    private final List<Integer> pageSeparatorRows = new ArrayList<>();

    public SegClassifyItemDisplay(PageType pageType,
                                  PageMetaDataManager metaDataManager,
                                  List<Predicate<ItemStack>> subClassifies,
                                  boolean includeRemainItems,
                                  boolean keepClassifiedItemInNextSeg) {
        super(pageType, metaDataManager);
        this.subClassifies = subClassifies == null ? List.of() : List.copyOf(subClassifies);
        this.includeRemainItems = includeRemainItems;
        this.keepClassifiedItemInNextSeg = keepClassifiedItemInNextSeg;
    }

    @Override
    public void refreshItems() {
        if (!suppressRefresh) {
            requestContents();
        }
        reloadViewFromCache();
    }

    @Override
    public void renderPage(GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
        int rowIndex = 0;
        int columnIndex = 0;
        int columns = meta.columns();
        for (int i = 0; i < items.size(); ++i) {
            if (columnIndex == 0 && pageSeparatorRows.contains(rowIndex)) {
                int y = topPos + rowIndex * 18;
                guiGraphics.fill(leftPos, y, leftPos + columns * 18 - 2, y + 1, 0xFF5A5A5A);
            }
            ItemStack stack = items.get(i);
            if (stack.isEmpty() && !stack.is(Items.AIR)) {
                renderEmpty(guiGraphics, leftPos + columnIndex * 18, topPos + rowIndex * 18 + 1, stack);
            }
            guiGraphics.renderItem(stack, leftPos + columnIndex * 18, topPos + rowIndex * 18 + 1, columnIndex + rowIndex * 180);
            if (!isHiddenBySortBox(rowIndex, columnIndex)) {
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack,
                        leftPos + columnIndex * 18, topPos + rowIndex * 18 + 1, getDisplayAmount(stack));
            }
            columnIndex++;
            if (columnIndex >= columns) {
                columnIndex = 0;
                rowIndex++;
            }
        }
        guiGraphics.pose().popPose();
    }

    @Override
    public void pageClicked(double XOffset, double YOffset, int button, ClickType clickType) {
        int slot = getSlotForMouseOffset(XOffset, YOffset);
        if (slot < 0 || slot >= items.size()) {
            return;
        }
        int viewIndex = startIndex + slot;
        ItemStack clicked = viewIndex < segmentedView.size() ? segmentedView.get(viewIndex).copy() : ItemStack.EMPTY;
        if (clicked.isEmpty()) {
            return;
        }
        switch (clickType) {
            case PICKUP -> handlePickup(clicked, button);
            case QUICK_MOVE -> handleQuickMove(clicked);
            case SWAP -> handleSwap(clicked, button);
            case THROW -> handleThrow(clicked);
            case PICKUP_ALL -> handlePickupAll(clicked);
            case CLONE -> handleClone(clicked);
            default -> {
            }
        }
        LOGGER.info("EI:sending:ItemClickPayload: player={} clickType={} button={} stack={}",
                meta.getPlayer(), clickType, button, clicked);
        getPacketDistributor().sendToServer(new ItemClickPayload(
                clicked.getCount() > 64 ? clicked.copyWithCount(64) : clicked.copy(),
                button, clickType));
        refreshItems();
    }

    @Override
    public ItemStack takeItem(ItemStack itemStack, int count) {
        setChanged();
        ItemStack result = this.srcInv.takeItem(itemStack, count);
        reloadViewFromCache();
        return result;
    }

    @Override
    public ItemStack takeItem(int index, int count) {
        int viewIndex = startIndex + index;
        if (viewIndex < 0 || viewIndex >= segmentedView.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack itemStack = segmentedView.get(viewIndex);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        setChanged();
        ItemStack result = srcInv.takeItem(itemStack, count);
        reloadViewFromCache();
        return result;
    }

    @Override
    public ItemStack addItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        setChanged();
        ItemStack remain = srcInv.addItem(itemStack.copy());
        reloadViewFromCache();
        return remain;
    }

    private void reloadViewFromCache() {
        List<ItemStack> source = CachedSrcInv.INSTANCE.getSortedAndFilteredItemView(
                0,
                Integer.MAX_VALUE,
                meta.sortType(),
                meta.isSortReversed(),
                getClassify(),
                meta.searching());
        rebuildSegments(source);
    }

    private void rebuildSegments(List<ItemStack> source) {
        segmentedView.clear();
        segmentStartSlots.clear();
        pageSeparatorRows.clear();
        List<ItemStack> filtered = new ArrayList<>();
        for (ItemStack stack : source) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            filtered.add(stack.copy());
        }
        if (filtered.isEmpty()) {
            initializeContents(List.of());
            return;
        }

        boolean[] consumed = new boolean[filtered.size()];
        boolean[] matchedAtLeastOnce = new boolean[filtered.size()];
        for (Predicate<ItemStack> classifier : subClassifies) {
            if (classifier == null) {
                continue;
            }
            int start = segmentedView.size();
            List<ItemStack> segment = new ArrayList<>();
            for (int i = 0; i < filtered.size(); ++i) {
                if (!keepClassifiedItemInNextSeg && consumed[i]) {
                    continue;
                }
                ItemStack stack = filtered.get(i);
                if (classifier.test(stack)) {
                    segment.add(stack.copy());
                    matchedAtLeastOnce[i] = true;
                    if (!keepClassifiedItemInNextSeg) {
                        consumed[i] = true;
                    }
                }
            }
            if (!segment.isEmpty()) {
                segmentStartSlots.add(start);
                appendSegment(segmentedView, segment);
            }
        }

        if (includeRemainItems) {
            List<ItemStack> remain = new ArrayList<>();
            for (int i = 0; i < filtered.size(); ++i) {
                boolean matched = matchedAtLeastOnce[i];
                boolean consumedFlag = keepClassifiedItemInNextSeg ? matched : consumed[i];
                if (!consumedFlag) {
                    remain.add(filtered.get(i).copy());
                }
            }
            if (!remain.isEmpty()) {
                segmentStartSlots.add(segmentedView.size());
                appendSegment(segmentedView, remain);
            }
        }

        updateDisplayedSlice();
    }

    private void appendSegment(List<ItemStack> target, List<ItemStack> segment) {
        if (segment.isEmpty()) {
            return;
        }
        for (ItemStack stack : segment) {
            target.add(stack);
        }
        int columns = meta.columns();
        if (columns <= 0) {
            return;
        }
        int remainder = segment.size() % columns;
        if (remainder == 0) {
            return;
        }
        int filler = columns - remainder;
        for (int i = 0; i < filler; ++i) {
            target.add(ItemStack.EMPTY);
        }
    }

    private void updateDisplayedSlice() {
        int columns = Math.max(1, meta.columns());
        int fromIndex = Math.min(startIndex, segmentedView.size());
        int toIndex = Math.min(fromIndex + length, segmentedView.size());
        List<ItemStack> slice = segmentedView.subList(fromIndex, toIndex);
        initializeContents(slice);
        pageSeparatorRows.clear();
        if (!segmentStartSlots.isEmpty()) {
            int firstRow = fromIndex / columns;
            int rowEndExclusive = Mth.positiveCeilDiv(Math.max(toIndex, fromIndex), columns);
            for (Integer start : segmentStartSlots) {
                if (start == null) {
                    continue;
                }
                int row = start / columns;
                if (row <= firstRow) {
                    continue;
                }
                if (row < rowEndExclusive) {
                    int relative = row - firstRow;
                    if (!pageSeparatorRows.contains(relative)) {
                        pageSeparatorRows.add(relative);
                    }
                }
            }
            if (!pageSeparatorRows.isEmpty()) {
                pageSeparatorRows.sort(Integer::compareTo);
            }
        }
    }
}
