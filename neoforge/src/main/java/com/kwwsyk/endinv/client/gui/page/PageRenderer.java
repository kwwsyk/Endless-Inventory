package com.kwwsyk.endinv.client.gui.page;

import com.kwwsyk.endinv.client.gui.bg.ScreenBgRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface PageRenderer extends ScreenBgRenderer.PageRenderer {

    void renderPage(GuiGraphics graphics,int pageXPos,int pageYPos);

    void renderHovering(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    boolean hasSearchBar();

    boolean hasSortTypeSwitchBar();

    default void renderPageIcon(GuiGraphics graphics, int x, int y, float partialTick) {
        if(getIcon()==null) return;
        Optional<Item> optionalItem = BuiltInRegistries.ITEM.getOptional(getIcon());
        if (optionalItem.isPresent()) {
            ItemStack stack = new ItemStack(optionalItem.get());
            graphics.renderItem(stack,x,y);
            return;
        }
        try {
            graphics.blitSprite(getIcon(),x,y,getIconX(),getIconY());
        }catch (Exception ignored){}
    }

    default ResourceLocation getIcon(){
        return null;
    }

    default int getIconX(){return 18;}

    default int getIconY(){return 18;}
}
