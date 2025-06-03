package com.kwwsyk.endinv.common.network.payloads;

import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.client.option.IClientConfig;
import com.kwwsyk.endinv.common.network.payloads.toClient.ToClientPacketContext;
import com.kwwsyk.endinv.common.network.payloads.toServer.ToServerPacketContext;
import com.kwwsyk.endinv.common.util.SortType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import static com.kwwsyk.endinv.common.ModInfo.getPacketDistributor;
import static com.kwwsyk.endinv.common.ModRegistries.NbtAttachments;
import static com.kwwsyk.endinv.common.client.ClientModInfo.getClientConfig;

/**Synced endless inventory config data across server player (attached) data and
 *  client ClientConfig of player.
 *  Used before open menu.
 *
 */
public record SyncedConfig(PageData pageData,boolean attaching,boolean autoPicking) implements ModPacketPayload {

    public static final SyncedConfig DEFAULT = new SyncedConfig(PageData.DEFAULT,true,true);
    public static final Codec<SyncedConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    PageData.CODEC.optionalFieldOf("page_data",PageData.DEFAULT).forGetter(SyncedConfig::pageData),
                    Codec.BOOL.optionalFieldOf("attaching",true).forGetter(SyncedConfig::attaching),
                    Codec.BOOL.optionalFieldOf("auto_pickup",true).forGetter(SyncedConfig::autoPicking)
            ).apply(instance, SyncedConfig::new)
    );

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
        IClientConfig clientConfig = getClientConfig();
        if(Minecraft.getInstance().player instanceof LocalPlayer player){
            int rows = clientConfig.rows().get();
            int syncedRows = config.pageData.rows();
            if(rows==0){
                rows = syncedRows;
            }
            int columns = clientConfig.columns().get();
            if(columns==0){
                columns = 9;
            }
            if(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && clientConfig.autoSuitColumn().get()){
                columns = Math.min(columns,clientConfig.calculateSuitInColumnCount(screen));
            }
            SyncedConfig config1 = new SyncedConfig(config.pageData.ofRowChanged(rows).ofColumnChanged(columns), config.attaching, config.autoPicking());
            NbtAttachments.getSyncedConfig().setTo(player,config1);
            getPacketDistributor().sendToServer(config1);
        }
    }
    public static SyncedConfig readClientConfig(boolean ofMenu){
        IClientConfig clientConfig = getClientConfig();
        if(Minecraft.getInstance().player instanceof LocalPlayer player){
            int rows = clientConfig.rows().get();
            if(rows==0){
                rows = clientConfig.calculateDefaultRowCount(ofMenu);
            }
            int columns = clientConfig.columns().get();
            if(columns==0){
                columns = 9;
            }
            if(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && clientConfig.autoSuitColumn().get()){
                columns = Math.min(columns,clientConfig.calculateSuitInColumnCount(screen));
            }
            SyncedConfig config = NbtAttachments.getSyncedConfig().getWith(player);
            if(config==null) config = DEFAULT;
            return new SyncedConfig(config.pageData.ofRowChanged(rows).ofColumnChanged(columns), clientConfig.attaching().get(), true);
        }else throw new IllegalStateException("Unable to read client config, as running on server or player is not existing.");
    }


    @Override
    public String id() {
        return "endinv_settings";
    }

    public void handleClient(ToClientPacketContext context){
        ModRegistries.NbtAttachments.getSyncedConfig().setTo(context.player(), this);
    }

    public void handleServer(ToServerPacketContext context){
        ModRegistries.NbtAttachments.getSyncedConfig().setTo(context.player(), this);
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

    public SyncedConfig pageKeyChanged(String regKey) {
        return new SyncedConfig(pageData.ofPageKeyChanged(regKey),attaching,autoPicking);
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