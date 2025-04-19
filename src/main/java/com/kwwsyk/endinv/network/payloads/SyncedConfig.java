package com.kwwsyk.endinv.network.payloads;

import com.kwwsyk.endinv.ModInitializer;
import com.kwwsyk.endinv.client.config.ClientConfig;
import com.kwwsyk.endinv.options.SortType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import static com.kwwsyk.endinv.ModInitializer.SYNCED_CONFIG;

/**Synced endless inventory config data across server player (attached) data and
 *  client ClientConfig of player.
 *  Used before open menu.
 * @param rows
 */
public record SyncedConfig(int rows, int pageId, SortType sortType,String search) implements CustomPacketPayload {

    public static final SyncedConfig DEFAULT = new SyncedConfig(15,0,SortType.DEFAULT,"");
    public static final Codec<SyncedConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.optionalFieldOf("rows", 15).forGetter(SyncedConfig::rows),
                    Codec.INT.optionalFieldOf("pageId",0).forGetter(SyncedConfig::pageId),
                    SortType.CODEC.optionalFieldOf("sortType",SortType.DEFAULT).forGetter(SyncedConfig::sortType),
                    Codec.STRING.optionalFieldOf("searching","").forGetter(SyncedConfig::search)
            ).apply(instance, SyncedConfig::new)
    );
    public static final Type<SyncedConfig> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInitializer.MOD_ID,"endinv_settings"));
    public static final StreamCodec<ByteBuf, SyncedConfig> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncedConfig::rows,
            ByteBufCodecs.INT, SyncedConfig::pageId,
            SortType.STREAM_CODEC,SyncedConfig::sortType,
            ByteBufCodecs.STRING_UTF8,SyncedConfig::search,
            SyncedConfig::new
    );
    public SyncedConfig ofRowChanged(int rows){
        return new SyncedConfig(rows,this.pageId,this.sortType,this.search);
    }
    public static void syncClientConfigToServer(){
        if(Minecraft.getInstance().player instanceof LocalPlayer player){
            int rows = ClientConfig.CONFIG.ROWS.getAsInt();
            if(rows==0){
                rows = ClientConfig.CONFIG.calculateDefaultRowCount();
            }
            SyncedConfig config = player.getData(SYNCED_CONFIG).ofRowChanged(rows);
            player.setData(SYNCED_CONFIG,config);
            PacketDistributor.sendToServer(config);
        }
    }
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}