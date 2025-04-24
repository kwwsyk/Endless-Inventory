package com.kwwsyk.endinv.network.payloads;

import com.kwwsyk.endinv.util.SortType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PageData(int rows, int columns, int pageId, SortType sortType, boolean reverseSort, String search) {
    
    public static final PageData DEFAULT = new PageData(0,9,0,SortType.DEFAULT,false,"");
    public static final Codec<PageData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.optionalFieldOf("rows", 15).forGetter(PageData::rows),
                    Codec.INT.optionalFieldOf("rows", 15).forGetter(PageData::columns),
                    Codec.INT.optionalFieldOf("pageId",0).forGetter(PageData::pageId),
                    SortType.CODEC.optionalFieldOf("sortType",SortType.DEFAULT).forGetter(PageData::sortType),
                    Codec.BOOL.optionalFieldOf("reverseSort",false).forGetter(PageData::reverseSort),
                    Codec.STRING.optionalFieldOf("searching","").forGetter(PageData::search)
            ).apply(instance, PageData::new)
    );

    public static final StreamCodec<ByteBuf, PageData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PageData::rows,
            ByteBufCodecs.INT, PageData::columns,
            ByteBufCodecs.INT, PageData::pageId,
            SortType.STREAM_CODEC,PageData::sortType,
            ByteBufCodecs.BOOL,PageData::reverseSort,
            ByteBufCodecs.STRING_UTF8,PageData::search,
            PageData::new
    );
    public PageData copy(){
        return new PageData(this.rows,this.columns,this.pageId,this.sortType,this.reverseSort,this.search);
    }
    public PageData ofRowChanged(int rows){
        return new PageData(rows,this.columns,this.pageId,this.sortType,this.reverseSort,this.search);
    }
    public PageData ofColumnChanged(int columns){
        return new PageData(this.rows,columns,this.pageId,this.sortType,this.reverseSort,this.search);
    }
    public PageData pageIdChanged(int id){
        return new PageData(this.rows,this.columns,id,this.sortType,this.reverseSort,this.search);
    }
    public PageData sortTypeChanged(SortType sortType){
        return new PageData(this.rows,this.columns,this.pageId,sortType,this.reverseSort,this.search);
    }
    public PageData searchingChanged(String searching){
        return new PageData(this.rows,this.columns,this.pageId,this.sortType,this.reverseSort,searching);
    }
    public PageData ofReverseSort(){
        return new PageData(this.rows,this.columns,this.pageId,this.sortType,!this.reverseSort,this.search);
    }

}
