package com.kwwsyk.endinv.menu.page;

import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.options.ItemClassify;
import net.minecraft.core.Holder;

public class ItemEntryDisplay extends ItemDisplay{

    public ItemEntryDisplay(PageMetaDataManager metaDataManager, Holder<ItemClassify> classify, int pageIndex){
        super(metaDataManager,classify,pageIndex);
    }
}
