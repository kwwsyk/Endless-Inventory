package com.kwwsyk.endinv.common.network.payloads;

import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.network.payloads.toServer.ItemPageContext;
import com.kwwsyk.endinv.common.util.SortType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.Charset;
import java.util.Objects;

/**Both stored and synced data of player and page, obtained in specific Payloads.
 * See {@link ItemPageContext},{@link SyncedConfig}
 */
public record PageData(String pageRegKey, int rows, int columns, SortType sortType, boolean reverseSort, String search) {
    
    public static final PageData DEFAULT = new PageData(PageType.DEFAULT_KEY,0,9,SortType.DEFAULT,false,"");
    public static final Codec<PageData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.STRING.optionalFieldOf("pageId","all_items").forGetter(PageData::pageRegKey),
                    Codec.INT.optionalFieldOf("rows", 15).forGetter(PageData::rows),
                    Codec.INT.optionalFieldOf("rows", 15).forGetter(PageData::columns),
                    SortType.CODEC.optionalFieldOf("sortType",SortType.DEFAULT).forGetter(PageData::sortType),
                    Codec.BOOL.optionalFieldOf("reverseSort",false).forGetter(PageData::reverseSort),
                    Codec.STRING.optionalFieldOf("searching","").forGetter(PageData::search)
            ).apply(instance, PageData::new)
    );

    public static void encode(FriendlyByteBuf o,PageData data){
        o.writeCharSequence(data.pageRegKey, Charset.defaultCharset());
        o.writeInt(data.rows);
        o.writeInt(data.columns);
        o.writeEnum(data.sortType);
        o.writeBoolean(data.reverseSort);
        o.writeCharSequence(data.search, Charset.defaultCharset());
    }

    public static PageData decode(FriendlyByteBuf o){
        return new PageData(
                o.readUtf(),
                o.readInt(),
                o.readInt(),
                o.readEnum(SortType.class),
                o.readBoolean(),
                o.readUtf()
        );
    }

    public PageData copy(){
        return new PageData(this.pageRegKey,this.rows,this.columns,this.sortType,this.reverseSort,this.search);
    }
    public PageData ofRowChanged(int rows){
        return new PageData(this.pageRegKey,rows,this.columns,this.sortType,this.reverseSort,this.search);
    }
    public PageData ofColumnChanged(int columns){
        return new PageData(this.pageRegKey,this.rows,columns,this.sortType,this.reverseSort,this.search);
    }
    public PageData sortTypeChanged(SortType sortType){
        return new PageData(this.pageRegKey,this.rows,this.columns,sortType,this.reverseSort,this.search);
    }
    public PageData searchingChanged(String searching){
        return new PageData(this.pageRegKey,this.rows,this.columns,this.sortType,this.reverseSort,searching);
    }
    public PageData ofReverseSort(){
        return new PageData(this.pageRegKey,this.rows,this.columns,this.sortType,!this.reverseSort,this.search);
    }
    public PageData ofPageKeyChanged(String pageRegKey) {
        return new PageData(pageRegKey,this.rows,this.columns,this.sortType,!this.reverseSort,this.search);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PageData pageData
                && pageData.pageRegKey.equals(pageRegKey)
                && pageData.rows == rows && pageData.columns() ==columns
                && pageData.reverseSort == reverseSort && sortType ==pageData.sortType
                && Objects.equals(pageData.search,search);
    }
}
