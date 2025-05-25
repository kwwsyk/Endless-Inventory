package com.kwwsyk.endinv.menu.page;

import com.kwwsyk.endinv.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.options.ItemClassify;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;

public class PageType {

    private final PageConstructor constructor;
    public final Holder<ItemClassify> classify;
    public ResourceLocation icon = null;
    public final String registerName;

    @FunctionalInterface
    public interface PageConstructor {
        DisplayPage create(PageType pageType, PageMetaDataManager manager);
    }

    public PageType(PageConstructor constructor,@Nullable Holder<ItemClassify> itemClassify, String registerName){
        this.constructor = constructor;
        this.classify = itemClassify;
        this.registerName = registerName;
    }

    public PageType(PageConstructor constructor,@Nullable Holder<ItemClassify> itemClassify,@Nullable ResourceLocation icon, String registerName){
        this.constructor = constructor;
        this.classify = itemClassify;
        this.icon = icon;
        this.registerName = registerName;
    }

    public DisplayPage buildPage(PageMetaDataManager meta){
        var page =  constructor.create(this, meta);
        if(icon!=null) page.icon = icon;
        return page;
    }

    public String toString(){
        try {
            return Holder.direct(this).getRegisteredName();
        } catch (Exception e) {
            return super.toString();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PageType pageType
                && Objects.equals(pageType.constructor,constructor)
                && Objects.equals(pageType.classify,classify)
                && Objects.equals(pageType.registerName, registerName);
    }
}
