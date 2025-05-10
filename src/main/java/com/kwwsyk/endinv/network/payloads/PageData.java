package com.kwwsyk.endinv.network.payloads;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.menu.page.PageType;
import com.kwwsyk.endinv.util.SortType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**Both stored and synced data of player and page, obtained in specific Payloads.
 * See {@link com.kwwsyk.endinv.network.payloads.toServer.page.PageContext},{@link SyncedConfig}
 */
public record PageData(Holder<PageType> pageType, int rows, int columns, SortType sortType, boolean reverseSort, String search) {
    
    public static final PageData DEFAULT = new PageData(ModInitializer.ALL_ITEMS,0,9,SortType.DEFAULT,false,"");
    public static final Codec<PageData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    ModInitializer.PAGE_REGISTRY.holderByNameCodec().optionalFieldOf("page_type", ModInitializer.ALL_ITEMS).forGetter(PageData::pageType),
                    Codec.INT.optionalFieldOf("rows", 15).forGetter(PageData::rows),
                    Codec.INT.optionalFieldOf("rows", 15).forGetter(PageData::columns),
                    SortType.CODEC.optionalFieldOf("sortType",SortType.DEFAULT).forGetter(PageData::sortType),
                    Codec.BOOL.optionalFieldOf("reverseSort",false).forGetter(PageData::reverseSort),
                    Codec.STRING.optionalFieldOf("searching","").forGetter(PageData::search)
            ).apply(instance, PageData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PageData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(ModInitializer.PAGE_REG_KEY),PageData::pageType,
            ByteBufCodecs.INT, PageData::rows,
            ByteBufCodecs.INT, PageData::columns,
            SortType.STREAM_CODEC,PageData::sortType,
            ByteBufCodecs.BOOL,PageData::reverseSort,
            ByteBufCodecs.STRING_UTF8,PageData::search,
            PageData::new
    );
    public PageData copy(){
        return new PageData(this.pageType,this.rows,this.columns,this.sortType,this.reverseSort,this.search);
    }
    public PageData ofRowChanged(int rows){
        return new PageData(this.pageType,rows,this.columns,this.sortType,this.reverseSort,this.search);
    }
    public PageData ofColumnChanged(int columns){
        return new PageData(this.pageType,this.rows,columns,this.sortType,this.reverseSort,this.search);
    }
    public PageData sortTypeChanged(SortType sortType){
        return new PageData(this.pageType,this.rows,this.columns,sortType,this.reverseSort,this.search);
    }
    public PageData searchingChanged(String searching){
        return new PageData(this.pageType,this.rows,this.columns,this.sortType,this.reverseSort,searching);
    }
    public PageData ofReverseSort(){
        return new PageData(this.pageType,this.rows,this.columns,this.sortType,!this.reverseSort,this.search);
    }
    public PageData ofPageTypeChanged(PageType pageType) {
        return new PageData(Holder.direct(pageType),this.rows,this.columns,this.sortType,!this.reverseSort,this.search);
    }
}
