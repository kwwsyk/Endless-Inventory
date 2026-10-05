package com.kwwsyk.endinv.common.client.gui.page;

import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.client.gui.bg.FromResource;
import com.kwwsyk.endinv.common.client.gui.bg.SFBgRenderer;
import com.kwwsyk.endinv.common.client.gui.bg.Transparent;
import com.kwwsyk.endinv.common.client.gui.page.slotView.EntryPageViewContainer;
import com.kwwsyk.endinv.common.client.gui.page.slotView.PageViewContainer;
import com.kwwsyk.endinv.common.client.option.ClientConfigs;
import com.kwwsyk.endinv.common.client.option.TextureMode;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.util.ItemKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class ItemEntryDisplay extends ItemDisplay{

    public interface DescriptionProvider extends BiFunction<ItemEntryDisplay, ItemStack, Component> {

        static Component fromTooltip(ItemEntryDisplay page,ItemStack stack,boolean jmpName/*Jmp the first row tooltip*/){
            List<Component> tooltips = AbstractContainerScreen.getTooltipFromItem(page.mc, stack);
            return fromTooltip(page, tooltips, jmpName);
        }

        static Component fromTooltip(ItemEntryDisplay page,List<Component> tooltips,boolean jmpName/*Jmp the first row tooltip*/){
            int strX = 18;
            int entry_max_x = page.framework.columns() * 18;
            if(strX > entry_max_x) return Component.empty();
            var it = tooltips.iterator();
            if(jmpName && it.hasNext()){
                it.next();
            }
            Component tip1 = Component.empty();
            while (it.hasNext()){
                var tip = it.next();

                int strX1 = strX + page.mc.font.width(tip.getVisualOrderText());
                if( strX1 >= entry_max_x - 3 && strX1 <= entry_max_x){
                    if(it.hasNext()){
                        tip1 = Component.literal(tip1.getString() + tip.getString() + ".".repeat(3));
                        break;
                    }
                    tip1 = Component.literal(tip1.getString() + tip.getString());
                    break;
                }
                if( strX1 > entry_max_x) {
                    tip1 = Component.literal(tip1.getString() + ".".repeat(3));
                    break;
                }
                tip1 = Component.literal(tip1.getString() + tip.getString());
            }
            return tip1;
        }

        static Component fromEnch(ItemEntryDisplay page,ItemStack stack){
            ItemEnchantments itemEnchantments = stack.get(DataComponents.ENCHANTMENTS);
            if(itemEnchantments == null || itemEnchantments.isEmpty()) itemEnchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if(itemEnchantments == null || itemEnchantments.isEmpty()) return Component.empty();
            List<Component> tooltips = new ArrayList<>();
            itemEnchantments.addToTooltip(
                    Item.TooltipContext.of(page.mc.level),
                    tooltips::add,
                    TooltipFlag.NORMAL,
                    stack
            );
            return fromTooltip(page, tooltips, false);
        }
    }

    public static final DescriptionProvider TOOLTIP_PROVIDER = (page, stack) -> DescriptionProvider.fromTooltip(page, stack, false);
    public static final DescriptionProvider TOOLTIP_PROVIDER_NO_NAME_ROW = (page, stack) -> DescriptionProvider.fromTooltip(page, stack, true);


    @Nullable
    protected SFBgRenderer.PageBgRender renderer = null;
    protected final DescriptionProvider entryProvider;

    public ItemEntryDisplay(PageType pageType, ScreenFramework screenFramework, DescriptionProvider descriptionProvider) {
        super(pageType,screenFramework);
        this.length = framework.rows();
        this.entryProvider = descriptionProvider;
    }

    public ItemEntryDisplay(PageType pageType, ScreenFramework screenFramework) {
        super(pageType,screenFramework);
        this.length = framework.rows();
        this.entryProvider = TOOLTIP_PROVIDER;
    }

    protected PageViewContainer buildView(List<ItemKey> items){
        return new EntryPageViewContainer(
                this,
                items,
                framework.rows(),
                getPageLeft() + MARGIN_SIDE_WIDTH,
                getPageTop() + MARGIN_TOP_HEIGHT,
                entryProvider
        );
    }

    @Override
    public void scrollTo(float pos) {
        int startIndex = getRowIndexForScroll(pos);
        this.setVisibleRange(startIndex,this.length);
    }

    @Override
    protected void setVisibleRange(int startIndex, int length) {
        this.startIndex = startIndex;
        this.length = Math.min(length, framework.rows());
        release();
        requestRemoteContents();
    }

    @Override
    protected boolean clickedInOneSlot(double XOffset, double YOffset, double lastX, double lastY) {
        return (int)YOffset/18 == (int)lastY/18;
    }

    @Override
    public int getSlotByMouseOffset(double XOffset, double YOffset) {
        XOffset = XOffset - MARGIN_SIDE_WIDTH; YOffset = YOffset - MARGIN_TOP_HEIGHT;
        if(XOffset<0||YOffset<0||XOffset>18* framework.columns()||YOffset>18* framework.rows()) return -1;
        return (int)YOffset/18;
    }

    @Override
    public Rect2i getSlotArea(int slot) {
        final int slotHeight = 18;
        final int slotWidth = framework.columns() * 18;
        return new Rect2i(leftPos, topPos+slotHeight*slot, slotWidth, slotHeight);
    }

    @Override
    protected boolean isHiddenBySortBox(int rowIndex, int columnIndex) {
        return rowIndex<=2 && Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen && (
                screen instanceof EndlessInventoryScreen EIS && EIS.getFrameWork().sortTypeSwitchBox.isOpen()
                        || framework.sortTypeSwitchBox.isOpen()
        );
    }

    @Override
    public void renderBg(GuiGraphicsExtractor GuiGraphicsExtractor, float partialTicks, int mouseX, int mouseY) {
        if (this.renderer == null) renderer = bgRender(framework.SFBgRenderer);
        renderer.renderBg(GuiGraphicsExtractor, partialTicks, mouseX, mouseY);
    }

    protected SFBgRenderer.PageBgRender bgRender(SFBgRenderer sfBgRenderer){
        TextureMode mode = ClientConfigs.ATTACHED_MENU_CONFIG.TextureMode.get();
        if(sfBgRenderer instanceof FromResource fromResource){
            if(mode == TextureMode.DEDICATED_LOCATION) return fromResource.dedicatePageBgRender(FromResource.ITEM_ENTRY_DISPLAY_RESOURCE);
            return (GuiGraphicsExtractor, partialTicks, mouseX, mouseY) ->{// assert mode = from_resource
                sfBgRenderer.getDefaultPageBgRenderer().ifPresent(bgRenderer -> bgRenderer.renderBg(GuiGraphicsExtractor, partialTicks, mouseX, mouseY));
                int pageX = leftPos + MARGIN_SIDE_WIDTH;
                int startY = topPos + MARGIN_TOP_HEIGHT;
                for(int i = 0; i< framework.rows(); ++i){
                    GuiGraphicsExtractor.fill(pageX,startY,pageX+18* framework.columns()-2,startY+1,0xFF373737);
                    GuiGraphicsExtractor.fill(pageX,startY+1,pageX+18* framework.columns()-2,startY+17,0xFF8b8b8b);
                    GuiGraphicsExtractor.fill(pageX,startY+17,pageX+18* framework.columns()-2,startY+18,0xFFFFFFFF);
                    startY+=18;
                }
            };
        }
        return (GuiGraphicsExtractor, partialTicks, mouseX, mouseY) ->{//assert mode = transparent
            ((Transparent)framework.SFBgRenderer).new GridPageRenderer().renderBg(GuiGraphicsExtractor, partialTicks, mouseX, mouseY);
            int pageX = leftPos + MARGIN_SIDE_WIDTH;
            int startY = topPos + MARGIN_TOP_HEIGHT;
            for(int i = 0; i< framework.rows(); ++i){
                GuiGraphicsExtractor.fill(pageX,startY,pageX+18* framework.columns()-2,startY+1,0xFF373737);
                GuiGraphicsExtractor.fill(pageX,startY+1,pageX+18* framework.columns()-2,startY+17,0x37606037);
                GuiGraphicsExtractor.fill(pageX,startY+17,pageX+18* framework.columns()-2,startY+18,0xFFFFFFFF);
                startY+=18;
            }
        };
    }
}

