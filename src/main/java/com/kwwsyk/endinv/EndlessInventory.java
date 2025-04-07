package com.kwwsyk.endinv;


import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.kwwsyk.endinv.ModInitializer.DEFAULT_UUID;
import static com.kwwsyk.endinv.ModInitializer.ENDINV_UUID;


public class EndlessInventory implements SourceInventory{

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final EndlessInventory EMPTY = new EndlessInventory(DEFAULT_UUID);

    private UUID uuid;
    private List<ItemStack> items;
    private int maxStackSize;
    private boolean infinityMode;

    public EndlessInventory(){
        this(UUID.randomUUID());
    }

    public EndlessInventory(UUID uuid){
        this.items = new ArrayList<>();
        this.uuid = uuid;
        this.maxStackSize = ServerConfig.CONFIG.MAX_STACK_SIZE.getAsInt();
        this.infinityMode = ServerConfig.CONFIG.ENABLE_INFINITE.getAsBoolean();
    }


    //TODO dangerous
    private static EndlessInventory createForPlayer(Player player){
        EndlessInventory endlessInventory = new EndlessInventory();
        EndlessInventoryData.levelEndInvData.addEndInvToLevel(endlessInventory);
        player.setData(ENDINV_UUID,endlessInventory.uuid);
        return endlessInventory;
    }

    private static EndlessInventory createForPlayer(Player player,UUID uuid){
        EndlessInventory endlessInventory = new EndlessInventory(uuid);
        EndlessInventoryData.levelEndInvData.addEndInvToLevel(endlessInventory);
        return endlessInventory;
    }

    public int getMaxItemStackSize() {
        return maxStackSize;
    }

    public void setMaxItemStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    public boolean isInfinityMode() {
        return infinityMode;
    }

    public void setInfinityMode(boolean infinityMode) {
        this.infinityMode = infinityMode;
    }

    public boolean isFull(ItemStack itemStack){
        return itemStack.getCount()>=maxStackSize;
    }

    public boolean isInfinite(ItemStack itemStack){
        return isFull(itemStack) && infinityMode;
    }

    public List<ItemStack> getItems(){
        return this.items;
    }

    public ItemStack getItemStack(int index){
        return this.items.get(index);
    }

    /**
     * Add item to EndInv and return remain item copy or IS.Empty.
     * @param itemStack to add
     * @return Remain item copied, or Empty if all inserted.
     */
    public ItemStack addItem(ItemStack itemStack){
        if(itemStack.isEmpty()) return ItemStack.EMPTY;
        for(ItemStack itemStack1 : this.items){
            if(ItemStack.isSameItemSameComponents(itemStack1,itemStack)){
                if(!isFull(itemStack1)) {
                    if(itemStack1.getCount()+itemStack.getCount()<maxStackSize) {
                        //normally
                        itemStack1.grow(itemStack.getCount());
                        this.setChanged();
                        return ItemStack.EMPTY;
                    }else {
                        itemStack1.setCount(maxStackSize);
                        this.setChanged();
                        return itemStack.copyWithCount(itemStack1.getCount()+itemStack.getCount()-maxStackSize);
                    }
                }else if(infinityMode){
                    return ItemStack.EMPTY;
                }else {
                    return itemStack.copy();
                }
            }

        }
        int c = itemStack.getCount();
        itemStack.limitSize(maxStackSize);
        this.items.add(itemStack);
        this.setChanged();
        return itemStack.copyWithCount(Math.max(0,c-maxStackSize));
    }

    public ItemStack takeItem(int index){
        if(index<0 || index>=this.items.size()){
            return ItemStack.EMPTY;
        }else {
            ItemStack itemStack = this.getItemStack(index);
            ItemStack taken;
            if(!isInfinite(itemStack)) {
                if (itemStack.getCount() > itemStack.getMaxStackSize()) {
                    //IS#split ret a new IS with split count
                    taken = itemStack.split(itemStack.getMaxStackSize());

                } else {
                    taken = this.items.remove(index);
                }
                this.setChanged();
            }else {
                taken = itemStack.copyWithCount(itemStack.getMaxStackSize());
            }
            return taken;
        }
    }

    public ItemStack takeItem(int index,int count){
        if(index<0 || index>=this.items.size()){
            return ItemStack.EMPTY;
        }else {
            ItemStack itemStack = this.getItemStack(index);
            ItemStack taken;
            if(count>itemStack.getMaxStackSize()) count=itemStack.getMaxStackSize();
            if(!isInfinite(itemStack)) {
                if (itemStack.getCount() > count) {
                    taken = itemStack.split(count);
                } else {
                    taken = this.items.remove(index);
                }
                this.setChanged();
            }else {
                taken = itemStack.copyWithCount(count);
            }
            return taken;
        }
    }

