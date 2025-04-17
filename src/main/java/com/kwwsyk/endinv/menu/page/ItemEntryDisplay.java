package com.kwwsyk.endinv.menu.page;

import com.kwwsyk.endinv.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.options.ItemClassify;
import net.minecraft.core.Holder;

public class ItemEntryDisplay extends ItemDisplay{

    public ItemEntryDisplay(EndlessInventoryMenu menu, Holder<ItemClassify> classify,int pageIndex){
        super(menu,menu.getSourceInventory(),0, menu.getRowCount(),classify,pageIndex);
    }
}
