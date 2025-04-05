package com.kwwsyk.endinv;


import com.mojang.logging.LogUtils;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.kwwsyk.endinv.ModInitializer.ENDINV_UUID;


public class EndlessInventory implements SourceInventory{

    public static final UUID DEFAULT_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final EndlessInventory EMPTY = new EndlessInventory(DEFAULT_UUID);

    private UUID uuid;
    private List<ItemStack> items;
    private ContainerListener[] listeners;

    public EndlessInventory(){
        this.items = new ArrayList<>();
        this.uuid= UUID.randomUUID();
    }

    public EndlessInventory(UUID uuid){
        this.items = new ArrayList<>();
        this.uuid = uuid;
    }

    public static EndlessInventory createEmpty(){
        return new EndlessInventory(null);
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

    public List<ItemStack> getItems(){
        return this.items;
    }

    public ItemStack getItemStack(int index){
        return this.items.get(index);
    }

    public void addItem(ItemStack itemStack){
        if(itemStack.isEmpty()) return;
        for(ItemStack itemStack1 : this.items){
            if(ItemStack.isSameItemSameComponents(itemStack1,itemStack)){
                itemStack1.grow(itemStack.getCount());
                this.setChanged();
                return;
            }

        }
        this.items.add(itemStack);
        this.setChanged();
    }

    public ItemStack takeItem(int index){
        if(index<0 || index>=this.items.size()){
            return ItemStack.EMPTY;
        }else {
            ItemStack itemStack = this.getItemStack(index);
            ItemStack taken;
            if(itemStack.getCount()> itemStack.getMaxStackSize()){
                taken = itemStack.split(itemStack.getMaxStackSize());

            }else {
                taken = this.items.remove(index);
            }
            this.setChanged();
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

            if(itemStack.getCount()> count){
                taken = itemStack.split(count);

            }else {
                taken = this.items.remove(index);
            }
            this.setChanged();
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
        for(ItemStack itemStack1 :this.items){
            if(ItemStack.isSameItemSameComponents(itemStack1,itemStack)){
                ItemStack taken;
                if(count>itemStack.getMaxStackSize()) count=itemStack.getMaxStackSize();
                if(itemStack1.getCount()> count){
                    taken = itemStack1.split(count);
                }else {
                    this.items.remove(itemStack1);
                    taken = itemStack1;
                }
                this.setChanged();
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

    public static boolean hasEndInvUuid(Player player){
        if(player.hasData(ENDINV_UUID)){
            if(player.getData(ENDINV_UUID)==DEFAULT_UUID){
                LOGGER.warn("Player {} has default endless inventory UUID.", player.getName().getString());
                return false;
            }
            return true;
        }else return false;
    }
    //TODO
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
                break;
            }
            case ID -> {

            }
            case LAST_MODIFIED -> {
                break;
            }
        }
        end:
        return;
    }


    /**
     *
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


    public ItemStack removeItem(int i, int i1) {
        return i>=0 && i<this.items.size() ? this.items.get(i).split(i1) : ItemStack.EMPTY;
    }


    public ItemStack removeItemNoUpdate(int i) {
        ItemStack itemstack = (ItemStack)this.items.get(i);
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.items.set(i, ItemStack.EMPTY);
            return itemstack;
        }
    }


    public void setItem(int i, ItemStack itemStack) {
        if(!itemStack.isEmpty()) {
            if(i<0) return;
            if(i<items.size()) {
                this.items.set(i, itemStack);
                this.setChanged();
            }else {
                this.addItem(itemStack);
            }
        }else if(!this.getItem(i).isEmpty()) this.removeItem(i);
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