    /**
     * Take away those itemStack having same id&component with given itemStack in EndInv
     *
     * @param itemStack given item, with id and component | 物品堆叠，id和组件的提供器
     * @param count the max count of items taken away | 拿走的最大数目
     * @return taken items | 被拿走的物品
     */
    public ItemStack takeItem(ItemStack itemStack, int count){
        for(ItemStack toTake :this.items){
            if(ItemStack.isSameItemSameComponents(toTake,itemStack)){
                ItemStack taken;
                if(count>itemStack.getMaxStackSize()) count=itemStack.getMaxStackSize();
                if(!isInfinite(toTake)) {
                    if (toTake.getCount() > count) {
                        taken = toTake.split(count);
                    } else {
                        this.items.remove(toTake);
                        taken = toTake;
                    }
                    this.setChanged();
                }else taken = toTake.copyWithCount(count);
                return taken;
            }

        }
        return  ItemStack.EMPTY;
    }

    /**
     * Take at most its max stack size count item
     * @param itemStack will take itemStack with same id and component
     * @return items taken
     */
    public ItemStack takeItem(ItemStack itemStack){
        return  takeItem(itemStack,itemStack.getMaxStackSize());
    }


    public List<ItemStack> removeItem(ItemLike item){
        List<ItemStack> ret = new ArrayList<>();
        this.items.removeIf((stack)->
        {
            if(stack.getItem()==item.asItem()){
                ret.add(stack);
                return true;
            }else{
                return false;
            }
        });
        this.setChanged();
        return ret;
    }

    public ItemStack removeItem(ItemStack itemStack){
        for(ItemStack itemStack1 : this.items){
            if(ItemStack.isSameItemSameComponents(itemStack1,itemStack)){
                this.items.remove(itemStack1);
                this.setChanged();
                return itemStack1;
            }
        }
       return  ItemStack.EMPTY;
    }

    public ItemStack removeItem(int index){
        ItemStack ret = this.items.remove(index);
        this.setChanged();
        return  ret;
    }

    public boolean isRemote(){
        return false;
    }

    public UUID getUuid(){
        return  this.uuid;
    }

    public UUID giveNewUuid(){
        uuid=UUID.randomUUID();
        return uuid;
    }

    /**
     * Get player's EndInv, if player has not or has invalid, create new.
     * @param player the player
     * @return Player's EndInv, original or created
     */
    public static EndlessInventory getEndInvForPlayer(Player player){
        EndlessInventory endlessInventory;
        if(hasEndInvUuid(player)){
            endlessInventory = getPlayerDefaultEndInv(player);
            if(endlessInventory==null) endlessInventory = createForPlayer(player,player.getData(ENDINV_UUID));
        }else{
            endlessInventory = createForPlayer(player);

        }
        return endlessInventory;
    }

    /**
     * Check whether player has a valid uuid, which is not {@link ModInitializer#DEFAULT_UUID}
     * @param player player to check endInv uuid
     * @return true if player has uuid and the uuid is valid.
     */
    public static boolean hasEndInvUuid(Player player){
        if(player.hasData(ENDINV_UUID)){
            if(player.getData(ENDINV_UUID)==DEFAULT_UUID){
                LOGGER.warn("Player {} has default endless inventory UUID.", player.getName().getString());
                return false;
            }
            return true;
        }else return false;
    }

    private static EndlessInventory getPlayerDefaultEndInv(Player player){
        return EndlessInventoryData.levelEndInvData.fromUUID(player.getData(ENDINV_UUID));
    }
    //TODO
    public void sortItems(SortType sortType){
        switch (sortType){
            case COUNT -> {
                this.items.sort(
                        Comparator.comparingInt(ItemStack::getCount)
                );
            }
            case ID -> {

            }
            case LAST_MODIFIED -> {

            }
        }
        end:
        return;
    }


    /**
     * @return Size of Endless Inventory.
     */
    public int getItemSize() {
        return this.items.size();
    }


    public boolean isEmpty() {
        for(ItemStack itemStack : this.items){
            if (itemStack!=ItemStack.EMPTY) return false;
        };
        return  true;
    }


    public ItemStack getItem(int i) {
        return i>=0 && i<this.items.size() ? this.items.get(i) : ItemStack.EMPTY;
    }


    public void setChanged() {
        EndlessInventoryData.levelEndInvData.setDirty();
    }


    public boolean stillValid(Player player) {
        //TODO ACCESSIBLE?
        return true;
    }


    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }


}
