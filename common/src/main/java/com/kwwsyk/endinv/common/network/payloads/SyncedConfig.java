package com.kwwsyk.endinv.common.network.payloads;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.config.ClientConfig;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.util.SortType;
import com.kwwsyk.endinv.neoforge.ModInitializer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**Synced endless inventory config data across server player (attached) data and
 *  client ClientConfig of player.
 *  Used before open menu.
 *
 */
public record SyncedConfig(PageData pageData,boolean attaching,boolean autoPicking) implements CustomPacketPayload {

    public static final SyncedConfig DEFAULT = new SyncedConfig(PageData.DEFAULT,true,true);
    public static final Codec<SyncedConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    PageData.CODEC.optionalFieldOf("page_data",PageData.DEFAULT).forGetter(SyncedConfig::pageData),
                    Codec.BOOL.optionalFieldOf("attaching",true).forGetter(SyncedConfig::attaching),
                    Codec.BOOL.optionalFieldOf("auto_pickup",true).forGetter(SyncedConfig::autoPicking)
            ).apply(instance, SyncedConfig::new)
    );
    public static final Type<SyncedConfig> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInfo.MOD_ID,"endinv_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncedConfig> STREAM_CODEC = StreamCodec.composite(
            PageData.STREAM_CODEC,SyncedConfig::pageData,
            ByteBufCodecs.BOOL,SyncedConfig::attaching,
            ByteBufCodecs.BOOL,SyncedConfig::autoPicking,
            SyncedConfig::new
    );

    /**
     * Used when player is not viewing EndInv.
     * e.g. player joined world or player opened menu screen with EndInv attaching allowed.
     */
    public static void readAndSyncClientConfigToServer(boolean ofMenu){
        if(Minecraft.getInstance().player instanceof LocalPlayer player){
            SyncedConfig config = readClientConfig(ofMenu);
            updateSyncedConfig(config);
        }
    }

    /**
     * Used when player changed page param in client page.
     * @param config new config
     */
    public static void updateSyncedConfig(SyncedConfig config){
        if(Minecraft.getInstance().player instanceof LocalPlayer player){
            int rows = ClientConfig.CONFIG.ROWS.getAsInt();
            int syncedRows = config.pageData.rows();
            if(rows==0){
                rows = syncedRows;
            }
            int columns = ClientConfig.CONFIG.COLUMNS.getAsInt();
            if(columns==0){
                columns = 9;
            }
            if(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
                    && ClientConfig.CONFIG.AUTO_SUIT_COLUMN.getAsBoolean()){
                columns = Math.min(columns,ClientConfig.CONFIG.calculateSuitInColumnCount(screen));
            }
            SyncedConfig config1 = new SyncedConfig(config.pageData.ofRowChanged(rows).ofColumnChanged(columns), config.attaching, config.autoPicking());
            player.setData(ModInitializer.SYNCED_CONFIG,config1);
            PacketDistributor.sendToServer(config1);
        }
    }
    public static SyncedConfig readClientConfig(boolean ofMenu){
        if(Minecraft.getInstance().player instanceof LocalPlayer player){
            int rows = ClientConfig.CONFIG.ROWS.getAsInt();
            if(rows==0){
                rows = ClientConfig.CONFIG.calculateDefaultRowCount(ofMenu);
            }
            int columns = ClientConfig.CONFIG.COLUMNS.getAsInt();
            if(columns==0){
                columns = 9;
            }
            if(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
                    && ClientConfig.CONFIG.AUTO_SUIT_COLUMN.getAsBoolean()){
                columns = Math.min(ClientConfig.CONFIG.calculateSuitInColumnCount(screen),columns);
            }
            SyncedConfig config = player.getData(ModInitializer.SYNCED_CONFIG);
            return new SyncedConfig(config.pageData.ofRowChanged(rows).ofColumnChanged(columns),ClientConfig.CONFIG.ATTACHING.getAsBoolean(),true);
        }else throw new IllegalStateException("Unable to read client config, as running on server or player is not existing.");
    }
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public SyncedConfig searchingChanged(String searching) {
        return new SyncedConfig(pageData.searchingChanged(searching),attaching,autoPicking);
    }

    public SyncedConfig sortTypeChanged(SortType type) {
        return new SyncedConfig(pageData.sortTypeChanged(type),attaching,autoPicking);
    }

    public SyncedConfig ofReverseSort() {
        return new SyncedConfig(pageData.ofReverseSort(),attaching,autoPicking);
    }

    public SyncedConfig ofRowChanged(int rows) {
        return new SyncedConfig(pageData.ofRowChanged(rows),attaching,autoPicking);
    }

    public SyncedConfig pageTypeChanged(PageType pageType) {
        return new SyncedConfig(pageData.ofPageTypeChanged(pageType),attaching,autoPicking);
    }

    /**
     * Though ClientConfig approves 0 valued row/col count for auto adjustment,
     *  SyncedConfig represents real row/col count involved in both client and server logics.
     * @return valid SyncedConfig state with positive row/col count.
     */
    public boolean checkState(){
        return pageData.rows()>0 && pageData.columns()>0;
    }

    public boolean checkForAttaching(){
        return checkState() && attaching;
    }
}